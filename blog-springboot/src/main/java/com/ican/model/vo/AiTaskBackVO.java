package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI任务后台视图
 */
@Data
public class AiTaskBackVO {

    private Integer id;

    private String bizType;

    private Integer bizId;

    private String taskType;

    private String status;

    private Integer retryCount;

    private Integer maxRetryCount;

    private String errorMessage;

    private String modelName;

    private String promptVersion;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long costTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
