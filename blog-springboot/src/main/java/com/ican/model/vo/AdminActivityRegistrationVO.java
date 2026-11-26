package com.ican.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

/** 管理端持久事实快照；预约次数不代表发送成功或broker确认。 */
@Data
public class AdminActivityRegistrationVO {
    private String requestId;
    private Integer activityId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer dispatchAttempts;
    private Integer failureCount;
    private LocalDateTime nextDispatchAt;
    private String failureSummary;
}
