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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private ArticleHotScoreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticleHotScoreServiceImpl(
                articleMapper,
                commentMapper,
                userFavoriteMapper,
                redisService,
                new ArticleHotScoreProperties(),
                blogMetrics
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
        verify(redisService).deleteObject(ARTICLE_HOT_SCORE);
        verify(redisService).setZsetScore(ARTICLE_HOT_SCORE, 1, 120 * 1.0 + 4 * 5.0 + 3 * 8.0 + 2 * 10.0 + 0 + 50.0 - 1 * 1.5);
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
