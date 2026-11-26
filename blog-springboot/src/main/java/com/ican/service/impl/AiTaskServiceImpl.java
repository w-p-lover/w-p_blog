package com.ican.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.entity.AiTask;
import com.ican.entity.Article;
import com.ican.enums.AiTaskStatusEnum;
import com.ican.exception.ServiceException;
import com.ican.mapper.AiTaskMapper;
import com.ican.mapper.ArticleMapper;
import com.ican.model.dto.AiTaskQueryDTO;
import com.ican.model.vo.AiTaskBackVO;
import com.ican.model.vo.PageResult;
import com.ican.service.AiArticleService;
import com.ican.service.AiTaskService;
import com.ican.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * AI任务中心服务实现
 */
@Service
@RequiredArgsConstructor
public class AiTaskServiceImpl extends ServiceImpl<AiTaskMapper, AiTask> implements AiTaskService {

    private static final String ARTICLE_BIZ_TYPE = "ARTICLE";
    private static final String ARTICLE_TASK_TYPE = "ARTICLE_SUMMARY_TAG_VECTOR";
    private static final String DEFAULT_MODEL_NAME = "deepseek-chat";
    private static final String DEFAULT_PROMPT_VERSION = "article-ai-v1";
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final AiTaskMapper aiTaskMapper;

    private final ArticleMapper articleMapper;

    private final AiArticleService aiArticleService;

    private final AiTaskDispatchService dispatcher;

    private final ObjectMapper objectMapper;

    private final AiTaskExecutionService executionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createArticleTask(Integer articleId, String title, String content) {
        if (articleId == null || !StringUtils.hasText(content)) {
            return null;
        }
        Article source = articleMapper.selectById(articleId);
        if (source == null || !StringUtils.hasText(source.getSourceVersion())
                || !java.util.Objects.equals(title, source.getArticleTitle())
                || !java.util.Objects.equals(content, source.getArticleContent())) {
            throw new ServiceException("AI任务源正文身份不明确");
        }
        AiTask task = new AiTask();
        task.setSourceVersion(source.getSourceVersion());
        task.setBizType(ARTICLE_BIZ_TYPE);
        task.setBizId(articleId);
        task.setTaskType(ARTICLE_TASK_TYPE);
        task.setStatus(AiTaskStatusEnum.PENDING.getStatus());
        task.setRetryCount(0);
        task.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);
        task.setRequestPayload(toJson(Map.of(
                "articleId", articleId,
                "title", title == null ? "" : title
        )));
        task.setModelName(DEFAULT_MODEL_NAME);
        task.setPromptVersion(DEFAULT_PROMPT_VERSION);
        task.setDispatchAttempts(0);
        if (aiTaskMapper.insert(task) != 1 || task.getId() == null) {
            throw new ServiceException("AI任务未成功持久化");
        }

        dispatcher.wakeAfterCommit(task.getId());
        return task.getId();
    }

    @Override
    public PageResult<AiTaskBackVO> listTaskBackVO(AiTaskQueryDTO condition) {
        Long count = aiTaskMapper.countTaskBackVO(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        List<AiTaskBackVO> records = aiTaskMapper.selectTaskBackVO(PageUtils.getLimit(), PageUtils.getSize(), condition);
        return new PageResult<>(records, count);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void executeTask(Integer taskId) {
        if (taskId == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        // SQL条件更新是认领点；消息重复或RUNNING/终态均不能再执行。
        if (aiTaskMapper.claimPendingTask(taskId, token) != 1) {
            return;
        }
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null || !token.equals(task.getExecutionToken())) {
            throw new ServiceException("AI任务认领结果未知，需核查持久状态");
        }
        try {
            Article article = articleMapper.selectById(task.getBizId());
            boolean current = article != null && StringUtils.hasText(task.getSourceVersion())
                    && task.getSourceVersion().equals(article.getSourceVersion())
                    && Integer.valueOf(0).equals(article.getIsDelete())
                    && Integer.valueOf(1).equals(article.getStatus());
            boolean prepared = current && aiArticleService.processArticle(task, article);
            executionService.finish(task, prepared);
        } catch (Exception e) {
            executionService.fail(task, e);
            throw new RuntimeException("AI任务执行失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Integer taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("AI任务不存在");
        }
        if (AiTaskStatusEnum.PENDING.getStatus().equals(task.getStatus())) {
            dispatcher.wakeAfterCommit(taskId);
            return;
        }
        int failures = task.getRetryCount() == null ? 0 : task.getRetryCount();
        if (aiTaskMapper.resetFailedTask(taskId, failures) != 1) {
            throw new ServiceException("仅有持久检查点且未达上限的失败任务可重试；UNKNOWN须先核查模型结果");
        }
        dispatcher.wakeAfterCommit(taskId);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ServiceException("AI任务数据序列化失败");
        }
    }
}
