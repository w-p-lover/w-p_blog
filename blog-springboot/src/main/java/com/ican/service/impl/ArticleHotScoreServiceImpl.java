package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.exception.ServiceException;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.ArrayList;
import java.util.UUID;
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
    private final StringRedisTemplate snapshotRedis;

    private static final String REFRESH_TOKEN = ARTICLE_HOT_SCORE + ":refresh";
    private static final DefaultRedisScript<Long> STAGE = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> PUBLISH = new DefaultRedisScript<>();

    static {
        STAGE.setLocation(new ClassPathResource("lua/article_hot_stage.lua"));
        STAGE.setResultType(Long.class);
        PUBLISH.setLocation(new ClassPathResource("lua/article_hot_publish.lua"));
        PUBLISH.setResultType(Long.class);
    }

    @Override
    public ArticleHotScoreRefreshResultVO refreshHotScores() {
        long start = System.currentTimeMillis();
        // 先登记刷新代，再读信号；晚完成的旧执行不能覆盖更新的刷新。
        String token = UUID.randomUUID().toString();
        String staging = ARTICLE_HOT_SCORE + ":build:" + token;
        snapshotRedis.opsForValue().set(REFRESH_TOKEN, token);
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

        try {
            List<String> batch = new ArrayList<>();
            batch.add(token);
            for (ArticleHotScoreSourceVO article : articles) {
                Integer articleId = article.getArticleId();
                int viewCount = getViewCount(redisViews, articleId, article.getViews());
                int likeCount = toInt(likes.get(String.valueOf(articleId)));
                int commentCount = Optional.ofNullable(comments.get(articleId)).orElse(0);
                int favoriteCount = Optional.ofNullable(favorites.get(articleId)).orElse(0);
                double score = calculateHotScore(article, viewCount, likeCount, commentCount, favoriteCount);
                if (!Double.isFinite(score)) {
                    throw new ServiceException("热度分无效，本次未发布");
                }
                batch.add(articleId.toString());
                batch.add(Double.toString(score));
                if (batch.size() == 201) {
                    stageHotScores(staging, batch);
                    batch.subList(1, batch.size()).clear();
                }
            }
            if (batch.size() > 1) {
                stageHotScores(staging, batch);
            }
            Long published = snapshotRedis.execute(PUBLISH,
                    List.of(REFRESH_TOKEN, staging, ARTICLE_HOT_SCORE), token, Integer.toString(articles.size()));
            if (!Long.valueOf(1).equals(published)) {
                throw new ServiceException("热榜刷新已被更新执行替代或快照已过期，请重新刷新");
            }
        } finally {
            // 临时键自身有TTL；清理失败不能把已经成功发布伪装成业务失败。
            try {
                snapshotRedis.delete(staging);
            } catch (RuntimeException e) {
                log.warn("热榜临时快照清理失败，等待TTL: key={}", staging, e);
            }
        }

        long costMillis = System.currentTimeMillis() - start;
        blogMetrics.recordArticleHotScoreRefresh(articles.size(), costMillis);
        log.info("文章热度分刷新完成: count={}, cost={}ms", articles.size(), costMillis);
        return new ArticleHotScoreRefreshResultVO(articles.size(), costMillis);
    }

    private void stageHotScores(String staging, List<String> batch) {
        Long staged = snapshotRedis.execute(STAGE, List.of(REFRESH_TOKEN, staging), batch.toArray());
        if (!Long.valueOf(1).equals(staged)) {
            throw new ServiceException("热榜刷新已被更新执行替代，请重新刷新");
        }
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
