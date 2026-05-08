package com.ican;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase1ConfigTemplateTest {

    private final Path projectRoot = Path.of("").toAbsolutePath();

    @Test
    void envExampleShouldExistAndContainCoreVariables() throws IOException {
        Path envExample = projectRoot.resolve(".env.example");

        assertTrue(Files.exists(envExample), ".env.example should exist");

        String content = Files.readString(envExample, StandardCharsets.UTF_8);
        assertTrue(content.contains("SPRING_PROFILES_ACTIVE=dev"));
        assertTrue(content.contains("BLOG_DB_URL="));
        assertTrue(content.contains("BLOG_OPENAI_API_KEY="));
        assertTrue(content.contains("BLOG_GITHUB_CLIENT_SECRET="));
    }

    @Test
    void privateExampleShouldExist() {
        Path privateExample = projectRoot.resolve("config").resolve("application-private.example.yml");

        assertTrue(Files.exists(privateExample), "config/application-private.example.yml should exist");
    }

    @Test
    void readmeShouldDocumentBothCopySteps() throws IOException {
        Path readme = projectRoot.resolve("README.md");

        String content = Files.readString(readme, StandardCharsets.UTF_8);
        assertTrue(content.contains("Copy-Item .env.example .env"));
        assertTrue(content.contains("Copy-Item config\\application-private.example.yml config\\application-private.yml"));
    }
}
