package com.ican.model.dto;

import lombok.Data;

@Data
public class FlowElementDTO {
    /**
     * 元素类型
     */
    private String elemType;

    /**
     * 元素标签
     */
    private String label;

    /**
     * 源节点
     */
    private String sourceId;

    /**
     * 目标节点
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
     * 关键点信息
     */
    private String keyPoints;
}
