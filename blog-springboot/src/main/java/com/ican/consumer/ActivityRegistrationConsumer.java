package com.ican.consumer;

import com.ican.model.dto.ActivityRegistrationMessage;
import com.ican.service.impl.ActivityRegistrationExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import static com.ican.config.ActivityRegistrationConfig.QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "blog.registration", name = "enabled", havingValue = "true")
public class ActivityRegistrationConsumer {
    private final ActivityRegistrationExecutionService execution;

    @RabbitListener(queues = QUEUE, containerFactory = "registrationListenerContainerFactory")
    public void consume(ActivityRegistrationMessage message) {
        if (message == null || message.getRequestId() == null || message.getDispatchToken() == null) {
            throw new AmqpRejectAndDontRequeueException("报名消息缺少请求身份或投递代");
        }
        try {
            execution.process(message.getRequestId(), message.getDispatchToken());
        } catch (RuntimeException error) {
            log.warn("报名确认事务失败或未知，核对当前事实后接续，requestId={}", message.getRequestId(), error);
            try {
                execution.recordFailure(message.getRequestId(), message.getDispatchToken());
            } catch (RuntimeException recoveryError) {
                // 故障副本不是唯一待办；原PENDING和预约到期仍可扫描恢复。
                throw new AmqpRejectAndDontRequeueException("报名失败状态暂不能记录，核查持久待办和故障队列", recoveryError);
            }
        }
    }
}
