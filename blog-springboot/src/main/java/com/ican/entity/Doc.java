package com.ican.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Doc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;               // 文档标题
    private Integer categoryId;        // 分类
    private String description;                // 摘要
    private String content;             // 正文内容 (HTML)
    private String leadAuthor;          // 主作者
    private Integer version;            // 版本
    private Integer views;              // 浏览量
    private Integer editCount;          // 编辑次数
    private Boolean isEditing;          // 是否正在编辑
    private LocalDateTime lastUpdateDate;
    private String tags;          // 标签
    private Integer comments;           // 评论数
    private String collaborators;
}
