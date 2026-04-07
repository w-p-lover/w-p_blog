package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 留言DTO
 *
 * @author xcs
 * @date 2022/12/28 19:52
 **/
@Data
public class MessageDTO {

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 头像
     */
    @NotBlank(message = "头像不能为空")
    private String avatar;

    /**
     * 留言内容
     */
    @NotBlank(message = "留言内容不能为空")
    private String messageContent;
}