package com.ican.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.config.ArticleAiQueueConfig;
import com.ican.consumer.AiArticleConsumer;
import com.ican.entity.AiTask;
import com.ican.handler.MyMetaObjectHandler;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.service.AiArticleService;
import com.ican.service.AiTaskService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.ican.constant.MqConstant.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Opt-in only, provisioned/removed by verify_queue_dependencies.py; no application bootstrap. */
@EnabledIfEnvironmentVariable(named = "BLOG_Q4_SCHEMA", matches = "blog_q4_[0-9a-f]{32}")
class AiTaskDependencyIntegrationTest {

    @Test
    void actualSpringTransactionsDispatchAndDedicatedConsumerUseOnlySyntheticResources() throws Exception {
        String schema = System.getenv("BLOG_Q4_SCHEMA");
        String url = System.getenv("BLOG_Q4_JDBC_URL");
        assertEquals(schema, java.net.URI.create(url.substring(5)).getPath().substring(1));
        var dataSource = new DriverManagerDataSource(url, System.getenv("BLOG_Q4_DB_USER"),
                System.getenv("BLOG_Q4_DB_PASSWORD"));
        var jdbc = new JdbcTemplate(dataSource);
        assertEquals(schema, jdbc.queryForObject("SELECT DATABASE()", String.class));
        var manager = new DataSourceTransactionManager(dataSource);
        var transactions = new TransactionTemplate(manager);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setTablePrefix("t_"))
                .setMetaObjectHandler(new MyMetaObjectHandler()));
        factory.setMapperLocations(new ClassPathResource("mapper/AiTaskMapper.xml"),
                new ClassPathResource("mapper/ArticleMapper.xml"));
        var sessions = new SqlSessionTemplate(factory.getObject());
        AiTaskMapper tasks = sessions.getMapper(AiTaskMapper.class);
        ArticleMapper articles = sessions.getMapper(ArticleMapper.class);
        var json = new ObjectMapper();
        var registry = new SimpleMeterRegistry();
        var metrics = new BlogMetrics(registry);
        var coordinator = transactional(new AiTaskExecutionService(tasks, articles, json, metrics), manager);

        // Inject failure at the real terminal SQL, after article UPDATE has executed.
        jdbc.update("UPDATE t_ai_task SET status='CANCELED'"); // Prior Python scenarios in this isolated schema only.
        jdbc.update("UPDATE t_article SET ai_summary=NULL,indexed_source_version=NULL WHERE id=1");
        jdbc.update("INSERT INTO t_ai_task(id,biz_type,biz_id,task_type,status,source_version,generated_payload) "
                        + "VALUES(501,'ARTICLE',1,'ARTICLE_SUMMARY_TAG_VECTOR','PENDING','source-v1',?)",
                "{\"sourceVersion\":\"source-v1\",\"summary\":\"Java synthetic\",\"tags\":\"synthetic\"}");
        assertEquals(1, tasks.claimPendingTask(501, "java-rollback"));
        AiTask execution = tasks.selectById(501);
        jdbc.execute("CREATE TRIGGER q4_fail_terminal BEFORE UPDATE ON t_ai_task FOR EACH ROW "
                + "BEGIN IF NEW.id=501 AND NEW.status='SUCCESS' THEN "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Q4 controlled terminal failure'; END IF; END");
        try {
            assertThrows(Exception.class, () -> coordinator.finish(execution, true));
            assertNull(jdbc.queryForObject("SELECT ai_summary FROM t_article WHERE id=1", String.class));
            assertEquals("RUNNING", tasks.selectById(501).getStatus());
        } finally {
            jdbc.execute("DROP TRIGGER q4_fail_terminal");
        }
        coordinator.finish(execution, true);
        assertEquals("SUCCESS", tasks.selectById(501).getStatus());
        assertEquals("Java synthetic", jdbc.queryForObject("SELECT ai_summary FROM t_article WHERE id=1", String.class));

        var connection = new CachingConnectionFactory(System.getenv("BLOG_Q4_MQ_HOST"),
                Integer.parseInt(System.getenv("BLOG_Q4_MQ_PORT")));
        connection.setUsername(System.getenv("BLOG_Q4_MQ_USER"));
        connection.setPassword(System.getenv("BLOG_Q4_MQ_PASSWORD"));
        connection.setVirtualHost(schema);
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var converter = new Jackson2JsonMessageConverter();
        var template = new RabbitTemplate(connection);
        template.setMessageConverter(converter);
        var admin = new RabbitAdmin(connection);
        var exchange = new DirectExchange(ARTICLE_AI_EXCHANGE);
        var queue = new Queue(ARTICLE_AI_QUEUE);
        AtomicInteger processing = new AtomicInteger();
        AiArticleService processor = mock(AiArticleService.class);
        when(processor.processArticle(any(), any())).thenAnswer(invocation -> {
            AiTask task = invocation.getArgument(0);
            processing.incrementAndGet();
            assertEquals(1, tasks.saveGeneratedPayload(task.getId(), task.getExecutionToken(),
                    "{\"sourceVersion\":\"source-v1\",\"summary\":\"consumer synthetic\",\"tags\":\"synthetic\"}"));
            return true; // No model or Embedding client is created.
        });
        var dispatcher = new AiTaskDispatchService(tasks, connection, converter, metrics, manager);
        AiTaskService service = transactional(new AiTaskServiceImpl(tasks, articles, processor, dispatcher, json, coordinator), manager);
        try {
            admin.declareExchange(exchange);
            admin.declareQueue(queue); // Intentionally no binding yet.
            AtomicReference<Integer> rolledBack = new AtomicReference<>();
            transactions.executeWithoutResult(status -> {
                rolledBack.set(service.createArticleTask(1, "Q4 synthetic", "Synthetic body"));
                status.setRollbackOnly();
            });
            assertNull(tasks.selectById(rolledBack.get()));
            assertNull(template.receive(ARTICLE_AI_QUEUE));

            Integer id = service.createArticleTask(1, "Q4 synthetic", "Synthetic body");
            await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> assertEquals(1.0,
                    registry.get("blog.ai.task.events").tags("stage", "confirm", "outcome", "returned").counter().count()));
            assertEquals("PENDING", tasks.selectById(id).getStatus());
            assertEquals(1, tasks.selectById(id).getDispatchAttempts());
            assertNull(template.receive(ARTICLE_AI_QUEUE));

            admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(ARTICLE_AI_KEY));
            // Force only the synthetic pending task's backoff due; invoke the real scan once.
            jdbc.update("UPDATE t_ai_task SET next_dispatch_at=TIMESTAMPADD(SECOND,-1,NOW(6)) WHERE id=?", id);
            dispatcher.recoverPending();
            assertEquals(2, tasks.selectById(id).getDispatchAttempts());
            // Duplicate the same identity before starting the dedicated AUTO listener.
            var duplicate = new com.ican.model.dto.ArticleAiMessage();
            duplicate.setTaskId(id);
            template.convertAndSend(ARTICLE_AI_EXCHANGE, ARTICLE_AI_KEY, duplicate);
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                    .withUserConfiguration(ArticleAiQueueConfig.class)
                    .withBean(org.springframework.amqp.rabbit.connection.ConnectionFactory.class, () -> connection)
                    .withBean(org.springframework.amqp.support.converter.MessageConverter.class, () -> converter)
                    .run(context -> {
                        assertNull(context.getStartupFailure());
                        var listenerFactory = context.getBean("articleAiListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
                        var container = listenerFactory.createListenerContainer();
                        container.setQueueNames(ARTICLE_AI_QUEUE);
                        AtomicInteger deliveries = new AtomicInteger();
                        container.addAfterReceivePostProcessors(message -> { deliveries.incrementAndGet(); return message; });
                        var adapter = new MessageListenerAdapter(new AiArticleConsumer(service), converter);
                        adapter.setDefaultListenerMethod("consume");
                        container.setMessageListener(adapter);
                        container.afterPropertiesSet();
                        try {
                            container.start();
                            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                                assertEquals("SUCCESS", tasks.selectById(id).getStatus());
                                assertEquals(1, processing.get());
                                assertEquals(2, deliveries.get());
                                var properties = admin.getQueueProperties(ARTICLE_AI_QUEUE);
                                assertNotNull(properties);
                                assertEquals(0, properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
                            });
                        } finally {
                            container.stop();
                        }
                    });
            assertEquals(1, processing.get());
            assertEquals("source-v1", jdbc.queryForObject("SELECT indexed_source_version FROM t_article WHERE id=1", String.class));
        } finally {
            connection.destroy();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T transactional(T target, DataSourceTransactionManager manager) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return (T) proxy.getProxy();
    }
}
