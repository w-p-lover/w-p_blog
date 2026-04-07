package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 标签DTO
 *
 * @author xcs
 */
@Data
public class TagDTO {

    /**
     * 标签id
     */
    private Integer id;

    /**
     * 标签名
     */
    @NotBlank(message = "标签名不能为空")
    private String tagName;

}
