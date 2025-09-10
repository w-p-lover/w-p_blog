package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流程元素VO类
 * 用于表示流程图中的各种元素信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowElementVO {
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
     * 元素样式类名
     */
    private String elemClass;

    /**
     * 源元素ID（用于连接线等元素）
     */
    private String sourceId;

    /**
     * 目标元素ID（用于连接线等元素）
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
    private String desc;

    /**
     * 元素关键点列表
     */
    private List<String> keyPoints;

    /**
     * 元素颜色
     */
    private String color;

    /**
     * 是否启用动画效果
     */
    private Boolean animated;

}

