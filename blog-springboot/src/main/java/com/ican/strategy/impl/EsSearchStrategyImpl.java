package com.ican.strategy.impl;

import com.alibaba.fastjson2.JSON;
import com.ican.mapper.ArticleMapper;
import com.ican.model.vo.ArticleInfoVO;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.strategy.SearchStrategy;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.constant.ElasticConstant.*;
import static com.ican.enums.ArticleStatusEnum.PUBLIC;

@Log4j2
@Service("esSearchStrategyImpl")
public class EsSearchStrategyImpl implements SearchStrategy {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public List<ArticleSearchVO> searchArticle(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new ArrayList<>();
        }
        try {
            // 构建查询条件
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("all", keyword))
                    .must(QueryBuilders.termQuery("isDelete", FALSE))
                    .must(QueryBuilders.termQuery("status", PUBLIC.getStatus()));

            // 高亮设置
            HighlightBuilder highlightBuilder = new HighlightBuilder()
                    .field(new HighlightBuilder.Field(ARTICLE_TITLE).preTags(PRE_TAG).postTags(POST_TAG))
                    .field(new HighlightBuilder.Field(ARTICLE_CONTENT).preTags(PRE_TAG).postTags(POST_TAG))
                    .requireFieldMatch(false);

            // 构建搜索请求
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .query(boolQueryBuilder)
                    .highlighter(highlightBuilder);

            SearchRequest searchRequest = new SearchRequest(ARTICLE_INDEX)
                    .source(searchSourceBuilder);

            // 执行搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 解析结果
            return handleResponse(searchResponse);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<ArticleSearchVO> handleResponse(SearchResponse response) {
        // 解析结果并返回
        return Arrays.stream(response.getHits().getHits())
                .map(hit -> {
                    String articleId = hit.getId();
                    ArticleSearchVO articleSearchVO = new ArticleSearchVO();
                    String articleTitle = articleMapper.selectArticleInfoById(Integer.valueOf(articleId)).getArticleTitle();
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();

                    articleSearchVO.setId(Integer.valueOf(articleId));
                    articleSearchVO.setArticleTitle(articleTitle);
                    if (highlightFields.containsKey(ARTICLE_TITLE)) {
                        articleSearchVO.setArticleTitle(highlightFields.get(ARTICLE_TITLE).fragments()[0].string());
                    }
                    if (highlightFields.containsKey(ARTICLE_CONTENT)) {
                        articleSearchVO.setArticleContent(highlightFields.get(ARTICLE_CONTENT).fragments()[0].string());
                    } else {
                        // 假设有一个从JSON转换的方法
                        articleSearchVO = JSON.parseObject(hit.getSourceAsString(), ArticleSearchVO.class);
                        articleSearchVO.setArticleContent(articleSearchVO.getArticleContent().substring(0, 300));
                    }
                    return articleSearchVO;
                })
                .collect(Collectors.toList());
    }

}
