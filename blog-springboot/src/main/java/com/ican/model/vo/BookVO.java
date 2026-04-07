package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书籍返回视图对象
 */
@Data
public class BookVO {
    /**
     * id
     */
    private Integer id;

    /**
     * 书名
     */
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

    /**
     * 标签
     */
    private String tags;

    /**
     * 添加时间
     */
    private LocalDateTime addTime;

    /**
     * 简介
     */
    private String brief;

    /**
     * 资源
     */
    private String resource;

    /**
     * 简介图片
     */
    private String briefImg;
}
