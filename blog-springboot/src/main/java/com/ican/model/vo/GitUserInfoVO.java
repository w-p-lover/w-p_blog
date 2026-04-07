package com.ican.model.vo;

import lombok.Data;

/**
 * Git用户信息
 *
 * @author xcs
 */
@Data
public class GitUserInfoVO {

    /**
     * 用户id
     */
    private String id;

    /**
     * 头像
     */
    private String avatar_url;

    /**
     * 昵称
     */
    private String name;

    /**
     * 登录
     */
    private String login;
}
