package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ican.entity.Article;
import com.ican.entity.AiTask;
import com.ican.mapper.AiTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.context.annotation.Profile("ai")
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

    private final AiTaskMapper taskMapper;

    private final ObjectMapper objectMapper;

    @Override
    public boolean processArticle(AiTask task, Article article) {
        Integer articleId = article.getId();
        String sourceVersion = task.getSourceVersion();
        if (!StringUtils.hasText(sourceVersion) || !sourceVersion.equals(article.getSourceVersion())
                || !StringUtils.hasText(article.getArticleContent())) {
            return false;
        }
        try {
            GeneratedResult result;
            if (task.getGeneratedPayload() != null) {
                if (!StringUtils.hasText(task.getGeneratedPayload())) {
                    throw new IllegalStateException("AI检查点为空，禁止重新生成");
                }
                result = objectMapper.readValue(task.getGeneratedPayload(), GeneratedResult.class);
                if (!sourceVersion.equals(result.sourceVersion())) {
                    throw new IllegalStateException("AI检查点源版本不匹配");
                }
            } else {
                String context = "\n标题：" + (article.getArticleTitle() == null ? "" : article.getArticleTitle())
                        + "\n内容：\n" + article.getArticleContent();
                String summary = chatClient.prompt("请对以下文章生成100-200字中文摘要，突出核心观点和结论：" + context).call().content();
                String tags = chatClient.prompt("请基于以下文章提取3-5个中文标签，仅返回逗号分隔结果，不要额外说明：" + context).call().content();
                if (!StringUtils.hasText(summary) || !StringUtils.hasText(tags)) {
                    throw new IllegalStateException("AI生成结果为空，未发布");
                }
                result = new GeneratedResult(sourceVersion, summary, tags);
                String payload = objectMapper.writeValueAsString(result);
                if (taskMapper.saveGeneratedPayload(task.getId(), task.getExecutionToken(), payload) != 1) {
                    throw new IllegalStateException("AI生成检查点未持久化，未发布");
                }
                task.setGeneratedPayload(payload);
            }
            if (!StringUtils.hasText(result.summary()) || !StringUtils.hasText(result.tags())) {
                throw new IllegalStateException("AI检查点不完整，未发布");
            }
            // 向量可部分写入，但发布标记在所有批次完成后才写；失败重试使用相同点ID。
            List<Document> documents = buildChunkedDocuments(articleId, article.getArticleTitle(),
                    article.getArticleContent(), sourceVersion);
            for (int i = 0; i < documents.size(); i += MAX_EMBEDDING_BATCH_SIZE) {
                if (taskMapper.renewExecution(task.getId(), task.getExecutionToken()) != 1) {
                    throw new IllegalStateException("AI执行租约已到期或执行代已变化，停止后续向量批次");
                }
                vectorStore.add(documents.subList(i, Math.min(i + MAX_EMBEDDING_BATCH_SIZE, documents.size())));
            }
            // true只代表向量批次已返回；最终发布和任务终态由调用方在同一短事务处理。
            return true;
        } catch (Exception e) {
            log.error("文章AI处理失败，articleId={}", articleId, e);
            throw new RuntimeException("文章AI处理失败", e);
        }
    }

    private record GeneratedResult(String sourceVersion, String summary, String tags) { }

    @Override
    public int reindexHistory() {
        return reindexHistory(DEFAULT_REINDEX_ARTICLE_LIMIT);
    }

    @Override
    public int reindexHistory(int articleLimit) {
        int effectiveLimit = articleLimit <= 0 ? DEFAULT_REINDEX_ARTICLE_LIMIT : articleLimit;
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleContent, Article::getSourceVersion)
                .eq(Article::getIsDelete, 0)
                .eq(Article::getStatus, ArticleStatusEnum.PUBLIC.getStatus())
                .orderByAsc(Article::getId)
                .last("LIMIT " + effectiveLimit));
        if (articles == null || articles.isEmpty()) {
            return 0;
        }

        int chunks = 0;
        try {
            for (Article article : articles) {
                if (!StringUtils.hasText(article.getSourceVersion())) {
                    continue; // 未迁移身份的旧文章不制造无版本向量。
                }
                List<Document> docs = buildChunkedDocuments(article.getId(), article.getArticleTitle(),
                        article.getArticleContent(), article.getSourceVersion());
                if (docs.isEmpty()) {
                    continue;
                }
                addDocumentsInBatches(docs);
                if (articleMapper.publishArticleVector(article.getId(), article.getSourceVersion()) == 1) {
                    chunks += docs.size();
                }
            }
            return chunks;
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

    private List<Document> buildChunkedDocuments(Integer articleId, String title, String content, String sourceVersion) {
        if (articleId == null || !StringUtils.hasText(content)) {
            return List.of();
        }

        String normalizedTitle = title == null ? "" : title;
        List<String> chunks = splitContent(content, CHUNK_SIZE, CHUNK_OVERLAP);
        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String identity = "article-chunks-v1:" + articleId + ":" + sourceVersion + ":" + i;
            String documentId = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
            documents.add(new Document(documentId, chunk, Map.of(
                    "sourceVersion", sourceVersion,
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
