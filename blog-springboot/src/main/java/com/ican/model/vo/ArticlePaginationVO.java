package com.ican.model.vo;

import lombok.Data;

/**
 * 文章上下篇
 *
 * @author xcs
 */
@Data
public class ArticlePaginationVO {

    /**
     * 文章id
     */
    private Integer id;

    /**
     * 文章缩略图
     */
    private String articleCover;

    /**
     * 文章标题
     */
    private String articleTitle;
}
