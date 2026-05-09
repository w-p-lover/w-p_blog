package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.mapper.ArticleMapper;
import com.ican.mapper.CommentMapper;
import com.ican.mapper.UserFavoriteMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleFavoriteCountVO;
import com.ican.model.vo.ArticleHotScoreRefreshResultVO;
import com.ican.model.vo.ArticleHotScoreSourceVO;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.CommentCountVO;
import com.ican.service.ArticleHotScoreService;
import com.ican.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.TRUE;
import static com.ican.constant.RedisConstant.ARTICLE_HOT_SCORE;
import static com.ican.constant.RedisConstant.ARTICLE_LIKE_COUNT;
import static com.ican.constant.RedisConstant.ARTICLE_VIEW_COUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleHotScoreServiceImpl implements ArticleHotScoreService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final RedisService redisService;
    private final ArticleHotScoreProperties properties;
    private final BlogMetrics blogMetrics;

    @Override
    public ArticleHotScoreRefreshResultVO refreshHotScores() {
        long start = System.currentTimeMillis();
        List<ArticleHotScoreSourceVO> articles = safeList(articleMapper.selectHotScoreSourceArticles());
        Map<Object, Double> redisViews = safeMap(redisService.getZsetAllScore(ARTICLE_VIEW_COUNT));
        Map<String, Object> likes = safeMap(redisService.getHashAll(ARTICLE_LIKE_COUNT));
        Map<Integer, Integer> comments = safeList(commentMapper.selectArticleCommentCount()).stream()
                .collect(Collectors.toMap(
                        CommentCountVO::getId,
                        CommentCountVO::getCommentCount,
                        Integer::sum));
        Map<Integer, Integer> favorites = safeList(userFavoriteMapper.selectArticleFavoriteCount()).stream()
                .collect(Collectors.toMap(
                        ArticleFavoriteCountVO::getArticleId,
                        ArticleFavoriteCountVO::getFavoriteCount,
                        Integer::sum));

        redisService.deleteObject(ARTICLE_HOT_SCORE);
        for (ArticleHotScoreSourceVO article : articles) {
            Integer articleId = article.getArticleId();
            int viewCount = getViewCount(redisViews, articleId, article.getViews());
            int likeCount = toInt(likes.get(String.valueOf(articleId)));
            int commentCount = Optional.ofNullable(comments.get(articleId)).orElse(0);
            int favoriteCount = Optional.ofNullable(favorites.get(articleId)).orElse(0);
            double score = calculateHotScore(article, viewCount, likeCount, commentCount, favoriteCount);
            redisService.setZsetScore(ARTICLE_HOT_SCORE, articleId, score);
        }

        long costMillis = System.currentTimeMillis() - start;
        log.info("文章热度分刷新完成: count={}, cost={}ms", articles.size(), costMillis);
        return new ArticleHotScoreRefreshResultVO(articles.size(), costMillis);
    }

    @Override
    public List<ArticleHotScoreVO> listTopHotScores(int limit) {
        int safeLimit = limit <= 0 ? properties.getTopLimit() : limit;
        Map<Object, Double> topScores = safeMap(redisService.zReverseRangeWithScore(
                ARTICLE_HOT_SCORE, 0, safeLimit - 1L));
        return topScores.entrySet().stream()
                .map(entry -> new ArticleHotScoreVO(toInt(entry.getKey()), null, entry.getValue()))
                .toList();
    }

    public double calculateHotScore(ArticleHotScoreSourceVO article,
                                    int viewCount,
                                    int likeCount,
                                    int commentCount,
                                    int favoriteCount) {
        ArticleHotScoreProperties.Weights weights = properties.getWeights();
        long ageDays = Optional.ofNullable(article.getCreateTime())
                .map(time -> ChronoUnit.DAYS.between(time.toLocalDate(), LocalDate.now()))
                .orElse(0L);
        double score = viewCount * weights.getView()
                + likeCount * weights.getLike()
                + commentCount * weights.getComment()
                + favoriteCount * weights.getFavorite()
                + (TRUE.equals(article.getIsRecommend()) ? weights.getRecommend() : 0)
                + (TRUE.equals(article.getIsTop()) ? weights.getTop() : 0)
                - Math.max(ageDays, 0) * weights.getTimeDecayPerDay();
        return Math.max(score, 0);
    }

    private int getViewCount(Map<Object, Double> redisViews, Integer articleId, Integer dbViews) {
        Double redisViewCount = Optional.ofNullable(redisViews.get(articleId))
                .orElse(redisViews.get(String.valueOf(articleId)));
        return Optional.ofNullable(redisViewCount)
                .map(Double::intValue)
                .orElse(Optional.ofNullable(dbViews).orElse(0));
    }

    private int toInt(Object value) {
        if (Objects.isNull(value)) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private <T> List<T> safeList(List<T> list) {
        return Objects.isNull(list) ? Collections.emptyList() : list;
    }

    private <K, V> Map<K, V> safeMap(Map<K, V> map) {
        return Objects.isNull(map) ? Collections.emptyMap() : map;
    }
}
