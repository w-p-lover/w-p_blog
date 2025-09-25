package com.ican.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocVersionVO {
    private Long id;
    private Long docId;
    private String version;        // 拼接后的版本号，如 "2.3"
    private String author;
    private String status;
    private LocalDateTime createdAt;
}
