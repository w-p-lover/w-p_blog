package com.ican.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ican.exception.ServiceException;
import com.ican.metrics.BlogMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 多级缓存管理器
 * L1: Caffeine 本地缓存（进程内，速度快）
 * L2: Redis 分布式缓存（跨节点共享）
 *
 * 缓存保护：
 * 1. 重复空值装载：空值缓存
 * 2. 并发回源：Redisson 分布式锁 + 本实例重建预算
 * 3. 集中过期：随机过期时间
 *
 * @author ican
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiLevelCacheManager {

    private static final String NULL_VALUE = "__NULL__";
    private static final String EVICT_TOPIC = "cache:multilevel:evict";
    private static final String GENERATION_PREFIX = "cache:generation:";
    private static final String NO_GENERATION = "__NO_GENERATION__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final MultiLevelCacheProperties cacheProperties;
    private final BlogMetrics metrics;

    private RTopic evictTopic;
    private Semaphore loadPermits;
    private final Object localMonitor = new Object();
    private long localEpoch;
    private final DefaultRedisScript<Long> fillScript = new DefaultRedisScript<>();
    private final DefaultRedisScript<Long> evictScript = new DefaultRedisScript<>();

    /**
     * L1 本地缓存：Caffeine
     * 容量：1000 条
     * 过期：写入后 5 分钟
     */
    private Cache<String, Object> localCache;

    @PostConstruct
    public void init() {
        fillScript.setLocation(new ClassPathResource("lua/cache_fill_if_current.lua"));
        fillScript.setResultType(Long.class);
        evictScript.setLocation(new ClassPathResource("lua/cache_advance_and_evict.lua"));
        evictScript.setResultType(Long.class);
        if (cacheProperties.getMaxConcurrentLoads() <= 0) {
            throw new IllegalArgumentException("缓存重建并发上限必须大于0");
        }
        loadPermits = new Semaphore(cacheProperties.getMaxConcurrentLoads());
        metrics.bindCacheLoadBudget(loadPermits, cacheProperties.getMaxConcurrentLoads());
        // 初始化 Caffeine 本地缓存
        localCache = Caffeine.newBuilder()
                .maximumSize(cacheProperties.getLocal().getMaxSize())
                .expireAfterWrite(cacheProperties.getLocal().getExpireMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();

        evictTopic = dependency("subscribe", () -> {
            RTopic topic = redissonClient.getTopic(EVICT_TOPIC);
            topic.addListener(String.class, (channel, key) -> {
                invalidateLocal(key);
                metrics.recordCacheEvent("notification", "received");
                log.debug("收到远程缓存失效通知: {}", key);
            });
            return topic;
        });
    }

    /**
     * 获取缓存数据（多级缓存 + 有界回源 + 分布式锁）
     *
     * @param key           缓存键
     * @param dbLoader      数据库加载函数
     * @param ttlMinutes    Redis 缓存过期时间（分钟）
     * @param <T>           返回值类型
     * @return 缓存数据
     */
    public <T> T get(String key, Function<String, T> dbLoader, int ttlMinutes) {
        long start = System.nanoTime();
        try {
            T value = getCached(key, dbLoader, ttlMinutes);
            metrics.recordCacheEvent("request", value == null ? "null" : "value");
            return value;
        } catch (RuntimeException e) {
            metrics.recordCacheEvent("request", "error");
            throw e;
        } finally {
            metrics.recordCacheDuration("request", System.nanoTime() - start);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, Function<String, T> dbLoader, int ttlMinutes) {
        // 1. 查询 L1 本地缓存
        Object cachedValue;
        long observedEpoch;
        synchronized (localMonitor) {
            cachedValue = localCache.getIfPresent(key);
            observedEpoch = localEpoch;
        }
        if (cachedValue != null) {
            metrics.recordCacheEvent("l1", NULL_VALUE.equals(cachedValue) ? "negative" : "hit");
            log.debug("L1 缓存命中: {}", key);
            return NULL_VALUE.equals(cachedValue) ? null : (T) cachedValue;
        }

        metrics.recordCacheEvent("l1", "miss");
        // 2. 查询 L2 Redis 缓存
        Object redisValue = readL2(key);
        if (redisValue != null) {
            log.debug("L2 缓存命中: {}", key);
            cacheLocally(key, redisValue, observedEpoch);
            return NULL_VALUE.equals(redisValue) ? null : (T) redisValue;
        }

        // 3. 使用 Redisson 分布式锁防止缓存击穿
        // 预算同时约束本实例等待锁与执行回源的请求；命中 L1/L2 不占用。
        if (!loadPermits.tryAcquire()) {
            metrics.recordCacheEvent("budget", "rejected");
            throw new ServiceException("内容加载繁忙，请稍后重试");
        }

        try {
            String lockKey = "lock:" + key;
            var lock = dependency("lock_create", () -> redissonClient.getLock(lockKey));
            // 等待时间和固定租期保持原配置；超时不直接回源。
            if (acquireLock(lock)) {
                try {
                    // 双重检查：获取锁后再次查询 Redis
                    redisValue = readL2(key);
                    if (redisValue != null) {
                        cacheLocally(key, redisValue, observedEpoch);
                        return NULL_VALUE.equals(redisValue) ? null : (T) redisValue;
                    }

                    // 4. 从数据库加载数据
                    Object generation = dependency("generation_read",
                            () -> redisTemplate.opsForValue().get(GENERATION_PREFIX + key));
                    Object expectedGeneration = generation == null ? NO_GENERATION : generation;
                    T dbValue = load(key, dbLoader);
                    Object cacheValue = dbValue == null ? NULL_VALUE : dbValue;
                    int cacheTtl = dbValue == null ? cacheProperties.getNullCacheTtlMinutes()
                            : ttlMinutes + (int) (Math.random() * cacheProperties.getTtlJitterMaxMinutes());
                    // 失效代比较和回填必须原子执行，不能用先GET再SET替代。
                    Long filled = dependency("fill", () -> redisTemplate.execute(fillScript,
                            List.of(key, GENERATION_PREFIX + key),
                            expectedGeneration, cacheValue, TimeUnit.MINUTES.toSeconds(cacheTtl), NO_GENERATION));
                    if (!Long.valueOf(1).equals(filled)) {
                        metrics.recordCacheEvent("fill", filled == null ? "unknown" : "stale");
                        throw new ServiceException(filled == null ? "缓存回填结果未知，请重新读取" : "内容已更新，请重新读取");
                    }
                    metrics.recordCacheEvent("fill", "accepted");
                    cacheLocally(key, cacheValue, observedEpoch);

                    log.debug("数据库加载并缓存: {}", key);
                    return dbValue;
                } finally {
                    dependency("lock_release", () -> {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                        return null;
                    });
                }
            } else {
                // 持锁者可能刚完成回填；最后检查一次，但不绕过锁直接查库。
                redisValue = readL2(key);
                if (redisValue != null) {
                    cacheLocally(key, redisValue, observedEpoch);
                    return NULL_VALUE.equals(redisValue) ? null : (T) redisValue;
                }
                throw new ServiceException("内容加载繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            metrics.recordCacheEvent("lock_wait", "interrupted");
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: {}", key, e);
            throw new ServiceException("内容加载已中断，请稍后重试");
        } finally {
            loadPermits.release();
        }
    }

    private Object readL2(String key) {
        Object value = dependency("l2", () -> redisTemplate.opsForValue().get(key));
        // 按实际读取次数统计，包含锁内二查和锁超时后的最后检查。
        metrics.recordCacheEvent("l2", value == null ? "miss" : NULL_VALUE.equals(value) ? "negative" : "hit");
        return value;
    }

    private boolean acquireLock(RLock lock) throws InterruptedException {
        long start = System.nanoTime();
        try {
            boolean acquired = lock.tryLock(cacheProperties.getLock().getWaitSeconds(),
                    cacheProperties.getLock().getLeaseSeconds(), TimeUnit.SECONDS);
            metrics.recordCacheEvent("lock_wait", acquired ? "acquired" : "timeout");
            return acquired;
        } catch (RuntimeException e) {
            metrics.recordCacheEvent("lock_wait", "error");
            throw e;
        } finally {
            metrics.recordCacheDuration("lock_wait", System.nanoTime() - start);
        }
    }

    private <T> T load(String key, Function<String, T> dbLoader) {
        long start = System.nanoTime();
        try {
            T value = dbLoader.apply(key);
            metrics.recordCacheEvent("loader", value == null ? "null" : "value");
            return value;
        } catch (RuntimeException e) {
            // 包括业务版本变化的主动拒绝，不等同于数据库基础设施故障。
            metrics.recordCacheEvent("loader", "error");
            throw e;
        } finally {
            metrics.recordCacheDuration("loader", System.nanoTime() - start);
        }
    }

    private <T> T dependency(String stage, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            metrics.recordCacheEvent(stage, "error");
            throw e;
        }
    }

    /**
     * 删除缓存（同时删除 L1 和 L2）
     */
    public void evict(String key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                if (synchronization instanceof PendingEvictions pending && pending.owner == this) {
                    pending.keys.add(key);
                    return;
                }
            }
            PendingEvictions pending = new PendingEvictions();
            pending.keys.add(key);
            TransactionSynchronizationManager.registerSynchronization(pending);
            return;
        }
        evictNow(key);
    }

    private void evictNow(String key) {
        invalidateLocal(key);
        // 标记与删除必须同一原子操作；保持失效代不会因正文过期被重置。
        try {
            Long deleted = dependency("evict", () -> redisTemplate.execute(evictScript,
                    List.of(key, GENERATION_PREFIX + key), UUID.randomUUID().toString()));
            if (deleted == null) {
                metrics.recordCacheEvent("evict", "unknown");
                throw new ServiceException("缓存失效结果未知");
            }
            metrics.recordCacheEvent("evict", "applied");
        } finally {
            // 防止Redis操作期间开始的L2读取在本实例重新留下旧值。
            invalidateLocal(key);
        }
        if (evictTopic != null) {
            dependency("publish", () -> evictTopic.publish(key));
            metrics.recordCacheEvent("publish", "sent");
        }
        log.debug("缓存已删除: {}", key);
    }

    private class PendingEvictions implements TransactionSynchronization {
        private final MultiLevelCacheManager owner = MultiLevelCacheManager.this;
        private final Set<String> keys = new LinkedHashSet<>();

        @Override
        public void afterCommit() {
            boolean failed = false;
            for (String key : keys) {
                try {
                    evictNow(key);
                } catch (RuntimeException e) {
                    failed = true;
                    log.error("数据库已提交但缓存失效失败: key={}", key, e);
                }
            }
            if (failed) {
                throw new ServiceException("内容已提交，但缓存同步失败，请稍后重新读取");
            }
        }
    }

    private void invalidateLocal(String key) {
        synchronized (localMonitor) {
            localEpoch++;
            localCache.invalidate(key);
        }
    }

    private void cacheLocally(String key, Object value, long observedEpoch) {
        synchronized (localMonitor) {
            if (localEpoch != observedEpoch) {
                metrics.recordCacheEvent("local_fill", "stale");
                throw new ServiceException("内容已更新，请重新读取");
            }
            localCache.put(key, value);
        }
    }

    /**
     * 仅清空当前实例L1，不删除共享Redis，不广播清空。
     */
    public void clear() {
        synchronized (localMonitor) {
            localEpoch++;
            localCache.invalidateAll();
        }
        log.info("L1 缓存已清空");
    }

    /**
     * 获取 L1 缓存统计信息
     */
    public String getStats() {
        var stats = localCache.stats();
        return String.format("L1 缓存统计 - 命中率: %.2f%%, 命中次数: %d, 未命中次数: %d, 驱逐次数: %d",
                stats.hitRate() * 100,
                stats.hitCount(),
                stats.missCount(),
                stats.evictionCount());
    }
}
