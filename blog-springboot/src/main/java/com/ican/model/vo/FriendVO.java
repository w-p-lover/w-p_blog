package com.ican.model.vo;

import lombok.Data;

/**
 * 友链VO
 *
 * @author xcs
 **/
@Data
public class FriendVO {

    /**
     * 友链id
     */
    private Integer id;

    /**
     * 友链颜色
     */
    private String color;

    /**
     * 友链名称
     */
    private String name;

    /**
     * 友链头像
     */
    private String avatar;

    /**
     * 友链地址
     */
    private String url;

    /**
     * 友链介绍
     */
    private String introduction;

}