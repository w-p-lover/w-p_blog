package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 目录DTO
 *
 * @author xcs
 * @date 2023/03/08 14:04
 **/
@Data
public class FolderDTO {

    /**
     * 文件路径
     */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    /**
     * 文件名称
     */
    @NotBlank(message = "文件名称不能为空")
    private String fileName;
}