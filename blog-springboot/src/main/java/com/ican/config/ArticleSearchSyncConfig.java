package com.ican.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecovererWithConfirms;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ican.constant.MqConstant.*;

@Slf4j
@Configuration
public class ArticleSearchSyncConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory articleSearchListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        // 专用于搜索同步，不改变邮件或 AI 消费者的重试与恢复行为。
        RabbitTemplate errorTemplate = new RabbitTemplate(connectionFactory);
        errorTemplate.setMandatory(true);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(3000, 1.0, 3000)
                .recoverer(searchSyncRecoverer(errorTemplate))
                .build());
        return factory;
    }

    MessageRecoverer searchSyncRecoverer(RabbitTemplate errorTemplate) {
        RepublishMessageRecovererWithConfirms recoverer = new RepublishMessageRecovererWithConfirms(
                errorTemplate, ARTICLE_SYNC_ERROR_EXCHANGE, ARTICLE_SYNC_ERROR_KEY,
                CachingConnectionFactory.ConfirmType.CORRELATED);
        recoverer.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        recoverer.setConfirmTimeout(10000);
        return (message, cause) -> {
            try {
                // 故障副本被 broker 确认且未退回之后，原消息才允许正常确认。
                recoverer.recover(message, cause);
                log.error("文章搜索同步重试耗尽，原始批次已转存至 {}", ARTICLE_SYNC_ERROR_QUEUE, cause);
            } catch (Exception e) {
                throw new ImmediateRequeueAmqpException("搜索同步故障转存失败，保留原消息重新投递", e);
            }
        };
    }
}
