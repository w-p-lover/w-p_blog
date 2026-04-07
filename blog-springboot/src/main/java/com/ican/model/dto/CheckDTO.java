package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 审核DTO
 *
 * @author xcs
 */
@Data
public class CheckDTO {

    /**
     * id集合
     */
    @NotNull(message = "id不能为空")
    private List<Integer> idList;

    /**
     * 是否通过 (0否 1是)
     */
    @NotNull(message = "状态值不能为空")
    private Integer isCheck;
}
