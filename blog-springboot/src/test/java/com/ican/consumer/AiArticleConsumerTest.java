package com.ican.consumer;

import com.ican.model.dto.ArticleAiMessage;
import com.ican.service.AiTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AiArticleConsumerTest {

    private AiTaskService aiTaskService;

    private AiArticleConsumer consumer;

    @BeforeEach
    void setUp() {
        aiTaskService = mock(AiTaskService.class);
        consumer = new AiArticleConsumer(aiTaskService);
    }

    @Test
    void consume_shouldExecuteTaskByTaskId() {
        ArticleAiMessage message = new ArticleAiMessage();
        message.setTaskId(9);

        consumer.consume(message);

        verify(aiTaskService).executeTask(9);
    }

    @Test
    void consume_shouldIgnoreEmptyMessage() {
        consumer.consume(null);

        verify(aiTaskService, never()).executeTask(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void consume_shouldRejectWithoutRequeueWhenTaskExecutionFails() {
        ArticleAiMessage message = new ArticleAiMessage();
        message.setTaskId(9);
        org.mockito.Mockito.doThrow(new RuntimeException("失败"))
                .when(aiTaskService).executeTask(9);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.consume(message));
    }
}
