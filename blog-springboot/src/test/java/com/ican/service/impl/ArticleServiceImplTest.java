package com.ican.service.impl;

import com.ican.cache.MultiLevelCacheManager;
import com.ican.mapper.*;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleVO;
import com.ican.model.vo.ArticleDetailStateVO;
import com.ican.model.vo.CategoryOptionVO;
import com.ican.model.vo.TagOptionVO;
import com.ican.service.AiTaskService;
import com.ican.service.ArticleAccessService;
import com.ican.model.vo.ArticlePaginationVO;
import com.ican.model.vo.ArchiveVO;
import com.ican.utils.PageUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.concurrent.atomic.AtomicReference;
import com.ican.service.HotArticleWarmupService;
import com.ican.service.RedisService;
import com.ican.service.TagService;
import com.ican.strategy.context.SearchStrategyContext;
import com.ican.strategy.context.UploadStrategyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Function;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ican.constant.RedisConstant.ARTICLE_LIKE_COUNT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleAccessService articleAccessService;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ArticleTagMapper articleTagMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private TagService tagService;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private SearchStrategyContext searchStrategyContext;
    @Mock
    private UploadStrategyContext uploadStrategyContext;
    @Mock
    private BlogFileMapper blogFileMapper;
    @Mock
    private BlogMetrics blogMetrics;
    @Mock
    private MultiLevelCacheManager cacheManager;
    @Mock
    private HotArticleWarmupService hotArticleWarmupService;
    @Mock
    private AiTaskService aiTaskService;
    @Mock
    private ArticleViewService articleViewService;

    private ArticleServiceImpl articleService;

    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        articleService = new ArticleServiceImpl(
                articleAccessService,
                categoryMapper,
                articleTagMapper,
                tagMapper,
                tagService,
                articleMapper,
                redisService,
                searchStrategyContext,
                uploadStrategyContext,
                blogFileMapper,
                blogMetrics,
                cacheManager,
                hotArticleWarmupService,
                aiTaskService,
                articleViewService
        );
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.initialize();
        ReflectionTestUtils.setField(articleService, "hotArticleExecutor", executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
        PageUtils.remove();
    }

    @Test
    void getArticleHomeById_shouldIncrementViewWhenCacheHit() {
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        Integer articleId = 1;
        ArticleVO cachedArticle = new ArticleVO();
        cachedArticle.setId(articleId);

        when(cacheManager.get(eq("article:" + articleId + ":v:v1:body"), any(Function.class), eq(30)))
                .thenReturn(cachedArticle);
        when(articleMapper.selectVisibleArticleDetailState(articleId, false)).thenReturn(detailState("v1", "category", "tag"));
        when(articleViewService.recordView(articleId, 10)).thenReturn(11);
        when(redisService.getHash(ARTICLE_LIKE_COUNT, articleId.toString())).thenReturn(3);

        ArticleVO result = articleService.getArticleHomeById(articleId);

        verify(articleViewService).recordView(articleId, 10);
        verify(articleMapper, never()).selectLastArticle(any(), anyBoolean());
        verify(articleMapper, never()).selectNextArticle(any(), anyBoolean());
        verify(articleMapper, never()).incrementViews(any());
        assertEquals(11, result.getViews());
    }

    @Test
    void staleCacheIsBlockedAfterWithdrawalAndVisibleAgainAfterRepublish() {
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        ArticleVO stale = new ArticleVO();
        stale.setId(1);
        stale.setArticleTitle("撤回前标题");
        stale.setArticleContent("撤回前正文");
        stale.setAiSummary("撤回前摘要");
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(stale);
        when(articleMapper.selectVisibleArticleDetailState(1, false))
                .thenReturn(null).thenReturn(detailState("v1", "category", "tag"));

        assertNull(articleService.getArticleHomeById(1));
        verifyNoInteractions(blogMetrics, redisService, articleViewService);
        verify(articleMapper, never()).incrementViews(any());

        ArticleVO restored = articleService.getArticleHomeById(1);
        assertNotSame(stale, restored);
        assertEquals(stale.getArticleContent(), restored.getArticleContent());
        verify(articleViewService).recordView(1, 10);
    }

    @Test
    void withdrawalDuringCacheLoadIsCheckedAfterLoadingRatherThanBefore() {
        AtomicBoolean withdrawn = new AtomicBoolean(false);
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        ArticleVO stale = new ArticleVO();
        stale.setId(1);
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenAnswer(invocation -> {
            withdrawn.set(true);
            return stale;
        });
        when(articleMapper.selectVisibleArticleDetailState(1, false)).thenAnswer(invocation ->
                withdrawn.get() ? null : detailState("v1", "category", "tag"));

        assertNull(articleService.getArticleHomeById(1));

        verifyNoInteractions(blogMetrics, redisService, articleViewService);
    }

    @Test
    void databaseFailureDoesNotFallBackToStaleCache() {
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(new ArticleVO());
        when(articleMapper.selectVisibleArticleDetailState(1, false))
                .thenThrow(new IllegalStateException("数据库暂时不可用"));

        assertThrows(IllegalStateException.class, () -> articleService.getArticleHomeById(1));

        verifyNoInteractions(blogMetrics, redisService, articleViewService);
    }

    @Test
    void absentContentNeedsNoAdditionalStatusQueryOrViewCount() {
        assertNull(articleService.getArticleHomeById(1));

        verify(articleMapper).selectVisibleArticleVersion(1, true);
        verifyNoInteractions(cacheManager, blogMetrics, redisService, articleViewService);
    }
    @Test
    void lossAndRecoveryOfPrivateAccessAreCheckedOnEveryCacheHit() {
        ArticleVO cached = new ArticleVO();
        cached.setId(1);
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(cached);
        when(articleAccessService.canReadPrivateArticles()).thenReturn(true, false, true);
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(articleMapper.selectVisibleArticleDetailState(1, false)).thenReturn(null);
        when(articleMapper.selectVisibleArticleDetailState(1, true)).thenReturn(detailState("v1", "category", "tag"));

        assertNotNull(articleService.getArticleHomeById(1));
        assertNull(articleService.getArticleHomeById(1));
        assertNotNull(articleService.getArticleHomeById(1));

        verify(articleViewService, times(2)).recordView(1, 10);
        verify(articleMapper, never()).selectNextArticle(1, false);
    }

    @Test
    void interleavedReadersCannotOverwriteNavigationInOtherResponsesOrSharedCache() {
        ArticleVO cached = new ArticleVO();
        cached.setId(1);
        ArticlePaginationVO oldPrivateNavigation = new ArticlePaginationVO();
        oldPrivateNavigation.setId(9);
        cached.setNextArticle(oldPrivateNavigation);
        ArticlePaginationVO publicNavigation = new ArticlePaginationVO();
        publicNavigation.setId(8);
        ArticlePaginationVO privateNavigation = new ArticlePaginationVO();
        privateNavigation.setId(2);
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(cached);
        when(articleAccessService.canReadPrivateArticles()).thenReturn(true, false);
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        ArticleDetailStateVO publicState = detailState("v1", "category", "tag");
        publicState.setNextArticle(publicNavigation);
        ArticleDetailStateVO privateState = detailState("v1", "category", "tag");
        privateState.setNextArticle(privateNavigation);
        when(articleMapper.selectVisibleArticleDetailState(1, false)).thenReturn(publicState);
        AtomicReference<ArticleVO> ordinaryResponse = new AtomicReference<>();
        // 私人快照读取期间插入普通读者，导航与权限均来自各自快照。
        when(articleMapper.selectVisibleArticleDetailState(1, true)).thenAnswer(invocation -> {
            ordinaryResponse.set(articleService.getArticleHomeById(1));
            return privateState;
        });

        ArticleVO specialResponse = articleService.getArticleHomeById(1);

        assertNotSame(cached, specialResponse);
        assertNotSame(cached, ordinaryResponse.get());
        assertSame(privateNavigation, specialResponse.getNextArticle());
        assertSame(publicNavigation, ordinaryResponse.get().getNextArticle());
        assertSame(oldPrivateNavigation, cached.getNextArticle());
        assertNull(ordinaryResponse.get().getLastArticle());
    }

    @Test
    void versionChangeDuringNegativeLoadRejectsResponseWithoutCounting() {
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(articleMapper.selectVisibleArticleDetailState(1, false)).thenReturn(detailState("v2", "category", "tag"));
        // 没有命中正文时也不能跳过读后检查，否则会错误地把旧缺失返回为当前缺失。
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(null);

        assertThrows(com.ican.exception.ServiceException.class, () -> articleService.getArticleHomeById(1));

        verifyNoInteractions(blogMetrics, redisService, articleViewService);
    }

    @Test
    void initialAuthorityFailureDoesNotReadAnyCacheOrCountViews() {
        when(articleMapper.selectVisibleArticleVersion(1, true))
                .thenThrow(new IllegalStateException("version authority unavailable"));

        assertThrows(IllegalStateException.class, () -> articleService.getArticleHomeById(1));

        verifyNoInteractions(cacheManager, blogMetrics, redisService, articleViewService);
    }

    @Test
    void independentRenamesReplaceCachedRelationsWithoutChangingBodyOrMutatingEarlierResponse() {
        ArticleVO cached = new ArticleVO();
        cached.setId(1);
        cached.setArticleContent("unchanged body");
        ArticleDetailStateVO old = detailState("v1", "old category", "old tag");
        cached.setCategory(old.getCategory());
        cached.setTagVOList(old.getTagVOList());
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(cached);
        when(articleMapper.selectVisibleArticleDetailState(1, false))
                .thenReturn(detailState("v1", "first category", "first tag"))
                .thenReturn(detailState("v1", "renamed category", "renamed tag"));

        ArticleVO first = articleService.getArticleHomeById(1);
        ArticleVO second = articleService.getArticleHomeById(1);

        assertEquals("renamed category", second.getCategory().getCategoryName());
        assertEquals("renamed tag", second.getTagVOList().get(0).getTagName());
        assertEquals("unchanged body", second.getArticleContent());
        assertEquals("first category", first.getCategory().getCategoryName());
        assertEquals("old category", cached.getCategory().getCategoryName());
        assertEquals("old tag", cached.getTagVOList().get(0).getTagName());
        verify(cacheManager, never()).evict(any());
    }

    @Test
    void missingRelationSnapshotSuppressesCachedBodyAndRecoveryDoesNotRequireEviction() {
        ArticleVO body = new ArticleVO();
        body.setId(1);
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(body);
        when(articleMapper.selectVisibleArticleDetailState(1, false)).thenReturn(null)
                .thenReturn(detailState("v1", "restored category", "restored tag"));

        assertNull(articleService.getArticleHomeById(1));
        verifyNoInteractions(blogMetrics, redisService, articleViewService);
        assertEquals("restored tag", articleService.getArticleHomeById(1).getTagVOList().get(0).getTagName());
        verify(articleViewService).recordView(1, 10);
    }

    @Test
    void relationQueryFailureDoesNotFallBackToCachedNamesAndRetryCanRecover() {
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30))).thenReturn(new ArticleVO());
        when(articleMapper.selectVisibleArticleDetailState(1, false))
                .thenThrow(new IllegalStateException("relation read unavailable"))
                .thenReturn(detailState("v1", "current", "current"));

        assertThrows(IllegalStateException.class, () -> articleService.getArticleHomeById(1));
        verifyNoInteractions(blogMetrics, redisService, articleViewService);
        assertNotNull(articleService.getArticleHomeById(1));
    }

    @Test
    void statisticsFailureDoesNotHideAuthorizedBodyAndColdLoadDoesNotReadStatistics() {
        when(articleMapper.selectVisibleArticleVersion(1, true)).thenReturn("v1");
        ArticleVO body = new ArticleVO();
        body.setArticleContent("readable body");
        when(articleMapper.selectArticleHomeById(1, "v1")).thenReturn(body);
        when(cacheManager.get(eq("article:1:v:v1:body"), any(Function.class), eq(30)))
                .thenAnswer(call -> call.<Function<String, ArticleVO>>getArgument(1).apply("body"));
        when(articleMapper.selectVisibleArticleDetailState(1, false)).thenReturn(detailState("v1", "current", "current"));
        when(articleViewService.recordView(1, 10)).thenReturn(10);
        when(redisService.getHash(ARTICLE_LIKE_COUNT, "1"))
                .thenThrow(new org.springframework.dao.QueryTimeoutException("likes unavailable"));

        ArticleVO result = articleService.getArticleHomeById(1);

        assertEquals("readable body", result.getArticleContent());
        assertEquals(10, result.getViews());
        assertEquals(0, result.getLikeCount());
        verify(blogMetrics).recordArticleStatsFailure("likes_read");
        verify(redisService).getHash(ARTICLE_LIKE_COUNT, "1");
        verifyNoMoreInteractions(redisService);
        assertNull(body.getLikeCount());
    }

    @Test
    void homeWarmupOnlyUsesCurrentPageAndCoalescesQueuedWorkThenRecoversAfterFailure() {
        com.ican.model.vo.ArticleHomeVO first = new com.ican.model.vo.ArticleHomeVO();
        first.setId(1);
        first.setIsTop(1);
        first.setArticleContent("#first");
        com.ican.model.vo.ArticleHomeVO second = new com.ican.model.vo.ArticleHomeVO();
        second.setId(2);
        second.setIsTop(1);
        second.setArticleContent("#second");
        when(articleMapper.PselectArticleAllList("create_time", null, null, null)).thenReturn(List.of(first, second));
        when(redisService.getZsetAllScore(com.ican.constant.RedisConstant.ARTICLE_VIEW_COUNT)).thenReturn(java.util.Map.of());
        PageUtils.setCurrentPage(new Page<>(1, 1));
        java.util.List<Runnable> queued = new java.util.ArrayList<>();
        ThreadPoolTaskExecutor controlled = mock(ThreadPoolTaskExecutor.class);
        doAnswer(call -> { queued.add(call.getArgument(0)); return null; })
                .when(controlled).execute(any(Runnable.class));
        ReflectionTestUtils.setField(articleService, "hotArticleExecutor", controlled);
        when(hotArticleWarmupService.warmupArticleIds(any()))
                .thenThrow(new IllegalStateException("warmup failure")).thenReturn(null);

        assertEquals(1, articleService.listArticleHomeVO(null, null, null, null).getRecordList().size());
        articleService.listArticleHomeVO(null, null, null, null);
        assertEquals(1, queued.size());
        queued.get(0).run();
        articleService.listArticleHomeVO(null, null, null, null);
        assertEquals(2, queued.size());
        queued.get(1).run();

        verify(hotArticleWarmupService, times(2)).warmupArticleIds(java.util.Set.of(1));
        assertEquals("#second", second.getArticleContent());
    }

    private static ArticleDetailStateVO detailState(String version, String categoryName, String tagName) {
        ArticleDetailStateVO state = new ArticleDetailStateVO();
        state.setContentVersion(version);
        state.setViews(10);
        CategoryOptionVO category = new CategoryOptionVO();
        category.setId(2);
        category.setCategoryName(categoryName);
        state.setCategory(category);
        TagOptionVO tag = new TagOptionVO();
        tag.setId(3);
        tag.setTagName(tagName);
        state.setTagVOList(List.of(tag));
        return state;
    }

    @Test
    void archiveCountAndPageUseTheSameReaderVisibility() {
        PageUtils.setCurrentPage(new Page<>(2, 2));
        when(articleAccessService.canReadPrivateArticles()).thenReturn(true);
        when(articleMapper.countVisibleArticles(true)).thenReturn(3L);
        when(articleMapper.selectArchiveList(2L, 2L, true)).thenReturn(List.of(new ArchiveVO()));

        var result = articleService.listArchiveVO();

        assertEquals(3L, result.getCount());
        assertEquals(1, result.getRecordList().size());
        verify(articleMapper).countVisibleArticles(true);
        verify(articleMapper).selectArchiveList(2L, 2L, true);
        verifyNoMoreInteractions(articleMapper);
    }

}
