package com.ican.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.AiTask;
import com.ican.exception.ServiceException;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import com.ican.metrics.BlogMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

/** 网络操作在外；发布与终态共用任务行锁和同一短事务。 */
@Service
@RequiredArgsConstructor
public class AiTaskExecutionService {
    private final AiTaskMapper tasks;
    private final ArticleMapper articles;
    private final ObjectMapper json;
    private final BlogMetrics metrics;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class, timeout = 10)
    public void finish(AiTask execution, boolean prepared) throws Exception {
        AiTask current = tasks.lockExecution(execution.getId(), execution.getExecutionToken());
        if (current == null || tasks.renewExecution(current.getId(), execution.getExecutionToken()) != 1) {
            metrics.recordAiTaskEvent("finish", "fenced");
            return;
        }
        boolean published = false;
        if (prepared) {
            var payload = json.readTree(current.getGeneratedPayload());
            String summary = payload.path("summary").asText();
            String tags = payload.path("tags").asText();
            if (!StringUtils.hasText(current.getSourceVersion())
                    || !current.getSourceVersion().equals(payload.path("sourceVersion").asText())
                    || !StringUtils.hasText(summary) || !StringUtils.hasText(tags)) {
                throw new ServiceException("AI检查点不完整或源版本不匹配");
            }
            published = articles.updateArticleAiResult(current.getBizId(), summary, tags, LocalDateTime.now(),
                    current.getSourceVersion(), current.getId(), execution.getExecutionToken()) == 1;
        }
        current.setStatus(published ? "SUCCESS" : "CANCELED");
        current.setResultPayload(json.writeValueAsString(java.util.Map.of("articleId", current.getBizId(),
                "outcome", published ? "PUBLISHED" : "SUPERSEDED_OR_WITHDRAWN")));
        current.setErrorMessage(null);
        complete(current, execution.getExecutionToken());
        recordAfterCommit("finish", published ? "published" : "canceled");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class, timeout = 10)
    public void fail(AiTask execution, Exception error) {
        // 重新读持久检查点：写检查点的响应丢失时，不能依据旧Java对象判定没有检查点。
        AiTask current = tasks.lockExecution(execution.getId(), execution.getExecutionToken());
        if (current == null) {
            metrics.recordAiTaskEvent("failure", "fenced");
            return;
        }
        int failures = (current.getRetryCount() == null ? 0 : current.getRetryCount()) + 1;
        current.setRetryCount(failures);
        boolean checkpoint = StringUtils.hasText(current.getGeneratedPayload());
        current.setStatus(!checkpoint ? "UNKNOWN" : failures >= (current.getMaxRetryCount() == null ? 3 : current.getMaxRetryCount())
                ? "FAILED" : "RETRYING");
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        current.setErrorMessage(message.substring(0, Math.min(1000, message.length())));
        current.setResultPayload(null);
        complete(current, execution.getExecutionToken());
        recordAfterCommit("failure", current.getStatus().toLowerCase(java.util.Locale.ROOT));
    }

    private void recordAfterCommit(String stage, String outcome) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { metrics.recordAiTaskEvent(stage, outcome); }
                });
    }

    private void complete(AiTask task, String token) {
        task.setFinishedAt(LocalDateTime.now());
        task.setCostTime(task.getStartedAt() == null ? null
                : Math.max(0, Duration.between(task.getStartedAt(), task.getFinishedAt()).toMillis()));
        if (tasks.completeExecution(task, token) != 1) {
            throw new ServiceException("AI发布事务终态未写入，回滚本次发布");
        }
    }
}
