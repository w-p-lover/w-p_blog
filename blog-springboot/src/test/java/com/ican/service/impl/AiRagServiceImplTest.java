package com.ican.service.impl;

import com.ican.controller.AiController;
import com.ican.handler.GlobalExceptionHandler;
import com.ican.service.AiArticleService;
import com.ican.service.AiWriteAssistService;
import com.ican.exception.ServiceException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ican.mapper.ArticleMapper;
import com.ican.entity.Article;
import com.ican.model.vo.AiChatResponseVO;
import com.ican.service.ArticleAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiRagServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleAccessService articleAccessService;

    private ChatClient chatClient;

    private AiRagServiceImpl aiRagService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        aiRagService = new AiRagServiceImpl(chatClient, vectorStore, articleMapper, articleAccessService);
    }

    @Test
    void chat_shouldReturnFallbackWhenNoDocumentsFound() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        assertEmpty(aiRagService.chat("缓存策略是什么"));

        verifyNoInteractions(articleMapper, articleAccessService, chatClient);
    }

    @Test
    void chat_shouldReturnFallbackWhenDocumentsHaveNoUsableText() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(document(1, "   "), document(2, "\n\t")));

        assertEmpty(aiRagService.chat("RAG问题"));

        verifyNoInteractions(articleMapper, articleAccessService, chatClient);
    }

    @Test
    void chat_shouldFallbackWhenModelReturnsBlankAnswer() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document(1, "Spring AI 介绍")));
        when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenReturn(current(1));
        modelAnswer("   ");

        AiChatResponseVO response = aiRagService.chat("如何实现RAG问答");

        assertEquals("博客中暂无相关内容", response.getAnswer());
        assertEquals(1, response.getSources().size());
        assertEquals("文章1", response.getSources().get(0).getTitle());
        assertEquals("/article/1", response.getSources().get(0).getUrl());
        verify(articleMapper, times(2)).selectVisibleAiArticles(List.of(1), false);
    }

    @Test
    void chat_shouldKeepVisibleChunkOrderAndDeduplicateSourcesByVerifiedArticleId() {
        Document first = new Document("第一片段", Map.of("sourceVersion", "source-1", "articleId", 1L, "title", "第一篇", "url", "/article/999"));
        Document second = new Document("第二片段", Map.of("sourceVersion", "source-1", "articleId", "2", "title", "第二篇", "url", "https://invalid.example"));
        Document duplicate = new Document("第一篇后续片段", Map.of("sourceVersion", "source-1", "articleId", 1.0D, "title", "第一篇旧标题"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(first, second, duplicate));
        when(articleMapper.selectVisibleAiArticles(List.of(1, 2), false)).thenReturn(current(2, 1));
        modelAnswer("这是模型回答");

        AiChatResponseVO response = aiRagService.chat("如何实现RAG问答");

        assertEquals("这是模型回答", response.getAnswer());
        assertEquals(List.of("/article/1", "/article/2"), response.getSources().stream().map(source -> source.getUrl()).toList());
        assertEquals("第一篇", response.getSources().get(0).getTitle());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt(prompt.capture());
        assertTrue(prompt.getValue().endsWith("第一片段\n\n第二片段\n\n第一篇后续片段"));
        verify(articleMapper, times(2)).selectVisibleAiArticles(List.of(1, 2), false);
    }

    @Test
    void withdrawnPrivateOrUnknownCandidatesNeverEnterPromptOrSources() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                document(1, "允许内容"), document(2, "已撤回敏感内容"), document(3, "私人内容"),
                new Document("没有归属的内容", Map.of("title", "无主文章", "url", "/article/1"))));
        when(articleMapper.selectVisibleAiArticles(List.of(1, 2, 3), false)).thenReturn(current(1));
        when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenReturn(current(1));
        modelAnswer("仅允许来源的回答");

        AiChatResponseVO response = aiRagService.chat("请解释");

        assertEquals(1, response.getSources().size());
        assertEquals("/article/1", response.getSources().get(0).getUrl());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt(prompt.capture());
        assertTrue(prompt.getValue().endsWith("允许内容"));
        assertFalse(prompt.getValue().contains("敏感内容"));
        assertFalse(prompt.getValue().contains("私人内容"));
        assertFalse(prompt.getValue().contains("没有归属"));
        verify(articleAccessService, times(2)).canReadPrivateArticles();
    }

    @Test
    void malformedOwnershipCannotFallBackToUrlOrTruncateToAnotherArticle() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Arrays.asList(null,
                document(1.5D, "小数归属"), document(4294967297L, "溢出归属"), document(0, "零主键"),
                document(-1, "负主键"), document("not-an-id", "非数字"), document(Double.NaN, "非有限数值"),
                document(true, "布尔值"), new Document("缺少主键", Map.of("url", "/article/1"))));

        assertEmpty(aiRagService.chat("问题"));

        verifyNoInteractions(articleMapper, articleAccessService, chatClient);
    }

    @Test
    void sameOldVectorsAreBlockedAfterWithdrawalAndUsableAgainAfterRepublish() {
        Document old = document(1, "旧向量正文");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(old));
        when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenReturn(current(), current(1), current(1));

        assertEmpty(aiRagService.chat("问题"));
        verifyNoInteractions(chatClient);

        modelAnswer("恢复发布后的回答");
        assertEquals("恢复发布后的回答", aiRagService.chat("问题").getAnswer());
        verify(chatClient).prompt(anyString());
    }

    @Test
    void withdrawalDuringGenerationDiscardsEntireMixedAnswerWithoutAutomaticRegeneration() {
        AtomicBoolean withdrawn = new AtomicBoolean(false);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(document(1, "保持公开"), document(2, "生成期间撤回")));
        when(articleMapper.selectVisibleAiArticles(List.of(1, 2), false)).thenAnswer(invocation ->
                withdrawn.get() ? current(1) : current(1, 2));
        when(chatClient.prompt(anyString()).call().content()).thenAnswer(invocation -> {
            withdrawn.set(true);
            return "已经混入撤回内容的完整回答";
        });
        clearInvocations(chatClient);

        ServiceException failure = assertThrows(ServiceException.class, () -> aiRagService.chat("问题"));

        assertEquals("参考文章的内容或访问范围已变化，请重新提问", failure.getMessage());
        assertFalse(failure.getMessage().contains("混入"));
        verify(chatClient).prompt(anyString());
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(articleMapper, times(2)).selectVisibleAiArticles(List.of(1, 2), false);
    }

    @Test
    void specialAccountLossDuringGenerationIsRecheckedAndRecoveryAllowsAnotherRequest() {
        AtomicBoolean privateAccess = new AtomicBoolean(true);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document(2, "私人正文")));
        when(articleAccessService.canReadPrivateArticles()).thenAnswer(invocation -> privateAccess.get());
        when(articleMapper.selectVisibleAiArticles(List.of(2), true)).thenReturn(current(2));
        when(articleMapper.selectVisibleAiArticles(List.of(2), false)).thenReturn(current());
        when(chatClient.prompt(anyString()).call().content()).thenAnswer(invocation -> {
            privateAccess.set(false);
            return "私人内容生成结果";
        }).thenReturn("资格恢复后的回答");
        clearInvocations(chatClient);

        assertThrows(ServiceException.class, () -> aiRagService.chat("问题"));
        verify(articleMapper).selectVisibleAiArticles(List.of(2), true);
        verify(articleMapper).selectVisibleAiArticles(List.of(2), false);

        privateAccess.set(true);
        AiChatResponseVO response = aiRagService.chat("问题");
        assertEquals("资格恢复后的回答", response.getAnswer());
        assertEquals("/article/2", response.getSources().get(0).getUrl());
        verify(chatClient, times(2)).prompt(anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void permissionLookupFailureAtEitherGateCannotReturnAnAnswer(boolean afterGeneration) {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document(1, "内容")));
        if (afterGeneration) {
            when(articleMapper.selectVisibleAiArticles(List.of(1), false))
                    .thenReturn(current(1)).thenThrow(new IllegalStateException("受控数据库故障"));
            modelAnswer("不得返回的答案");
        } else {
            when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenThrow(new IllegalStateException("受控数据库故障"));
        }

        ServiceException failure = assertThrows(ServiceException.class, () -> aiRagService.chat("问题"));

        assertEquals("暂时无法确认参考文章访问权限，请稍后重试", failure.getMessage());
        verify(chatClient, times(afterGeneration ? 1 : 0)).prompt(anyString());
    }

    @Test
    void vectorFailureIsAnExplicitRetryableFailureRatherThanEmptyKnowledge() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("受控向量故障"));

        ServiceException failure = assertThrows(ServiceException.class, () -> aiRagService.chat("问题"));

        assertEquals("博客问答检索暂时不可用，请稍后重试", failure.getMessage());
        verifyNoInteractions(articleMapper, articleAccessService, chatClient);
    }

    @Test
    void actualControllerAndExceptionHandlerReturnNoAnswerPayloadWhenSourceIsWithdrawn() throws Exception {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document(1, "原始上下文")));
        when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenReturn(current(1), current());
        modelAnswer("不能从HTTP返回的生成答案");
        var mvc = MockMvcBuilders.standaloneSetup(new AiController(aiRagService,
                        mock(AiWriteAssistService.class), mock(AiArticleService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"问题\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.msg").value("参考文章的内容或访问范围已变化，请重新提问"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("不能从HTTP返回"))));
    }

    private List<Article> current(Integer... ids) {
        return Arrays.stream(ids).map(id -> Article.builder().id(id).sourceVersion("source-1")
                .indexedSourceVersion("source-1").build()).toList();
    }

    @Test
    void oldAndUnversionedChunksCannotReachModelEvenWhenArticleIsVisible() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("旧正文", Map.of("articleId", 1, "sourceVersion", "old")),
                new Document("历史无版本", Map.of("articleId", 1))));
        when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenReturn(current(1));
        assertEmpty(aiRagService.chat("问题"));
        verifyNoInteractions(chatClient);
    }

    @Test
    void editDuringGenerationDiscardsAnswerEvenIfArticleRemainsPublic() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document(1, "旧正文")));
        when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenReturn(current(1),
                List.of(Article.builder().id(1).sourceVersion("source-2").indexedSourceVersion("source-2").build()));
        modelAnswer("已经过时的答案");
        assertThrows(ServiceException.class, () -> aiRagService.chat("问题"));
        verify(chatClient).prompt(anyString());
    }

    @Test
    void incompleteCurrentVectorPublicationCannotReachModel() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document(1, "部分正文")));
        when(articleMapper.selectVisibleAiArticles(List.of(1), false)).thenReturn(
                List.of(Article.builder().id(1).sourceVersion("source-1").indexedSourceVersion("old").build()));
        assertEmpty(aiRagService.chat("问题"));
        verifyNoInteractions(chatClient);
    }

    private Document document(Object articleId, String text) {
        return new Document(text, Map.of("sourceVersion", "source-1", "articleId", articleId, "title", "文章" + articleId, "url", "/article/" + articleId));
    }

    private void modelAnswer(String answer) {
        when(chatClient.prompt(anyString()).call().content()).thenReturn(answer);
        clearInvocations(chatClient);
    }

    private void assertEmpty(AiChatResponseVO response) {
        assertNotNull(response);
        assertEquals("博客中暂无相关内容", response.getAnswer());
        assertTrue(response.getSources().isEmpty());
    }
}
