package com.ican.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/** 迁移/权限/配置核查后再显式开启；默认不声明新拓扑、不启动新扫描或消费者。 */
@Configuration
@ConditionalOnProperty(prefix = "blog.registration", name = "enabled", havingValue = "true")
public class ActivityRegistrationConfig {
    public static final String EXCHANGE = "activity.registration";
    public static final String QUEUE = "activity.registration.queue";
    public static final String KEY = "activity.registration.key";
    public static final String ERROR_EXCHANGE = "activity.registration.error";
    public static final String ERROR_QUEUE = "activity.registration.error.queue";

    @Bean
    public DirectExchange registrationExchange() { return new DirectExchange(EXCHANGE); }
    @Bean
    public DirectExchange registrationErrorExchange() { return new DirectExchange(ERROR_EXCHANGE); }
    @Bean
    public Queue registrationQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(ERROR_EXCHANGE).deadLetterRoutingKey(KEY).build();
    }
    @Bean
    public Queue registrationErrorQueue() { return QueueBuilder.durable(ERROR_QUEUE).build(); }
    @Bean
    public Binding registrationBinding() { return BindingBuilder.bind(registrationQueue()).to(registrationExchange()).with(KEY); }
    @Bean
    public Binding registrationErrorBinding() {
        return BindingBuilder.bind(registrationErrorQueue()).to(registrationErrorExchange()).with(KEY);
    }
    @Bean
    public SimpleRabbitListenerContainerFactory registrationListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connection) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connection);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setAdviceChain();
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        return factory;
    }
    @Bean
    public ThreadPoolTaskExecutor registrationDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("registration-dispatch-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
