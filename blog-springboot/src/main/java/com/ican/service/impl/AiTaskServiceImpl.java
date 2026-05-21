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
import com.ican.model.dto.ArticleAiMessage;
import com.ican.model.vo.AiTaskBackVO;
import com.ican.model.vo.PageResult;
import com.ican.service.AiArticleService;
import com.ican.service.AiTaskService;
import com.ican.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

import static com.ican.constant.MqConstant.ARTICLE_AI_EXCHANGE;
import static com.ican.constant.MqConstant.ARTICLE_AI_KEY;

/**
 * AI任务中心服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskServiceImpl extends ServiceImpl<AiTaskMapper, AiTask> implements AiTaskService {

    private static final String ARTICLE_BIZ_TYPE = "ARTICLE";
    private static final String ARTICLE_TASK_TYPE = "ARTICLE_SUMMARY_TAG_VECTOR";
    private static final String DEFAULT_MODEL_NAME = "deepseek-chat";
    private static final String DEFAULT_PROMPT_VERSION = "article-ai-v1";
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final AiTaskMapper aiTaskMapper;

    private final ArticleMapper articleMapper;

    private final AiArticleService aiArticleService;

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public Integer createArticleTask(Integer articleId, String title, String content) {
        if (articleId == null || !StringUtils.hasText(content)) {
            return null;
        }
        AiTask task = new AiTask();
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
        aiTaskMapper.insert(task);

        ArticleAiMessage message = new ArticleAiMessage();
        message.setTaskId(task.getId());
        rabbitTemplate.convertAndSend(ARTICLE_AI_EXCHANGE, ARTICLE_AI_KEY, message);
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
    public void executeTask(Integer taskId) {
        if (taskId == null) {
            return;
        }
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("AI任务不存在");
        }
        if (AiTaskStatusEnum.SUCCESS.getStatus().equals(task.getStatus())) {
            return;
        }
        if (AiTaskStatusEnum.RUNNING.getStatus().equals(task.getStatus())) {
            log.warn("AI任务正在执行，跳过重复消费: taskId={}", taskId);
            return;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        markRunning(taskId, startedAt);
        try {
            Article article = articleMapper.selectById(task.getBizId());
            if (article == null) {
                throw new ServiceException("文章不存在");
            }
            aiArticleService.processArticle(article.getId(), article.getArticleTitle(), article.getArticleContent());
            markSuccess(taskId, startedAt, article.getId());
        } catch (Exception e) {
            markFailure(task, startedAt, e);
            throw new RuntimeException("AI任务执行失败", e);
        }
    }

    @Override
    public void retryTask(Integer taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("AI任务不存在");
        }
        if (AiTaskStatusEnum.SUCCESS.getStatus().equals(task.getStatus())) {
            throw new ServiceException("成功任务无需重试");
        }
        AiTask retryTask = new AiTask();
        retryTask.setId(taskId);
        retryTask.setStatus(AiTaskStatusEnum.PENDING.getStatus());
        retryTask.setErrorMessage(null);
        retryTask.setStartedAt(null);
        retryTask.setFinishedAt(null);
        retryTask.setCostTime(null);
        aiTaskMapper.updateById(retryTask);

        ArticleAiMessage message = new ArticleAiMessage();
        message.setTaskId(taskId);
        rabbitTemplate.convertAndSend(ARTICLE_AI_EXCHANGE, ARTICLE_AI_KEY, message);
    }

    private void markRunning(Integer taskId, LocalDateTime startedAt) {
        AiTask runningTask = new AiTask();
        runningTask.setId(taskId);
        runningTask.setStatus(AiTaskStatusEnum.RUNNING.getStatus());
        runningTask.setStartedAt(startedAt);
        runningTask.setFinishedAt(null);
        runningTask.setErrorMessage(null);
        aiTaskMapper.updateById(runningTask);
    }

    private void markSuccess(Integer taskId, LocalDateTime startedAt, Integer articleId) {
        LocalDateTime finishedAt = LocalDateTime.now();
        AiTask successTask = new AiTask();
        successTask.setId(taskId);
        successTask.setStatus(AiTaskStatusEnum.SUCCESS.getStatus());
        successTask.setResultPayload(toJson(Map.of("articleId", articleId)));
        successTask.setFinishedAt(finishedAt);
        successTask.setCostTime(Duration.between(startedAt, finishedAt).toMillis());
        successTask.setErrorMessage(null);
        aiTaskMapper.updateById(successTask);
    }

    private void markFailure(AiTask task, LocalDateTime startedAt, Exception e) {
        LocalDateTime finishedAt = LocalDateTime.now();
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetryCount = task.getMaxRetryCount() == null ? DEFAULT_MAX_RETRY_COUNT : task.getMaxRetryCount();
        int nextRetryCount = retryCount + 1;

        AiTask failedTask = new AiTask();
        failedTask.setId(task.getId());
        failedTask.setStatus(nextRetryCount >= maxRetryCount
                ? AiTaskStatusEnum.FAILED.getStatus()
                : AiTaskStatusEnum.RETRYING.getStatus());
        failedTask.setRetryCount(nextRetryCount);
        failedTask.setErrorMessage(limitErrorMessage(e));
        failedTask.setFinishedAt(finishedAt);
        failedTask.setCostTime(Duration.between(startedAt, finishedAt).toMillis());
        aiTaskMapper.updateById(failedTask);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ServiceException("AI任务数据序列化失败");
        }
    }

    private String limitErrorMessage(Exception e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
