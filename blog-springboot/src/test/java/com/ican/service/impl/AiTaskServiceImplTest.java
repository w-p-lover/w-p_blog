package com.ican.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.AiTask;
import com.ican.entity.Article;
import com.ican.exception.ServiceException;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import com.ican.service.AiArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiTaskServiceImplTest {

    private final AiTaskMapper mapper = mock(AiTaskMapper.class);
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final AiArticleService processor = mock(AiArticleService.class);
    private final AiTaskDispatchService dispatcher = mock(AiTaskDispatchService.class);
    private AiTaskServiceImpl service;
    private final AtomicReference<String> status = new AtomicReference<>("PENDING");
    private final AtomicInteger failures = new AtomicInteger();
    private final AiTask task = new AiTask();

    @BeforeEach
    void setUp() {
        var execution = new AiTaskExecutionService(mapper, articles, new ObjectMapper(),
                new com.ican.metrics.BlogMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        var factory = new org.springframework.aop.framework.ProxyFactory(execution);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                new TestTransactions(), new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        service = new AiTaskServiceImpl(mapper, articles, processor, dispatcher, new ObjectMapper(),
                (AiTaskExecutionService) factory.getProxy());
        task.setId(12);
        task.setBizId(7);
        task.setSourceVersion("source-1");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        when(mapper.selectById(12)).thenAnswer(call -> {
            task.setStatus(status.get());
            task.setRetryCount(failures.get());
            return task;
        });
        when(mapper.claimPendingTask(eq(12), anyString())).thenAnswer(call -> {
            if (!status.compareAndSet("PENDING", "RUNNING")) {
                return 0;
            }
            task.setExecutionToken(call.getArgument(1));
            task.setStartedAt(java.time.LocalDateTime.now());
            return 1;
        });
        when(mapper.lockExecution(eq(12), anyString())).thenAnswer(call ->
                "RUNNING".equals(status.get()) && call.getArgument(1).equals(task.getExecutionToken()) ? task : null);
        when(mapper.renewExecution(eq(12), anyString())).thenReturn(1);
        task.setGeneratedPayload("{\"sourceVersion\":\"source-1\",\"summary\":\"摘要\",\"tags\":\"标签\"}");
        when(articles.updateArticleAiResult(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(mapper.completeExecution(any(), anyString())).thenAnswer(call -> {
            AiTask result = call.getArgument(0);
            if (!call.getArgument(1).equals(task.getExecutionToken())
                    || !status.compareAndSet("RUNNING", result.getStatus())) {
                return 0;
            }
            failures.set(result.getRetryCount());
            return 1;
        });
        when(processor.processArticle(any(), any())).thenReturn(true);
        when(articles.selectById(7)).thenReturn(Article.builder().id(7).sourceVersion("source-1").isDelete(0).status(1)
                .articleTitle("缓存治理").articleContent("文章内容").build());
    }

    @Test
    void createPersistsPendingAndDelegatesCommitAwareWakeup() {
        when(mapper.insert(any(AiTask.class))).thenAnswer(call -> {
            AiTask saved = call.getArgument(0);
            saved.setId(12);
            return 1;
        });
        assertEquals(12, service.createArticleTask(7, "缓存治理", "文章内容"));
        var saved = ArgumentCaptor.forClass(AiTask.class);
        verify(mapper).insert(saved.capture());
        assertEquals("ARTICLE", saved.getValue().getBizType());
        assertEquals("ARTICLE_SUMMARY_TAG_VECTOR", saved.getValue().getTaskType());
        assertEquals("PENDING", saved.getValue().getStatus());
        assertEquals(0, saved.getValue().getDispatchAttempts());
        assertEquals("source-1", saved.getValue().getSourceVersion());
        assertTrue(saved.getValue().getRequestPayload().contains("缓存治理"));
        verify(dispatcher).wakeAfterCommit(12);
    }

    @Test
    void executionCompletesUsingTheClaimTokenAndDuplicateMessageDoesNotRunAgain() {
        service.executeTask(12);
        service.executeTask(12);
        assertEquals("SUCCESS", status.get());
        var result = ArgumentCaptor.forClass(AiTask.class);
        verify(mapper).completeExecution(result.capture(), eq(task.getExecutionToken()));
        assertNotNull(result.getValue().getFinishedAt());
        assertNotNull(result.getValue().getCostTime());
        verify(processor, times(1)).processArticle(eq(task), any(Article.class));
        verify(mapper, never()).updateById(any(AiTask.class));
    }

    @Test
    void processingFailureRetainsErrorAndDuplicateDeliveryCannotRetrySideEffects() {
        doThrow(new RuntimeException("模型异常")).when(processor).processArticle(any(), any());
        assertThrows(RuntimeException.class, () -> service.executeTask(12));
        service.executeTask(12);
        assertEquals("RETRYING", status.get());
        assertEquals(1, failures.get());
        var result = ArgumentCaptor.forClass(AiTask.class);
        verify(mapper).completeExecution(result.capture(), anyString());
        assertTrue(result.getValue().getErrorMessage().contains("模型异常"));
        verify(processor, times(1)).processArticle(any(), any());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void competingConsumersRunOnlyOneProcessorAndCannotResetRunningTask() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> {
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return true;
        }).when(processor).processArticle(any(), any());
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> service.executeTask(12));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            service.executeTask(12);
            assertThrows(ServiceException.class, () -> service.retryTask(12));
            assertEquals("RUNNING", status.get());
            verify(processor, times(1)).processArticle(any(), any());
            verifyNoInteractions(dispatcher);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertEquals("SUCCESS", status.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void obsoleteLegacyAndWithdrawnTasksAreCanceledWithoutProcessing() {
        for (String source : new String[]{"old", null}) {
            status.set("PENDING");
            task.setSourceVersion(source);
            service.executeTask(12);
            assertEquals("CANCELED", status.get());
        }
        status.set("PENDING");
        task.setSourceVersion("source-1");
        when(articles.selectById(7)).thenReturn(Article.builder().id(7).sourceVersion("source-1")
                .isDelete(1).status(1).build());
        service.executeTask(12);
        assertEquals("CANCELED", status.get());
        verify(processor, never()).processArticle(any(), any());
        assertEquals(0, failures.get());
    }

    @Test
    void sourceChangedDuringProcessingFinishesAsCanceledRatherThanSuccessOrRetry() {
        when(processor.processArticle(any(), any())).thenReturn(false);
        service.executeTask(12);
        assertEquals("CANCELED", status.get());
        var result = ArgumentCaptor.forClass(AiTask.class);
        verify(mapper).completeExecution(result.capture(), anyString());
        assertTrue(result.getValue().getResultPayload().contains("SUPERSEDED_OR_WITHDRAWN"));
        assertEquals(0, failures.get());
    }

    @Test
    void ambiguousProducerSourceCannotCreateAnUnboundTask() {
        assertThrows(ServiceException.class, () -> service.createArticleTask(7, "旧标题", "旧正文"));
        verify(mapper, never()).insert(any(AiTask.class));
        verifyNoInteractions(dispatcher);
    }

    @Test
    void manualRetryIsConditionalPreservesFailureBudgetAndOnlyWinnerWakes() {
        status.set("RETRYING");
        failures.set(1);
        when(mapper.resetFailedTask(12, 1)).thenReturn(1, 0);
        service.retryTask(12);
        assertThrows(ServiceException.class, () -> service.retryTask(12));
        verify(dispatcher, times(1)).wakeAfterCommit(12);
        assertEquals(1, failures.get());
        verify(mapper, never()).updateById(any(AiTask.class));
    }

    @Test
    void terminalRunningAndExhaustedRetriesCannotBeReset() {
        for (String state : new String[]{"RUNNING", "SUCCESS", "CANCELED", "FAILED"}) {
            status.set(state);
            failures.set(3);
            assertThrows(ServiceException.class, () -> service.retryTask(12));
            service.executeTask(12);
        }
        verifyNoInteractions(dispatcher, processor);
    }

    private static class TestTransactions extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition definition) { }
        @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) { }
        @Override protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) { }
    }

    @Test
    void missingCheckpointFailureIsUnknownAndCannotBeRetriedByDelivery() {
        task.setGeneratedPayload(null);
        doThrow(new RuntimeException("模型请求结果未知")).when(processor).processArticle(any(), any());
        assertThrows(RuntimeException.class, () -> service.executeTask(12));
        service.executeTask(12);
        assertEquals("UNKNOWN", status.get());
        verify(processor, times(1)).processArticle(any(), any());
        verify(articles, never()).updateArticleAiResult(any(), any(), any(), any(), any(), any(), any());
    }
}
