package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.mapper.ArticleMapper;
import com.ican.mapper.CommentMapper;
import com.ican.mapper.UserFavoriteMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleFavoriteCountVO;
import com.ican.model.vo.ArticleHotScoreSourceVO;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.CommentCountVO;
import com.ican.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ican.constant.RedisConstant.ARTICLE_HOT_SCORE;
import static com.ican.constant.RedisConstant.ARTICLE_LIKE_COUNT;
import static com.ican.constant.RedisConstant.ARTICLE_VIEW_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ArticleHotScoreServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserFavoriteMapper userFavoriteMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private BlogMetrics blogMetrics;
    @Mock
    private StringRedisTemplate snapshotRedis;
    @Mock
    private ValueOperations<String, String> snapshotValues;

    private ArticleHotScoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticleHotScoreServiceImpl(
                articleMapper,
                commentMapper,
                userFavoriteMapper,
                redisService,
                new ArticleHotScoreProperties(),
                blogMetrics,
                snapshotRedis
        );
    }

    @Test
    void calculateHotScore_shouldUseConfiguredWeightsAndTimeDecay() {
        ArticleHotScoreSourceVO source = source(1, "Redis 缓存", 100, 1, 1,
                LocalDateTime.now().minusDays(2));

        double score = service.calculateHotScore(source, 100, 20, 3, 2);

        assertThat(score).isEqualTo(100 * 1.0 + 20 * 5.0 + 3 * 8.0 + 2 * 10.0 + 30.0 + 50.0 - 2 * 1.5);
    }

    @Test
    void refreshHotScores_shouldAggregateSignalsAndWriteExactZsetScores() {
        when(snapshotRedis.opsForValue()).thenReturn(snapshotValues);
        when(snapshotRedis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        ArticleHotScoreSourceVO source = source(1, "Redis 缓存", 100, 1, 0,
                LocalDateTime.now().minusDays(1));
        ArticleFavoriteCountVO favoriteCount = new ArticleFavoriteCountVO();
        favoriteCount.setArticleId(1);
        favoriteCount.setFavoriteCount(2);
        CommentCountVO commentCount = new CommentCountVO();
        commentCount.setId(1);
        commentCount.setCommentCount(3);

        when(articleMapper.selectHotScoreSourceArticles()).thenReturn(List.of(source));
        when(redisService.getZsetAllScore(ARTICLE_VIEW_COUNT)).thenReturn(Map.of(1, 120.0));
        when(redisService.getHashAll(ARTICLE_LIKE_COUNT)).thenReturn(Map.of("1", 4));
        when(commentMapper.selectArticleCommentCount()).thenReturn(List.of(commentCount));
        when(userFavoriteMapper.selectArticleFavoriteCount()).thenReturn(List.of(favoriteCount));

        var result = service.refreshHotScores();

        assertThat(result.getRefreshedCount()).isEqualTo(1);
        var arguments = ArgumentCaptor.forClass(Object[].class);
        verify(snapshotRedis, times(2)).execute(any(RedisScript.class), anyList(), arguments.capture());
        assertThat(arguments.getAllValues().get(0)).hasSize(3);
        assertThat(arguments.getAllValues().get(0)[1]).isEqualTo("1");
        assertThat(arguments.getAllValues().get(0)[2]).isEqualTo("232.5");
        verify(redisService, never()).deleteObject(ARTICLE_HOT_SCORE);
        verify(redisService, never()).setZsetScore(eq(ARTICLE_HOT_SCORE), any(), anyDouble());
    }

    @Test
    void listTopHotScores_shouldUseDefaultLimitAndKeepRedisOrder() {
        Map<Object, Double> scores = new LinkedHashMap<>();
        scores.put(2, 91.0);
        scores.put(1, 88.5);
        when(redisService.zReverseRangeWithScore(ARTICLE_HOT_SCORE, 0, 19)).thenReturn(scores);

        List<ArticleHotScoreVO> result = service.listTopHotScores(0);

        assertThat(result).extracting(ArticleHotScoreVO::getArticleId).containsExactly(2, 1);
        assertThat(result).extracting(ArticleHotScoreVO::getHotScore).containsExactly(91.0, 88.5);
    }

    @Test
    void failedSecondBuildBatchDoesNotPublishOrDeleteTheExistingRanking() {
        when(snapshotRedis.opsForValue()).thenReturn(snapshotValues);
        when(articleMapper.selectHotScoreSourceArticles()).thenReturn(java.util.stream.IntStream.rangeClosed(1, 101)
                .mapToObj(id -> source(id, "article", 0, 0, 0, LocalDateTime.now())).toList());
        when(snapshotRedis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L).thenThrow(new org.springframework.dao.QueryTimeoutException("second batch failed"));

        assertThrows(org.springframework.dao.QueryTimeoutException.class, service::refreshHotScores);

        var keys = ArgumentCaptor.forClass(List.class);
        var arguments = ArgumentCaptor.forClass(Object[].class);
        verify(snapshotRedis, times(2)).execute(any(RedisScript.class), keys.capture(), arguments.capture());
        assertThat(keys.getAllValues()).allMatch(value -> value.size() == 2);
        assertThat(arguments.getAllValues().get(0)).hasSize(201);
        assertThat(arguments.getAllValues().get(1)).hasSize(3);
        verify(redisService, never()).deleteObject(anyString());
        verifyNoInteractions(blogMetrics);
    }

    @Test
    void knownEmptySourcePublishesAnExplicitEmptySnapshot() {
        when(snapshotRedis.opsForValue()).thenReturn(snapshotValues);
        when(articleMapper.selectHotScoreSourceArticles()).thenReturn(List.of());
        when(snapshotRedis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        assertThat(service.refreshHotScores().getRefreshedCount()).isZero();

        var arguments = ArgumentCaptor.forClass(Object[].class);
        verify(snapshotRedis).execute(any(RedisScript.class), anyList(), arguments.capture());
        assertThat(arguments.getValue()[1]).isEqualTo("0");
    }

    private ArticleHotScoreSourceVO source(Integer id, String title, Integer views, Integer isTop,
                                           Integer isRecommend, LocalDateTime createTime) {
        ArticleHotScoreSourceVO source = new ArticleHotScoreSourceVO();
        source.setArticleId(id);
        source.setArticleTitle(title);
        source.setViews(views);
        source.setIsTop(isTop);
        source.setIsRecommend(isRecommend);
        source.setCreateTime(createTime);
        return source;
    }
}
