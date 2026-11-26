package com.ican.service.impl;

import com.ican.cache.ArticleHotScoreProperties;
import com.ican.cache.MultiLevelCacheManager;
import com.ican.mapper.ArticleMapper;
import com.ican.exception.ServiceException;
import com.ican.metrics.BlogMetrics;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.ArticleVO;
import com.ican.model.vo.HotArticleWarmupResultVO;
import com.ican.service.ArticleHotScoreService;
import com.ican.service.HotArticleWarmupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static com.ican.constant.RedisConstant.ARTICLE_DETAIL_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleWarmupServiceImpl implements HotArticleWarmupService {

    private final ArticleHotScoreService hotScoreService;
    private final ArticleMapper articleMapper;
    private final MultiLevelCacheManager cacheManager;
    private final ArticleHotScoreProperties properties;
    private final BlogMetrics blogMetrics;

    @Override
    public HotArticleWarmupResultVO warmupTopHotArticles(int limit) {
        int safeLimit = limit <= 0 ? properties.getWarmupLimit() : limit;
        List<Integer> articleIds = hotScoreService.listTopHotScores(safeLimit).stream()
                .map(ArticleHotScoreVO::getArticleId)
                .toList();
        return warmupArticleIds(articleIds);
    }

    @Override
    public HotArticleWarmupResultVO warmupArticleIds(Collection<Integer> articleIds) {
        long start = System.currentTimeMillis();
        Collection<Integer> distinctIds = Objects.isNull(articleIds)
                ? Collections.emptySet()
                : new LinkedHashSet<>(articleIds);
        int warmed = 0;
        int skipped = 0;
        int failed = 0;
        for (Integer articleId : distinctIds) {
            if (Objects.isNull(articleId)) {
                skipped++;
                continue;
            }
            try {
                String contentVersion = articleMapper.selectVisibleArticleVersion(articleId, true);
                if (contentVersion == null) {
                    skipped++;
                    continue;
                }
                String cacheKey = ARTICLE_DETAIL_PREFIX + articleId + ":v:" + contentVersion + ":body";
                ArticleVO article = cacheManager.get(cacheKey, key -> {
                    ArticleVO loaded = articleMapper.selectArticleHomeById(articleId, contentVersion);
                    if (loaded == null) {
                        throw new ServiceException("预热时文章版本或状态已变化");
                    }
                    return loaded;
                }, properties.getDetailCacheTtlMinutes());
                if (Objects.isNull(article)) {
                    cacheManager.evict(cacheKey);
                    skipped++;
                } else {
                    warmed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("热点文章缓存预热失败: articleId={}, message={}", articleId, e.getMessage());
            }
        }
        long costMillis = System.currentTimeMillis() - start;
        blogMetrics.recordHotArticleWarmup(warmed, failed, costMillis);
        log.info("热点文章缓存预热完成: requested={}, warmed={}, skipped={}, failed={}, cost={}ms",
                distinctIds.size(), warmed, skipped, failed, costMillis);
        return new HotArticleWarmupResultVO(distinctIds.size(), warmed, skipped, failed, costMillis);
    }
}
