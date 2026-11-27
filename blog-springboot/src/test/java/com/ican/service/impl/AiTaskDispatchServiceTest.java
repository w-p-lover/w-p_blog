package com.ican.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.AiTask;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.dto.ArticleAiMessage;
import com.ican.service.AiArticleService;
import com.ican.service.AiTaskService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.ican.constant.MqConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiTaskDispatchServiceTest {

    private final AiTaskMapper mapper = mock(AiTaskMapper.class);
    private final RabbitTemplate template = mock(RabbitTemplate.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final TestTransactions transactions = new TestTransactions();
    private AiTaskDispatchService dispatcher;
    private AiTaskService service;
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final AiArticleService processor = mock(AiArticleService.class);
    private final AtomicReference<CorrelationData> lastCorrelation = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        when(articles.selectById(7)).thenReturn(com.ican.entity.Article.builder().id(7)
                .sourceVersion("source-1").isDelete(0).status(1).articleTitle("title").articleContent("body").build());
        dispatcher = new AiTaskDispatchService(mapper, template, new BlogMetrics(registry), transactions);
        var target = new AiTaskServiceImpl(mapper, articles, processor,
                dispatcher, new ObjectMapper(), mock(AiTaskExecutionService.class));
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (AiTaskService) factory.getProxy();
        when(mapper.insert(any(AiTask.class))).thenAnswer(call -> {
            AiTask task = call.getArgument(0);
            task.setId(12);
            return 1;
        });
        when(mapper.reserveDispatch(12)).thenReturn(1);
        doAnswer(call -> {
            assertTrue(transactions.commits.get() >= 1, "reservation must commit before send");
            assertTrue(java.util.Set.of(12, 13).contains(((ArticleAiMessage) call.getArgument(2)).getTaskId()));
            Message raw = new Message(new byte[0], new MessageProperties());
            MessagePostProcessor processor = call.getArgument(3);
            assertEquals(MessageDeliveryMode.PERSISTENT, processor.postProcessMessage(raw)
                    .getMessageProperties().getDeliveryMode());
            lastCorrelation.set(call.getArgument(4));
            return null;
        }).when(template).convertAndSend(eq(ARTICLE_AI_EXCHANGE), eq(ARTICLE_AI_KEY),
                any(ArticleAiMessage.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void realTransactionProxyDefersWakeUntilCommitAndReservationCommitsIndependently() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertEquals(12, service.createArticleTask(7, "title", "body"));
            verify(mapper, never()).reserveDispatch(anyInt());
            assertNull(lastCorrelation.get());
        });
        assertEquals(2, transactions.commits.get());
        verify(mapper).reserveDispatch(12);
        lastCorrelation.get().getFuture().complete(new CorrelationData.Confirm(true, null));
        assertEquals(1, count("confirm", "ack"));
        verify(template).setMandatory(true);
    }

    @Test
    void rollbackNeverReservesOrSendsAndDirectCreateHasItsOwnTransaction() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.createArticleTask(7, "title", "body");
            status.setRollbackOnly();
        });
        verify(mapper, never()).reserveDispatch(anyInt());
        assertNull(lastCorrelation.get());
        assertEquals(1, transactions.rollbacks.get());
        service.createArticleTask(7, "title", "body");
        assertEquals(2, transactions.commits.get());
        assertNotNull(lastCorrelation.get());
    }

    @Test
    void sendFailureAfterCommitDoesNotReportArticleRollbackAndPendingCanWakeAgain() {
        doThrow(new IllegalStateException("controlled broker failure"))
                .doAnswer(call -> { lastCorrelation.set(call.getArgument(4)); return null; })
                .when(template).convertAndSend(anyString(), anyString(), any(ArticleAiMessage.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));
        assertEquals(12, service.createArticleTask(7, "title", "body"));
        assertEquals(2, transactions.commits.get());
        assertEquals(1, count("publish", "error"));
        assertEquals(1, count("confirm", "unknown"));
        when(mapper.selectDuePendingTasks(100)).thenReturn(List.of(12));
        dispatcher.recoverPending();
        assertNotNull(lastCorrelation.get());
        assertEquals(1, count("publish", "sent"));
        verify(mapper, times(2)).reserveDispatch(12);
    }

    @Test
    void confirmAckDoesNotRemovePendingAndReservedButUnsentWorkRecoversWhenDue() {
        // 0模拟被另一实例预约、尚未到期；后续1模拟其预约后崩溃且时间已到。
        when(mapper.selectDuePendingTasks(100)).thenReturn(List.of(12));
        when(mapper.reserveDispatch(12)).thenReturn(0, 1, 1);
        dispatcher.recoverPending();
        assertNull(lastCorrelation.get());
        dispatcher.recoverPending();
        lastCorrelation.get().getFuture().complete(new CorrelationData.Confirm(true, null));
        dispatcher.recoverPending();
        verify(mapper, times(3)).reserveDispatch(12);
        assertEquals(2, count("publish", "sent"));
        assertEquals(1, count("confirm", "ack"));
        verify(mapper, never()).updateById(any(AiTask.class));
    }

    @Test
    void returnedNackAndUnknownHaveSeparateOutcomesAndDoNotChangeTaskState() {
        dispatcher.wakeAfterCommit(12);
        CorrelationData returned = lastCorrelation.get();
        returned.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()),
                312, "NO_ROUTE", ARTICLE_AI_EXCHANGE, ARTICLE_AI_KEY));
        returned.getFuture().complete(new CorrelationData.Confirm(true, null));
        dispatcher.wakeAfterCommit(12);
        lastCorrelation.get().getFuture().complete(new CorrelationData.Confirm(false, "controlled nack"));
        dispatcher.wakeAfterCommit(12);
        lastCorrelation.get().getFuture().completeExceptionally(new TimeoutException("controlled unknown"));
        assertEquals(1, count("confirm", "returned"));
        assertEquals(1, count("confirm", "nack"));
        assertEquals(1, count("confirm", "unknown"));
        verify(mapper, never()).completeExecution(any(), anyString());
        verify(mapper, never()).updateById(any(AiTask.class));
    }

    @Test
    void reservationOrScanFailureKeepsRecoveryAvailableWithoutSending() {
        when(mapper.reserveDispatch(12)).thenThrow(new IllegalStateException("controlled DB timeout"));
        assertDoesNotThrow(() -> dispatcher.wakeAfterCommit(12));
        assertNull(lastCorrelation.get());
        assertEquals(1, count("reserve", "error"));
        when(mapper.selectDuePendingTasks(100)).thenThrow(new IllegalStateException("controlled scan failure"));
        dispatcher.recoverPending();
        assertEquals(1, count("scan", "error"));
        doReturn(List.of(12)).when(mapper).selectDuePendingTasks(100);
        doReturn(1).when(mapper).reserveDispatch(12);
        dispatcher.recoverPending();
        assertNotNull(lastCorrelation.get());
    }

    @Test
    void anEarlierCommitCallbackFailureCanSkipTheWakeupButTheScannerRecoversIt() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions)
                .executeWithoutResult(status -> {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() {
                            throw new IllegalStateException("controlled earlier cache callback failure");
                        }
                    });
                    service.createArticleTask(7, "title", "body");
                }));
        assertEquals(1, transactions.commits.get());
        verify(mapper, never()).reserveDispatch(anyInt());
        when(mapper.selectDuePendingTasks(100)).thenReturn(List.of(12));
        dispatcher.recoverPending();
        assertNotNull(lastCorrelation.get());
    }

    @Test
    void executionSuspendsTheCallerTransactionDuringProcessorSideEffects() {
        AiTask task = new AiTask();
        task.setId(12);
        task.setSourceVersion("source-1");
        task.setBizId(7);
        when(mapper.claimPendingTask(eq(12), anyString())).thenAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            task.setExecutionToken(call.getArgument(1));
            return 1;
        });
        when(mapper.selectById(12)).thenReturn(task);
        when(processor.processArticle(any(), any())).thenReturn(true);
        when(articles.selectById(7)).thenReturn(com.ican.entity.Article.builder()
                .id(7).sourceVersion("source-1").isDelete(0).status(1).articleTitle("title").articleContent("body").build());
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return true;
        }).when(processor).processArticle(any(), any());
        when(mapper.completeExecution(any(), anyString())).thenReturn(1);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.executeTask(12);
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        });
        verify(processor).processArticle(eq(task), any(com.ican.entity.Article.class));
    }

    @Test
    void recoveryExpiresBeforeResumingAndOnlyDueCheckpointWorkIsDispatched() {
        when(mapper.selectRecoverableTasks(100)).thenReturn(List.of(12, 13));
        when(mapper.expireExecution(12)).thenReturn(1);
        when(mapper.resumeCheckpointTask(13)).thenReturn(1);
        when(mapper.selectDuePendingTasks(100)).thenReturn(List.of(13));
        when(mapper.reserveDispatch(13)).thenReturn(1);
        dispatcher.recoverPending();
        var order = inOrder(mapper);
        order.verify(mapper).selectRecoverableTasks(100);
        order.verify(mapper).expireExecution(12);
        order.verify(mapper).expireExecution(13);
        order.verify(mapper).resumeCheckpointTask(13);
        order.verify(mapper).selectDuePendingTasks(100);
        order.verify(mapper).reserveDispatch(13);
        verify(mapper, never()).resumeCheckpointTask(12);
        assertEquals(1, count("recover", "expired"));
        assertEquals(1, count("recover", "resumed"));
    }

    private double count(String stage, String outcome) {
        var meter = registry.find("blog.ai.task.events").tags("stage", stage, "outcome", outcome).counter();
        return meter == null ? 0 : meter.count();
    }

    /** 真实Spring事务同步/挂起/恢复，持久存储由受控mapper替代，不冒充数据库事务。 */
    private static class TestTransactions extends AbstractPlatformTransactionManager {
        private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);
        private final AtomicInteger commits = new AtomicInteger();
        private final AtomicInteger rollbacks = new AtomicInteger();
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected boolean isExistingTransaction(Object tx) { return active.get(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) { active.set(true); }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits.incrementAndGet(); }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks.incrementAndGet(); }
        @Override protected Object doSuspend(Object tx) { active.set(false); return true; }
        @Override protected void doResume(Object tx, Object suspended) { active.set((Boolean) suspended); }
        @Override protected void doCleanupAfterCompletion(Object tx) { active.remove(); }
    }
}
