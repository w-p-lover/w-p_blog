package com.ican.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityRegistration {
    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String SOLD_OUT = "SOLD_OUT";
    public static final String CANCELED = "CANCELED";
    public static final String FAILED = "FAILED";

    private String requestId;
    private Integer activityId;
    private Integer userId;
    private String status;
    private Integer dispatchAttempts;
    private String dispatchToken;
    private Integer failureCount;
    private LocalDateTime nextDispatchAt;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
