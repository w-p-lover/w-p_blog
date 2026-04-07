package com.ican.model.vo;

import lombok.Data;

/**
 * 评论数量VO
 *
 * @author xcs
 **/
@Data
public class CommentCountVO {
    /**
     * 类型id
     */
    private Integer id;

    /**
     * 评论数量
     */
    private Integer commentCount;
}