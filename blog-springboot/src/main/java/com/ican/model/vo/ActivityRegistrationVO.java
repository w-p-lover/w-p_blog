package com.ican.model.vo;

import com.ican.entity.ActivityRegistration;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityRegistrationVO {
    private String requestId;
    private Integer activityId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ActivityRegistrationVO from(ActivityRegistration row) {
        if (row == null) {
            return null;
        }
        ActivityRegistrationVO result = new ActivityRegistrationVO();
        result.setRequestId(row.getRequestId());
        result.setActivityId(row.getActivityId());
        result.setStatus(row.getStatus());
        result.setCreateTime(row.getCreateTime());
        result.setUpdateTime(row.getUpdateTime());
        return result;
    }
}
