package com.ican.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.AiTask;
import com.ican.entity.Article;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

class AiArticleServiceImplTest {
    private final VectorStore vectors = mock(VectorStore.class);
    private final ArticleMapper articles = mock(ArticleMapper.class);
    private final AiTaskMapper tasks = mock(AiTaskMapper.class);
    private final ChatClient chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final ObjectMapper json = new ObjectMapper();
    private AiArticleServiceImpl service;
    private AiTask task;
    private Article article;

    @BeforeEach
    void setUp() {
        var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "test"), Article.class);
        service = new AiArticleServiceImpl(chat, vectors, articles, tasks, json);
        task = new AiTask();
        task.setId(12);
        task.setSourceVersion("v1");
        task.setExecutionToken("exec-1");
        article = Article.builder().id(7).articleTitle("标题").articleContent("正文")
                .sourceVersion("v1").status(1).isDelete(0).build();
        when(tasks.saveGeneratedPayload(eq(12), eq("exec-1"), anyString())).thenReturn(1);
        when(tasks.renewExecution(12, "exec-1")).thenReturn(1);
    }

    private void generated() {
        when(chat.prompt(anyString()).call().content()).thenReturn("摘要", "标签");
        clearInvocations(chat);
    }

    @Test
    void checkpointPrecedesVectorsAndPublicationIsLast() {
        generated();
        assertTrue(service.processArticle(task, article));
        var order = inOrder(tasks, vectors, articles);
        order.verify(tasks).saveGeneratedPayload(eq(12), eq("exec-1"), anyString());
        order.verify(vectors).add(anyList());
        verify(articles, never()).updateArticleAiResult(any(), any(), any(), any(), any(), any(), any());
        assertTrue(task.getGeneratedPayload().contains("v1"));
    }

    @Test
    void partialBatchFailureThenRetryReusesCheckpointAndStablePointIds() throws Exception {
        generated();
        article.setArticleContent("文".repeat(13000));
        Map<String, Document> points = new LinkedHashMap<>();
        List<String> attempted = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        doAnswer(call -> {
            List<Document> batch = call.getArgument(0);
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("第二批故障");
            }
            for (Document doc : batch) {
                UUID.fromString(doc.getId());
                attempted.add(doc.getId());
                points.put(doc.getId(), doc);
            }
            return null;
        }).when(vectors).add(anyList());
        assertThrows(RuntimeException.class, () -> service.processArticle(task, article));
        assertEquals(10, points.size());
        verify(articles, never()).updateArticleAiResult(any(), any(), any(), any(), any(), any(), any());
        // 用重新读取的任务模拟进程边界；检查点不依赖原Java对象。
        AiTask restored = new AiTask();
        restored.setId(12);
        restored.setSourceVersion("v1");
        restored.setExecutionToken("exec-1");
        restored.setGeneratedPayload(task.getGeneratedPayload());
        assertTrue(service.processArticle(restored, article));
        verify(chat, times(2)).prompt(anyString());
        verify(tasks, times(1)).saveGeneratedPayload(any(), any(), any());
        assertEquals(attempted.subList(0, 10), attempted.subList(10, 20));
        assertEquals(13, points.size());
        assertTrue(points.values().stream().allMatch(doc -> "v1".equals(doc.getMetadata().get("sourceVersion"))));
    }

    @Test
    void expiredExecutionStopsBeforeStartingAnotherVectorBatch() {
        generated();
        article.setArticleContent("文".repeat(13000));
        when(tasks.renewExecution(12, "exec-1")).thenReturn(1, 0);
        assertThrows(RuntimeException.class, () -> service.processArticle(task, article));
        verify(vectors, times(1)).add(anyList());
        verifyNoInteractions(articles);
    }

    @Test
    void obsoleteTaskDoesNotInvokeModelsOrVectors() {
        article.setSourceVersion("v2");
        assertFalse(service.processArticle(task, article));
        verifyNoInteractions(chat, vectors);
    }

    @Test
    void unknownCheckpointWriteCannotStartVectorsOrPublish() {
        generated();
        when(tasks.saveGeneratedPayload(any(), any(), any())).thenReturn(0);
        assertThrows(RuntimeException.class, () -> service.processArticle(task, article));
        verifyNoInteractions(vectors);
        verify(articles, never()).updateArticleAiResult(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void mismatchedCheckpointCannotBeReusedOrSilentlyRegenerated() {
        for (String payload : List.of("", " \n", "broken-json",
                "{\"sourceVersion\":\"old\",\"summary\":\"摘要\",\"tags\":\"标签\"}")) {
            task.setGeneratedPayload(payload);
            assertThrows(RuntimeException.class, () -> service.processArticle(task, article));
        }
        verifyNoInteractions(chat, vectors);
    }

    @Test
    void historyReindexUsesSameStablePointsAndOnlyPublishesCurrentSource() {
        when(articles.selectList(any())).thenReturn(List.of(article));
        when(articles.publishArticleVector(7, "v1")).thenReturn(1, 0);
        List<String> ids = new ArrayList<>();
        doAnswer(call -> {
            List<Document> batch = call.getArgument(0);
            ids.add(batch.get(0).getId());
            return null;
        }).when(vectors).add(anyList());
        assertEquals(1, service.reindexHistory(1));
        assertEquals(0, service.reindexHistory(1));
        assertEquals(ids.get(0), ids.get(1));
        verifyNoInteractions(chat, tasks);
    }
}
