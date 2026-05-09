package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArticleHotScoreRefreshResultVO {
    private Integer refreshedCount;
    private Long costMillis;
}
