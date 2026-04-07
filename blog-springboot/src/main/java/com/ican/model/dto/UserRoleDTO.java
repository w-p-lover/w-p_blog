package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户角色DTO
 *
 * @author xcs
 **/
@Data
public class UserRoleDTO {

    /**
     * 用户id
     */
    @NotNull(message = "用户id不能为空")
    private Integer id;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 角色id
     */
    @NotNull(message = "角色id不能为空")
    private List<String> roleIdList;
}