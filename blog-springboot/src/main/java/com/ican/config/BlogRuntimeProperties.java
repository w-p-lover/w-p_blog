package com.ican.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "blog")
public class BlogRuntimeProperties {

    private Site site = new Site();
    private Security security = new Security();
    private Satoken satoken = new Satoken();

    @Data
    public static class Site {
        private String baseUrl;
        private String frontendUrl;
        private String adminUrl;
        private String apiDocUrl;
    }

    @Data
    public static class Security {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> websocketAllowedOrigins = new ArrayList<>();
    }

    @Data
    public static class Satoken {
        private long renewThresholdSeconds = 600;
        private long renewTimeoutSeconds = 1800;
    }
}
