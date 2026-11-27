package com.ican.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI任务记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String bizType;

    private Integer bizId;

    private String taskType;

    private String status;

    private Integer retryCount;

    private Integer maxRetryCount;

    private Integer dispatchAttempts;

    /** PENDING投递到期 / RUNNING租约到期 / RETRYING业务退避；状态决定语义。 */
    private LocalDateTime nextDispatchAt;

    private String executionToken;

    private String sourceVersion;

    /** 已生成的摘要/标签检查点，失败重试保留，避免向量失败后再次生成。 */
    private String generatedPayload;

    private String requestPayload;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String resultPayload;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String errorMessage;

    private String modelName;

    private String promptVersion;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime startedAt;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime finishedAt;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
