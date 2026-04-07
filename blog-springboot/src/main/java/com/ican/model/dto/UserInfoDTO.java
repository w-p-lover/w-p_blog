package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户信息
 *
 * @author xcs
 */
@Data
public class UserInfoDTO {

    /**
     * 用户昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 个人网站
     */
    private String webSite;

    /**
     * 个人简介
     */
    private String intro;
}
