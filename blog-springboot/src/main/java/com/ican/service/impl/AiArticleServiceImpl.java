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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiArticleServiceImpl implements AiArticleService {

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

            Document document = new Document(content, Map.of(
                    "articleId", articleId,
                    "title", title == null ? "" : title,
                    "url", "/article/" + articleId
            ));
            vectorStore.add(List.of(document));

            log.info("文章AI处理完成，articleId={}", articleId);
        } catch (Exception e) {
            log.error("文章AI处理失败，articleId={}", articleId, e);
            throw new RuntimeException("文章AI处理失败", e);
        }
    }

    @Override
    public int reindexHistory() {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleContent)
                .eq(Article::getIsDelete, 0)
                .eq(Article::getStatus, ArticleStatusEnum.PUBLIC.getStatus())
                .orderByAsc(Article::getId));
        if (articles == null || articles.isEmpty()) {
            return 0;
        }

        List<Document> docs = articles.stream()
                .filter(article -> article.getId() != null && StringUtils.hasText(article.getArticleContent()))
                .map(article -> new Document(article.getArticleContent(), Map.of(
                        "articleId", article.getId(),
                        "title", article.getArticleTitle() == null ? "" : article.getArticleTitle(),
                        "url", "/article/" + article.getId()
                )))
                .toList();

        if (docs.isEmpty()) {
            return 0;
        }

        try {
            vectorStore.add(docs);
            log.info("历史文章向量回填完成，count={}", docs.size());
            return docs.size();
        } catch (Exception e) {
            log.error("历史文章向量回填失败", e);
            throw new RuntimeException("历史文章向量回填失败", e);
        }
    }
}
