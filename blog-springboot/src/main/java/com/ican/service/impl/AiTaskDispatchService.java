package com.ican.service.impl;

import com.ican.mapper.AiTaskMapper;
import com.ican.metrics.BlogMetrics;
import com.ican.model.dto.ArticleAiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.ican.constant.MqConstant.ARTICLE_AI_EXCHANGE;
import static com.ican.constant.MqConstant.ARTICLE_AI_KEY;

/** 持久PENDING是待办，消息/confirm仅用于唤醒和诊断，不决定任务完成。 */
@org.springframework.context.annotation.Profile("ai")
@Service
@Slf4j
public class AiTaskDispatchService {

    private final AiTaskMapper mapper;
    private final RabbitTemplate template;
    private final BlogMetrics metrics;
    private final TransactionTemplate reservation;

    @Autowired
    public AiTaskDispatchService(AiTaskMapper mapper, ConnectionFactory connectionFactory,
                                 MessageConverter converter, BlogMetrics metrics,
                                 PlatformTransactionManager transactionManager) {
        this(mapper, new RabbitTemplate(connectionFactory), metrics, transactionManager);
        template.setMessageConverter(converter);
    }

    AiTaskDispatchService(AiTaskMapper mapper, RabbitTemplate template,
                          BlogMetrics metrics, PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.template = template;
        template.setMandatory(true);
        this.metrics = metrics;
        reservation = new TransactionTemplate(transactionManager);
        // afterCommit仍绑定旧事务资源，预约必须提交到独立短事务。
        reservation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void wakeAfterCommit(Integer taskId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(taskId);
                }
            });
        } else {
            dispatch(taskId);
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void recoverPending() {
        try {
            // 每条UPDATE都是独立原子状态竞争；不回放DLQ，也不自动重跑无检查点任务。
            for (Integer taskId : mapper.selectRecoverableTasks(100)) {
                if (mapper.expireExecution(taskId) == 1) {
                    metrics.recordAiTaskEvent("recover", "expired");
                } else if (mapper.resumeCheckpointTask(taskId) == 1) {
                    metrics.recordAiTaskEvent("recover", "resumed");
                }
            }
            for (Integer taskId : mapper.selectDuePendingTasks(100)) {
                dispatch(taskId);
            }
            metrics.setAiTaskBacklog(mapper.selectTaskBacklog());
        } catch (RuntimeException e) {
            metrics.recordAiTaskEvent("scan", "error");
            log.error("文章任务待投递扫描失败，保留持久待办待下轮恢复", e);
        }
    }

    private void dispatch(Integer taskId) {
        try {
            Integer reserved = reservation.execute(status -> mapper.reserveDispatch(taskId));
            if (!Integer.valueOf(1).equals(reserved)) {
                return;
            }
        } catch (RuntimeException e) {
            metrics.recordAiTaskEvent("reserve", "error");
            log.error("文章任务投递预约失败或结果未知，taskId={}", taskId, e);
            return;
        }
        // 预约已提交；进程在此退出仍由next_dispatch_at恢复，不以已发送标记终止扫描。
        ArticleAiMessage message = new ArticleAiMessage();
        message.setTaskId(taskId);
        CorrelationData correlation = new CorrelationData(taskId + ":" + UUID.randomUUID());
        correlation.getFuture().orTimeout(10, TimeUnit.SECONDS).whenComplete((confirm, error) -> {
            String outcome = error != null ? "unknown" : correlation.getReturned() != null ? "returned"
                    : confirm.isAck() ? "ack" : "nack";
            metrics.recordAiTaskEvent("confirm", outcome);
            if (!"ack".equals(outcome)) {
                log.warn("文章任务发布未获有效确认，taskId={}, outcome={}，由持久PENDING恢复", taskId, outcome);
            }
        });
        try {
            template.convertAndSend(ARTICLE_AI_EXCHANGE, ARTICLE_AI_KEY, message, raw -> {
                raw.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return raw;
            }, correlation);
            metrics.recordAiTaskEvent("publish", "sent");
        } catch (RuntimeException e) {
            correlation.getFuture().completeExceptionally(e);
            metrics.recordAiTaskEvent("publish", "error");
            // 不抛出afterCommit：内容已持久化，任务留在PENDING到期重发。
            log.error("文章任务发布失败或结果未知，taskId={}", taskId, e);
        }
    }
}
