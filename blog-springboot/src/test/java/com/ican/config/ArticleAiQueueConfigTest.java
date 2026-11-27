package com.ican.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ArticleAiQueueConfigTest {

    @Test
    void aiFactoryOverridesGlobalRetryWithoutReplacingTheSharedTemplate() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=ai")
                .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(RabbitMqConfig.class, ArticleAiQueueConfig.class)
                .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class))
                .withPropertyValues("spring.rabbitmq.listener.simple.retry.enabled=true",
                        "spring.rabbitmq.listener.simple.acknowledge-mode=manual",
                        "spring.rabbitmq.listener.simple.prefetch=20",
                        "spring.rabbitmq.listener.simple.concurrency=2",
                        "spring.rabbitmq.listener.simple.max-concurrency=4")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(RabbitTemplate.class).size());
                    assertNotNull(context.getBean("rabbitTemplate", RabbitTemplate.class));
                    var factory = context.getBean("articleAiListenerContainerFactory",
                            SimpleRabbitListenerContainerFactory.class);
                    var container = factory.createListenerContainer();
                    assertEquals(AcknowledgeMode.AUTO, container.getAcknowledgeMode());
                    assertEquals(1, ReflectionTestUtils.getField(container, "prefetchCount"));
                    assertEquals(1, ReflectionTestUtils.getField(container, "concurrentConsumers"));
                    assertEquals(1, ReflectionTestUtils.getField(container, "maxConcurrentConsumers"));
                    assertEquals(0, ((Object[]) ReflectionTestUtils.getField(container, "adviceChain")).length);
                });
    }
}
