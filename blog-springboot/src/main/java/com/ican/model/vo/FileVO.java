package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件后台VO
 *
 * @author xcs
 * @date 2023/03/08 12:40
 **/
@Data
public class FileVO {
    /**
     * 文件id
     */
    private Integer id;

    /**
     * 文件url
     */
    private String fileUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Integer fileSize;

    /**
     * 文件类型
     */
    private String extendName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 是否为目录 (0否 1是)
     */
    private Integer isDir;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}