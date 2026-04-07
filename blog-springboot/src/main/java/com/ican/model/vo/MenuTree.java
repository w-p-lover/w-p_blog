package com.ican.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 菜单下拉树
 *
 * @author xcs
 * @date 2022/12/07 17:07
 **/
@Data
public class MenuTree {

    /**
     * 菜单id
     */
    private Integer id;

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
    private List<MenuTree> children;
}