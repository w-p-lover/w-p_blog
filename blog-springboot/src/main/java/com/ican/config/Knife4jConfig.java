package com.ican.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 文档配置（knife4j 4.x + springdoc-openapi）
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("博客系统 API 文档")
                        .description("基于 Spring Boot 3 + Vue 的前后端分离博客")
                        .version("2.0")
                        .contact(new Contact()
                                .name("w&p")
                                .url("https://github.com/ICAN1999")
                                .email("3169468598@qq.com")));
    }
}
