package com.ican.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cache.multilevel")
public class MultiLevelCacheProperties {

    private Local local = new Local();
    private Lock lock = new Lock();
    private int nullCacheTtlMinutes = 5;
    private int ttlJitterMaxMinutes = 5;
    private int maxConcurrentLoads = 8;

    @Data
    public static class Local {
        private long maxSize = 1000;
        private long expireMinutes = 5;
    }

    @Data
    public static class Lock {
        private long waitSeconds = 3;
        private long leaseSeconds = 10;
    }
}
