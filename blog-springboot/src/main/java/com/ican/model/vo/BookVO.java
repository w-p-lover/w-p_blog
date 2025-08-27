package com.ican.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书籍返回视图对象
 */
@Data
@ApiModel(description = "书籍返回视图对象")
public class BookVO {
    /**
     * id
     */
    @ApiModelProperty(value = "id")
    private Integer id;

    /**
     * 书名
     */
    @ApiModelProperty(value = "书名")
    private String title;

    /**
     * 作者
     */
    @ApiModelProperty(value = "作者")
    private String author;

    /**
     * 封面
     */
    @ApiModelProperty(value = "封面")
    private String cover;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private String status;

    /**
     * 标签
     */
    @ApiModelProperty(value = "标签")
    private String tags;

    /**
     * 添加时间
     */
    @ApiModelProperty(value = "添加时间")
    private LocalDateTime addTime;

    /**
     * 简介
     */
    @ApiModelProperty(value = "简介")
    private String brief;

    /**
     * 资源
     */
    @ApiModelProperty(value = "资源")
    private String resource;

    /**
     * 简介图片
     */
    @ApiModelProperty(value = "简介图片")
    private String briefImg;
}
