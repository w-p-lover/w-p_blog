package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 分类DTO
 *
 * @author xcs
 */
@Data
public class CategoryDTO {

    /**
     * 分类id
     */
    private Integer id;

    /**
     * 分类名
     */
    @NotBlank(message = "分类名不能为空")
    private String categoryName;

}