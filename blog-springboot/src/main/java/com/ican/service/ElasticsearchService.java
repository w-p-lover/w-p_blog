package com.ican.service;

/**
 * es文章业务接口
 *
 * @author xcs
 */
public interface ElasticsearchService {

    /**
     * 按数据库当前状态同步文章；重复执行不会追加文档，失败必须向调用方传播。
     *
     * @param articleId 文章id
     */
    void syncArticle(Integer articleId);

}
