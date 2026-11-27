package com.ican.service;

import com.ican.entity.AiTask;
import com.ican.entity.Article;

/**
 * 文章 AI 处理服务
 */
public interface AiArticleService {

    /**
     * 异步处理文章内容，生成摘要和标签并入库向量。
     *
     * @param task 已认领且绑定源版本的任务
     * @param article 同一源版本的作者正文
     * @return 是否已完成向量准备（最终发布与任务终态由独立短事务完成）
     */
    boolean processArticle(AiTask task, Article article);

    /**
     * 历史文章回填向量（默认实验数量）
     *
     * @return 回填成功数量
     */
    int reindexHistory();

    /**
     * 历史文章回填向量（按文章数量限制）
     *
     * @param articleLimit 本次最多回填的文章数
     * @return 回填成功数量
     */
    int reindexHistory(int articleLimit);
}
