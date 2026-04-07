package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志VO
 *
 * @author xcs
 */
@Data
public class OperationLogVO {

    /**
     * 操作日志id
     */
    private Integer id;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作uri
     */
    private String uri;

    /**
     * 操作类型
     */
    private String type;

    /**
     * 操作方法
     */
    private String name;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 返回数据
     */
    private String data;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 操作ip
     */
    private String ipAddress;

    /**
     * 操作地址
     */
    private String ipSource;

    /**
     * 操作耗时 (毫秒)
     */
    private Long times;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}