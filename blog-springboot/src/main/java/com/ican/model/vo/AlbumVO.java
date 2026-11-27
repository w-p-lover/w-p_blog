package com.ican.model.vo;

import lombok.Data;

/**
 * 相册
 *
 * @author xcs
 **/
@Data
public class AlbumVO {
    /**
     * 相册id
     */
    private Integer id;

    /**
     * 相册名
     */
    private String albumName;

    /**
     * 相册描述
     */
    private String albumDesc;

    /**
     * 相册封面
     */
    private String albumCover;
}