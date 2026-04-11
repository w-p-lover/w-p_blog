package com.ican.service.impl;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRagServiceImpl implements AiRagService {

    private static final String EMPTY_ANSWER = "博客中暂无相关内容";

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    @Override
    public AiChatResponseVO chat(String question) {
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.query(question).withTopK(5));
        if (docs == null || docs.isEmpty()) {
            return new AiChatResponseVO(EMPTY_ANSWER, List.of());
        }

        String context = docs.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        if (!StringUtils.hasText(context)) {
            return new AiChatResponseVO(EMPTY_ANSWER, List.of());
        }

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
        List<AiSourceVO> sources = buildSources(docs);
        return new AiChatResponseVO(answer, sources);
    }

    private List<AiSourceVO> buildSources(List<Document> docs) {
        Map<String, AiSourceVO> uniqueSources = new LinkedHashMap<>();
        for (Document doc : docs) {
            if (doc == null || doc.getMetadata() == null) {
                continue;
            }
            Object titleObj = doc.getMetadata().get("title");
            Object urlObj = doc.getMetadata().get("url");
            if (!(titleObj instanceof String title) || !(urlObj instanceof String url)) {
                continue;
            }
            if (title == null || title.isBlank() || url == null || url.isBlank()) {
                continue;
            }
            uniqueSources.putIfAbsent(url + "|" + title, new AiSourceVO(title, url));
        }
        return new ArrayList<>(uniqueSources.values());
    }
}
