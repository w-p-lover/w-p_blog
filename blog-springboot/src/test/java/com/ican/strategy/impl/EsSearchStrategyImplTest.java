package com.ican.strategy.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.ican.exception.ServiceException;
import com.ican.handler.GlobalExceptionHandler;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.model.vo.Result;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EsSearchStrategyImplTest {

    @Test
    void searchFailureUsesBusinessFailureInsteadOfEmptySuccess() throws IOException {
        ElasticsearchTransport transport = mock(ElasticsearchTransport.class);
        doThrow(new IOException("连接失败")).when(transport).performRequest(any(), any(), any());
        EsSearchStrategyImpl strategy = new EsSearchStrategyImpl(new ElasticsearchClient(transport));

        ServiceException failure = assertThrows(ServiceException.class, () -> strategy.searchArticle("词"));
        Result<?> response = new GlobalExceptionHandler().handleServiceException(failure);

        assertFalse(response.getFlag());
        assertEquals("搜索服务暂时不可用，请稍后重试", response.getMsg());
    }

    @Test
    void realEmptySearchResultRemainsEmpty() throws IOException {
        ElasticsearchTransport transport = mock(ElasticsearchTransport.class);
        SearchResponse<ArticleSearchVO> response = SearchResponse.of(s -> s.took(1).timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(HitsMetadata.of(h -> h.hits(List.of()))));
        doReturn(response).when(transport).performRequest(any(), any(), any());

        assertTrue(new EsSearchStrategyImpl(new ElasticsearchClient(transport)).searchArticle("无匹配").isEmpty());
    }
}
