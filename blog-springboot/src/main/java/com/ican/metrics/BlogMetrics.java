package com.ican.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 博客业务自定义指标
 * 指标均会暴露到 /actuator/prometheus，由 Prometheus 采集
 */
@Component
public class BlogMetrics {

    private final MeterRegistry registry;

    public BlogMetrics(MeterRegistry registry) {
        this.registry = registry;
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
