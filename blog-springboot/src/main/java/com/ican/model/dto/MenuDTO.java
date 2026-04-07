package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 菜单DTO
 *
 * @author xcs
 * @date 2022/12/08 11:57
 **/
@Data
public class MenuDTO {

    /**
     * 菜单id
     */
    private Integer id;

    /**
     * 父菜单id
     */
    private Integer parentId;

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    /**
     * 类型（M目录 C菜单 B按钮）
     */
    @NotBlank(message = "类型不能为空")
    private String menuType;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 菜单组件
     */
    private String component;

    /**
     * 是否隐藏 (0否 1是)
     */
    private Integer isHidden;

    /**
     * 是否禁用 (0否 1是)
     */
    private Integer isDisable;

    /**
     * 菜单排序
     */
    @NotNull(message = "菜单排序不能为空")
    private Integer orderNum;

    /**
     * 权限标识
     */
    private String perms;

}