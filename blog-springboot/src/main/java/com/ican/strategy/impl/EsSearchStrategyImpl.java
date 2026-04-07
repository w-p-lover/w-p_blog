package com.ican.strategy.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.strategy.SearchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.constant.ElasticConstant.*;
import static com.ican.enums.ArticleStatusEnum.PUBLIC;

@Slf4j
@Service("esSearchStrategyImpl")
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class EsSearchStrategyImpl implements SearchStrategy {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public List<ArticleSearchVO> searchArticle(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new ArrayList<>();
        }
        try {
            SearchResponse<ArticleSearchVO> response = elasticsearchClient.search(s -> s
                    .index(ARTICLE_INDEX)
                    .query(q -> q.bool(b -> b
                            .must(m -> m.match(mm -> mm.field("all").query(keyword)))
                            .must(m -> m.term(t -> t.field("isDelete").value((long) FALSE)))
                            .must(m -> m.term(t -> t.field("status")
                                    .value((long) (int) PUBLIC.getStatus())))
                    ))
                    .highlight(h -> h
                            .requireFieldMatch(false)
                            .fields(ARTICLE_TITLE, f -> f
                                    .preTags(PRE_TAG).postTags(POST_TAG))
                            .fields(ARTICLE_CONTENT, f -> f
                                    .preTags(PRE_TAG).postTags(POST_TAG))
                    ),
                    ArticleSearchVO.class);

            return response.hits().hits().stream()
                    .map(this::mapHitToVO)
                    .toList();
        } catch (Exception e) {
            log.error("ES 搜索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private ArticleSearchVO mapHitToVO(Hit<ArticleSearchVO> hit) {
        ArticleSearchVO vo = hit.source();
        if (vo == null) return new ArticleSearchVO();
        Map<String, List<String>> highlights = hit.highlight();
        if (highlights.containsKey(ARTICLE_TITLE)) {
            vo.setArticleTitle(highlights.get(ARTICLE_TITLE).get(0));
        }
        if (highlights.containsKey(ARTICLE_CONTENT)) {
            vo.setArticleContent(highlights.get(ARTICLE_CONTENT).get(0));
        } else if (vo.getArticleContent() != null
                && vo.getArticleContent().length() > 300) {
            vo.setArticleContent(vo.getArticleContent().substring(0, 300));
        }
        return vo;
    }
}
