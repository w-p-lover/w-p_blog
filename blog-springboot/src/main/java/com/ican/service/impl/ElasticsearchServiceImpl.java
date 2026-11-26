package com.ican.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ican.entity.Article;
import com.ican.mapper.ArticleMapper;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import static com.ican.constant.ElasticConstant.ARTICLE_INDEX;
import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.enums.ArticleStatusEnum.PUBLIC;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;

    private final ArticleMapper articleMapper;

    @Override
    public void syncArticle(Integer articleId) {
        Article article = articleMapper.selectById(articleId);
        try {
            if (article == null || !PUBLIC.getStatus().equals(article.getStatus())
                    || !Integer.valueOf(FALSE).equals(article.getIsDelete())) {
                elasticsearchClient.delete(d -> d.index(ARTICLE_INDEX).id(articleId.toString()));
                return;
            }
            ArticleSearchVO document = new ArticleSearchVO();
            BeanUtils.copyProperties(article, document);
            // 全文覆盖兼容缺失文档和整批重放，避免 UPDATE 在首次同步遗漏后持续失败。
            elasticsearchClient.index(i -> i.index(ARTICLE_INDEX).id(articleId.toString()).document(document));
        } catch (Exception e) {
            throw new IllegalStateException("文章搜索同步失败, id=" + articleId, e);
        }
    }
}
