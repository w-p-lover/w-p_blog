package com.ican.quartz.task;

import com.ican.mapper.ChatMapper;
import com.ican.mapper.VisitLogMapper;
import com.ican.service.ArticleHotScoreService;
import com.ican.service.HotArticleWarmupService;
import com.ican.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimedTaskTest {

    @Mock
    private RedisService redisService;
    @Mock
    private VisitLogMapper visitLogMapper;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private ArticleHotScoreService articleHotScoreService;
    @Mock
    private HotArticleWarmupService hotArticleWarmupService;

    private TimedTask timedTask;

    @BeforeEach
    void setUp() {
        timedTask = new TimedTask();
        ReflectionTestUtils.setField(timedTask, "redisService", redisService);
        ReflectionTestUtils.setField(timedTask, "visitLogMapper", visitLogMapper);
        ReflectionTestUtils.setField(timedTask, "chatMapper", chatMapper);
        ReflectionTestUtils.setField(timedTask, "articleHotScoreService", articleHotScoreService);
        ReflectionTestUtils.setField(timedTask, "hotArticleWarmupService", hotArticleWarmupService);
    }

    @Test
    void refreshArticleHotScore_shouldDelegateToService() {
        timedTask.refreshArticleHotScore();

        verify(articleHotScoreService).refreshHotScores();
    }

    @Test
    void warmupHotArticles_shouldDelegateToWarmupService() {
        timedTask.warmupHotArticles();

        verify(hotArticleWarmupService).warmupTopHotArticles(0);
    }
}
