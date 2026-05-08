package com.ican.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiVectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(AiVectorStoreConfig.class);

    @Bean
    public ChatClient.Builder chatClientBuilder(@Qualifier("openAiChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    @Profile("!test")
    public ApplicationRunner aiVectorStoreHealthRunner(VectorStore vectorStore) {
        return args -> {
            try {
                vectorStore.similaritySearch(SearchRequest.builder().query("health-check").topK(1).build());
            } catch (Exception e) {
                log.warn("Qdrant startup check failed, skip blocking startup", e);
            }
        };
    }
}
