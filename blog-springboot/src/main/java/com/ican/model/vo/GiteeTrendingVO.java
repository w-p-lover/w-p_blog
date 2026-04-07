package com.ican.model.vo;

import lombok.Data;

/**
 * Gitee趋势信息
 *
 */
@Data
public class GiteeTrendingVO {
    /**
     * id
     */
    private Long id;

    /**
     * 仓库名
     */
    private String repoName;

    /**
     * 作者
     */
    private String author;

    /**
     * stars
     */
    private Integer stars;

    /**
     * forks
     */
    private Integer forks;

    /**
     * 语言
     */
    private String language;

    /**
     * 描述
     */
    private String description;

    /**
     * url
     */
    private String url;

    /**
     * topCategory
     */
    private String projectImage;

}
