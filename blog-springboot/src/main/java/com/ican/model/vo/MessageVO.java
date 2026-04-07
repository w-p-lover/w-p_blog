package com.ican.model.vo;

import lombok.Data;

/**
 * 留言VO
 *
 * @author xcs
 */
@Data
public class MessageVO {

    /**
     * 留言id
     */
    private Integer id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 留言内容
     */
    private String messageContent;
}