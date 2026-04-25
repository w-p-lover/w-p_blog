package com.ican.service.impl;

import com.ican.cache.MultiLevelCacheManager;
import com.ican.mapper.*;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleVO;
import com.ican.service.RedisService;
import com.ican.service.TagService;
import com.ican.strategy.context.SearchStrategyContext;
import com.ican.strategy.context.UploadStrategyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Function;

import static com.ican.constant.RedisConstant.ARTICLE_LIKE_COUNT;
import static com.ican.constant.RedisConstant.ARTICLE_VIEW_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private UserMapper userMapper;
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
    private RabbitTemplate rabbitTemplate;

    private ArticleServiceImpl articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleServiceImpl(
                userMapper,
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
                rabbitTemplate
        );
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.initialize();
        ReflectionTestUtils.setField(articleService, "hotArticleExecutor", executor);
    }

    @Test
    void getArticleHomeById_shouldIncrementViewWhenCacheHit() {
        Integer articleId = 1;
        ArticleVO cachedArticle = new ArticleVO();
        cachedArticle.setId(articleId);

        when(cacheManager.get(eq("article:" + articleId), any(Function.class), eq(30)))
                .thenReturn(cachedArticle);
        when(redisService.getZsetScore(ARTICLE_VIEW_COUNT, articleId)).thenReturn(10D);
        when(redisService.getHash(ARTICLE_LIKE_COUNT, articleId.toString())).thenReturn(3);

        ArticleVO result = articleService.getArticleHomeById(articleId);

        verify(blogMetrics).incrementArticleView(articleId);
        verify(redisService).incrZet(ARTICLE_VIEW_COUNT, articleId, 1D);
        assertEquals(11, result.getViews());
    }
}
