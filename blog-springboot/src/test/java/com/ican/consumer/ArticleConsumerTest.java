package com.ican.consumer;

import com.ican.service.ElasticsearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ArticleConsumerTest {

    private ElasticsearchService service;
    private ArticleConsumer consumer;

    @BeforeEach
    void setUp() {
        service = mock(ElasticsearchService.class);
        consumer = new ArticleConsumer(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INSERT", "UPDATE", "DELETE"})
    void syncsEveryRowAndAcceptsRealCanalTimestamp(String type) {
        consumer.listenSaveArticle(message(type, "[{\"id\":\"11\"},{\"id\":12},{\"id\":13}]"));

        verify(service).syncArticle(11);
        verify(service).syncArticle(12);
        verify(service).syncArticle(13);
        verifyNoMoreInteractions(service);
    }

    @Test
    void partialFailurePropagatesAndReplayRetriesTheWholeBatch() {
        Message message = message("UPDATE", "[{\"id\":11},{\"id\":12},{\"id\":13}]");
        doThrow(new IllegalStateException("ES 故障")).doNothing().when(service).syncArticle(12);

        assertThrows(IllegalStateException.class, () -> consumer.listenSaveArticle(message));
        verify(service, never()).syncArticle(13);
        consumer.listenSaveArticle(message);

        verify(service, times(2)).syncArticle(11);
        verify(service, times(2)).syncArticle(12);
        verify(service).syncArticle(13);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "[{\"id\":11},{}]", "[{\"id\":0}]"})
    void rejectsMalformedBatchesBeforeApplyingAnyRow(String rows) {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.listenSaveArticle(message("UPDATE", rows)));

        verifyNoInteractions(service);
    }

    @Test
    void ignoresDdlAndUnrelatedTables() {
        consumer.listenSaveArticle(raw("{\"isDdl\":true,\"table\":\"t_article\"}"));
        consumer.listenSaveArticle(raw("{\"isDdl\":false,\"table\":\"t_comment\",\"type\":\"DELETE\"}"));

        verifyNoInteractions(service);
    }

    private Message message(String type, String rows) {
        return raw("{\"isDdl\":false,\"table\":\"t_article\",\"type\":\"" + type
                + "\",\"es\":1792140743101,\"data\":" + rows + "}");
    }

    private Message raw(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
