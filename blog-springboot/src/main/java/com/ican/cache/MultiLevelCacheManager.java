package com.ican.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多级缓存管理器
 * L1: Caffeine 本地缓存（进程内，速度快）
 * L2: Redis 分布式缓存（跨节点共享）
 *
 * 解决缓存三大问题：
 * 1. 缓存穿透：布隆过滤器 + 空值缓存
 * 2. 缓存击穿：Redisson 分布式锁
 * 3. 缓存雪崩：随机过期时间
 *
 * @author ican
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiLevelCacheManager {

    private static final String NULL_VALUE = "__NULL__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final MultiLevelCacheProperties cacheProperties;

    /**
     * L1 本地缓存：Caffeine
     * 容量：1000 条
     * 过期：写入后 5 分钟
     */
    private Cache<String, Object> localCache;

    /**
     * 布隆过滤器：防止缓存穿透
     * 预期元素数量：100万
     * 误判率：1%
     */
    private RBloomFilter<String> bloomFilter;

    @PostConstruct
    public void init() {
        // 初始化 Caffeine 本地缓存
        localCache = Caffeine.newBuilder()
                .maximumSize(cacheProperties.getLocal().getMaxSize())
                .expireAfterWrite(cacheProperties.getLocal().getExpireMinutes(), TimeUnit.MINUTES)
                .recordStats()
                .build();

        // 初始化布隆过滤器
        bloomFilter = redissonClient.getBloomFilter("cache:bloom-filter");
        if (!bloomFilter.isExists()) {
            // 预期元素数量：100万，误判率：1%
            bloomFilter.tryInit(
                    cacheProperties.getBloom().getExpectedInsertions(),
                    cacheProperties.getBloom().getFalseProbability()
            );
            log.info("布隆过滤器初始化完成");
        }
    }

    /**
     * 获取缓存数据（多级缓存 + 布隆过滤器 + 分布式锁）
     *
     * @param key           缓存键
     * @param dbLoader      数据库加载函数
     * @param ttlMinutes    Redis 缓存过期时间（分钟）
     * @param <T>           返回值类型
     * @return 缓存数据
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Function<String, T> dbLoader, int ttlMinutes) {
        // 1. 查询 L1 本地缓存
        Object cachedValue = localCache.getIfPresent(key);
        if (cachedValue != null) {
            log.debug("L1 缓存命中: {}", key);
            return NULL_VALUE.equals(cachedValue) ? null : (T) cachedValue;
        }

        // 2. 布隆过滤器仅作为辅助判断，避免误判导致有效数据被拦截
        boolean maybeExists = bloomFilter.contains(key);
        if (!maybeExists) {
            log.debug("布隆过滤器未命中，继续执行 Redis/DB 回源: {}", key);
        }

        // 3. 查询 L2 Redis 缓存
        Object redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            log.debug("L2 缓存命中: {}", key);
            localCache.put(key, redisValue);
            return NULL_VALUE.equals(redisValue) ? null : (T) redisValue;
        }

        // 4. 使用 Redisson 分布式锁防止缓存击穿
        String lockKey = "lock:" + key;
        var lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁（等待时间 3s，锁自动释放时间 10s）
            if (lock.tryLock(
                    cacheProperties.getLock().getWaitSeconds(),
                    cacheProperties.getLock().getLeaseSeconds(),
                    TimeUnit.SECONDS
            )) {
                try {
                    // 双重检查：获取锁后再次查询 Redis
                    redisValue = redisTemplate.opsForValue().get(key);
                    if (redisValue != null) {
                        localCache.put(key, redisValue);
                        return NULL_VALUE.equals(redisValue) ? null : (T) redisValue;
                    }

                    // 5. 从数据库加载数据
                    T dbValue = dbLoader.apply(key);
                    if (dbValue == null) {
                        // 缓存空值，防止缓存穿透（短过期时间 5 分钟）
                        redisTemplate.opsForValue().set(
                                key,
                                NULL_VALUE,
                                cacheProperties.getNullCacheTtlMinutes(),
                                TimeUnit.MINUTES
                        );
                        localCache.put(key, NULL_VALUE);
                        log.debug("缓存空值: {}", key);
                        return null;
                    }

                    // 6. 写入 L2 Redis（随机 TTL 防止缓存雪崩）
                    int randomTtl = ttlMinutes + (int) (Math.random() * cacheProperties.getTtlJitterMaxMinutes());
                    redisTemplate.opsForValue().set(key, dbValue, randomTtl, TimeUnit.MINUTES);

                    // 7. 写入 L1 本地缓存
                    localCache.put(key, dbValue);

                    // 8. 加入布隆过滤器
                    bloomFilter.add(key);

                    log.debug("数据库加载并缓存: {}", key);
                    return dbValue;
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，降级查询数据库（避免阻塞）
                log.warn("获取分布式锁失败，降级查询数据库: {}", key);
                return dbLoader.apply(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: {}", key, e);
            return dbLoader.apply(key);
        }
    }

    /**
     * 删除缓存（同时删除 L1 和 L2）
     */
    public void evict(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
        log.debug("缓存已删除: {}", key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        localCache.invalidateAll();
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
