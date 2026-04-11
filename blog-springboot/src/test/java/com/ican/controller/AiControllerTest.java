package com.ican.controller;

import com.ican.service.AiRagService;
import com.ican.service.AiWriteAssistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiRagService aiRagService;

    @MockBean
    private AiWriteAssistService aiWriteAssistService;

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
