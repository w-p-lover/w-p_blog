package com.ican.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 新增/修改书籍 DTO
 */
@Data
@ApiModel(description = "新增/修改书籍 DTO")
public class BookDTO {

    /**
     * id
     */
    @ApiModelProperty(value = "id")
    private Integer id;

    /**
     * 书名
     */
    @ApiModelProperty(value = "书名")
    @NotBlank(message = "书名不能为空")
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

    /** 标签逗号分隔 */
    @ApiModelProperty(value = "标签")
    private String tags;

    /**
     * 简介
     */
    @ApiModelProperty(value = "简介")
    private String brief;

    /**
     * 简介图片
     */
    @ApiModelProperty(value = "简介图片")
    private String briefImg;

    /**
     * 资源
     */
    @ApiModelProperty(value = "资源")
    private String resource;
}
