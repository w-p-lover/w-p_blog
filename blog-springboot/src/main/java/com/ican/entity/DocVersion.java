package com.ican.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocVersion {
    private Long id;
    private Long docId;
    private Integer majorVersion;    // 主版本
    private Integer minorVersion;    // 小版本
    private String content;
    private String author;
    private Integer baseMajor;       // 编辑时基线主版本
    private Integer baseMinor;       // 编辑时基线小版本
    private String status;           // DRAFT / PENDING / PUBLISHED
    private LocalDateTime createdAt;
}
