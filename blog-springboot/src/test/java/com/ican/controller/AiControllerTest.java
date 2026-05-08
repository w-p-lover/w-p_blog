package com.ican.controller;

import com.ican.config.BlogRuntimeProperties;
import com.ican.service.AiArticleService;
import com.ican.service.AiRagService;
import com.ican.service.AiWriteAssistService;
import com.ican.service.RedisService;
import com.ican.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@EnableConfigurationProperties(BlogRuntimeProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiRagService aiRagService;

    @MockBean
    private AiWriteAssistService aiWriteAssistService;

    @MockBean
    private AiArticleService aiArticleService;

    @MockBean
    private RedisService redisService;

    @MockBean
    private UserService userService;

    @Test
    void writeAssist_shouldReturnEventStream() throws Exception {
        when(aiWriteAssistService.writeAssist(anyString(), anyString()))
                .thenReturn(Flux.just("chunk-1", "chunk-2"));

        mockMvc.perform(get("/api/ai/write-assist")
                        .param("action", "expand")
                        .param("content", "缓存优化"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
