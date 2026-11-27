package com.ican.service.impl;

import com.ican.exception.ServiceException;
import com.ican.entity.Article;
import com.ican.mapper.ArticleMapper;
import com.ican.service.ArticleAccessService;
import com.ican.model.vo.AiChatResponseVO;
import com.ican.model.vo.AiSourceVO;
import com.ican.service.AiRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@org.springframework.context.annotation.Profile("ai")
@Service
@RequiredArgsConstructor
@Slf4j
public class AiRagServiceImpl implements AiRagService {

    private static final String EMPTY_ANSWER = "博客中暂无相关内容";

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    private final ArticleMapper articleMapper;

    private final ArticleAccessService articleAccessService;

    @Override
    public AiChatResponseVO chat(String question) {
        List<Document> docs;
        try {
            docs = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(5).build());
        } catch (Exception e) {
            log.warn("博客问答向量检索失败", e);
            throw new ServiceException("博客问答检索暂时不可用，请稍后重试");
        }
        if (docs == null || docs.isEmpty()) {
            return new AiChatResponseVO(EMPTY_ANSWER, List.of());
        }

        // 向量只提供候选：无法确认文章归属的片段不进入模型上下文。
        docs = docs.stream().filter(Objects::nonNull)
                .filter(doc -> StringUtils.hasText(doc.getText()) && getArticleId(doc) != null).toList();
        List<Integer> candidateIds = docs.stream().map(this::getArticleId).distinct().toList();
        if (candidateIds.isEmpty()) {
            return new AiChatResponseVO(EMPTY_ANSWER, List.of());
        }
        Map<Integer, String> currentVersions = selectCurrentVersions(candidateIds);
        docs = docs.stream().filter(doc -> isCurrent(doc, currentVersions)).toList();
        if (docs.isEmpty()) {
            return new AiChatResponseVO(EMPTY_ANSWER, List.of());
        }
        List<Integer> usedIds = docs.stream().map(this::getArticleId).distinct().toList();

        String context = docs.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        String prompt = "你是博客问答助手。请严格根据给定上下文回答用户问题。\n"
                + "如果上下文中没有答案，请直接回复：" + EMPTY_ANSWER + "。\n"
                + "禁止编造、猜测或补充上下文之外的信息。\n"
                + "请在回答末尾注明参考的文章标题。\n\n"
                + "问题：" + question + "\n\n"
                + "上下文：\n" + context;

        String answer = chatClient.prompt(prompt).call().content();
        if (!StringUtils.hasText(answer)) {
            answer = EMPTY_ANSWER;
        }
        // 生成可能耗时；来源撤回或账号资格收回后，不能只删引用却返回已混入来源的答案。
        Map<Integer, String> finalVersions = selectCurrentVersions(usedIds);
        if (docs.stream().anyMatch(doc -> !isCurrent(doc, finalVersions))) {
            throw new ServiceException("参考文章的内容或访问范围已变化，请重新提问");
        }
        List<AiSourceVO> sources = buildSources(docs);
        return new AiChatResponseVO(answer, sources);
    }

    private Map<Integer, String> selectCurrentVersions(List<Integer> articleIds) {
        try {
            return articleMapper.selectVisibleAiArticles(articleIds,
                    articleAccessService.canReadPrivateArticles()).stream()
                    .filter(article -> StringUtils.hasText(article.getSourceVersion())
                            && article.getSourceVersion().equals(article.getIndexedSourceVersion()))
                    .collect(Collectors.toMap(Article::getId, Article::getSourceVersion));
        } catch (Exception e) {
            log.warn("博客问答参考文章可见性校验失败", e);
            throw new ServiceException("暂时无法确认参考文章访问权限，请稍后重试");
        }
    }

    private boolean isCurrent(Document doc, Map<Integer, String> currentVersions) {
        Object version = doc.getMetadata().get("sourceVersion");
        return version instanceof String && version.equals(currentVersions.get(getArticleId(doc)));
    }

    private Integer getArticleId(Document doc) {
        Object value = doc.getMetadata().get("articleId");
        if (!(value instanceof Number) && !(value instanceof String)) {
            return null;
        }
        try {
            // 向量metadata的数值可能反序列化成浮点数，禁止截断小数或溢出后误指向其他文章。
            int articleId = new BigDecimal(value.toString()).intValueExact();
            return articleId > 0 ? articleId : null;
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    private List<AiSourceVO> buildSources(List<Document> docs) {
        Map<String, AiSourceVO> uniqueSources = new LinkedHashMap<>();
        for (Document doc : docs) {
            if (doc == null || doc.getMetadata() == null) {
                continue;
            }
            Object titleObj = doc.getMetadata().get("title");
            Integer articleId = getArticleId(doc);
            if (!(titleObj instanceof String title) || title.isBlank() || articleId == null) {
                continue;
            }
            String url = "/article/" + articleId;
            uniqueSources.putIfAbsent(url, new AiSourceVO(title, url));
        }
        return new ArrayList<>(uniqueSources.values());
    }
}
