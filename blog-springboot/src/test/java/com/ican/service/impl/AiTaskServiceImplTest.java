package com.ican.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.AiTask;
import com.ican.entity.Article;
import com.ican.enums.AiTaskStatusEnum;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import com.ican.model.dto.ArticleAiMessage;
import com.ican.service.AiArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static com.ican.constant.MqConstant.ARTICLE_AI_EXCHANGE;
import static com.ican.constant.MqConstant.ARTICLE_AI_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTaskServiceImplTest {

    @Mock
    private AiTaskMapper aiTaskMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private AiArticleService aiArticleService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AiTaskServiceImpl aiTaskService;

    @BeforeEach
    void setUp() {
        aiTaskService = new AiTaskServiceImpl(
                aiTaskMapper,
                articleMapper,
                aiArticleService,
                rabbitTemplate,
                new ObjectMapper()
        );
    }

    @Test
    void createArticleTask_shouldPersistPendingTaskAndSendOnlyTaskIdMessage() {
        when(aiTaskMapper.insert(any(AiTask.class))).thenAnswer(invocation -> {
            AiTask task = invocation.getArgument(0);
            task.setId(12);
            return 1;
        });

        Integer taskId = aiTaskService.createArticleTask(7, "缓存治理", "文章内容");

        assertEquals(12, taskId);
        ArgumentCaptor<AiTask> taskCaptor = ArgumentCaptor.forClass(AiTask.class);
        verify(aiTaskMapper).insert(taskCaptor.capture());
        AiTask savedTask = taskCaptor.getValue();
        assertEquals("ARTICLE", savedTask.getBizType());
        assertEquals(7, savedTask.getBizId());
        assertEquals("ARTICLE_SUMMARY_TAG_VECTOR", savedTask.getTaskType());
        assertEquals(AiTaskStatusEnum.PENDING.getStatus(), savedTask.getStatus());
        assertEquals(0, savedTask.getRetryCount());
        assertEquals("deepseek-chat", savedTask.getModelName());
        assertEquals("article-ai-v1", savedTask.getPromptVersion());
        assertTrue(savedTask.getRequestPayload().contains("缓存治理"));

        ArgumentCaptor<ArticleAiMessage> messageCaptor = ArgumentCaptor.forClass(ArticleAiMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(ARTICLE_AI_EXCHANGE), eq(ARTICLE_AI_KEY), messageCaptor.capture());
        assertEquals(12, messageCaptor.getValue().getTaskId());
    }

    @Test
    void executeTask_shouldMarkSuccessWhenArticleAiProcessingSucceeds() {
        AiTask task = buildTask(AiTaskStatusEnum.PENDING.getStatus());
        Article article = Article.builder()
                .id(7)
                .articleTitle("缓存治理")
                .articleContent("文章内容")
                .build();
        when(aiTaskMapper.selectById(12)).thenReturn(task);
        when(articleMapper.selectById(7)).thenReturn(article);

        aiTaskService.executeTask(12);

        ArgumentCaptor<AiTask> taskCaptor = ArgumentCaptor.forClass(AiTask.class);
        verify(aiTaskMapper, org.mockito.Mockito.times(2)).updateById(taskCaptor.capture());
        AiTask runningTask = taskCaptor.getAllValues().get(0);
        assertEquals(AiTaskStatusEnum.RUNNING.getStatus(), runningTask.getStatus());
        assertNotNull(runningTask.getStartedAt());
        AiTask successTask = taskCaptor.getAllValues().get(1);
        assertEquals(AiTaskStatusEnum.SUCCESS.getStatus(), successTask.getStatus());
        assertNotNull(successTask.getFinishedAt());
        assertNotNull(successTask.getCostTime());
        verify(aiArticleService).processArticle(7, "缓存治理", "文章内容");
    }

    @Test
    void executeTask_shouldRecordRetryingFailureWhenProcessingFails() {
        AiTask task = buildTask(AiTaskStatusEnum.PENDING.getStatus());
        Article article = Article.builder()
                .id(7)
                .articleTitle("缓存治理")
                .articleContent("文章内容")
                .build();
        when(aiTaskMapper.selectById(12)).thenReturn(task);
        when(articleMapper.selectById(7)).thenReturn(article);
        org.mockito.Mockito.doThrow(new RuntimeException("模型异常"))
                .when(aiArticleService).processArticle(7, "缓存治理", "文章内容");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> aiTaskService.executeTask(12));

        assertTrue(exception.getMessage().contains("AI任务执行失败"));
        ArgumentCaptor<AiTask> taskCaptor = ArgumentCaptor.forClass(AiTask.class);
        verify(aiTaskMapper, org.mockito.Mockito.times(2)).updateById(taskCaptor.capture());
        AiTask failedTask = taskCaptor.getAllValues().get(1);
        assertEquals(AiTaskStatusEnum.RETRYING.getStatus(), failedTask.getStatus());
        assertEquals(1, failedTask.getRetryCount());
        assertTrue(failedTask.getErrorMessage().contains("模型异常"));
        assertNotNull(failedTask.getFinishedAt());
    }

    @Test
    void executeTask_shouldSkipAlreadySuccessfulTask() {
        when(aiTaskMapper.selectById(12)).thenReturn(buildTask(AiTaskStatusEnum.SUCCESS.getStatus()));

        aiTaskService.executeTask(12);

        verify(articleMapper, never()).selectById(7);
        verify(aiArticleService, never()).processArticle(any(), any(), any());
    }

    private AiTask buildTask(String status) {
        AiTask task = new AiTask();
        task.setId(12);
        task.setBizType("ARTICLE");
        task.setBizId(7);
        task.setTaskType("ARTICLE_SUMMARY_TAG_VECTOR");
        task.setStatus(status);
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setStartedAt(LocalDateTime.now().minusSeconds(1));
        return task;
    }

}
