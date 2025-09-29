package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocVersionVO {
    private Long id;
    private Long docId;
    private String version;        // 拼接后的版本号，如 "2.3"
    private String author;
    private String status;
    private LocalDateTime createdAt;
    private String content;
    private String description;
}
