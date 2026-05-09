package com.ican.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

    @Test
    void hotScoreAndWarmupMetrics_shouldBeRecorded() {
        MeterRegistry registry = new SimpleMeterRegistry();
        BlogMetrics metrics = new BlogMetrics(registry);

        metrics.recordArticleHotScoreRefresh(3, 25);
        metrics.recordHotArticleWarmup(2, 1, 30);

        Counter refreshCounter = registry.find("blog.article.hot_score.refresh").counter();
        Counter warmupCounter = registry.find("blog.article.hot_cache.warmup").counter();
        Counter failureCounter = registry.find("blog.article.hot_cache.warmup.failures").counter();
        Timer refreshTimer = registry.find("blog.article.hot_score.refresh.duration").timer();
        Timer warmupTimer = registry.find("blog.article.hot_cache.warmup.duration").timer();

        assertThat(refreshCounter).isNotNull();
        assertThat(refreshCounter.count()).isEqualTo(3.0);
        assertThat(warmupCounter).isNotNull();
        assertThat(warmupCounter.count()).isEqualTo(2.0);
        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1.0);
        assertThat(refreshTimer).isNotNull();
        assertThat(refreshTimer.count()).isEqualTo(1);
        assertThat(warmupTimer).isNotNull();
        assertThat(warmupTimer.count()).isEqualTo(1);
    }
}
