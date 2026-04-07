package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章搜索VO
 *
 * @author xcs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchVO {

    /**
     * 文章id
     */
    private Integer id;

    /**
     * 文章标题
     */
    private String articleTitle;

    /**
     * 文章内容
     */
    private String articleContent;

    /**
     * 是否删除
     */
    private Integer isDelete;

    /**
     * 文章状态
     */
    private Integer status;

    /**
     * 索引名
     */
    private Integer categoryId;
}
