package com.ican.controller;

import com.ican.model.vo.PageResult;
import com.ican.model.vo.Result;
import com.ican.service.AiTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskControllerTest {

    private AiTaskService aiTaskService;

    private AiTaskController controller;

    @BeforeEach
    void setUp() {
        aiTaskService = mock(AiTaskService.class);
        controller = new AiTaskController(aiTaskService);
    }

    @Test
    void listAiTask_shouldReturnPageResult() {
        when(aiTaskService.listTaskBackVO(any())).thenReturn(new PageResult<>(List.of(), 0L));

        Result<PageResult<com.ican.model.vo.AiTaskBackVO>> result =
                controller.listAiTask(new com.ican.model.dto.AiTaskQueryDTO());

        assertTrue(result.getFlag());
        assertEquals(0L, result.getData().getCount());
    }

    @Test
    void retryAiTask_shouldDelegateToService() {
        Result<?> result = controller.retryAiTask(9);

        assertTrue(result.getFlag());
        verify(aiTaskService).retryTask(9);
    }
}
