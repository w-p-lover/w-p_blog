package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleHotScoreVO {
    private Integer articleId;
    private String articleTitle;
    private Double hotScore;
}
