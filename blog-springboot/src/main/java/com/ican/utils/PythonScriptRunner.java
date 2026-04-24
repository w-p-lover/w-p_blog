package com.ican.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Unified Python script runner for crawler scripts.
 * It handles temp file creation, UTF-8 output, timeout control and total count parsing.
 */
@Component
@Slf4j
public class PythonScriptRunner {

    private static final String TOTAL_COUNT_PREFIX = "TOTAL_COUNT: ";

    @Value("${spider.python.cmd}")
    private String pythonCmd;

    @Value("${spider.python.timeout-seconds:1800}")
    private long timeoutSeconds;

    public PythonExecutionResult run(String scriptResource, List<String> args, Consumer<String> logConsumer) {
        Objects.requireNonNull(scriptResource, "scriptResource must not be null");

        Process process = null;
        File tempFile = null;
        List<String> outputLines = new ArrayList<>();
        Integer totalCount = null;
        int exitCode = -1;
        boolean timedOut = false;

        try {
            ClassPathResource resource = new ClassPathResource(scriptResource);
            String originalName = resource.getFilename();
            String prefix = StringUtils.hasText(originalName)
                    ? originalName.replace('.', '_')
                    : "python_script";
            if (prefix.length() < 3) {
                prefix = prefix + "_py";
            }
            tempFile = File.createTempFile(prefix, ".py");
            Files.copy(resource.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            List<String> command = new ArrayList<>();
            command.add(pythonCmd);
            command.add(tempFile.getAbsolutePath());
            if (args != null && !args.isEmpty()) {
                command.addAll(args);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "UTF-8");
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);
                    if (logConsumer != null) {
                        logConsumer.accept(line);
                    }
                    if (line.startsWith(TOTAL_COUNT_PREFIX)) {
                        totalCount = parseTotalCount(line);
                    }
                }
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                timedOut = true;
                process.destroyForcibly();
                log.warn("Python script timed out after {} seconds: {}", timeoutSeconds, scriptResource);
            } else {
                exitCode = process.exitValue();
            }
        } catch (Exception e) {
            log.error("Failed to execute python script: {}", scriptResource, e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("Failed to delete temp python script: {}", tempFile.getAbsolutePath());
            }
        }

        return new PythonExecutionResult(exitCode, totalCount, timedOut, Collections.unmodifiableList(outputLines));
    }

    private Integer parseTotalCount(String line) {
        try {
            return Integer.parseInt(line.substring(TOTAL_COUNT_PREFIX.length()).trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse total count from python output: {}", line);
            return null;
        }
    }

    public record PythonExecutionResult(int exitCode, Integer totalCount, boolean timedOut, List<String> outputLines) {
        public boolean isSuccess() {
            return !timedOut && exitCode == 0;
        }
    }
}
