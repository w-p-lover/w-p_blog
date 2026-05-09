package com.ican.controller;

import com.ican.cache.MultiLevelCacheManager;
import com.ican.config.BlogRuntimeProperties;
import com.ican.model.vo.ArticleHotScoreRefreshResultVO;
import com.ican.model.vo.ArticleHotScoreVO;
import com.ican.model.vo.HotArticleWarmupResultVO;
import com.ican.service.ArticleHotScoreService;
import com.ican.service.HotArticleWarmupService;
import com.ican.service.RedisService;
import com.ican.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CacheController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(BlogRuntimeProperties.class)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MultiLevelCacheManager cacheManager;
    @MockBean
    private UserService userService;
    @MockBean
    private RedisService redisService;
    @MockBean
    private ArticleHotScoreService hotScoreService;
    @MockBean
    private HotArticleWarmupService warmupService;

    @Test
    void refreshHotScores_shouldReturnResult() throws Exception {
        when(hotScoreService.refreshHotScores()).thenReturn(new ArticleHotScoreRefreshResultVO(3, 25L));

        mockMvc.perform(post("/admin/cache/hot-scores/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshedCount").value(3));
    }

    @Test
    void listHotScores_shouldReturnTopScores() throws Exception {
        when(hotScoreService.listTopHotScores(2))
                .thenReturn(List.of(new ArticleHotScoreVO(1, "Redis 缓存", 99.0)));

        mockMvc.perform(get("/admin/cache/hot-scores").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value(1));
    }

    @Test
    void warmupHotArticles_shouldReturnResult() throws Exception {
        when(warmupService.warmupTopHotArticles(2))
                .thenReturn(new HotArticleWarmupResultVO(2, 2, 0, 0, 30L));

        mockMvc.perform(post("/admin/cache/hot-articles/warmup").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warmedCount").value(2));
    }
}
