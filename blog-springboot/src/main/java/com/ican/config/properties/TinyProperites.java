package com.ican.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author：yep
 * @Project：blog
 * @name：TinifyProperites
 * @Date：2024/10/22 14:40
 * @Filename：TinifyProperites
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "photo.apikey")
public class TinyProperites {
    /**
     * API密钥
     */
    private String key;

}
