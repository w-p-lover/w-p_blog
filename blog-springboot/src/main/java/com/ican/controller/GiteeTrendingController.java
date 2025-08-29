package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GiteeTrendingVO;
import com.ican.model.vo.PageResult;
import com.ican.model.vo.Result;
import com.ican.service.GiteeTrendingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Gitee 趋势控制器
 */
@Api(tags = "Gitee 趋势模块")
@RestController
@RequestMapping("/gitee/trending")
@RequiredArgsConstructor
public class GiteeTrendingController {

    private final GiteeTrendingService giteeTrendingService;

    /**
     * 查看Gitee趋势列表
     *
     * @param condition 查询条件
     * @return Gitee趋势列表
     */
    @ApiOperation(value = "查看Gitee趋势列表")
    @GetMapping("/list")
    public Result<PageResult<GiteeTrendingVO>> listGiteeTrending(ConditionDTO condition) {
        return Result.success(giteeTrendingService.listGiteeTrending(condition));
    }

    @PostMapping("/run")
    public Result<?> runSpider() {
        giteeTrendingService.runPythonSpider();
        return Result.success("爬虫任务已启动");
    }
}
