package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论后台VO
 *
 * @author xcs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentBackVO {

    /**
     * 评论id
     */
    private String id;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 评论用户昵称
     */
    private String fromNickname;

    /**
     * 被回复用户昵称
     */
    private String toNickname;

    /**
     * 文章标题
     */
    private String articleTitle;

    /**
     * 评论内容
     */
    private String commentContent;

    /**
     * 评论类型
     */
    private Integer commentType;

    /**
     * 是否通过 (0否 1是)
     */
    private Integer isCheck;

    /**
     * 发表时间
     */
    private LocalDateTime createTime;

}