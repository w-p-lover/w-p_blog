package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类后台VO
 *
 * @author xcs
 * @date 2022/12/03 21:43
 **/
@Data
public class CategoryBackVO {

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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}