package com.ican.model.vo;

/**
 * @author xcs
 * @date 2023/02/07 11:29
 **/

import lombok.Data;

/**
 * 回复数VO
 *
 * @author xcs
 */
@Data
public class ReplyCountVO {

    /**
     * 评论id
     */
    private Integer commentId;

    /**
     * 回复数
     */
    private Integer replyCount;
}
