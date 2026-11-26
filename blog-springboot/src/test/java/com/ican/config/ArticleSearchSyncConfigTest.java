package com.ican.config;

import com.ican.consumer.ArticleConsumer;
import com.ican.service.ElasticsearchService;
import com.rabbitmq.client.Channel;
import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static com.ican.constant.MqConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleSearchSyncConfigTest {

    @Test
    void realRetryInterceptorRetriesThenQuarantinesTheOriginalBatch() {
        MessageRecoverer recovery = mock(MessageRecoverer.class);
        ElasticsearchService service = mock(ElasticsearchService.class);
        doThrow(new IllegalStateException("ES 持续故障")).when(service).syncArticle(11);
        Dispatch consumer = retryingConsumer(service, recovery);
        Message message = message();

        consumer.dispatch(null, message);

        verify(service, times(3)).syncArticle(11);
        verify(recovery).recover(same(message), any(Throwable.class));
    }

    @Test
    void realRetryInterceptorRecoversWithoutQuarantineAfterTransientFailure() {
        MessageRecoverer recovery = mock(MessageRecoverer.class);
        ElasticsearchService service = mock(ElasticsearchService.class);
        doThrow(new IllegalStateException("短暂故障")).doNothing().when(service).syncArticle(11);

        retryingConsumer(service, recovery).dispatch(null, message());

        verify(service, times(2)).syncArticle(11);
        verifyNoInteractions(recovery);
    }

    @Test
    void recoveryFailureEscapesTheRetryInterceptorForBrokerRequeue() {
        MessageRecoverer recovery = mock(MessageRecoverer.class);
        doThrow(new ImmediateRequeueAmqpException("转存失败")).when(recovery).recover(any(), any());
        ElasticsearchService service = mock(ElasticsearchService.class);
        doThrow(new IllegalStateException("ES 故障")).when(service).syncArticle(11);
        Dispatch consumer = retryingConsumer(service, recovery);

        assertThrows(ImmediateRequeueAmqpException.class, () -> consumer.dispatch(null, message()));

        verify(service, times(3)).syncArticle(11);
    }

    @Test
    void confirmedRecoveryPreservesBodyAndAddsFailureContext() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        Message message = message();
        byte[] originalBody = message.getBody().clone();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(eq(ARTICLE_SYNC_ERROR_EXCHANGE), eq(ARTICLE_SYNC_ERROR_KEY), same(message), any(CorrelationData.class));

        new ArticleSearchSyncConfig().searchSyncRecoverer(template)
                .recover(message, new IllegalStateException("同步失败"));

        assertArrayEquals(originalBody, message.getBody());
        assertTrue(message.getMessageProperties().getHeaders().containsKey("x-exception-message"));
        assertEquals(ARTICLE_EXCHANGE, message.getMessageProperties().getHeader("x-original-exchange"));
        verify(template).send(eq(ARTICLE_SYNC_ERROR_EXCHANGE), eq(ARTICLE_SYNC_ERROR_KEY), same(message), any(CorrelationData.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nack", "returned", "connection"})
    void unsuccessfulQuarantineRequeuesOriginalInsteadOfDiscardingIt(String failure) {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            if ("connection".equals(failure)) {
                throw new IllegalStateException("broker 不可用");
            }
            CorrelationData correlation = invocation.getArgument(3);
            if ("returned".equals(failure)) {
                correlation.setReturned(new ReturnedMessage(message(), 312, "NO_ROUTE",
                        ARTICLE_SYNC_ERROR_EXCHANGE, ARTICLE_SYNC_ERROR_KEY));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(!"nack".equals(failure), "test"));
            return null;
        }).when(template).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        assertThrows(ImmediateRequeueAmqpException.class,
                () -> new ArticleSearchSyncConfig().searchSyncRecoverer(template)
                        .recover(message(), new IllegalStateException("ES 故障")));
    }

    private Dispatch retryingConsumer(ElasticsearchService service, MessageRecoverer recovery) {
        ArticleSearchSyncConfig config = spy(new ArticleSearchSyncConfig());
        doReturn(recovery).when(config).searchSyncRecoverer(any(RabbitTemplate.class));
        SimpleRabbitListenerContainerFactory factory = config.articleSearchListenerContainerFactory(
                mock(SimpleRabbitListenerContainerFactoryConfigurer.class), mock(ConnectionFactory.class));
        Advice[] advice = (Advice[]) ReflectionTestUtils.getField(factory, "adviceChain");
        assertNotNull(advice);
        // Spring AMQP 在容器的 Channel/Message 调用边界应用重试。
        ArticleConsumer consumer = new ArticleConsumer(service);
        Dispatch target = (channel, message) -> consumer.listenSaveArticle(message);
        ProxyFactory proxy = new ProxyFactory(target);
        for (Advice item : advice) {
            proxy.addAdvice(item);
        }
        return (Dispatch) proxy.getProxy();
    }

    interface Dispatch {
        void dispatch(Channel channel, Message message);
    }

    private Message message() {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedExchange(ARTICLE_EXCHANGE);
        properties.setReceivedRoutingKey(ARTICLE_KEY);
        return new Message(("{\"isDdl\":false,\"table\":\"t_article\",\"type\":\"UPDATE\","
                + "\"data\":[{\"id\":11}]}").getBytes(StandardCharsets.UTF_8), properties);
    }
}
