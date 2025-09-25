package com.ican.model.dto;

import lombok.Data;

@Data
public class DocVersionDTO {
    private Long docId;
    private String content;
    private Integer baseMajor;     // 提交时的基线版本
    private Integer baseMinor;
    private String author;
}
