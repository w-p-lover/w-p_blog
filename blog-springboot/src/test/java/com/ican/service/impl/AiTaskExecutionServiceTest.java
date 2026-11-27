package com.ican.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.AiTask;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiTaskExecutionServiceTest {
    private final AiTaskMapper tasks = mock(AiTaskMapper.class);
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final Transactions transactions = new Transactions();
    private AiTaskExecutionService service;
    private AiTask task;

    @BeforeEach
    void setUp() {
        var target = new AiTaskExecutionService(tasks, articles, new ObjectMapper(),
                new BlogMetrics(new SimpleMeterRegistry()));
        var factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (AiTaskExecutionService) factory.getProxy();
        task = new AiTask();
        task.setId(12);
        task.setBizId(7);
        task.setSourceVersion("source-1");
        task.setExecutionToken("exec");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setStartedAt(LocalDateTime.now());
        task.setGeneratedPayload("{\"sourceVersion\":\"source-1\",\"summary\":\"摘要\",\"tags\":\"标签\"}");
        when(tasks.lockExecution(12, "exec")).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return task;
        });
        when(tasks.renewExecution(12, "exec")).thenReturn(1);
        when(articles.updateArticleAiResult(any(), any(), any(), any(), any(), any(), any())).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        });
        when(tasks.completeExecution(any(), eq("exec"))).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        });
    }

    @Test
    void publicationAndTerminalWriteShareOneShortTransactionAfterTaskLock() throws Exception {
        service.finish(task, true);
        var order = inOrder(tasks, articles);
        order.verify(tasks).lockExecution(12, "exec");
        order.verify(tasks).renewExecution(12, "exec");
        order.verify(articles).updateArticleAiResult(eq(7), eq("摘要"), eq("标签"), any(), eq("source-1"), eq(12), eq("exec"));
        order.verify(tasks).completeExecution(task, "exec");
        assertEquals("SUCCESS", task.getStatus());
        assertEquals(1, transactions.commits);
        assertEquals(0, transactions.rollbacks);
    }

    @Test
    void terminalWriteFailureRequestsRollbackOfPublication() {
        when(tasks.completeExecution(any(), any())).thenReturn(0);
        assertThrows(RuntimeException.class, () -> service.finish(task, true));
        verify(articles).updateArticleAiResult(any(), any(), any(), any(), any(), any(), any());
        assertEquals(0, transactions.commits);
        assertEquals(1, transactions.rollbacks);
    }

    @Test
    void expiredOrReclaimedWorkerCannotPublishOrComplete() throws Exception {
        when(tasks.renewExecution(12, "exec")).thenReturn(0);
        service.finish(task, true);
        doReturn(null).when(tasks).lockExecution(12, "exec");
        service.finish(task, true);
        service.fail(task, new RuntimeException("late old worker"));
        verifyNoInteractions(articles);
        verify(tasks, never()).completeExecution(any(), any());
    }

    @Test
    void sourceOrVisibilityChangeCompletesCanceledInSameTransaction() throws Exception {
        doReturn(0).when(articles).updateArticleAiResult(any(), any(), any(), any(), any(), any(), any());
        service.finish(task, true);
        assertEquals("CANCELED", task.getStatus());
        assertEquals(0, task.getRetryCount());
        assertEquals(1, transactions.commits);
    }

    @Test
    void persistentCheckpointWinsOverStaleObjectAfterUnknownCheckpointResponse() {
        AiTask stale = new AiTask();
        stale.setId(12);
        stale.setExecutionToken("exec");
        assertNull(stale.getGeneratedPayload());
        service.fail(stale, new RuntimeException("checkpoint response lost"));
        assertEquals("RETRYING", task.getStatus());
        assertEquals(1, task.getRetryCount());
        assertNotNull(task.getGeneratedPayload());
    }

    @Test
    void unknownModelResultIsNotAutomaticRetryAndCheckpointRetriesShareBudget() {
        task.setGeneratedPayload(null);
        service.fail(task, new RuntimeException("timeout before checkpoint"));
        assertEquals("UNKNOWN", task.getStatus());
        task.setGeneratedPayload("checkpoint");
        task.setRetryCount(2);
        service.fail(task, new RuntimeException("vector error"));
        assertEquals("FAILED", task.getStatus());
        assertEquals(3, task.getRetryCount());
    }

    /** 验证Spring事务边界/回滚决定；不是MySQL行锁或数据库回滚证据。 */
    private static class Transactions extends AbstractPlatformTransactionManager {
        private int commits;
        private int rollbacks;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) {
            assertEquals(10, definition.getTimeout());
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
