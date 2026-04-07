package com.ican.model.vo;

import com.ican.entity.SiteConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博客信息
 *
 * @author xcs
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogInfoVO {

    /**
     * 文章数量
     */
    private Long articleCount;

    /**
     * 分类数量
     */
    private Long categoryCount;

    /**
     * 标签数量
     */
    private Long tagCount;

    /**
     * 网站访问量
     */
    private String viewCount;

    /**
     * 网站配置
     */
    private SiteConfig siteConfig;

}