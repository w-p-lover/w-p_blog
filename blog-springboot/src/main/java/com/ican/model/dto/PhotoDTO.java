package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 照片DTO
 *
 * @author xcs
 **/
@Data
public class PhotoDTO {

    /**
     * 相册id
     */
    @NotNull(message = "相册id不能为空")
    private Integer albumId;

    /**
     * 照片链接
     */
    private List<String> photoUrlList;

    /**
     * 照片id
     */
    private List<Integer> photoIdList;
}