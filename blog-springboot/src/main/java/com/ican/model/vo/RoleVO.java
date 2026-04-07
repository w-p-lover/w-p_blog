package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色VO
 *
 * @author xcs
 * @date 2022/12/07 09:50
 **/
@Data
public class RoleVO {

    /**
     * 角色id
     */
    private String id;

    /**
     * 角色名
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 是否禁用 (0否 1是)
     */
    private Integer isDisable;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}