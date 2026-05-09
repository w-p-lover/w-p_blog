package com.ican.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleHotScorePropertiesTest {

    @Test
    void defaults_shouldMatchPhase2HotScoreModel() {
        ArticleHotScoreProperties properties = new ArticleHotScoreProperties();

        assertThat(properties.getTopLimit()).isEqualTo(20);
        assertThat(properties.getWarmupLimit()).isEqualTo(10);
        assertThat(properties.getDetailCacheTtlMinutes()).isEqualTo(30);
        assertThat(properties.getWeights().getView()).isEqualTo(1.0);
        assertThat(properties.getWeights().getLike()).isEqualTo(5.0);
        assertThat(properties.getWeights().getComment()).isEqualTo(8.0);
        assertThat(properties.getWeights().getFavorite()).isEqualTo(10.0);
        assertThat(properties.getWeights().getRecommend()).isEqualTo(30.0);
        assertThat(properties.getWeights().getTop()).isEqualTo(50.0);
        assertThat(properties.getWeights().getTimeDecayPerDay()).isEqualTo(1.5);
    }
}
