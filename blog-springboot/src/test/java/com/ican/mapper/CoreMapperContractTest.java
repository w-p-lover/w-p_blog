package com.ican.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.model.dto.ConditionDTO;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreMapperContractTest {
    @Test
    void filteredCommentSqlKeepsItsConditionsJoined() throws Exception {
        var configuration = new MybatisConfiguration();
        var resource = Path.of("src/main/resources/mapper/CommentMapper.xml");
        try (var stream = Files.newInputStream(resource)) {
            new XMLMapperBuilder(stream, configuration, resource.toString(), configuration.getSqlFragments()).parse();
        }
        var queries = new ArrayList<Map<String, Object>>();
        for (Integer type : new Integer[]{null, 1, 2, 3, 4}) {
            var condition = new ConditionDTO();
            condition.setCommentType(type);
            var params = Map.of("condition", condition, "limit", 0L, "size", 10L);
            for (String method : new String[]{"countComment", "listCommentBackVO"}) {
                var bound = configuration.getMappedStatement(CommentMapper.class.getName() + "." + method).getBoundSql(params);
                var sql = bound.getSql().replaceAll("\\s+", " ").trim();
                if (type != null) assertTrue(sql.contains("IN (1, 2, 3) AND c.comment_type"), sql);
                var arguments = bound.getParameterMappings().stream()
                        .map(mapping -> SystemMetaObject.forObject(params).getValue(mapping.getProperty())).toList();
                var query = new LinkedHashMap<String, Object>();
                query.put("type", type);
                query.put("method", method);
                query.put("sql", sql);
                query.put("arguments", arguments);
                queries.add(query);
            }
        }
        new ObjectMapper().writeValue(Path.of("target/core-comments-sql.json").toFile(), queries);
    }

    @Test
    void remainingMapperResourcesLoadWithoutRetiredTypes() throws Exception {
        var configuration = new MybatisConfiguration();
        try (var resources = Files.list(Path.of("src/main/resources/mapper"))) {
            for (var resource : resources.filter(path -> path.toString().endsWith(".xml")).toList()) {
                try (var stream = Files.newInputStream(resource)) {
                    new XMLMapperBuilder(stream, configuration, resource.toString(), configuration.getSqlFragments()).parse();
                }
            }
        }
    }
}
