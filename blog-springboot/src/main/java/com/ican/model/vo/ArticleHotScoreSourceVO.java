package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleHotScoreSourceVO {
    private Integer articleId;
    private String articleTitle;
    private Integer views;
    private Integer isTop;
    private Integer isRecommend;
    private LocalDateTime createTime;
}
