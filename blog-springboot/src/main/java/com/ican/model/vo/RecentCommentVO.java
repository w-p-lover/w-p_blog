package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 最新评论
 *
 * @author xcs
 **/
@Data
public class RecentCommentVO {

    /**
     * 评论id
     */
    private Integer id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 评论内容
     */
    private String commentContent;

    /**
     * 评论时间
     */
    private LocalDateTime createTime;
}