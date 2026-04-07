package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 在线用户VO
 *
 * @author xcs
 * @date 2022/12/10 11:32
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineVO {

    /**
     * 用户id
     */
    private Integer id;

    /**
     * 在线token
     */
    private String token;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * ip地址
     */
    private String ipAddress;

    /**
     * ip来源
     */
    private String ipSource;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

}