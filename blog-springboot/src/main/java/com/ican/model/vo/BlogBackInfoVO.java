package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 网站后台信息VO
 *
 * @author xcs
 **/
@Data
@Builder
public class BlogBackInfoVO {

    /**
     * 访问量
     */
    private Integer viewCount;

    /**
     * 留言量
     */
    private Long messageCount;

    /**
     * 用户量
     */
    private Long userCount;

    /**
     * 文章量
     */
    private Long articleCount;

    /**
     * 分类统计
     */
    private List<CategoryVO> categoryVOList;

    /**
     * 标签列表
     */
    private List<TagOptionVO> tagVOList;

    /**
     * 文章贡献统计
     */
    private List<ArticleStatisticsVO> articleStatisticsList;

    /**
     * 文章浏览量排行
     */
    private List<ArticleRankVO> articleRankVOList;

    /**
     * 一周访问量
     */
    private List<UserViewVO> userViewVOList;
}