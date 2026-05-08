package com.ican;

import com.ican.cache.MultiLevelCacheManager;
import com.ican.model.vo.AiChatResponseVO;
import com.ican.model.vo.BlogInfoVO;
import com.ican.service.AiArticleService;
import com.ican.service.AiRagService;
import com.ican.service.AiWriteAssistService;
import com.ican.service.BlogInfoService;
import com.ican.service.RedisService;
import com.ican.service.TaskService;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogInfoService blogInfoService;

    @MockBean
    private AiRagService aiRagService;

    @MockBean
    private AiWriteAssistService aiWriteAssistService;

    @MockBean
    private AiArticleService aiArticleService;

    @MockBean
    private RedisService redisService;

    @MockBean
    private TaskService taskService;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private MultiLevelCacheManager multiLevelCacheManager;

    @MockBean
    private VectorStore vectorStore;

    @Test
    void contextLoads() {
        // Spring 上下文启动成功即通过
    }

    @Test
    void blogInfoEndpointReturns200() throws Exception {
        when(blogInfoService.getBlogInfo()).thenReturn(new BlogInfoVO());

        mockMvc.perform(get("/"))
               .andExpect(status().isOk());
    }

    @Test
    void aiChatEndpoint_shouldReturn200() throws Exception {
        when(aiRagService.chat(anyString())).thenReturn(new AiChatResponseVO("ok", List.of()));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Redis缓存击穿怎么解决\"}"))
                .andExpect(status().isOk());
    }
}
