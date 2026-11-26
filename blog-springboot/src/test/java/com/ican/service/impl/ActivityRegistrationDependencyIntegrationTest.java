package com.ican.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ican.config.ActivityRegistrationConfig;
import com.ican.consumer.ActivityRegistrationConsumer;
import com.ican.entity.Activity;
import com.ican.mapper.ActivityMapper;
import com.ican.model.dto.ActivitySaveDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ican.config.ActivityRegistrationConfig.QUEUE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Explicit isolated schema/vhost only; the production application is never bootstrapped. */
@EnabledIfEnvironmentVariable(named = "BLOG_ACTIVITY_SCHEMA", matches = "blog_activity_[0-9a-f]{32}")
class ActivityRegistrationDependencyIntegrationTest {
    @Test
    void admissionDelayedDeliveryCapacityCancellationAndRecoveryUseRealTransactionsAndRabbit() throws Exception {
        String schema = System.getenv("BLOG_ACTIVITY_SCHEMA");
        String url = System.getenv("BLOG_ACTIVITY_JDBC_URL");
        assertEquals(schema, java.net.URI.create(url.substring(5)).getPath().substring(1));
        // This target accepts selecting a newly-created catalog after authentication; initial-catalog login timed out.
        var dataSource = new DriverManagerDataSource(url.replace("/" + schema + "?", "/information_schema?"),
                System.getenv("BLOG_ACTIVITY_DB_USER"), System.getenv("BLOG_ACTIVITY_DB_PASSWORD")) {
            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                var connection = super.getConnection();
                try { connection.setCatalog(schema); return connection; }
                catch (java.sql.SQLException error) { connection.close(); throw error; }
            }
        };
        var jdbc = new JdbcTemplate(dataSource);
        assertEquals(schema, jdbc.queryForObject("SELECT DATABASE()", String.class));
        var manager = new DataSourceTransactionManager(dataSource);
        var transactions = new TransactionTemplate(manager);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/ActivityMapper.xml"));
        ActivityMapper mapper = new SqlSessionTemplate(factory.getObject()).getMapper(ActivityMapper.class);
        // Verify the same ISO Beijing input under UTC and +08 sessions, with actual JDBC LocalDateTime mapping.
        for (String timezone : new String[]{"+00:00", "+08:00"}) {
            transactions.executeWithoutResult(status -> {
                jdbc.execute("SET SESSION time_zone='" + timezone + "'");
                var activity = new Activity();
                activity.setOwnerId(1); activity.setTitle("Synthetic clock"); activity.setDescription("Synthetic body");
                activity.setCapacity(1);
                activity.setStartsAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusMinutes(2).withNano(0));
                activity.setEndsAt(activity.getStartsAt().plusMinutes(5));
                assertEquals(1, mapper.insertActivity(activity));
                Activity stored = mapper.selectActivity(activity.getId());
                assertEquals(activity.getStartsAt().withNano(activity.getStartsAt().getNano() / 1000 * 1000), stored.getStartsAt());
                assertTrue(Duration.between(LocalDateTime.now(ZoneId.of("Asia/Shanghai")), stored.getCreateTime()).abs().toSeconds() < 10);
                assertEquals(1, mapper.publish(activity.getId()));
                assertEquals(0, mapper.reserveAdmission(activity.getId())); // Two minutes early, not eight hours early/late.
                jdbc.update("UPDATE t_activity SET starts_at=TIMESTAMPADD(MINUTE,-1,TIMESTAMPADD(HOUR,8,UTC_TIMESTAMP(6))) WHERE id=?", activity.getId());
                assertEquals(1, mapper.reserveAdmission(activity.getId()));
                status.setRollbackOnly();
            });
        }
        var connection = new CachingConnectionFactory(System.getenv("BLOG_ACTIVITY_MQ_HOST"),
                Integer.parseInt(System.getenv("BLOG_ACTIVITY_MQ_PORT")));
        connection.setUsername(System.getenv("BLOG_ACTIVITY_MQ_USER"));
        connection.setPassword(System.getenv("BLOG_ACTIVITY_MQ_PASSWORD"));
        connection.setVirtualHost(schema);
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var converter = new Jackson2JsonMessageConverter();
        var metrics = new SimpleMeterRegistry();
        try {
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                    .withUserConfiguration(ActivityRegistrationConfig.class)
                    .withPropertyValues("blog.registration.enabled=true")
                    .withBean(org.springframework.amqp.rabbit.connection.ConnectionFactory.class, () -> connection)
                    .withBean(org.springframework.amqp.support.converter.MessageConverter.class, () -> converter)
                    .run(context -> {
                        assertNull(context.getStartupFailure());
                        var admin = new RabbitAdmin(connection);
                        admin.declareExchange(context.getBean("registrationExchange", org.springframework.amqp.core.Exchange.class));
                        admin.declareQueue(context.getBean("registrationQueue", org.springframework.amqp.core.Queue.class));
                        // Boot's RabbitAdmin may declare all bindings on first connection; explicitly remove this one.
                        admin.removeBinding(context.getBean("registrationBinding", org.springframework.amqp.core.Binding.class));
                        var executor = context.getBean("registrationDispatchExecutor", org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.class);
                        var dispatcher = new ActivityRegistrationDispatchService(mapper, connection, converter, executor, manager, metrics);
                        var service = transactional(new ActivityService(mapper, dispatcher), manager);
                        var execution = transactional(new ActivityRegistrationExecutionService(mapper, dispatcher), manager);
                        var save = new ActivitySaveDTO();
                        save.setTitle("Synthetic limited activity"); save.setDescription("No business users or models"); save.setCapacity(1);
                        save.setStartsAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")).plusMinutes(5).withNano(0)); save.setEndsAt(save.getStartsAt().plusMinutes(10));
                        int id = service.create(save, 1).getId(); service.publish(id);
                        jdbc.update("UPDATE t_activity SET starts_at=TIMESTAMPADD(MINUTE,-1,TIMESTAMPADD(HOUR,8,UTC_TIMESTAMP(6))) WHERE id=?", id);
                        String first = UUID.randomUUID().toString();
                        transactions.executeWithoutResult(status -> { service.submit(id, 9, first); status.setRollbackOnly(); });
                        assertNull(service.mine(id, 9));
                        assertEquals("PENDING", service.submit(id, 7, first).getStatus());
                        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                            var returned = metrics.find("blog.registration.events").tags("stage", "confirm", "outcome", "returned").counter();
                            assertNotNull(returned);
                            assertEquals(1.0, returned.count());
                        });
                        String token = mapper.findMine(id, 7).getDispatchToken();
                        assertNotNull(token);
                        assertEquals(first, service.submit(id, 7, UUID.randomUUID().toString()).getRequestId());
                        admin.declareBinding(context.getBean("registrationBinding", org.springframework.amqp.core.Binding.class));
                        due(jdbc, first); dispatcher.recoverPending();
                        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertEquals(1, messageCount(admin)));
                        // Simulate a queue delay longer than the next dispatch deadline before starting consumption.
                        due(jdbc, first); dispatcher.recoverPending();
                        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertEquals(2, messageCount(admin)));
                        assertEquals(token, mapper.findMine(id, 7).getDispatchToken());
                        var listenerFactory = context.getBean("registrationListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
                        var container = listenerFactory.createListenerContainer(); container.setQueueNames(QUEUE);
                        var deliveries = new AtomicInteger();
                        container.addAfterReceivePostProcessors(message -> { deliveries.incrementAndGet(); return message; });
                        var adapter = new MessageListenerAdapter(new ActivityRegistrationConsumer(execution), converter);
                        adapter.setDefaultListenerMethod("consume"); container.setMessageListener(adapter); container.afterPropertiesSet();
                        try {
                            container.start();
                            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                                assertEquals("CONFIRMED", service.mine(id, 7).getStatus()); assertEquals(2, deliveries.get());
                            });
                            String second = UUID.randomUUID().toString(); service.submit(id, 8, second);
                            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertEquals("SOLD_OUT", service.mine(id, 8).getStatus()));
                        } finally { container.stop(); }
                        assertEquals(0, mapper.selectActivity(id).getRemaining()); assertEquals(0, mapper.selectActivity(id).getPendingCount());
                        execution.cancel(id, 7); execution.cancel(id, 7);
                        assertEquals(1, mapper.selectActivity(id).getRemaining());
                        assertEquals("CANCELED", service.submit(id, 7, UUID.randomUUID().toString()).getStatus());

                        // Independent request: terminal SQL failure rolls capacity and pending budget back together.
                        var quiet = mock(ActivityRegistrationDispatchService.class);
                        var direct = transactional(new ActivityService(mapper, quiet), manager);
                        String failed = UUID.randomUUID().toString(); direct.submit(id, 10, failed);
                        assertEquals(1, mapper.reserveDispatch(failed, "failure-token"));
                        jdbc.execute("CREATE TRIGGER reject_activity_terminal BEFORE UPDATE ON t_activity_registration FOR EACH ROW "
                                + "BEGIN IF NEW.status='CONFIRMED' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic terminal failure'; END IF; END");
                        try { assertThrows(Exception.class, () -> execution.process(failed, "failure-token")); }
                        finally { jdbc.execute("DROP TRIGGER reject_activity_terminal"); }
                        assertEquals(1, mapper.selectActivity(id).getRemaining()); assertEquals(1, mapper.selectActivity(id).getPendingCount());
                        for (int i = 0; i < 3; i++) {
                            String generation = "failure-" + i;
                            if (i > 0) { due(jdbc, failed); assertEquals(1, mapper.reserveDispatch(failed, generation)); }
                            else { generation = "failure-token"; }
                            execution.recordFailure(failed, generation);
                        }
                        assertEquals("FAILED", direct.mine(id, 10).getStatus()); assertEquals(0, mapper.selectActivity(id).getPendingCount());
                        execution.retry(failed);
                        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertNotNull(mapper.findMine(id, 10).getDispatchToken()));
                        String current = mapper.findMine(id, 10).getDispatchToken();
                        execution.process(failed, "failure-token"); assertEquals("PENDING", direct.mine(id, 10).getStatus());
                        execution.process(failed, current); assertEquals("CONFIRMED", direct.mine(id, 10).getStatus());
                        assertEquals(failed, direct.mine(id, 10).getRequestId());
                        assertEquals(0, mapper.selectActivity(id).getRemaining()); assertEquals(0, mapper.selectActivity(id).getPendingCount());
                    });
        } finally { connection.destroy(); }
    }
    private static void due(JdbcTemplate jdbc, String id) {
        jdbc.update("UPDATE t_activity_registration SET next_dispatch_at=TIMESTAMPADD(SECOND,-1,TIMESTAMPADD(HOUR,8,UTC_TIMESTAMP(6))) WHERE request_id=?", id);
    }
    private static int messageCount(RabbitAdmin admin) {
        return (Integer) admin.getQueueProperties(QUEUE).get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
    }
    @SuppressWarnings("unchecked")
    private static <T> T transactional(T target, DataSourceTransactionManager manager) {
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return (T) proxy.getProxy();
    }
}
