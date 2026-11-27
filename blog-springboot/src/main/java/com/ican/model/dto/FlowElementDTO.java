package com.ican.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class FlowElementDTO {
    /**
     * 元素ID
     */
    private String id;

    /**
     * 元素类型: node | edge
     */
    private String elemType;

    /**
     * 元素标签
     */
    private String label;

    /**
     * 元素样式类名 (前端 class)
     */
    private String elemClass;

    /**
     * 元素在画布中的X坐标位置
     */
    private int positionX;

    /**
     * 元素在画布中的Y坐标位置
     */
    private int positionY;


    /**
     * 源节点ID
     */
    private String sourceId;

    /**
     * 目标节点ID
     */
    private String targetId;

    /**
     * 元素描述信息
     */
    private String description;

    /**
     * 关键点信息 (存储为逗号分隔字符串)
     */
    private List<String> keyPoints;

    /**
     * 是否启用动画
     */
    private Boolean animated;

    /**
     * 元素颜色
     */
    private String color;
}
