package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 后台登录用户信息
 *
 * @author xcs
 * @date 2022/12/05 16:10
 **/
@Data
@Builder
public class UserBackInfoVO {

    /**
     * 用户id
     */
    private Integer id;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色
     */
    private List<String> roleList;

    /**
     * 权限标识
     */
    private List<String> permissionList;

}