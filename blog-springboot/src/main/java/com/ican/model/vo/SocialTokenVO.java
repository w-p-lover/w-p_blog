package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 第三方token
 *
 * @author xcs
 */
@Data
@Builder
public class SocialTokenVO {

    /**
     * 开放id
     */
    private String openId;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 登录类型
     */
    private Integer loginType;

}