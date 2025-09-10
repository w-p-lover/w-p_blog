package com.ican.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 流程元素实体类
 * 用于表示流程图中的各种元素，包括节点、连线等图形元素
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowElement {

    /**
     * 元素唯一标识符
     */
    private String id;

    /**
     * 元素类型
     */
    private String elemType;

    /**
     * 元素标签名称
     */
    private String label;

    /**
     * 元素分类
     */
    private String elemClass;

    /**
     * 源元素ID（用于连线元素，表示起始节点）
     */
    private String sourceId;

    /**
     * 目标元素ID（用于连线元素，表示结束节点）
     */
    private String targetId;

    /**
     * 元素在画布中的X坐标位置
     */
    private Integer positionX;

    /**
     * 元素在画布中的Y坐标位置
     */
    private Integer positionY;

    /**
     * 元素描述信息
     */
    private String description;

    /**
     * 关键点信息（用于存储元素的关键业务数据）
     */
    private String keyPoints;

    /**
     * 元素颜色
     */
    private String color;

    /**
     * 是否启用动画效果
     */
    private Boolean animated;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
