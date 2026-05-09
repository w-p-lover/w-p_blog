package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HotArticleWarmupResultVO {
    private Integer requestedCount;
    private Integer warmedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private Long costMillis;
}
