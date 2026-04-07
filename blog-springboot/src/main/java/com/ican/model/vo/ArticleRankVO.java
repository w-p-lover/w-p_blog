package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文章浏览量排行
 *
 * @author xcs
 */
@Data
@Builder
public class ArticleRankVO {

    /**
     * 标题
     */
    private String articleTitle;

    /**
     * 浏览量
     */
    private Integer viewCount;

}
