package com.ican;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Spring 上下文启动成功即通过
    }

    @Test
    void blogInfoEndpointReturns200() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk());
    }

    @Test
    void articleListEndpointReturns200() throws Exception {
        mockMvc.perform(get("/article/list"))
               .andExpect(status().isOk());
    }
}
