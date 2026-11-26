package com.ican.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.transport.ElasticsearchTransport;
import com.ican.consumer.ArticleConsumer;
import com.ican.entity.Article;
import com.ican.mapper.ArticleMapper;
import com.ican.model.vo.ArticleSearchVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ElasticsearchServiceImplTest {

    private ArticleMapper mapper;
    private ElasticsearchTransport transport;
    private ElasticsearchServiceImpl service;
    private List<Object> requests;

    @BeforeEach
    void setUp() throws IOException {
        mapper = mock(ArticleMapper.class);
        transport = mock(ElasticsearchTransport.class);
        service = new ElasticsearchServiceImpl(new ElasticsearchClient(transport), mapper);
        requests = new ArrayList<>();
        doAnswer(invocation -> {
            requests.add(invocation.getArgument(0));
            return null;
        }).when(transport).performRequest(any(), any(), any());
    }

    @Test
    void lateOldEventAndRepeatedReplayUseCurrentDatabaseContent() {
        when(mapper.selectById(11)).thenReturn(article(1, 0, "新正文"));
        ArticleConsumer consumer = new ArticleConsumer(service);
        Message oldEvent = oldEvent();

        consumer.listenSaveArticle(oldEvent);
        consumer.listenSaveArticle(oldEvent);

        assertEquals(2, requests.size());
        for (Object request : requests) {
            IndexRequest<?> index = assertInstanceOf(IndexRequest.class, request);
            assertEquals("11", index.id());
            assertEquals("article", index.index());
            ArticleSearchVO document = assertInstanceOf(ArticleSearchVO.class, index.document());
            assertEquals("新正文", document.getArticleContent());
            assertEquals("当前标题", document.getArticleTitle());
            assertEquals(8, document.getCategoryId());
        }
    }

    @ParameterizedTest
    @CsvSource({"2,0", "3,0", "1,1"})
    void oldPublicEventRemovesCurrentlyWithdrawnArticle(int status, int isDelete) {
        when(mapper.selectById(11)).thenReturn(article(status, isDelete, "不应再索引的正文"));

        new ArticleConsumer(service).listenSaveArticle(oldEvent());

        DeleteRequest request = assertInstanceOf(DeleteRequest.class, requests.get(0));
        assertEquals("11", request.id());
        assertEquals(1, requests.size());
    }

    @Test
    void repeatedPhysicalDeleteIsSafe() {
        when(mapper.selectById(11)).thenReturn(null);

        service.syncArticle(11);
        service.syncArticle(11);

        assertEquals(2, requests.size());
        assertTrue(requests.stream().allMatch(DeleteRequest.class::isInstance));
    }

    @Test
    void outageMidBatchCanBeReplayedToACompleteIndex() throws IOException {
        when(mapper.selectById(11)).thenReturn(article(1, 0, "第一篇"));
        Article second = article(1, 0, "第二篇");
        second.setId(12);
        when(mapper.selectById(12)).thenReturn(second);
        Map<String, ArticleSearchVO> index = new HashMap<>();
        doAnswer(invocation -> {
            IndexRequest<?> request = invocation.getArgument(0);
            if ("12".equals(request.id()) && !index.containsKey("12")) {
                index.put("12", null);
                throw new IOException("第二篇同步时 ES 故障");
            }
            index.put(request.id(), (ArticleSearchVO) request.document());
            return null;
        }).when(transport).performRequest(any(), any(), any());
        ArticleConsumer consumer = new ArticleConsumer(service);
        Message batch = new Message(("{\"isDdl\":false,\"table\":\"t_article\",\"type\":\"UPDATE\","
                + "\"data\":[{\"id\":11},{\"id\":12}]}").getBytes(StandardCharsets.UTF_8), new MessageProperties());

        assertThrows(IllegalStateException.class, () -> consumer.listenSaveArticle(batch));
        consumer.listenSaveArticle(batch);

        assertEquals(2, index.size());
        assertEquals("第一篇", index.get("11").getArticleContent());
        assertEquals("第二篇", index.get("12").getArticleContent());
    }

    @Test
    void deleteFailurePropagatesInsteadOfAcknowledgingSuccess() throws IOException {
        doThrow(new IOException("ES 不可用")).when(transport).performRequest(any(), any(), any());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.syncArticle(11));

        assertInstanceOf(IOException.class, error.getCause());
    }

    private Article article(int status, int isDelete, String content) {
        return Article.builder().id(11).status(status).isDelete(isDelete).articleTitle("当前标题")
                .articleContent(content).categoryId(8).build();
    }

    private Message oldEvent() {
        return new Message(("{\"isDdl\":false,\"table\":\"t_article\",\"type\":\"UPDATE\","
                + "\"data\":[{\"id\":11,\"article_content\":\"旧正文\",\"status\":1,\"is_delete\":0}]}")
                .getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
