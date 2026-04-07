package com.ican.model.vo;

import lombok.Data;

/**
 * 文章贡献统计
 *
 * @author xcs
 */
@Data
public class ArticleStatisticsVO {

    /**
     * 日期
     */
    private String date;

    /**
     * 数量
     */
    private Integer count;
}
