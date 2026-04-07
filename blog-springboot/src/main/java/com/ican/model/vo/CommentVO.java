package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论VO
 *
 * @author xcs
 **/
@Data
public class CommentVO {

    /**
     * 评论id
     */
    private Integer id;

    /**
     * 评论用户id
     */
    private Integer fromUid;

    /**
     * 昵称
     */
    private String fromNickname;

    /**
     * 个人网站
     */
    private String webSite;

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
     * 回复量
     */
    private Integer replyCount;

    /**
     * 回复列表
     */
    private List<ReplyVO> replyVOList;

    /**
     * 评论时间
     */
    private LocalDateTime createTime;
}
