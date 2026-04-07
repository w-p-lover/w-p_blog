package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 逻辑删除
 *
 * @author xcs
 */
@Data
public class DeleteDTO {

    /**
     * id列表
     */
    @NotNull(message = "id不能为空")
    private List<Integer> idList;

    /**
     * 是否删除 (0否 1是)
     */
    @NotNull(message = "状态值不能为空")
    private Integer isDelete;
}
