package com.ican.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ican.entity.Article;
import com.ican.model.vo.ArticleDetailStateVO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArticleVersionSqlTest {

    private MybatisConfiguration configuration;

    @BeforeEach
    void setUp() throws Exception {
        configuration = new MybatisConfiguration();
        try (var stream = Resources.getResourceAsStream("mapper/ArticleMapper.xml")) {
            new XMLMapperBuilder(stream, configuration, "mapper/ArticleMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    @Test
    void versionSelectionRetainsVisibilityAndDetailLoadPinsTheSelectedVersion() {
        String ordinary = sql("selectVisibleArticleVersion", Map.of("articleId", 1, "privateAccess", false));
        String privileged = sql("selectVisibleArticleVersion", Map.of("articleId", 1, "privateAccess", true));
        assertTrue(ordinary.contains("is_delete = 0 AND `status` = 1"));
        assertTrue(ordinary.contains("NOT EXISTS"));
        assertTrue(ordinary.contains("category_id NOT IN (60, 61)"));
        assertFalse(privileged.contains("NOT EXISTS"));
        String detail = sql("selectArticleHomeById", Map.of("articleId", 1, "contentVersion", "v1"));
        assertTrue(detail.contains("a.content_version = ?"));
        var bound = configuration.getMappedStatement(ArticleMapper.class.getName() + ".selectArticleHomeById")
                .getBoundSql(Map.of("articleId", 1, "contentVersion", "v1"));
        assertEquals("contentVersion", bound.getParameterMappings().get(1).getProperty());
    }

    @Test
    void contentEditsChangeVersionButViewCountersDoNot() {
        Article edited = Article.builder().id(1).articleContent("new body").contentVersion("v2").sourceVersion("source-2").build();
        String edit = sql("updateById", Map.of("et", edited));
        assertTrue(edit.contains("content_version=?"));
        assertTrue(edit.contains("source_version=?"));
        assertTrue(edit.contains("article_content=?"));
        assertTrue(sql("insert", edited).contains("content_version"));
        assertFalse(sql("incrementViews", Map.of("articleId", 1)).contains("content_version"));
        assertFalse(sql("incrementViewsByi", Map.of("articleId", 1, "i", 1)).contains("content_version"));
        String snapshot = sql("persistViewSnapshot", Map.of("articleId", 1, "views", 101));
        assertTrue(snapshot.contains("GREATEST(COALESCE(views, 0), ?)"));
        assertFalse(snapshot.contains("content_version"));
    }

    @Test
    void bodyLoadExcludesRelationsAndFinalSnapshotUsesCurrentVisibilityWithoutLongtext() {
        String body = sql("selectArticleHomeById", Map.of("articleId", 1, "contentVersion", "v1"));
        assertFalse(body.contains("JOIN"));
        assertFalse(body.contains("tag_name"));
        assertFalse(body.contains("category_name"));
        String state = sql("selectVisibleArticleDetailState", Map.of("articleId", 1, "privateAccess", false));
        assertTrue(state.contains("a.content_version"));
        assertTrue(state.contains("INNER JOIN t_category"));
        assertTrue(state.contains("INNER JOIN t_article_tag"));
        assertTrue(state.contains("INNER JOIN t_tag"));
        assertTrue(state.contains("a.is_delete = 0 AND a.`status` = 1"));
        assertTrue(state.contains("private_tat.article_id = a.id"));
        assertTrue(state.contains("a.category_id NOT IN (60, 61)"));
        assertFalse(state.contains("article_content"));
        assertTrue(state.contains("MAX(prev_candidate.id)"));
        assertTrue(state.contains("MIN(next_candidate.id)"));
        assertTrue(state.contains("prev_candidate.is_delete = 0 AND prev_candidate.`status` = 1"));
        assertTrue(state.contains("next_candidate.is_delete = 0 AND next_candidate.`status` = 1"));
        assertTrue(state.contains("private_tat.article_id = prev_candidate.id"));
        assertTrue(state.contains("private_tat.article_id = next_candidate.id"));
        assertFalse(sql("selectVisibleArticleDetailState", Map.of("articleId", 1, "privateAccess", true))
                .contains("NOT EXISTS"));
        var mappings = configuration.getResultMap(ArticleMapper.class.getName() + ".articleDetailState")
                .getResultMappings();
        assertTrue(mappings.stream().anyMatch(mapping -> "tagVOList".equals(mapping.getProperty())
                && mapping.getNestedResultMapId() != null));
    }

    @Test
    void actualResultMappingCombinesMultipleTagRowsIntoOneFreshSnapshot() throws Exception {
        var mapped = configuration.getMappedStatement(ArticleMapper.class.getName() + ".selectVisibleArticleDetailState");
        var bound = mapped.getBoundSql(Map.of("articleId", 1, "privateAccess", false));
        var handler = new DefaultResultSetHandler(mock(Executor.class), mapped, mock(ParameterHandler.class),
                null, bound, RowBounds.DEFAULT);
        String[] columns = {"content_version", "category_id", "category_name", "tag_id", "tag_name",
                "views", "last_id", "last_cover", "last_title", "next_id", "next_cover", "next_title"};
        List<Map<String, Object>> rows = List.of(
                Map.of("content_version", "v1", "category_id", 2, "category_name", "current category", "tag_id", 3, "tag_name", "first"),
                Map.of("content_version", "v1", "category_id", 2, "category_name", "current category", "tag_id", 4, "tag_name", "second"));
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        Statement statement = mock(Statement.class);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(statement.getUpdateCount()).thenReturn(-1);
        when(resultSet.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(columns.length);
        for (int i = 0; i < columns.length; i++) {
            when(metadata.getColumnLabel(i + 1)).thenReturn(columns[i]);
            when(metadata.getColumnType(i + 1)).thenReturn(columns[i].endsWith("_id") ? Types.INTEGER : Types.VARCHAR);
        }
        rows = rows.stream().map(values -> {
            Map<String, Object> expanded = new java.util.HashMap<>(values);
            expanded.put("views", 100);
            expanded.put("last_id", 9);
            expanded.put("last_cover", "previous cover");
            expanded.put("last_title", "previous title");
            return expanded;
        }).toList();
        List<Map<String, Object>> resultRows = rows;
        AtomicInteger row = new AtomicInteger(-1);
        java.util.concurrent.atomic.AtomicBoolean wasNull = new java.util.concurrent.atomic.AtomicBoolean();
        when(resultSet.wasNull()).thenAnswer(call -> wasNull.get());
        when(resultSet.getObject(anyString())).thenAnswer(call -> {
            Object value = resultRows.get(row.get()).get(call.getArgument(0));
            wasNull.set(value == null);
            return value;
        });
        when(resultSet.next()).thenAnswer(call -> row.incrementAndGet() < resultRows.size());
        when(resultSet.getString(anyString())).thenAnswer(call -> {
            Object value = resultRows.get(row.get()).get(call.getArgument(0));
            wasNull.set(value == null);
            return value == null ? null : value.toString();
        });
        when(resultSet.getInt(anyString())).thenAnswer(call -> {
            Object value = resultRows.get(row.get()).get(call.getArgument(0));
            wasNull.set(value == null);
            return value == null ? 0 : value;
        });

        List<Object> snapshots = handler.handleResultSets(statement);

        assertEquals(1, snapshots.size());
        ArticleDetailStateVO snapshot = (ArticleDetailStateVO) snapshots.get(0);
        assertEquals("v1", snapshot.getContentVersion());
        assertEquals("current category", snapshot.getCategory().getCategoryName());
        assertEquals(100, snapshot.getViews());
        assertEquals(9, snapshot.getLastArticle().getId());
        assertEquals("previous title", snapshot.getLastArticle().getArticleTitle());
        assertNull(snapshot.getNextArticle());
        assertEquals(List.of("first", "second"), snapshot.getTagVOList().stream().map(tag -> tag.getTagName()).toList());
    }

    private String sql(String method, Object arguments) {
        return configuration.getMappedStatement(ArticleMapper.class.getName() + "." + method)
                .getBoundSql(arguments).getSql().replaceAll("\\s+", " ").trim();
    }
}
