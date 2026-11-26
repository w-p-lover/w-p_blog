package com.ican.service.impl;

import com.ican.mapper.ActivityMapper;
import com.ican.model.dto.ActivityRegistrationMessage;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static com.ican.config.ActivityRegistrationConfig.*;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "blog.registration", name = "enabled", havingValue = "true")
public class ActivityRegistrationDispatchService {
    private final ActivityMapper mapper;
    private final RabbitTemplate template;
    private final ThreadPoolTaskExecutor executor;
    private final TransactionTemplate reservation;
    private final MeterRegistry metrics;

    public ActivityRegistrationDispatchService(ActivityMapper mapper, ConnectionFactory connection,
            MessageConverter converter, @Qualifier("registrationDispatchExecutor") ThreadPoolTaskExecutor executor,
            PlatformTransactionManager transactionManager, MeterRegistry metrics) {
        this.mapper = mapper;
        this.executor = executor;
        this.metrics = metrics;
        template = new RabbitTemplate(connection);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        reservation = new TransactionTemplate(transactionManager);
        reservation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void wakeAfterCommit(String requestId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue(requestId);
                }
            });
        } else {
            enqueue(requestId);
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void recoverPending() {
        try {
            for (String id : mapper.selectDue(100)) {
                enqueue(id);
            }
        } catch (RuntimeException error) {
            event("scan", "error");
            log.error("报名待办扫描失败，持久请求保留", error);
        }
    }

    private void enqueue(String requestId) {
        try {
            executor.execute(() -> dispatch(requestId));
        } catch (TaskRejectedException error) {
            // 不CallerRuns：MQ故障或本地队列满不能让受理HTTP线程执行网络发送。
            event("wake", "deferred");
        }
    }

    private void dispatch(String requestId) {
        try {
            String token = reservation.execute(status -> {
                if (mapper.reserveDispatch(requestId, UUID.randomUUID().toString()) != 1) {
                    return null;
                }
                // 预约锁内读取持久代；积压重发沿用本代，已排队消息仍能推进业务。
                return mapper.lockRequest(requestId).getDispatchToken();
            });
            if (token == null) {
                return;
            }
            CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
            correlation.getFuture().orTimeout(10, TimeUnit.SECONDS).whenComplete((confirm, error) -> {
                String outcome = error != null ? "unknown" : correlation.getReturned() != null ? "returned"
                        : confirm.isAck() ? "ack" : "nack";
                event("confirm", outcome);
            });
            template.convertAndSend(EXCHANGE, KEY, new ActivityRegistrationMessage(requestId, token), message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            }, correlation);
            event("publish", "sent");
        } catch (RuntimeException error) {
            event("publish", "error");
            log.warn("报名唤醒失败或未知，由持久PENDING退避恢复，requestId={}", requestId, error);
        }
    }

    private void event(String stage, String outcome) {
        metrics.counter("blog.registration.events", "stage", stage, "outcome", outcome).increment();
    }
}
