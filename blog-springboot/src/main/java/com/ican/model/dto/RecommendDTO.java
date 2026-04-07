package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 推荐DTO
 *
 * @author xcs
 **/
@Data
public class RecommendDTO {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    private Integer id;

    /**
     * 是否推荐 (0否 1是)
     */
    @NotNull(message = "推荐状态不能为空")
    private Integer isRecommend;
}