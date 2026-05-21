package com.ican.model.dto;

import lombok.Data;

/**
 * 文章AI处理消息
 */
@Data
public class ArticleAiMessage {

    private Integer taskId;

    private Integer articleId;

    private String articleTitle;

    private String articleContent;
}
