package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.RateLimit;
import com.ican.model.dto.AiChatRequestDTO;
import com.ican.model.vo.AiChatResponseVO;
import com.ican.model.vo.Result;
import com.ican.service.AiArticleService;
import com.ican.service.AiRagService;
import com.ican.service.AiWriteAssistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class AiController {

    private final AiRagService aiRagService;

    private final AiWriteAssistService aiWriteAssistService;

    private final AiArticleService aiArticleService;

    @PostMapping("/api/ai/chat")
    public Result<AiChatResponseVO> chat(@Validated @RequestBody AiChatRequestDTO request) {
        return Result.success(aiRagService.chat(request.getQuestion()));
    }

    @SaCheckPermission("blog:article:update")
    @PostMapping("/api/ai/reindex")
    public Result<Integer> reindex(@RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(aiArticleService.reindexHistory(limit));
    }

    @RateLimit(key = "api:ai:write:#{#action}", limit = 20, period = 3600)
    @GetMapping(value = "/api/ai/write-assist", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> writeAssist(@RequestParam String action, @RequestParam String content) {
        return aiWriteAssistService.writeAssist(action, content);
    }
}
