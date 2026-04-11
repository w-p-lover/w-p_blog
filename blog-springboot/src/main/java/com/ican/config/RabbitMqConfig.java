package com.ican.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ican.constant.MqConstant.*;

/**
 * RabbitMQ配置
 *
 * @author xcs
 **/
@Slf4j
@Configuration
public class RabbitMqConfig {

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 邮件交换机
     */
    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE, true, false);
    }

    /**
     * 邮件Simple队列
     */
    @Bean
    public Queue emailSimpleQueue() {
        return new Queue(EMAIL_SIMPLE_QUEUE, true);
    }

    /**
     * 邮件Html队列
     */
    @Bean
    public Queue emailHtmlQueue() {
        return new Queue(EMAIL_HTML_QUEUE, true);
    }

    /**
     * 绑定邮件Simple队列
     */
    @Bean
    public Binding simpleQueueBinding() {
        return BindingBuilder.bind(emailSimpleQueue()).to(emailExchange()).with(EMAIL_SIMPLE_KEY);
    }

    /**
     * 绑定邮件Html队列
     */
    @Bean
    public Binding htmlQueueBinding() {
        return BindingBuilder.bind(emailHtmlQueue()).to(emailExchange()).with(EMAIL_HTML_KEY);
    }

    /**
     * 文章交换机
     */
    @Bean
    public TopicExchange articleExchange() {
        return new TopicExchange(ARTICLE_EXCHANGE, true, false);
    }

    /**
     * 文章队列
     */
    @Bean
    public Queue articleQueue() {
        return new Queue(ARTICLE_QUEUE, true);
    }

    /**
     * 绑定文章队列
     */
    @Bean
    public Binding articleQueueBinding() {
        return BindingBuilder.bind(articleQueue()).to(articleExchange()).with(ARTICLE_KEY);
    }

    /**
     * 文章AI交换机
     */
    @Bean
    public TopicExchange articleAiExchange() {
        return new TopicExchange(ARTICLE_AI_EXCHANGE, true, false);
    }

    /**
     * 文章AI死信交换机
     */
    @Bean
    public TopicExchange articleAiDeadLetterExchange() {
        return new TopicExchange(ARTICLE_AI_DLX, true, false);
    }

    /**
     * 文章AI队列
     */
    @Bean
    public Queue articleAiQueue() {
        return QueueBuilder.durable(ARTICLE_AI_QUEUE)
                .withArgument("x-dead-letter-exchange", ARTICLE_AI_DLX)
                .withArgument("x-dead-letter-routing-key", ARTICLE_AI_DLQ_KEY)
                .build();
    }

    /**
     * 文章AI死信队列
     */
    @Bean
    public Queue articleAiDeadLetterQueue() {
        return QueueBuilder.durable(ARTICLE_AI_DLQ).build();
    }

    /**
     * 绑定文章AI队列
     */
    @Bean
    public Binding articleAiQueueBinding() {
        return BindingBuilder.bind(articleAiQueue()).to(articleAiExchange()).with(ARTICLE_AI_KEY);
    }

    /**
     * 绑定文章AI死信队列
     */
    @Bean
    public Binding articleAiDeadLetterQueueBinding() {
        return BindingBuilder.bind(articleAiDeadLetterQueue()).to(articleAiDeadLetterExchange()).with(ARTICLE_AI_DLQ_KEY);
    }
}