package com.ican.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlogMetricsTest {

    @Test
    void articleViewCounter_incrementsOnView() {
        MeterRegistry registry = new SimpleMeterRegistry();
        BlogMetrics metrics = new BlogMetrics(registry);

        metrics.incrementArticleView(42);
        metrics.incrementArticleView(42);

        Counter counter = registry.find("blog.article.views")
                .tag("articleId", "42")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }
}
