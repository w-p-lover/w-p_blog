package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 角色状态
 *
 * @author xcs
 * @date 2022/12/19 10:31
 **/
@Data
public class RoleStatusDTO {
    /**
     * 角色id
     */
    @NotNull(message = "角色id不能为空")
    private String id;

    /**
     * 是否禁用 (0否 1是)
     */
    @NotNull(message = "角色状态不能为空")
    private Integer isDisable;
}