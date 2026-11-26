package com.ican.service.impl;

import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.ican.constant.RedisConstant.ARTICLE_VIEW_COUNT;

/** 展示型PV，非UV/审计账本；Redis保留累计值和待同步标记，DB写入幂等快照。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleViewService {

    private static final String PENDING = "article:view:pending";
    private static final List<String> KEYS = List.of(ARTICLE_VIEW_COUNT, PENDING);
    private static final DefaultRedisScript<Long> RECORD = new DefaultRedisScript<>();
    private static final DefaultRedisScript<List> SNAPSHOT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> ACK = new DefaultRedisScript<>();

    static {
        RECORD.setLocation(new ClassPathResource("lua/article_view_record.lua"));
        RECORD.setResultType(Long.class);
        SNAPSHOT.setLocation(new ClassPathResource("lua/article_view_snapshot.lua"));
        SNAPSHOT.setResultType(List.class);
        ACK.setLocation(new ClassPathResource("lua/article_view_ack.lua"));
        ACK.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final ArticleMapper articleMapper;
    private final BlogMetrics metrics;

    public int recordView(Integer articleId, Integer persistedViews) {
        int baseline = Math.max(persistedViews == null ? 0 : persistedViews, 0);
        try {
            Long total = redis.execute(RECORD, KEYS, articleId.toString(), Integer.toString(baseline));
            if (total != null) {
                metrics.incrementArticleView(articleId);
                return total.intValue();
            }
        } catch (DataAccessException e) {
            // 结果可能已生效但响应丢失；不重试本次计数，避免重复放大。
            log.debug("文章PV计数不可用，返回数据库快照: articleId={}", articleId, e);
        }
        metrics.recordArticleStatsFailure("view_record");
        return baseline;
    }

    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    public void flushPendingViews() {
        long start = System.nanoTime();
        int persisted = 0;
        int failed = 0;
        RuntimeException firstFailure = null;
        try {
            // 每轮最多100篇；不按PV条数排队，重复阅读合并到同一文章累计值。
            List<?> snapshots = redis.execute(SNAPSHOT, KEYS, "100");
            if (snapshots == null) {
                throw new IllegalStateException("文章PV快照结果未知");
            }
            for (int i = 0; i < snapshots.size(); i += 2) {
                try {
                    Integer articleId = Integer.valueOf(snapshots.get(i).toString());
                    int total = Integer.parseInt(snapshots.get(i + 1).toString());
                    articleMapper.persistViewSnapshot(articleId, total);
                    // SQL返回即已结束单条提交；ACK失败/未知保留可重复同步语义。
                    Long acked = redis.execute(ACK, KEYS, articleId.toString(), Integer.toString(total));
                    if (acked == null) {
                        throw new IllegalStateException("文章PV同步确认结果未知");
                    }
                    persisted++;
                } catch (RuntimeException e) {
                    failed++;
                    if (firstFailure == null) {
                        firstFailure = e;
                    }
                }
            }
            Long pending = redis.opsForZSet().zCard(PENDING);
            if (pending != null) {
                metrics.setArticleViewsPending(pending);
            }
        } catch (RuntimeException e) {
            failed++;
            firstFailure = e;
        }
        if (firstFailure != null) {
            log.warn("文章PV同步部分失败，下一轮重试待同步快照: failed={}", failed, firstFailure);
        }
        metrics.recordArticleViewFlush(persisted, failed, System.nanoTime() - start);
    }
}
