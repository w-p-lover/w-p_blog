package com.ican.cache;

import com.ican.exception.ServiceException;
import com.ican.metrics.BlogMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MultiLevelCacheManagerTest {

    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, Object> values = mock(ValueOperations.class);
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final RTopic topic = mock(RTopic.class);
    private MultiLevelCacheManager manager;
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() throws Exception {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setMaxConcurrentLoads(1);
        doThrow(new AssertionError("Bloom must not affect cache availability"))
                .when(redisson).getBloomFilter(anyString());
        when(redisson.getTopic(anyString())).thenReturn(topic);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        manager = new MultiLevelCacheManager(redis, redisson, properties, new BlogMetrics(registry));
        manager.init();
    }

    @Test
    void bloomUnavailableDoesNotBlockInitializationL2HitOrColdRefill() {
        when(values.get("article:cached")).thenReturn("cached");
        assertEquals("cached", manager.get("article:cached", key -> fail("L2 hit loaded DB"), 30));
        assertEquals("loaded", manager.get("article:cold", key -> "loaded", 30));
        assertEquals("loaded", manager.get("article:cold", key -> fail("L1 hit loaded DB"), 30));

        verify(redisson, never()).getBloomFilter(anyString());
        verify(lock, times(1)).unlock();
    }

    @Test
    void saturatedRebuildRejectsOtherMissesButStillServesCachedContentAndRecovers() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> manager.get("article:1", key -> {
                loads.incrementAndGet();
                started.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("loader not released");
                    }
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
                return "first";
            }, 30));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertEquals(1, activeRebuilds());
            assertThrows(ServiceException.class, () -> manager.get("article:2", key -> {
                loads.incrementAndGet();
                return "unexpected";
            }, 30));
            when(values.get("article:cached")).thenReturn("cached");
            assertEquals("cached", manager.get("article:cached", key -> fail("cache hit loaded DB"), 30));
            assertEquals(1, loads.get());
            assertEquals(1, count("budget", "rejected"));
            assertEquals(1, activeRebuilds());
            release.countDown();
            assertEquals("first", first.get(5, TimeUnit.SECONDS));
            assertEquals(0, activeRebuilds());
            assertEquals("recovered", manager.get("article:2", key -> "recovered", 30));
            assertEquals("first", manager.get("article:1", key -> fail("L1 hit loaded DB"), 30));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void lockTimeoutNeverLoadsDatabaseAndMayReadTheWinnersFill() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(false);
        Function<String, String> loader = mock(Function.class);

        assertThrows(ServiceException.class, () -> manager.get("article:1", loader, 30));
        when(values.get("article:2")).thenReturn(null, "winner");
        assertEquals("winner", manager.get("article:2", loader, 30));
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        assertEquals("recovered", manager.get("article:3", key -> "recovered", 30));

        verifyNoInteractions(loader);
        verify(lock, times(1)).unlock();
        assertEquals(2, count("lock_wait", "timeout"));
        assertEquals(1, count("lock_wait", "acquired"));
        assertEquals(3, registry.get("blog.cache.duration").tag("stage", "lock_wait").timer().count());
        assertEquals(0, activeRebuilds());
    }

    @Test
    void interruptedWaitPreservesInterruptWithoutLoadingAndReleasesBudget() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenThrow(new InterruptedException());
        Function<String, String> loader = mock(Function.class);
        try {
            assertThrows(ServiceException.class, () -> manager.get("article:1", loader, 30));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, count("lock_wait", "interrupted"));
            assertEquals(0, count("lock_wait", "error"));
            assertEquals(0, activeRebuilds());
        } finally {
            Thread.interrupted();
        }
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        assertEquals("recovered", manager.get("article:1", key -> "recovered", 30));
        verifyNoInteractions(loader);
    }

    @Test
    void loaderFailureReleasesLockAndBudgetForRecovery() {
        assertThrows(IllegalStateException.class, () -> manager.get("article:1", key -> {
            throw new IllegalStateException("controlled DB failure");
        }, 30));
        assertEquals("recovered", manager.get("article:1", key -> "recovered", 30));
        verify(lock, times(2)).unlock();
        assertEquals(1, count("loader", "error"));
        assertEquals(1, count("loader", "value"));
        assertEquals(2, registry.get("blog.cache.duration").tag("stage", "loader").timer().count());
        assertEquals(0, activeRebuilds());
    }

    @Test
    void redisFailureAfterAdmissionDoesNotReachDatabaseAndDoesNotLeakBudget() {
        when(values.get("article:1")).thenReturn(null).thenThrow(new QueryTimeoutException("controlled timeout"));
        Function<String, String> loader = mock(Function.class);

        assertThrows(QueryTimeoutException.class, () -> manager.get("article:1", loader, 30));
        assertEquals("recovered", manager.get("article:2", key -> "recovered", 30));
        verifyNoInteractions(loader);
        assertEquals(1, count("l2", "error"));
        assertEquals(0, count("loader", "error"));
        assertEquals(0, activeRebuilds());
    }

    @Test
    void missingContentIsNegativelyCachedAndLostLockOwnershipIsNotUnlocked() {
        Function<String, String> loader = mock(Function.class);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        assertNull(manager.get("article:missing", loader, 30));
        assertNull(manager.get("article:missing", loader, 30));

        verify(loader, times(1)).apply("article:missing");
        verify(lock, never()).unlock();
        assertEquals(1, count("loader", "null"));
        assertEquals(1, count("l1", "negative"));
        assertEquals(2, count("request", "null"));
    }

    @Test
    void failedOrUnknownFillNeverInstallsL1AndRecoversWithoutLeakingBudget() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(null, 0L)
                .thenThrow(new QueryTimeoutException("unknown fill outcome"))
                .thenReturn(1L);
        AtomicInteger loads = new AtomicInteger();
        Function<String, String> loader = key -> "loaded-" + loads.incrementAndGet();

        assertThrows(ServiceException.class, () -> manager.get("article:1", loader, 30));
        assertThrows(ServiceException.class, () -> manager.get("article:1", loader, 30));
        assertThrows(QueryTimeoutException.class, () -> manager.get("article:1", loader, 30));
        assertEquals(0, activeRebuilds());
        assertEquals("loaded-4", manager.get("article:1", loader, 30));
        assertEquals("loaded-4", manager.get("article:1", key -> fail("L1 miss"), 30));
        assertEquals(4, loads.get());
        assertEquals(1, count("fill", "unknown"));
        assertEquals(1, count("fill", "stale"));
        assertEquals(1, count("fill", "error"));
        assertEquals(1, count("fill", "accepted"));
        assertEquals(3, count("request", "error"));
        assertEquals(2, count("request", "value"));
        assertEquals(5, registry.get("blog.cache.duration").tag("stage", "request").timer().count());
        verify(lock, times(4)).unlock();
        registry.getMeters().stream().filter(m -> m.getId().getName().startsWith("blog.cache."))
                .forEach(m -> m.getId().getTags().forEach(tag ->
                        assertTrue(tag.getKey().equals("stage") || tag.getKey().equals("outcome"))));
    }

    @Test
    void lockDependencyFailureDoesNotFallBackToLoaderAndReleasesBudget() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new IllegalStateException("controlled lock failure")).thenReturn(true);
        Function<String, String> loader = mock(Function.class);
        assertThrows(IllegalStateException.class, () -> manager.get("article:1", loader, 30));
        verifyNoInteractions(loader);
        assertEquals(1, count("lock_wait", "error"));
        assertEquals(0, activeRebuilds());
        assertEquals("recovered", manager.get("article:1", key -> "recovered", 30));
    }

    @Test
    void evictAndPublishFailuresAreDistinctAndRetryCanPublishAgain() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new QueryTimeoutException("controlled eviction failure"))
                .thenReturn(null, 0L, 1L);
        when(topic.publish("article:1"))
                .thenThrow(new IllegalStateException("controlled publication failure")).thenReturn(0L);

        assertThrows(QueryTimeoutException.class, () -> manager.evict("article:1"));
        assertThrows(ServiceException.class, () -> manager.evict("article:1"));
        verify(topic, never()).publish(anyString());
        assertThrows(IllegalStateException.class, () -> manager.evict("article:1"));
        manager.evict("article:1");
        assertEquals(1, count("evict", "error"));
        assertEquals(1, count("evict", "unknown"));
        assertEquals(2, count("evict", "applied"));
        assertEquals(1, count("publish", "error"));
        // 返回0个订阅者也只表示发布调用完成，不冒充其他实例已经接收。
        assertEquals(1, count("publish", "sent"));
    }

    private double count(String stage, String outcome) {
        var counter = registry.find("blog.cache.events").tags("stage", stage, "outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    private double activeRebuilds() {
        return registry.get("blog.cache.rebuild.active").gauge().value();
    }
}
