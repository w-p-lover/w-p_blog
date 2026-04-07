package com.ican.model.vo;

import lombok.Data;

/**
 * 照片VO
 *
 * @author xcs
 **/
@Data
public class PhotoVO {

    /**
     * 照片id
     */
    private Integer id;

    /**
     * 照片链接
     */
    private String photoUrl;
}