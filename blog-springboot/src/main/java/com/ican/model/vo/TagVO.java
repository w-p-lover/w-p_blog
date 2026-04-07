package com.ican.model.vo;

import lombok.Data;

/**
 * 标签VO
 *
 * @author xcs
 **/
@Data
public class TagVO {

    /**
     * 标签id
     */
    private Integer id;

    /**
     * 标签名
     */
    private String tagName;

    /**
     * 文章数量
     */
    private Integer articleCount;
}