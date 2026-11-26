package com.ican.service.impl;

import com.ican.entity.Activity;
import com.ican.entity.ActivityRegistration;
import com.ican.exception.ServiceException;
import com.ican.mapper.ActivityMapper;
import com.ican.model.vo.ActivityRegistrationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import static com.ican.entity.ActivityRegistration.*;

/** 事务内仅做数据库操作；请求行→活动行，确认/取消/失败恢复共用顺序。 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "blog.registration", name = "enabled", havingValue = "true")
public class ActivityRegistrationExecutionService {
    private final ActivityMapper mapper;
    private final ActivityRegistrationDispatchService dispatcher;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void process(String requestId, String token) {
        ActivityRegistration row = mapper.lockDispatched(requestId, token);
        if (row == null) {
            return; // 重复、旧投递代、已取消或终态不能再次扣减。
        }
        String result;
        if (mapper.consumeCapacity(row.getActivityId()) == 1) {
            result = CONFIRMED;
        } else {
            Activity activity = mapper.selectActivity(row.getActivityId());
            if (activity == null || !"PUBLISHED".equals(activity.getStatus()) || activity.getPendingCount() <= 0) {
                throw new ServiceException("活动受理计数异常，需核查后恢复");
            }
            changed(mapper.releasePending(row.getActivityId()));
            result = SOLD_OUT;
        }
        changed(mapper.changeStatus(requestId, PENDING, result));
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityRegistrationVO cancel(Integer activityId, Integer userId) {
        ActivityRegistration row = mapper.lockUserRequest(activityId, userId);
        if (row == null) {
            throw new ServiceException("没有可取消的报名");
        }
        if (PENDING.equals(row.getStatus())) {
            changed(mapper.releasePending(activityId));
        } else if (CONFIRMED.equals(row.getStatus())) {
            changed(mapper.restoreCapacity(activityId));
        } else {
            return ActivityRegistrationVO.from(row);
        }
        changed(mapper.changeStatus(row.getRequestId(), row.getStatus(), CANCELED));
        return ActivityRegistrationVO.from(mapper.lockRequest(row.getRequestId()));
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String requestId, String token) {
        ActivityRegistration row = mapper.lockRequest(requestId);
        if (row == null || !PENDING.equals(row.getStatus()) || !Objects.equals(token, row.getDispatchToken())) {
            return; // 事务提交结果未知时先看当前结果；旧错误不得影响后续投递/人工恢复。
        }
        boolean terminal = row.getFailureCount() + 1 >= 3;
        if (terminal) {
            changed(mapper.releasePending(row.getActivityId()));
        }
        changed(mapper.failDispatch(requestId, token, terminal));
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityRegistrationVO retry(String requestId) {
        ActivityRegistration row = mapper.lockRequest(requestId);
        if (row == null || !FAILED.equals(row.getStatus())) {
            throw new ServiceException("仅能恢复处理失败的报名");
        }
        if (mapper.reserveRetry(row.getActivityId()) != 1) {
            throw new ServiceException("活动受理繁忙或状态异常，暂不能恢复");
        }
        changed(mapper.retryFailed(requestId));
        dispatcher.wakeAfterCommit(requestId);
        return ActivityRegistrationVO.from(mapper.lockRequest(requestId));
    }

    private static void changed(int rows) {
        if (rows != 1) {
            throw new ServiceException("报名状态发生冲突，请查询当前结果后重试");
        }
    }
}
