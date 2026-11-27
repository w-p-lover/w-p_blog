package com.ican.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 博客业务自定义指标
 * 指标均会暴露到 /actuator/prometheus，由 Prometheus 采集
 */
@Component
public class BlogMetrics {

    private final MeterRegistry registry;
    private final java.util.Map<String, AtomicLong> aiTaskCounts = new java.util.HashMap<>();
    private final java.util.Map<String, AtomicLong> aiTaskAges = new java.util.HashMap<>();
    private final AtomicLong aiTaskSnapshotAt = new AtomicLong();
    private final AtomicLong articleViewsPending = new AtomicLong();

    public BlogMetrics(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("blog.ai.task.snapshot.timestamp", aiTaskSnapshotAt, AtomicLong::get)
                .description("持久任务积压快照最近成功采集的Unix秒，0表示尚未成功")
                .register(registry);
        for (String status : java.util.List.of("PENDING", "RUNNING", "RETRYING", "UNKNOWN", "FAILED")) {
            AtomicLong count = new AtomicLong();
            AtomicLong age = new AtomicLong();
            aiTaskCounts.put(status, count);
            aiTaskAges.put(status, age);
            Gauge.builder("blog.ai.task.backlog", count, AtomicLong::get).tag("status", status).register(registry);
            Gauge.builder("blog.ai.task.oldest.seconds", age, AtomicLong::get).tag("status", status).register(registry);
        }
        Gauge.builder("blog.article.views.pending", articleViewsPending, AtomicLong::get)
                .description("最近一次PV同步观察到的待同步文章数")
                .register(registry);
    }

    /** 只使用缓存阶段/结果等有限标签，不以文章ID、缓存key或异常消息打标签。 */
    public void recordCacheEvent(String stage, String outcome) {
        Counter.builder("blog.cache.events")
                .tag("stage", stage).tag("outcome", outcome)
                .register(registry).increment();
    }

    public void recordAiTaskEvent(String stage, String outcome) {
        Counter.builder("blog.ai.task.events").tag("stage", stage).tag("outcome", outcome)
                .register(registry).increment();
    }

    /** 最近一次成功扫描的持久任务快照；年龄从任务创建开始，不是broker队列长度。 */
    public void setAiTaskBacklog(java.util.List<java.util.Map<String, Object>> rows) {
        aiTaskCounts.values().forEach(value -> value.set(0));
        aiTaskAges.values().forEach(value -> value.set(0));
        for (var row : rows) {
            String status = String.valueOf(row.get("taskStatus"));
            if (aiTaskCounts.containsKey(status)) {
                aiTaskCounts.get(status).set(((Number) row.get("taskCount")).longValue());
                aiTaskAges.get(status).set(((Number) row.get("oldestSeconds")).longValue());
            }
        }
        aiTaskSnapshotAt.set(java.time.Instant.now().getEpochSecond());
    }

    public void recordCacheDuration(String stage, long costNanos) {
        Timer.builder("blog.cache.duration").tag("stage", stage)
                .register(registry).record(costNanos, TimeUnit.NANOSECONDS);
    }

    public void bindCacheLoadBudget(Semaphore permits, int capacity) {
        Gauge.builder("blog.cache.rebuild.active", permits, value -> capacity - value.availablePermits())
                .description("本实例正在等锁或执行正文重建的请求数，不含权威SQL")
                .register(registry);
    }

    /**
     * 文章浏览量 Counter
     * Prometheus 中查询：sum(increase(blog_article_views_total[5m])) by (articleId)
     */
    public void incrementArticleView(Integer articleId) {
        Counter.builder("blog.article.views")
                .description("文章浏览次数")
                .tag("articleId", String.valueOf(articleId))
                .register(registry)
                .increment();
    }

    public void recordArticleStatsFailure(String operation) {
        Counter.builder("blog.article.stats.failures")
                .tag("operation", operation)
                .register(registry).increment();
    }

    public void setArticleViewsPending(long pending) {
        articleViewsPending.set(pending);
    }

    public void recordArticleViewFlush(int persisted, int failed, long costNanos) {
        Counter.builder("blog.article.views.flush.snapshots").register(registry).increment(persisted);
        Counter.builder("blog.article.views.flush.failures").register(registry).increment(failed);
        Timer.builder("blog.article.views.flush.duration").register(registry)
                .record(costNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 记录文章热度分刷新指标
     */
    public void recordArticleHotScoreRefresh(int articleCount, long costMillis) {
        Counter.builder("blog.article.hot_score.refresh")
                .description("文章热度分刷新文章数")
                .register(registry)
                .increment(articleCount);
        Timer.builder("blog.article.hot_score.refresh.duration")
                .description("文章热度分刷新耗时")
                .register(registry)
                .record(costMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录热点文章缓存预热指标
     */
    public void recordHotArticleWarmup(int warmedCount, int failedCount, long costMillis) {
        Counter.builder("blog.article.hot_cache.warmup")
                .description("热点文章缓存预热成功数")
                .register(registry)
                .increment(warmedCount);
        Counter.builder("blog.article.hot_cache.warmup.failures")
                .description("热点文章缓存预热失败数")
                .register(registry)
                .increment(failedCount);
        Timer.builder("blog.article.hot_cache.warmup.duration")
                .description("热点文章缓存预热耗时")
                .register(registry)
                .record(costMillis, TimeUnit.MILLISECONDS);
    }
}
