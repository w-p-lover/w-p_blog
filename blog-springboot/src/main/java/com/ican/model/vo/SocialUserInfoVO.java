package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 第三方账号信息
 *
 * @author xcs
 */
@Data
@Builder
public class SocialUserInfoVO {

    /**
     * id
     */
    private String id;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickname;
}