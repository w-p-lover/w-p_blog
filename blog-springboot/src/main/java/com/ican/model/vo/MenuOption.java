package com.ican.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 菜单选项树
 *
 * @author xcs
 * @date 2022/12/23 16:35
 **/
@Data
public class MenuOption {

    /**
     * id
     */
    private Integer value;

    /**
     * 父菜单id
     */
    @JsonIgnore
    private Integer parentId;

    /**
     * 菜单名称
     */
    private String label;

    /**
     * 子菜单树
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MenuOption> children;
}