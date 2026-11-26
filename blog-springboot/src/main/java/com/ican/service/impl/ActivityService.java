package com.ican.service.impl;

import com.ican.entity.Activity;
import com.ican.entity.ActivityRegistration;
import com.ican.exception.ServiceException;
import com.ican.mapper.ActivityMapper;
import com.ican.model.dto.ActivitySaveDTO;
import com.ican.model.vo.ActivityRegistrationVO;
import com.ican.model.vo.AdminActivityRegistrationVO;
import com.ican.model.vo.ActivityVO;
import com.ican.model.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "blog.registration", name = "enabled", havingValue = "true")
public class ActivityService {
    private final ActivityMapper mapper;
    private final ActivityRegistrationDispatchService dispatcher;

    public PageResult<ActivityVO> list(boolean admin, int page, int size) {
        if (page < 1 || page > 10000 || size < 1 || size > 50) {
            throw new ServiceException("分页范围无效");
        }
        return new PageResult<>(mapper.listActivities(admin, (page - 1) * size, size), mapper.countActivities(admin));
    }

    public ActivityVO detail(Integer id) {
        Activity row = mapper.selectActivity(id);
        if (row == null || !"PUBLISHED".equals(row.getStatus())) {
            throw new ServiceException("活动不存在或尚未发布");
        }
        return view(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityVO create(ActivitySaveDTO dto, Integer ownerId) {
        Activity row = new Activity();
        BeanUtils.copyProperties(dto, row);
        row.setTitle(dto.getTitle().trim());
        row.setOwnerId(ownerId);
        if (mapper.insertActivity(row) != 1) {
            throw new ServiceException("活动开始时间须晚于当前时间，结束时间须晚于开始时间");
        }
        return view(mapper.selectActivity(row.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, ActivitySaveDTO dto) {
        if (mapper.updateBeforeStart(id, dto) != 1) {
            throw new ServiceException("活动不存在、已开始或时间范围无效，不能修改");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Integer id) {
        if (mapper.publish(id) != 1) {
            throw new ServiceException("仅能发布尚未开始的草稿活动");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivityRegistrationVO submit(Integer activityId, Integer userId, String requestId) {
        String identity = UUID.fromString(requestId).toString();
        try {
            // 先创建/竞争唯一报名身份，再竞争活动行；与确认、取消保持相同锁顺序。
            mapper.insertRegistration(identity, activityId, userId);
        } catch (DuplicateKeyException duplicate) {
            // 使用当前锁定读；重复请求可能刚由另一事务提交，不能依赖RR旧快照。
            ActivityRegistration existing = mapper.lockRequest(identity);
            if (existing != null) {
                if (!Objects.equals(existing.getActivityId(), activityId) || !Objects.equals(existing.getUserId(), userId)) {
                    throw new ServiceException("请求编号已被其他报名占用，请使用本次报名的编号");
                }
            } else {
                existing = mapper.lockUserRequest(activityId, userId);
            }
            if (existing == null) {
                throw new ServiceException("报名结果暂不可确认，请使用同一请求编号重试");
            }
            return ActivityRegistrationVO.from(existing);
        }
        if (mapper.reserveAdmission(activityId) != 1) {
            // 回滚刚插入的身份；拒绝受理不占名额，也不会遗留有效待办。
            throw new ServiceException("活动当前不在报名时间内，或受理繁忙，请稍后重试");
        }
        dispatcher.wakeAfterCommit(identity);
        return ActivityRegistrationVO.from(mapper.lockRequest(identity));
    }

    public ActivityRegistrationVO mine(Integer activityId, Integer userId) {
        return ActivityRegistrationVO.from(mapper.findMine(activityId, userId));
    }

    public PageResult<AdminActivityRegistrationVO> registrations(Integer activityId, String status, int page, int size) {
        if (page < 1 || page > 10000 || size < 1 || size > 50
                || (status != null && !java.util.Set.of("PENDING", "CONFIRMED", "SOLD_OUT", "CANCELED", "FAILED").contains(status))) {
            throw new ServiceException("分页或报名状态无效");
        }
        return new PageResult<>(mapper.listRegistrations(activityId, status, (page - 1) * size, size),
                mapper.countRegistrations(activityId, status));
    }

    private static ActivityVO view(Activity row) {
        ActivityVO result = new ActivityVO();
        BeanUtils.copyProperties(row, result);
        return result;
    }
}
