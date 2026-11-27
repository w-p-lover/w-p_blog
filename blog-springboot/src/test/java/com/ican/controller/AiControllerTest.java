package com.ican.controller;

import com.ican.config.BlogRuntimeProperties;
import com.ican.service.AiArticleService;
import com.ican.service.AiRagService;
import com.ican.service.AiWriteAssistService;
import com.ican.service.RedisService;
import com.ican.service.UserService;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@EnableConfigurationProperties(BlogRuntimeProperties.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"test", "ai"})
class AiControllerTest {

    private SaTokenContext previousContext;

    @BeforeEach
    void useServletContext() {
        previousContext = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
    }

    @AfterEach
    void restoreContext() {
        SaManager.setSaTokenContext(previousContext);
    }

    @Autowired private MockMvc mockMvc;
    @MockBean private AiRagService aiRagService;
    @MockBean private AiWriteAssistService aiWriteAssistService;
    @MockBean private AiArticleService aiArticleService;
    @MockBean private RedisService redisService;
    @MockBean private UserService userService;

    @Test
    void anonymousReaderCannotInvokeInternalAiEvenWhenProfileIsEnabled() throws Exception {
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"缓存如何失效\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(402));
        mockMvc.perform(get("/api/ai/write-assist").param("action", "expand").param("content", "缓存优化"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(402));
        mockMvc.perform(post("/api/ai/reindex"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(402));
        verifyNoInteractions(aiRagService, aiWriteAssistService, aiArticleService);
    }
}
