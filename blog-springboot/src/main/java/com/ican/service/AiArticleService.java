package com.ican.service;

/**
 * 文章 AI 处理服务
 */
public interface AiArticleService {

    /**
     * 异步处理文章内容，生成摘要和标签并入库向量。
     *
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     */
    void processArticle(Integer articleId, String title, String content);

    /**
     * 历史文章回填向量
     *
     * @return 回填成功数量
     */
    int reindexHistory();
}
