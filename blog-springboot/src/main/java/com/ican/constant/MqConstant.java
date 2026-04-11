package com.ican.constant;

/**
 * MQ常量
 *
 * @author xcs
 */
public class MqConstant {

    /**
     * 邮件交换机
     */
    public static final String EMAIL_EXCHANGE = "email.topic";

    /**
     * 邮件SIMPLE队列
     */
    public static final String EMAIL_SIMPLE_QUEUE = "email.simple.queue";

    /**
     * 邮件HTML队列
     */
    public static final String EMAIL_HTML_QUEUE = "email.html.queue";

    /**
     * 邮件Simple RoutingKey
     */
    public static final String EMAIL_SIMPLE_KEY = "email.simple.key";

    /**
     * 邮件Html RoutingKey
     */
    public static final String EMAIL_HTML_KEY = "email.html.key";

    /**
     * 文章交换机
     */
    public static final String ARTICLE_EXCHANGE = "article.topic";

    /**
     * 文章队列
     */
    public static final String ARTICLE_QUEUE = "article.queue";

    /**
     * 文章RoutingKey
     */
    public final static String ARTICLE_KEY = "article.key";

    /**
     * 文章AI交换机
     */
    public static final String ARTICLE_AI_EXCHANGE = "article.ai";

    /**
     * 文章AI队列
     */
    public static final String ARTICLE_AI_QUEUE = "article.ai.queue";

    /**
     * 文章AI RoutingKey
     */
    public static final String ARTICLE_AI_KEY = "article.ai.key";

    /**
     * 文章AI死信交换机
     */
    public static final String ARTICLE_AI_DLX = "article.ai.dlx";

    /**
     * 文章AI死信队列
     */
    public static final String ARTICLE_AI_DLQ = "article.ai.dlq";

    /**
     * 文章AI死信RoutingKey
     */
    public static final String ARTICLE_AI_DLQ_KEY = "article.ai.dlq.key";

}
