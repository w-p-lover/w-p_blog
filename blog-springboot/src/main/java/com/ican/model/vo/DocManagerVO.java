package com.ican.model.vo;

import com.ican.model.dto.DocDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocManagerVO {
    private Long id;
    private String title;
    private String categoryName;
    private String description;
    private String content;
    private String leadAuthor;
    private String version;
    private Integer views;
    private Integer editCount;
    private LocalDateTime lastUpdateDate;
    private List<String> tags;
    private Integer comments;
    private String status;
    private String rejectReason;

    private Integer currentVersion;     // 已发布版本号
    private Integer draftVersion;       // 正在编辑的草稿最新号

}
