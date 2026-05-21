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
