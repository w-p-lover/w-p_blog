package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.model.dto.AiTaskQueryDTO;
import com.ican.model.vo.AiTaskBackVO;
import com.ican.model.vo.PageResult;
import com.ican.model.vo.Result;
import com.ican.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ican.constant.OptTypeConstant.UPDATE;

/**
 * AI任务中心控制器
 */
@RestController
@RequiredArgsConstructor
public class AiTaskController {

    private final AiTaskService aiTaskService;

    @SaCheckPermission("ai:task:list")
    @GetMapping("/admin/ai/task/list")
    public Result<PageResult<AiTaskBackVO>> listAiTask(AiTaskQueryDTO condition) {
        return Result.success(aiTaskService.listTaskBackVO(condition));
    }

    @OptLogger(value = UPDATE)
    @SaCheckPermission("ai:task:retry")
    @PostMapping("/admin/ai/task/{taskId}/retry")
    public Result<?> retryAiTask(@PathVariable Integer taskId) {
        aiTaskService.retryTask(taskId);
        return Result.success();
    }
}
