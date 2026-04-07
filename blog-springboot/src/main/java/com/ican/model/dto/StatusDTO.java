package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 状态DTO
 *
 * @author xcs
 */
@Data
public class StatusDTO {
    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private Integer id;

    /**
     * 状态
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
