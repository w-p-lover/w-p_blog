package com.ican.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocVersionDTO {

    private Long id;
    /**
     * 文档id
     */
    private Long docId;
    /**
     * 版本
     */
    private String version;
    /**
     * 作者
     */
    private String author;
    /**
     * 状态
     */
    private String status;
    /**
     * 创建时间
     */
    private String createdAt;
    /**
     * 文档内容
     */
    private String content;
    /**
     * 文档描述
     */
    private String description;
}
