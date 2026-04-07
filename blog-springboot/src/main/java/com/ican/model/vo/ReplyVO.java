package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回复VO
 *
 * @author xcs
 **/
@Data
public class ReplyVO {

    /**
     * 评论id
     */
    private Integer id;

    /**
     * 父级评论id
     */
    private Integer parentId;

    /**
     * 评论用户id
     */
    private Integer fromUid;

    /**
     * 被评论用户id
     */
    private Integer toUid;

    /**
     * 评论用户昵称
     */
    private String fromNickname;

    /**
     * 个人网站
     */
    private String webSite;

    /**
     * 被评论用户昵称
     */
    private String toNickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 评论内容
     */
    private String commentContent;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论时间
     */
    private LocalDateTime createTime;
}

