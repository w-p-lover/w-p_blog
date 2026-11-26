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
    private final AtomicLong articleViewsPending = new AtomicLong();

    public BlogMetrics(MeterRegistry registry) {
        this.registry = registry;
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



    /** 最近一次成功扫描的持久任务快照；年龄从任务创建开始，不是broker队列长度。 */


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
