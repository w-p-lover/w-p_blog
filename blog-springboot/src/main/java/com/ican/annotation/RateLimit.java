package com.ican.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 基于 Redis + Lua 脚本实现滑动窗口限流
 *
 * 使用示例：
 * @RateLimit(key = "api:article:view", limit = 100, period = 60)
 * public void viewArticle(Integer articleId) { ... }
 *
 * @author ican
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key（支持 SpEL 表达式）
     * 例如：api:article:#{#articleId}
     */
    String key();

    /**
     * 时间窗口内允许的最大请求数
     */
    int limit() default 100;

    /**
     * 时间窗口大小（秒）
     */
    int period() default 60;

    /**
     * 限流失败时的提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
