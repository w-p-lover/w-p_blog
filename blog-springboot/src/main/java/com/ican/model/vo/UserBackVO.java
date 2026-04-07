package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户后台VO
 *
 * @author xcs
 * @date 2022/12/10 10:40
 **/
@Data
public class UserBackVO {

    /**
     * 用户id
     */
    private Integer id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 登录ip
     */
    private String ipAddress;

    /**
     * 登录地址
     */
    private String ipSource;

    /**
     * 登录方式 (1邮箱 2QQ 3Gitee 4Github)
     */
    private Integer loginType;

    /**
     * 用户角色
     */
    private List<UserRoleVO> roleList;

    /**
     * 是否禁用 (0否 1是)
     */
    private Integer isDisable;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}