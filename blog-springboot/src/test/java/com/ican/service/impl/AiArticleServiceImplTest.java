package com.ican.service.impl;

import com.ican.mapper.ArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AiArticleServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ArticleMapper articleMapper;

    private ChatClient chatClient;

    private AiArticleServiceImpl aiArticleService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        aiArticleService = new AiArticleServiceImpl(chatClient, vectorStore, articleMapper);
    }

    @Test
    void processArticle_shouldStoreVectorAndUpdateArticleAiResult() {
        when(chatClient.prompt(anyString()).call().content())
                .thenReturn("这是摘要")
                .thenReturn("Java,Spring Boot,AI");

        aiArticleService.processArticle(1, "测试标题", "测试内容");

        verify(chatClient).prompt(anyString());
        verify(vectorStore).add(any(List.class));
        verify(articleMapper).updateArticleAiResult(anyInt(), anyString(), anyString(), any());
    }

    @Test
    void processArticle_shouldDoNothingWhenContentIsBlank() {
        aiArticleService.processArticle(1, "测试标题", "   ");

        verify(chatClient, never()).prompt(anyString());
        verify(vectorStore, never()).add(any(List.class));
        verify(articleMapper, never()).updateArticleAiResult(anyInt(), anyString(), anyString(), any());
    }

    @Test
    void processArticle_shouldDoNothingWhenArticleIdIsNull() {
        aiArticleService.processArticle(null, "测试标题", "测试内容");

        verify(chatClient, never()).prompt(anyString());
        verify(vectorStore, never()).add(any(List.class));
        verify(articleMapper, never()).updateArticleAiResult(anyInt(), anyString(), anyString(), any());
    }
}
