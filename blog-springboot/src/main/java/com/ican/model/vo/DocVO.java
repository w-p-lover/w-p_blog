package com.ican.model.vo;

import com.ican.model.dto.DocDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocVO {
    private Long id;
    private String title;
    private String categoryName;
    private String description;
    private String content;
    private String leadAuthor;
    private Integer version;
    private Integer views;
    private Integer editCount;
    private Boolean isEditing;
    private LocalDateTime lastUpdateDate;
    private List<String> tags;
    private List<DocDTO.CollabDTO> collaborators;
    private Integer comments;


    private Integer currentVersion;     // 已发布版本号
    private Integer draftVersion;       // 正在编辑的草稿最新号

}
