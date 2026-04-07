package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 照片信息DTO
 *
 * @author xcs
 **/
@Data
public class PhotoInfoDTO {

    /**
     * 照片id
     */
    @NotNull(message = "照片id不能为空")
    private Integer id;

    /**
     * 照片名
     */
    @NotBlank(message = "照片名不能为空")
    private String photoName;

    /**
     * 照片描述
     */
    private String photoDesc;
}