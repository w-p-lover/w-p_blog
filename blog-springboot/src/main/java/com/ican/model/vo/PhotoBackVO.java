package com.ican.model.vo;

import lombok.Data;

/**
 * 后台照片VO
 *
 * @author xcs
 * @date 2022/12/30 20:56
 **/
@Data
public class PhotoBackVO {

    /**
     * 照片id
     */
    private Integer id;

    /**
     * 照片名
     */
    private String photoName;

    /**
     * 照片描述
     */
    private String photoDesc;

    /**
     * 照片地址
     */
    private String photoUrl;
}