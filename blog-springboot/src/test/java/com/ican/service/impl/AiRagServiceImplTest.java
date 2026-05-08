package com.ican.service.impl;

import com.ican.model.vo.AiChatResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRagServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    private ChatClient chatClient;

    private AiRagServiceImpl aiRagService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        aiRagService = new AiRagServiceImpl(chatClient, vectorStore);
    }

    @Test
    void chat_shouldReturnFallbackWhenNoDocumentsFound() {
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of());

        AiChatResponseVO response = aiRagService.chat("缓存策略是什么");

        assertNotNull(response);
        assertEquals("博客中暂无相关内容", response.getAnswer());
        assertTrue(response.getSources().isEmpty());
    }

    @Test
    void chat_shouldReturnFallbackWhenDocumentsHaveNoUsableText() {
        Document emptyDoc1 = new Document("   ", Map.of("title", "文档1", "url", "/article/1"));
        Document emptyDoc2 = new Document("\n\t", Map.of("title", "文档2", "url", "/article/2"));
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of(emptyDoc1, emptyDoc2));

        AiChatResponseVO response = aiRagService.chat("RAG问题");

        assertNotNull(response);
        assertEquals("博客中暂无相关内容", response.getAnswer());
        assertTrue(response.getSources().isEmpty());
        verify(chatClient, never()).prompt(anyString());
    }

    @Test
    void chat_shouldFallbackWhenModelReturnsBlankAnswer() {
        Document doc = new Document("Spring AI 介绍", Map.of("title", "Spring AI 入门", "url", "/article/1"));
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of(doc));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("   ");
        clearInvocations(chatClient);

        AiChatResponseVO response = aiRagService.chat("如何实现RAG问答");

        assertNotNull(response);
        assertEquals("博客中暂无相关内容", response.getAnswer());
        assertEquals(1, response.getSources().size());
        assertEquals("Spring AI 入门", response.getSources().get(0).getTitle());
        assertEquals("/article/1", response.getSources().get(0).getUrl());
        verify(chatClient, times(1)).prompt(anyString());
    }

    @Test
    void chat_shouldReturnModelAnswerAndMappedSourcesWhenDocumentsFound() {
        Document doc1 = new Document("Spring AI 介绍", Map.of("title", "Spring AI 入门", "url", "/article/1"));
        Document doc2 = new Document("RAG 实践", Map.of("title", "RAG 实践", "url", "/article/2"));
        Document duplicateDoc = new Document("重复来源", Map.of("title", "Spring AI 入门", "url", "/article/1"));

        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2, duplicateDoc));
        when(chatClient.prompt(anyString()).call().content()).thenReturn("这是模型回答");
        clearInvocations(chatClient);

        AiChatResponseVO response = aiRagService.chat("如何实现RAG问答");

        assertNotNull(response);
        assertEquals("这是模型回答", response.getAnswer());
        assertEquals(2, response.getSources().size());
        assertEquals("Spring AI 入门", response.getSources().get(0).getTitle());
        assertEquals("/article/1", response.getSources().get(0).getUrl());
        assertEquals("RAG 实践", response.getSources().get(1).getTitle());
        assertEquals("/article/2", response.getSources().get(1).getUrl());
        verify(chatClient, times(1)).prompt(anyString());
    }
}
