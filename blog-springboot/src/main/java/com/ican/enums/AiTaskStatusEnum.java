package com.ican.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI任务状态
 */
@Getter
@AllArgsConstructor
public enum AiTaskStatusEnum {

    PENDING("PENDING"),
    RUNNING("RUNNING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    RETRYING("RETRYING"),
    CANCELED("CANCELED");

    private final String status;
}
