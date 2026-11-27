package com.ican.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GiteeTrending {
    private Long id;
    private String repoName;
    private String author;
    private Integer stars;
    private Integer forks;
    private String language;
    private String description;
    private String url;
    private String topCategory;
    private String subCategory;
    private String projectImage;
    private String orderBy;
    private LocalDateTime scrapedAt;
}
