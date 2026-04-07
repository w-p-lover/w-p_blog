package com.ican.controller;

import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GiteeTrendingVO;
import com.ican.model.vo.PageResult;
import com.ican.model.vo.Result;
import com.ican.service.GiteeTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gitee 趋势控制器
 */
@Slf4j
@RestController
@RequestMapping("/gitee/trending")
@RequiredArgsConstructor
public class GiteeTrendingController {

    private final GiteeTrendingService giteeTrendingService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final AtomicReference<String> spiderStatus = new AtomicReference<>("IDLE");
    /**
     * 查看Gitee趋势列表
     *
     * @param condition 查询条件
     * @return Gitee趋势列表
     */
    @GetMapping("/list")
    public Result<PageResult<GiteeTrendingVO>> listGiteeTrending(ConditionDTO condition) {
        return Result.success(giteeTrendingService.listGiteeTrending(condition));
    }


    /**
     * 查看Gitee项目种类
     *
     * @return Gitee趋势列表
     */
    @GetMapping("/type")
    public Result<List<String>> listGiteeTrendingType() {
        return Result.success(giteeTrendingService.listGiteeTrendingType());
    }

    /**
     * 查看Gitee项目语言
     *
     * @return Gitee趋势列表
     */
    @GetMapping("/language")
    public Result<List<String>> listGiteeTrendingLang() {
        return Result.success(giteeTrendingService.listGiteeTrendingLang());
    }


    @PostMapping("/run")
    public Result<?> runSpider( @RequestParam String language,
                                @RequestParam String category) {
        if ("RUNNING".equals(spiderStatus.get())) {
            return Result.fail("爬虫正在运行，请稍后重试");
        }
        spiderStatus.set("RUNNING");
        executor.submit(() -> {
            try {
                giteeTrendingService.runPythonSpider(language,category);
                spiderStatus.set("COMPLETED");
            } catch (Exception e) {
                spiderStatus.set("FAILED");
                log.error("爬虫任务发生异常：{}", e.getMessage());
            }
        });
        return Result.success("爬虫任务已启动");
    }

    @GetMapping("/status")
    public Result<String> getStatus() {
        return Result.success(spiderStatus.get());
    }

}
