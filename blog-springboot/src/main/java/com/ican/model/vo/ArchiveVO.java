package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章归档
 *
 * @author xcs
 */
@Data
public class ArchiveVO {

    /**
     * 文章id
     */
    private Integer id;

    /**
     * 标题
     */
    private String articleTitle;

    /**
     * 文章缩略图
     */
    private String articleCover;

    /**
     * 发表时间
     */
    private LocalDateTime createTime;
}
