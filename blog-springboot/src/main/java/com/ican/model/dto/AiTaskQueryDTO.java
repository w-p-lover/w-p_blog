package com.ican.model.dto;

import lombok.Data;

/**
 * AI任务查询条件
 */
@Data
public class AiTaskQueryDTO {

    private String keyword;

    private String bizType;

    private String taskType;

    private String status;
}
