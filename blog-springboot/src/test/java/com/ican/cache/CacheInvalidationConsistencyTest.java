package com.ican.cache;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ican.entity.Article;
import com.ican.entity.Category;
import com.ican.exception.ServiceException;
import com.ican.mapper.*;
import com.ican.metrics.BlogMetrics;
import com.ican.model.dto.ArticleDTO;
import com.ican.model.vo.ArticleVO;
import com.ican.model.vo.ArticleDetailStateVO;
import com.ican.model.vo.CategoryOptionVO;
import com.ican.model.vo.TagOptionVO;
import com.ican.service.*;
import com.ican.service.impl.ArticleServiceImpl;
import com.ican.service.impl.ArticleViewService;
import com.ican.strategy.context.SearchStrategyContext;
import com.ican.strategy.context.UploadStrategyContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CacheInvalidationConsistencyTest {

    private final Map<String, Object> state = new ConcurrentHashMap<>();
    private final Object redisMonitor = new Object();
    private final AtomicBoolean failEviction = new AtomicBoolean();
    private final AtomicBoolean failPublish = new AtomicBoolean();
    private final AtomicReference<Runnable> duringEviction = new AtomicReference<>();
    private final AtomicReference<String> committed = new AtomicReference<>("old");
    private final AtomicReference<String> nextCommit = new AtomicReference<>("new");
    private final AtomicReference<MessageListener<String>> listener = new AtomicReference<>();
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, Object> values = mock(ValueOperations.class);
    private final RTopic topic = mock(RTopic.class);
    private MultiLevelCacheManager manager;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() throws Exception {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> state.get(call.getArgument(0)));
        // 模拟服务端原子操作；实际Lua源码另执行，不把此替身当Redis集成证据。
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenAnswer(call -> {
            RedisScript<Long> script = call.getArgument(0);
            List<String> keys = call.getArgument(1);
            synchronized (redisMonitor) {
                if (script.getScriptAsString().contains("local generation")) {
                    Object expected = call.getArgument(2);
                    Object absent = call.getArgument(5);
                    if (!state.getOrDefault(keys.get(1), absent).equals(expected)) {
                        return 0L;
                    }
                    state.put(keys.get(0), call.getArgument(3));
                    return 1L;
                }
                if (failEviction.getAndSet(false)) {
                    throw new QueryTimeoutException("controlled eviction failure");
                }
                Runnable overlappingRead = duringEviction.getAndSet(null);
                if (overlappingRead != null) {
                    overlappingRead.run();
                }
                state.put(keys.get(1), call.getArgument(2));
                return state.remove(keys.get(0)) == null ? 0L : 1L;
            }
        });
        when(topic.addListener(eq(String.class), any(MessageListener.class))).thenAnswer(call -> {
            listener.set(call.getArgument(1));
            return 1;
        });
        doAnswer(call -> {
            if (failPublish.get()) {
                throw new IllegalStateException("controlled publish failure");
            }
            // 不自动投递广播，以验证共享Redis保护不依赖消费者收到消息。
            return 0L;
        }).when(topic).publish(any());
        manager = newManager();
        transaction = new TransactionTemplate(new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object tx, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                committed.set(nextCommit.get());
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        });
    }

    private MultiLevelCacheManager newManager() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.getTopic(anyString())).thenReturn(topic);
        RLock lock = mock(RLock.class);
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        MultiLevelCacheManager result = new MultiLevelCacheManager(redis, redisson, new MultiLevelCacheProperties(), mock(BlogMetrics.class));
        result.init();
        return result;
    }

    @Test
    void transactionCommitInvalidatesAfterTheDatabaseChangesAndDeduplicatesKeys() {
        assertEquals("old", manager.get("article:1", key -> committed.get(), 30));
        transaction.executeWithoutResult(status -> {
            manager.evict("article:1");
            manager.evict("article:1");
            assertEquals("old", state.get("article:1"));
            assertEquals("old", committed.get());
            verify(topic, never()).publish(any());
        });

        assertEquals("new", committed.get());
        assertFalse(state.containsKey("article:1"));
        assertEquals("new", manager.get("article:1", key -> committed.get(), 30));
        verify(topic, times(1)).publish("article:1");
    }

    @Test
    void rollbackKeepsExistingCacheAndDoesNotAdvanceGeneration() {
        assertEquals("old", manager.get("article:1", key -> committed.get(), 30));
        transaction.executeWithoutResult(status -> {
            manager.evict("article:1");
            status.setRollbackOnly();
        });

        assertEquals("old", committed.get());
        assertFalse(state.containsKey("cache:generation:article:1"));
        assertEquals("old", manager.get("article:1", key -> fail("rollback evicted cache"), 30));
        verify(topic, never()).publish(any());
    }

    @Test
    void oldLoaderCannotRefillAfterAnotherManagerInvalidatesEvenWithoutBroadcast() throws Exception {
        MultiLevelCacheManager other = newManager();
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var old = executor.submit(() -> manager.get("article:1", key -> {
                String snapshot = committed.get();
                loaded.countDown();
                await(release);
                return snapshot;
            }, 30));
            assertTrue(loaded.await(5, TimeUnit.SECONDS));
            committed.set("new");
            other.evict("article:1");
            assertEquals("new", other.get("article:1", key -> committed.get(), 30));
            release.countDown();
            assertInstanceOf(ServiceException.class, assertThrows(ExecutionException.class,
                    () -> old.get(5, TimeUnit.SECONDS)).getCause());
            assertEquals("new", state.get("article:1"));
            assertEquals("new", manager.get("article:1", key -> fail("fresh L2 was overwritten"), 30));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void delayedL2ReadCannotReinstallL1AfterAnObservedInvalidation() throws Exception {
        state.put("article:1", "old");
        CountDownLatch read = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(values.get("article:1")).thenAnswer(call -> {
            Object snapshot = state.get("article:1");
            read.countDown();
            await(release);
            return snapshot;
        });
        var executor = Executors.newSingleThreadExecutor();
        try {
            var old = executor.submit(() -> manager.get("article:1", key -> fail("L2 missed"), 30));
            assertTrue(read.await(5, TimeUnit.SECONDS));
            manager.evict("article:1");
            release.countDown();
            assertInstanceOf(ServiceException.class, assertThrows(ExecutionException.class,
                    () -> old.get(5, TimeUnit.SECONDS)).getCause());
            when(values.get("article:1")).thenAnswer(call -> state.get("article:1"));
            assertEquals("new", manager.get("article:1", key -> "new", 30));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void remoteInvalidationDuringLoadPreventsL1InstallEvenIfRedisFillSucceeds() {
        MessageListener<String> remote = listener.get();
        assertThrows(ServiceException.class, () -> manager.get("article:1", key -> {
            remote.onMessage("cache:multilevel:evict", "article:1");
            return "snapshot";
        }, 30));
        // 替身只有通知没有Redis切代；验证本地guard，不宣称该值可安全继续使用。
        state.remove("article:1");
        assertEquals("new", manager.get("article:1", key -> "new", 30));
    }

    @Test
    void l2ReadStartedDuringEvictionIsClearedBeforeEvictionCompletes() {
        state.put("article:1", "old");
        duringEviction.set(() -> assertEquals("old",
                manager.get("article:1", key -> fail("L2 missed"), 30)));

        manager.evict("article:1");

        assertEquals("new", manager.get("article:1", key -> "new", 30));
    }

    @Test
    void oldNegativeLoadCannotHideANewArticleAfterItsInvalidation() {
        assertThrows(ServiceException.class, () -> manager.get("article:1", key -> {
            manager.evict(key);
            return null;
        }, 30));
        assertFalse(state.containsKey("article:1"));
        assertEquals("created", manager.get("article:1", key -> "created", 30));
    }

    @Test
    void evictionFailureAfterCommitIsExplicitAndDoesNotSkipOtherKeys() {
        state.put("article:1", "old");
        state.put("article:2", "old");
        failEviction.set(true);

        ServiceException failure = assertThrows(ServiceException.class, () -> transaction.executeWithoutResult(status -> {
            manager.evict("article:1");
            manager.evict("article:2");
        }));

        assertTrue(failure.getMessage().contains("内容已提交"));
        assertEquals("new", committed.get());
        assertEquals("old", state.get("article:1"));
        assertFalse(state.containsKey("article:2"));
        manager.evict("article:1");
        assertEquals("new", manager.get("article:1", key -> committed.get(), 30));
    }

    @Test
    void failedBroadcastStillLeavesRedisGenerationAdvancedAndRetryCanRecover() {
        state.put("article:1", "old");
        failPublish.set(true);
        assertThrows(IllegalStateException.class, () -> manager.evict("article:1"));
        assertFalse(state.containsKey("article:1"));
        assertNotNull(state.get("cache:generation:article:1"));
        failPublish.set(false);
        manager.evict("article:1");
        assertEquals("new", manager.get("article:1", key -> "new", 30));
    }

    @Test
    void realArticleDeleteProxyDefersEvictionUntilAfterCommit() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        ArticleService service = articleProxy(mapper, mock(CategoryMapper.class));
        assertEquals("old", manager.get("article:1", key -> committed.get(), 30));
        when(mapper.deleteBatchIds(List.of(1))).thenAnswer(call -> {
            assertEquals("old", state.get("article:1"));
            assertEquals("old", committed.get());
            return 1;
        });

        service.deleteArticle(List.of(1));

        verify(mapper).deleteBatchIds(List.of(1));
        assertEquals("new", committed.get());
        assertFalse(state.containsKey("article:1"));
        assertEquals("new", manager.get("article:1", key -> committed.get(), 30));
    }

    @Test
    void realArticleCreateProxyClearsPreviouslyCachedAbsenceAfterCommit() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "cache-create-test"),
                Category.class);
        ArticleMapper mapper = mock(ArticleMapper.class);
        CategoryMapper categories = mock(CategoryMapper.class);
        when(categories.selectOne(any())).thenReturn(Category.builder().id(2).build());
        when(mapper.insert(any(Article.class))).thenAnswer(call -> {
            assertNotNull(call.<Article>getArgument(0).getContentVersion());
            call.<Article>getArgument(0).setId(1);
            return 1;
        });
        ArticleService service = articleProxy(mapper, categories);
        assertNull(manager.get("article:1", key -> null, 30));
        ArticleDTO article = new ArticleDTO();
        article.setCategoryName("category");
        article.setArticleCover("cover");
        article.setTagNameList(List.of());
        // 空正文不触发派生任务；这里只验证真实创建事务与负缓存失效调用链。
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsInt).thenReturn(9);
            service.addArticle(article);
        }

        assertFalse(state.containsKey("article:1"));
        assertEquals("created", manager.get("article:1", key -> "created", 30));
    }

    @Test
    void versionedDetailsIgnoreMissedInvalidationAndOldL1EvenAfterRedisDataLoss() throws Exception {
        ArticleMapper mapper = mock(ArticleMapper.class);
        AtomicReference<String> version = new AtomicReference<>("v1");
        when(mapper.selectVisibleArticleVersion(eq(1), anyBoolean())).thenAnswer(call -> version.get());
        when(mapper.selectVisibleArticleDetailState(eq(1), anyBoolean())).thenAnswer(call -> detailState(version.get()));
        when(mapper.selectArticleHomeById(eq(1), anyString())).thenAnswer(call ->
                version.get().equals(call.getArgument(1)) ? article(version.get()) : null);
        ArticleService reader = articleProxy(mapper, mock(CategoryMapper.class));
        assertEquals("v1", reader.getArticleHomeById(1).getArticleContent());
        MultiLevelCacheManager writer = newManager();
        version.set("v2");
        failEviction.set(true);
        assertThrows(QueryTimeoutException.class, () -> writer.evict("article:1"));
        // 旧正文仍在本实例L1及共享L2；没有成功删除，也没有收到广播。
        assertEquals("v1", state.get("article:1:v:v1:body") instanceof ArticleVO old ? old.getArticleContent() : null);
        assertEquals("v2", reader.getArticleHomeById(1).getArticleContent());
        state.clear();
        version.set("v3");
        assertEquals("v3", reader.getArticleHomeById(1).getArticleContent());
        verify(mapper, times(1)).selectArticleHomeById(1, "v1");
        verify(mapper, times(1)).selectArticleHomeById(1, "v2");
        verify(mapper, times(1)).selectArticleHomeById(1, "v3");
    }

    @Test
    void oldVersionLoaderMayFinishButCannotReturnOldBodyOrOverwriteNewVersion() throws Exception {
        ArticleMapper mapper = mock(ArticleMapper.class);
        AtomicReference<String> version = new AtomicReference<>("v1");
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(mapper.selectVisibleArticleVersion(eq(1), anyBoolean())).thenAnswer(call -> version.get());
        when(mapper.selectVisibleArticleDetailState(eq(1), anyBoolean())).thenAnswer(call -> detailState(version.get()));
        when(mapper.selectArticleHomeById(eq(1), anyString())).thenAnswer(call -> {
            String observed = call.getArgument(1);
            if (!version.get().equals(observed)) {
                return null;
            }
            ArticleVO snapshot = article(observed);
            if (observed.equals("v1")) {
                loaded.countDown();
                await(release);
            }
            return snapshot;
        });
        ArticleService reader = articleProxy(mapper, mock(CategoryMapper.class));
        var executor = Executors.newSingleThreadExecutor();
        try {
            var old = executor.submit(() -> reader.getArticleHomeById(1));
            assertTrue(loaded.await(5, TimeUnit.SECONDS));
            version.set("v2");
            assertEquals("v2", reader.getArticleHomeById(1).getArticleContent());
            release.countDown();
            assertInstanceOf(ServiceException.class, assertThrows(ExecutionException.class,
                    () -> old.get(5, TimeUnit.SECONDS)).getCause());
            assertEquals("v2", reader.getArticleHomeById(1).getArticleContent());
            assertEquals("v2", ((ArticleVO) state.get("article:1:v:v2:body")).getArticleContent());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void newPersistedVersionBypassesOldNegativeCache() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectVisibleArticleVersion(eq(1), anyBoolean())).thenReturn("v2");
        when(mapper.selectVisibleArticleDetailState(eq(1), anyBoolean())).thenReturn(detailState("v2"));
        when(mapper.selectArticleHomeById(1, "v2")).thenReturn(article("v2"));
        assertNull(manager.get("article:1:v:v1:body", key -> null, 30));

        ArticleService reader = articleProxy(mapper, mock(CategoryMapper.class));

        assertEquals("v2", reader.getArticleHomeById(1).getArticleContent());
    }

    @Test
    void repeatedRealEditsAssignDistinctVersionsInTheArticleUpdate() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "cache-edit-test"),
                Category.class);
        ArticleMapper mapper = mock(ArticleMapper.class);
        CategoryMapper categories = mock(CategoryMapper.class);
        when(categories.selectOne(any())).thenReturn(Category.builder().id(2).build());
        ArticleService service = articleProxy(mapper, categories);
        ArticleDTO dto = new ArticleDTO();
        dto.setId(1);
        dto.setCategoryName("category");
        dto.setTagNameList(List.of());
        var versions = new java.util.ArrayList<String>();
        when(mapper.updateById(any(Article.class))).thenAnswer(call -> {
            versions.add(call.<Article>getArgument(0).getContentVersion());
            return 1;
        });
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsInt).thenReturn(9);
            service.updateArticle(dto);
            service.updateArticle(dto);
        }
        assertEquals(2, versions.size());
        assertNotNull(versions.get(0));
        assertNotEquals(versions.get(0), versions.get(1));
    }

    private static ArticleDetailStateVO detailState(String version) {
        ArticleDetailStateVO result = new ArticleDetailStateVO();
        result.setContentVersion(version);
        return result;
    }

    @Test
    void oldJoinedNegativeCacheIsIgnoredAndRelationRecoveryReusesOnlyTheBody() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(mapper.selectArticleHomeById(1, "v1")).thenReturn(article("unchanged body"));
        ArticleDetailStateVO restored = detailState("v1");
        CategoryOptionVO category = new CategoryOptionVO();
        category.setId(2);
        category.setCategoryName("restored");
        restored.setCategory(category);
        TagOptionVO tag = new TagOptionVO();
        tag.setId(3);
        tag.setTagName("renamed");
        restored.setTagVOList(List.of(tag));
        when(mapper.selectVisibleArticleDetailState(1, false)).thenReturn(null).thenReturn(restored);
        // 旧结构曾因关联缺失产生负缓存，新正文结构必须绕开它。
        assertNull(manager.get("article:1:v:v1", key -> null, 30));
        ArticleService reader = articleProxy(mapper, mock(CategoryMapper.class));

        assertNull(reader.getArticleHomeById(1));
        verify(mapper, never()).incrementViews(any());
        ArticleVO response = reader.getArticleHomeById(1);

        assertEquals("unchanged body", response.getArticleContent());
        assertEquals("restored", response.getCategory().getCategoryName());
        assertEquals("renamed", response.getTagVOList().get(0).getTagName());
        verify(mapper, times(1)).selectArticleHomeById(1, "v1");
        ArticleVO cached = (ArticleVO) state.get("article:1:v:v1:body");
        assertNull(cached.getCategory());
        assertNull(cached.getTagVOList());
    }

    @Test
    void withdrawnBodyLoadDoesNotLeaveNegativeCacheThatHidesSameVersionRestoration() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(mapper.selectArticleHomeById(1, "v1")).thenReturn(null).thenReturn(article("restored"));
        when(mapper.selectVisibleArticleDetailState(1, false)).thenReturn(detailState("v1"));
        ArticleService reader = articleProxy(mapper, mock(CategoryMapper.class));

        assertThrows(ServiceException.class, () -> reader.getArticleHomeById(1));
        assertFalse(state.containsKey("article:1:v:v1:body"));
        assertEquals("restored", reader.getArticleHomeById(1).getArticleContent());
        verify(mapper, times(2)).selectArticleHomeById(1, "v1");
        // 升级前已存在的body负缓存也能清理，下一次读取恢复，不等待TTL。
        manager.evict("article:1:v:v1:body");
        state.put("article:1:v:v1:body", "__NULL__");
        assertThrows(ServiceException.class, () -> reader.getArticleHomeById(1));
        assertFalse(state.containsKey("article:1:v:v1:body"));
        assertEquals("restored", reader.getArticleHomeById(1).getArticleContent());
        verify(mapper, times(3)).selectArticleHomeById(1, "v1");
    }

    private static ArticleVO article(String content) {
        ArticleVO result = new ArticleVO();
        result.setId(1);
        result.setArticleContent(content);
        return result;
    }

    private ArticleService articleProxy(ArticleMapper mapper, CategoryMapper categories) {
        ArticleServiceImpl service = new ArticleServiceImpl(mock(ArticleAccessService.class), categories,
                mock(ArticleTagMapper.class), mock(TagMapper.class), mock(TagService.class), mapper,
                mock(RedisService.class), mock(SearchStrategyContext.class), mock(UploadStrategyContext.class),
                mock(BlogFileMapper.class), mock(BlogMetrics.class), manager,
                mock(HotArticleWarmupService.class), mock(AiTaskService.class), mock(ArticleViewService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ThreadPoolTaskExecutor stats = mock(ThreadPoolTaskExecutor.class);
        doAnswer(call -> {
            call.<Runnable>getArgument(0).run();
            return null;
        }).when(stats).execute(any(Runnable.class));
        ReflectionTestUtils.setField(service, "hotArticleExecutor", stats);
        ProxyFactory proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(transaction.getTransactionManager(),
                new AnnotationTransactionAttributeSource()));
        return (ArticleService) proxy.getProxy();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
