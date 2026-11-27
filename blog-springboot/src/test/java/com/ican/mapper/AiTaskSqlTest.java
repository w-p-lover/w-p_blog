package com.ican.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ican.entity.AiTask;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiTaskSqlTest {

    @Test
    void realMybatisBindsRecoveryClaimCompletionAndRetryToThePersistenceProtocol() throws Exception {
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        try (var input = Resources.getResourceAsStream("mapper/AiTaskMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/AiTaskMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        BoundSql due = bound(configuration, "selectDuePendingTasks", Map.of("size", 100));
        assertTrue(due.getSql().contains("ORDER BY next_dispatch_at, id LIMIT ?"));
        BoundSql claim = bound(configuration, "claimPendingTask", Map.of("taskId", 12, "token", "run"));
        assertEquals(java.util.List.of("token", "taskId"), claim.getParameterMappings().stream()
                .map(parameter -> parameter.getProperty()).toList());
        AiTask task = new AiTask();
        task.setId(12);
        task.setStatus("SUCCESS");
        BoundSql complete = bound(configuration, "completeExecution", Map.of("task", task, "token", "run"));
        assertTrue(complete.getSql().contains("status = 'RUNNING' AND execution_token = ?"));
        assertTrue(complete.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("task.id")));
        String insert = bound(configuration, "insert", task).getSql();
        assertNotNull(insert);
        // BaseMapper映射实际包含新持久字段，完整表迁移在运行前是必需条件。
        task.setDispatchAttempts(1);
        task.setExecutionToken("run");
        task.setSourceVersion("source-1");
        task.setGeneratedPayload("checkpoint");
        String update = bound(configuration, "updateById", Map.of("et", task)).getSql();
        assertTrue(update.contains("execution_token"));
        assertTrue(update.contains("dispatch_attempts"));
        assertTrue(update.contains("source_version"));
        assertTrue(update.contains("generated_payload"));
        var checkpoint = bound(configuration, "saveGeneratedPayload", Map.of("taskId", 12, "token", "run", "payload", "json"));
        assertTrue(checkpoint.getSql().contains("status = 'RUNNING' AND execution_token = ?"));
        assertFalse(claim.getSql().contains("generated_payload ="));
        assertFalse(bound(configuration, "resetFailedTask", Map.of("taskId", 12, "retryCount", 1))
                .getSql().contains("generated_payload ="));
        assertNotNull(bound(configuration, "reserveDispatch", Map.of("taskId", 12)));
        assertNotNull(bound(configuration, "resetFailedTask", Map.of("taskId", 12, "retryCount", 1)));
    }

    private BoundSql bound(MybatisConfiguration configuration, String name, Object argument) {
        return configuration.getMappedStatement(AiTaskMapper.class.getName() + "." + name).getBoundSql(argument);
    }
}
