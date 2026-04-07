package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * Git信息
 *
 * @author xcs
 **/
@Data
public class GitDTO {

    /**
     * code
     */
    @NotBlank(message = "code不能为空")
    private String code;
}