package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 新增/修改书籍 DTO
 */
@Data
public class BookDTO {

    /**
     * id
     */
    private Integer id;

    /**
     * 书名
     */
    @NotBlank(message = "书名不能为空")
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 封面
     */
    private String cover;

    /**
     * 状态
     */
    private String status;

    /** 标签逗号分隔 */
    private String tags;

    /**
     * 简介
     */
    private String brief;

    /**
     * 简介图片
     */
    private String briefImg;

    /**
     * 资源
     */
    private String resource;
}
