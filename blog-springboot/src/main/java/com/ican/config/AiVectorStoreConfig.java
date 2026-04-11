package com.ican.config;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiVectorStoreConfig {

    @Bean
    public ApplicationRunner aiVectorStoreHealthRunner(VectorStore vectorStore) {
        return args -> vectorStore.similaritySearch(SearchRequest.query("health-check").withTopK(1));
    }
}
