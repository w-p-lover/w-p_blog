package com.ican.model.dto;

import lombok.Data;

/**
 * 定时任务运行
 *
 * @author xcs
 */
@Data
public class TaskRunDTO {

    /**
     * 任务id
     */
    private Integer id;

    /**
     * 任务组别
     */
    private String taskGroup;
}