package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 置顶DTO
 *
 * @author xcs
 */
@Data
public class TopDTO {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private Integer id;

    /**
     * 是否置顶 (0否 1是)
     */
    @NotNull(message = "置顶状态不能为空")
    private Integer isTop;
}