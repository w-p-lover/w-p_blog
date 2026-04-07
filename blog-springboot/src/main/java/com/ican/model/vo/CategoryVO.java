package com.ican.model.vo;

import lombok.Data;

/**
 * 分类列表
 *
 * @author xcs
 **/
@Data
public class CategoryVO {

    /**
     * 分类id
     */
    private Integer id;

    /**
     * 分类名
     */
    private String categoryName;

    /**
     * 文章数量
     */
    private Integer articleCount;
}