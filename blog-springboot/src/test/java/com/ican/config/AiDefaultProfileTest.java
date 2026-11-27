package com.ican.config;

import com.ican.controller.AiController;
import com.ican.controller.AiTaskController;
import com.ican.consumer.AiArticleConsumer;
import com.ican.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeImageAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioSpeechAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioTranscriptionAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeRerankAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class AiDefaultProfileTest {
    @Test
    void normalBlogStartsWithoutModelKeysVectorConnectionOrAiEndpoints() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=dev")
                .withUserConfiguration(Providers.class, AiController.class, AiTaskController.class,
                        AiVectorStoreConfig.class, ArticleAiQueueConfig.class, AiArticleConsumer.class,
                        AiArticleServiceImpl.class, AiRagServiceImpl.class, AiWriteAssistServiceImpl.class,
                        AiTaskServiceImpl.class, AiTaskDispatchService.class, AiTaskExecutionService.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(ChatModel.class).isEmpty());
                    assertTrue(context.getBeansOfType(VectorStore.class).isEmpty());
                    assertTrue(context.getBeansOfType(AiController.class).isEmpty());
                    assertTrue(context.getBeansOfType(AiTaskController.class).isEmpty());
                    assertTrue(context.getBeansOfType(AiArticleConsumer.class).isEmpty());
                    assertTrue(context.getBeansOfType(AiTaskDispatchService.class).isEmpty());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({OpenAiChatAutoConfiguration.class,
            OpenAiEmbeddingAutoConfiguration.class,
            OpenAiImageAutoConfiguration.class,
            OpenAiAudioSpeechAutoConfiguration.class,
            OpenAiAudioTranscriptionAutoConfiguration.class,
            OpenAiModerationAutoConfiguration.class,
            QdrantVectorStoreAutoConfiguration.class,
            DashScopeChatAutoConfiguration.class,
            DashScopeAgentAutoConfiguration.class,
            DashScopeImageAutoConfiguration.class,
            DashScopeAudioSpeechAutoConfiguration.class,
            DashScopeAudioTranscriptionAutoConfiguration.class,
            DashScopeRerankAutoConfiguration.class,
            DashScopeEmbeddingAutoConfiguration.class})
    static class Providers {}
}
