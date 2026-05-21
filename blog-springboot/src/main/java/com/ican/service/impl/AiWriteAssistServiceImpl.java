package com.ican.service.impl;

import com.ican.exception.ServiceException;
import com.ican.service.AiWriteAssistService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AiWriteAssistServiceImpl implements AiWriteAssistService {

    private final ChatClient chatClient;

    @Override
    public Flux<String> writeAssist(String action, String content) {
        String prompt = buildPrompt(action, content);
        return chatClient.prompt().user(prompt).stream().content();
    }

    private String buildPrompt(String action, String content) {
        return switch (action) {
            case "chat" -> content;
            case "continue" -> "请基于以下内容继续写作，保持技术风格：\n\n" + content;
            case "summary" -> "请提炼以下内容为简洁摘要：\n\n" + content;
            case "expand" -> "请对以下内容扩写，补充细节和案例：\n\n" + content;
            case "polish" -> "请润色以下内容，保留原意，让表达更自然、清晰、有节奏：\n\n" + content;
            case "rewrite" -> "请将以下内容改写为更通俗易懂的技术表达：\n\n" + content;
            default -> throw new ServiceException("不支持的写作动作");
        };
    }
}
