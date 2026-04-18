package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ican.entity.Article;
import com.ican.enums.ArticleStatusEnum;
import com.ican.mapper.ArticleMapper;
import com.ican.service.AiArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiArticleServiceImpl implements AiArticleService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 150;
    private static final int DEFAULT_REINDEX_ARTICLE_LIMIT = 5;
    private static final int MAX_EMBEDDING_BATCH_SIZE = 10;

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    private final ArticleMapper articleMapper;

    @Override
    public void processArticle(Integer articleId, String title, String content) {
        if (articleId == null || !StringUtils.hasText(content)) {
            return;
        }
        try {
            String summaryPrompt = "请对以下文章生成100-200字中文摘要，突出核心观点和结论：\n标题："
                    + (title == null ? "" : title)
                    + "\n内容：\n"
                    + content;
            String tagsPrompt = "请基于以下文章提取3-5个中文标签，仅返回逗号分隔结果，不要额外说明：\n标题："
                    + (title == null ? "" : title)
                    + "\n内容：\n"
                    + content;

            String summary = chatClient.prompt(summaryPrompt).call().content();
            String tags = chatClient.prompt(tagsPrompt).call().content();

            articleMapper.updateArticleAiResult(articleId, summary, tags, LocalDateTime.now());

            List<Document> documents = buildChunkedDocuments(articleId, title, content);
            addDocumentsInBatches(documents);

            log.info("文章AI处理完成，articleId={}", articleId);
        } catch (Exception e) {
            log.error("文章AI处理失败，articleId={}", articleId, e);
            throw new RuntimeException("文章AI处理失败", e);
        }
    }

    @Override
    public int reindexHistory() {
        return reindexHistory(DEFAULT_REINDEX_ARTICLE_LIMIT);
    }

    @Override
    public int reindexHistory(int articleLimit) {
        int effectiveLimit = articleLimit <= 0 ? DEFAULT_REINDEX_ARTICLE_LIMIT : articleLimit;
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleContent)
                .eq(Article::getIsDelete, 0)
                .eq(Article::getStatus, ArticleStatusEnum.PUBLIC.getStatus())
                .orderByAsc(Article::getId)
                .last("LIMIT " + effectiveLimit));
        if (articles == null || articles.isEmpty()) {
            return 0;
        }

        List<Document> docs = articles.stream()
                .filter(article -> article.getId() != null && StringUtils.hasText(article.getArticleContent()))
                .flatMap(article -> buildChunkedDocuments(article.getId(), article.getArticleTitle(), article.getArticleContent()).stream())
                .toList();

        if (docs.isEmpty()) {
            return 0;
        }

        try {
            addDocumentsInBatches(docs);
            log.info("历史文章向量回填完成，articleLimit={}, chunkCount={}", effectiveLimit, docs.size());
            return docs.size();
        } catch (Exception e) {
            log.error("历史文章向量回填失败", e);
            throw new RuntimeException("历史文章向量回填失败", e);
        }
    }

    private void addDocumentsInBatches(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (int i = 0; i < documents.size(); i += MAX_EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + MAX_EMBEDDING_BATCH_SIZE, documents.size());
            vectorStore.add(documents.subList(i, end));
        }
    }

    private List<Document> buildChunkedDocuments(Integer articleId, String title, String content) {
        if (articleId == null || !StringUtils.hasText(content)) {
            return List.of();
        }

        String normalizedTitle = title == null ? "" : title;
        List<String> chunks = splitContent(content, CHUNK_SIZE, CHUNK_OVERLAP);
        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            documents.add(new Document(chunk, Map.of(
                    "articleId", articleId,
                    "title", normalizedTitle,
                    "url", "/article/" + articleId,
                    "chunkIndex", i,
                    "chunkTotal", chunks.size()
            )));
        }
        return documents;
    }

    private List<String> splitContent(String content, int chunkSize, int overlap) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        if (chunkSize <= 0) {
            return List.of(content);
        }

        int safeOverlap = Math.max(0, Math.min(overlap, chunkSize - 1));
        int step = chunkSize - safeOverlap;
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = content.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= length) {
                break;
            }
            start += step;
        }

        return chunks;
    }
}
