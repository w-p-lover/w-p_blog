package com.ican.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

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
}
