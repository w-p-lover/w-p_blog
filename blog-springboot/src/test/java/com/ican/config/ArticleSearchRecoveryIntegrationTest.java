package com.ican.config;

import com.ican.consumer.ArticleConsumer;
import com.ican.service.ElasticsearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ican.constant.MqConstant.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Uses only a newly provisioned vhost, synthetic batches and a controlled ES boundary. */
@EnabledIfEnvironmentVariable(named = "BLOG_SEARCH_REPLAY_TEST_VHOST", matches = "blog_search_[0-9a-f]{32}")
class ArticleSearchRecoveryIntegrationTest {

    @Test
    void quarantineFailureRepairReplayAndCurrentStateUseTheActualSearchConsumer() throws Exception {
        String vhost = System.getenv("BLOG_SEARCH_REPLAY_TEST_VHOST");
        assertEquals(vhost, System.getenv("BLOG_RABBITMQ_VIRTUAL_HOST"));
        var connection = new CachingConnectionFactory(System.getenv("BLOG_RABBITMQ_HOST"),
                Integer.parseInt(System.getenv("BLOG_RABBITMQ_PORT")));
        connection.setUsername(System.getenv("BLOG_RABBITMQ_USERNAME"));
        connection.setPassword(System.getenv("BLOG_RABBITMQ_PASSWORD"));
        connection.setVirtualHost(vhost);
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        var admin = new RabbitAdmin(connection);
        var template = new RabbitTemplate(connection);
        template.setMandatory(true);
        var topology = new RabbitMqConfig();
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger deliveries = new AtomicInteger();
        Map<Integer, String> current = new ConcurrentHashMap<>(Map.of(11, "new title", 13, "current body"));
        Map<Integer, String> index = new ConcurrentHashMap<>();
        index.put(12, "withdrawn old body");
        ElasticsearchService service = mock(ElasticsearchService.class);
        doAnswer(invocation -> {
            int id = invocation.getArgument(0);
            if (id == 12) {
                attempts.incrementAndGet();
                if (fail.get()) {
                    throw new IllegalStateException("Controlled index failure at the second article");
                }
            }
            // A controlled DB/ES boundary, not an actual MySQL/Elasticsearch integration.
            if (current.containsKey(id)) {
                index.put(id, current.get(id));
            } else {
                index.remove(id);
            }
            return null;
        }).when(service).syncArticle(anyInt());
        byte[] body = ("{\"isDdl\":false,\"table\":\"t_article\",\"type\":\"UPDATE\","
                + "\"data\":[{\"id\":11,\"article_title\":\"obsolete\"},{\"id\":12},{\"id\":13}]}")
                .getBytes(StandardCharsets.UTF_8);
        try {
            admin.declareExchange(topology.articleExchange());
            admin.declareQueue(topology.articleQueue());
            admin.declareBinding(topology.articleQueueBinding());
            admin.declareExchange(topology.articleSyncErrorExchange());
            admin.declareQueue(topology.articleSyncErrorQueue());
            // Intentionally omit the failure binding: exhausted retries must not discard the source.
            template.send(ARTICLE_EXCHANGE, ARTICLE_KEY, message(body));
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                    .withUserConfiguration(ArticleSearchSyncConfig.class)
                    .withBean(org.springframework.amqp.rabbit.connection.ConnectionFactory.class, () -> connection)
                    .run(context -> {
                        assertNull(context.getStartupFailure());
                        var factory = context.getBean("articleSearchListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
                        var container = factory.createListenerContainer();
                        container.setQueueNames(ARTICLE_QUEUE);
                        container.setPrefetchCount(1);
                        container.setShutdownTimeout(15000);
                        container.addAfterReceivePostProcessors(message -> { deliveries.incrementAndGet(); return message; });
                        container.setMessageListener((org.springframework.amqp.core.MessageListener)
                                new ArticleConsumer(service)::listenSaveArticle);
                        container.afterPropertiesSet();
                        try {
                            container.start();
                            await().atMost(Duration.ofSeconds(20)).until(() -> deliveries.get() >= 2);
                            container.stop();
                            assertTrue(attempts.get() >= 3);
                            assertEquals(1, ready(admin, ARTICLE_QUEUE));
                            assertEquals(0, ready(admin, ARTICLE_SYNC_ERROR_QUEUE));
                            verify(service, never()).syncArticle(13);

                            admin.declareBinding(topology.articleSyncErrorBinding());
                            int before = attempts.get();
                            container.start();
                            await().atMost(Duration.ofSeconds(15)).until(() -> ready(admin, ARTICLE_SYNC_ERROR_QUEUE) == 1);
                            container.stop();
                            assertEquals(3, attempts.get() - before);
                            assertEquals(0, ready(admin, ARTICLE_QUEUE));

                            assertEquals(0, replay(false)); // Passive count only.
                            assertEquals(1, ready(admin, ARTICLE_SYNC_ERROR_QUEUE));
                            admin.removeBinding(topology.articleQueueBinding());
                            assertEquals(1, replay(true)); // Mandatory return: keep the source.
                            assertEquals(1, ready(admin, ARTICLE_SYNC_ERROR_QUEUE));
                            assertEquals(0, ready(admin, ARTICLE_QUEUE));
                            admin.declareBinding(topology.articleQueueBinding());
                            assertEquals(0, replay(true));
                            assertEquals(0, ready(admin, ARTICLE_SYNC_ERROR_QUEUE));
                            assertEquals(1, ready(admin, ARTICLE_QUEUE));
                            Message replayed = template.receive(ARTICLE_QUEUE, 5000);
                            assertNotNull(replayed);
                            assertArrayEquals(body, replayed.getBody());
                            assertEquals(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT,
                                    replayed.getMessageProperties().getReceivedDeliveryMode());
                            assertNotNull(replayed.getMessageProperties().getHeader("x-exception-message"));
                            template.send(ARTICLE_EXCHANGE, ARTICLE_KEY, replayed);
                            template.send(ARTICLE_EXCHANGE, ARTICLE_KEY, replayed); // Delivery/ACK outcome may be unknown.
                            fail.set(false);
                            int previousDeliveries = deliveries.get();
                            container.start();
                            await().atMost(Duration.ofSeconds(10)).until(() -> deliveries.get() == previousDeliveries + 2
                                    && attempts.get() == before + 5 && "current body".equals(index.get(13)));
                            container.stop();
                            assertEquals(current, index); // No obsolete body resurrection; withdrawn id 12 is absent.
                            assertEquals(0, ready(admin, ARTICLE_SYNC_ERROR_QUEUE));

                            byte[] invalid = "{\"table\":\"t_article\",\"type\":\"UPDATE\",\"data\":[{\"id\":11},{}]}"
                                    .getBytes(StandardCharsets.UTF_8);
                            template.send(ARTICLE_SYNC_ERROR_EXCHANGE, ARTICLE_SYNC_ERROR_KEY, message(invalid));
                            assertEquals(1, replay(true));
                            assertEquals(1, ready(admin, ARTICLE_SYNC_ERROR_QUEUE));
                            assertEquals(0, ready(admin, ARTICLE_QUEUE));
                            assertArrayEquals(invalid, template.receive(ARTICLE_SYNC_ERROR_QUEUE, 5000).getBody());
                        } finally {
                            container.stop();
                        }
                    });
        } finally {
            connection.destroy();
        }
    }

    private int replay(boolean execute) throws Exception {
        var command = new java.util.ArrayList<>(java.util.List.of(System.getenv("BLOG_SEARCH_REPLAY_PYTHON"),
                Path.of("docs/09-tooling/replay_article_search.py").toString(),
                "--expect-host", System.getenv("BLOG_RABBITMQ_HOST"),
                "--expect-vhost", System.getenv("BLOG_SEARCH_REPLAY_TEST_VHOST"), "--limit", "1"));
        if (execute) {
            command.add("--execute");
        }
        Process process = new ProcessBuilder(command).inheritIO().start();
        if (!process.waitFor(135, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor();
            fail("Replay client exceeded the integration check deadline");
        }
        return process.exitValue();
    }

    private static int ready(RabbitAdmin admin, String queue) {
        var properties = admin.getQueueProperties(queue);
        assertNotNull(properties);
        return (Integer) properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
    }

    private static Message message(byte[] body) {
        var properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        return new Message(body, properties);
    }
}
