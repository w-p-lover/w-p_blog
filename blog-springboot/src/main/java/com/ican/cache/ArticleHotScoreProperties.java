package com.ican.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "article.hot-score")
public class ArticleHotScoreProperties {

    private int topLimit = 20;
    private int warmupLimit = 10;
    private int detailCacheTtlMinutes = 30;
    private Weights weights = new Weights();

    @Data
    public static class Weights {
        private double view = 1.0;
        private double like = 5.0;
        private double comment = 8.0;
        private double favorite = 10.0;
        private double recommend = 30.0;
        private double top = 50.0;
        private double timeDecayPerDay = 1.5;
    }
}
