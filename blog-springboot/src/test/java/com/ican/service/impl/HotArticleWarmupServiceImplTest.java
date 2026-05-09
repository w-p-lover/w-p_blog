package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.cache.MultiLevelCacheManager;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.ArticleVO;
import com.ican.service.ArticleHotScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Function;

import static com.ican.constant.RedisConstant.ARTICLE_DETAIL_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotArticleWarmupServiceImplTest {

    @Mock
    private ArticleHotScoreService hotScoreService;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private MultiLevelCacheManager cacheManager;
    @Mock
    private BlogMetrics blogMetrics;

    private HotArticleWarmupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HotArticleWarmupServiceImpl(
                hotScoreService,
                articleMapper,
                cacheManager,
                new ArticleHotScoreProperties(),
                blogMetrics
        );
    }

    @Test
    void warmupTopHotArticles_shouldWarmArticleDetailCaches() {
        ArticleVO article = new ArticleVO();
        article.setId(1);
        when(hotScoreService.listTopHotScores(2))
                .thenReturn(List.of(new ArticleHotScoreVO(1, null, 99.0)));
        when(cacheManager.get(eq(ARTICLE_DETAIL_PREFIX + 1), any(Function.class), eq(30)))
                .thenReturn(article);

        var result = service.warmupTopHotArticles(2);

        assertThat(result.getRequestedCount()).isEqualTo(1);
        assertThat(result.getWarmedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        verify(cacheManager).get(eq(ARTICLE_DETAIL_PREFIX + 1), any(Function.class), eq(30));
    }

    @Test
    void warmupArticleIds_shouldDeduplicateAndCountSkippedAndFailedArticles() {
        ArticleVO article = new ArticleVO();
        article.setId(1);
        when(cacheManager.get(eq(ARTICLE_DETAIL_PREFIX + 1), any(Function.class), eq(30)))
                .thenReturn(article);
        when(cacheManager.get(eq(ARTICLE_DETAIL_PREFIX + 2), any(Function.class), eq(30)))
                .thenThrow(new IllegalStateException("cache loader failed"));

        var result = service.warmupArticleIds(new java.util.ArrayList<>(java.util.Arrays.asList(1, 1, null, 2)));

        assertThat(result.getRequestedCount()).isEqualTo(3);
        assertThat(result.getWarmedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        verify(cacheManager).get(eq(ARTICLE_DETAIL_PREFIX + 1), any(Function.class), eq(30));
        verify(cacheManager).get(eq(ARTICLE_DETAIL_PREFIX + 2), any(Function.class), eq(30));
    }
}
