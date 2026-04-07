package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 相册DTO
 *
 * @author xcs
 * @date 2022/12/30 15:42
 **/
@Data
public class AlbumDTO {

    /**
     * 相册id
     */
    private Integer id;

    /**
     * 相册名
     */
    @NotBlank(message = "相册名不能为空")
    private String albumName;

    /**
     * 相册描述
     */
    private String albumDesc;

    /**
     * 相册封面
     */
    @NotBlank(message = "相册封面不能为空")
    private String albumCover;

    /**
     * 状态 (1公开 2私密)
     */
    private Integer status;
}