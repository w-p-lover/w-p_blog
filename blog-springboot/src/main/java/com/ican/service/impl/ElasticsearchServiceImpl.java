package com.ican.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import static com.ican.constant.ElasticConstant.ARTICLE_INDEX;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void addArticle(ArticleSearchVO article) {
        try {
            elasticsearchClient.index(i -> i
                    .index(ARTICLE_INDEX)
                    .id(article.getId().toString())
                    .document(article));
        } catch (Exception e) {
            log.error("ES 索引文章失败, id={}: {}", article.getId(), e.getMessage());
        }
    }

    @Override
    public void updateArticle(ArticleSearchVO article) {
        try {
            elasticsearchClient.update(u -> u
                    .index(ARTICLE_INDEX)
                    .id(article.getId().toString())
                    .doc(article),
                    ArticleSearchVO.class);
        } catch (Exception e) {
            log.error("ES 更新文章失败, id={}: {}", article.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteArticle(Integer articleId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(ARTICLE_INDEX)
                    .id(articleId.toString()));
        } catch (Exception e) {
            log.error("ES 删除文章失败, id={}: {}", articleId, e.getMessage());
        }
    }
}
