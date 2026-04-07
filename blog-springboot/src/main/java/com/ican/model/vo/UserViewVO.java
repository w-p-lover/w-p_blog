package com.ican.model.vo;

import lombok.Data;

/**
 * 用户浏览
 *
 * @author xcs
 **/
@Data
public class UserViewVO {

    /**
     * 日期
     */
    private String date;

    /**
     * pv
     */
    private Integer pv;

    /**
     * uv
     */
    private Integer uv;
}