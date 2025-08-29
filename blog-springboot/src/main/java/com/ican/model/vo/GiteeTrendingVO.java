package com.ican.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Gitee趋势信息
 *
 */
@Data
@ApiModel(description = "Gitee趋势信息")
public class GiteeTrendingVO {
    /**
     * id
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 仓库名
     */
    @ApiModelProperty(value = "仓库名")
    private String repoName;

    /**
     * 作者
     */
    @ApiModelProperty(value = "作者")
    private String author;

    /**
     * stars
     */
    @ApiModelProperty(value = " stars")
    private Integer stars;

    /**
     * forks
     */
    @ApiModelProperty(value = "forks")
    private Integer forks;

    /**
     * 语言
     */
    @ApiModelProperty(value = "语言")
    private String language;

    /**
     * 描述
     */
    @ApiModelProperty(value = "描述")
    private String description;

    /**
     * url
     */
    @ApiModelProperty(value = "url")
    private String url;

    /**
     * topCategory
     */
    @ApiModelProperty(value = "topCategory")
    private String projectImage;

}
