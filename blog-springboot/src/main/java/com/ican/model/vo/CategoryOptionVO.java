package com.ican.model.vo;

import lombok.Data;

/**
 * 分类选项VO
 *
 * @author xcs
 **/
@Data
public class CategoryOptionVO {

    /**
     * 分类id
     */
    private Integer id;

    /**
     * 分类名
     */
    private String categoryName;
}