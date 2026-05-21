package com.ican.consumer;

import com.ican.model.dto.ArticleAiMessage;
import com.ican.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.ican.constant.MqConstant.ARTICLE_AI_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiArticleConsumer {

    private final AiTaskService aiTaskService;

    @RabbitListener(queues = ARTICLE_AI_QUEUE)
    public void consume(@Payload ArticleAiMessage message) {
        if (message == null) {
            log.warn("收到空的文章AI消息，已忽略");
            return;
        }
        if (message.getTaskId() == null) {
            log.warn("收到缺少taskId的文章AI消息，已忽略");
            return;
        }
        try {
            aiTaskService.executeTask(message.getTaskId());
        } catch (Exception e) {
            log.error("文章AI任务消费失败，进入死信队列，taskId={}", message.getTaskId(), e);
            throw new AmqpRejectAndDontRequeueException("文章AI处理失败", e);
        }
    }
}
