package com.ican.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.service.ElasticsearchService;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.ican.constant.ElasticConstant.ARTICLE_INDEX;

/**
 * es文章业务接口实现类
 *
 * @author ican
 **/
@Service
public class ElasticsearchServiceImpl implements ElasticsearchService {

    @Autowired
    private RestHighLevelClient elasticsearchClient;

    @Override
    public void addArticle(ArticleSearchVO article) {
        try {
            String json = JSON.toJSONString(article);
            IndexRequest indexRequest = new IndexRequest()
                    .index(ARTICLE_INDEX)
                    .id(article.getId().toString())
                    .source(json, XContentType.JSON);
            elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateArticle(ArticleSearchVO article) {
        try {
            UpdateRequest request = new UpdateRequest()
                    .index(ARTICLE_INDEX)
                    .doc(article)
                    .id(article.getId().toString());
            elasticsearchClient.update(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteArticle(Integer id) {
        try {
            DeleteRequest request = new DeleteRequest()
                    .index(ARTICLE_INDEX)
                    .id(id.toString());
            // 2.发送请求
            elasticsearchClient.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
