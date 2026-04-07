package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 禁用状态
 *
 * @author xcs
 **/
@Data
public class DisableDTO {
    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private Integer id;

    /**
     * 是否禁用 (0否 1是)
     */
    @NotNull(message = "状态不能为空")
    private Integer isDisable;
}