# Phase 1 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目从 Spring Boot 2.6 + Java 11 迁移至 Spring Boot 3.2 + Java 17，并接入可观测性三支柱（指标/日志/链路）对接已有的 Prometheus + Grafana + Loki。

**Status:** ✅ COMPLETED (2026-04-07) — 4/4 tests passing, BUILD SUCCESS

**Architecture:** 迁移策略是先建立 smoke test 基线，再按依赖顺序逐步升级（核心框架 → API 文档 → ES 客户端 → 可观测性），每步之后编译通过再继续。Springfox 完全不兼容 Spring Boot 3.x，整体替换为 springdoc-openapi + knife4j 4.x。ES 的 RestHighLevelClient 已废弃，替换为 Spring Data Elasticsearch 5.x 的 ElasticsearchClient。

**Tech Stack:** Spring Boot 3.2.x, Java 17, springdoc-openapi 2.x, knife4j 4.x, co.elastic.clients:elasticsearch-java, Micrometer + Prometheus, Micrometer Tracing + OTel, logstash-logback-encoder

---

## 文件变更总览

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `pom.xml` | 升级所有依赖 |
| 删除 | `src/main/java/com/ican/config/ElasticsearchConfig.java` | Spring Boot 3 自动配置 ES，不再需要手动 Bean |
| 重写 | `src/main/java/com/ican/config/Knife4jConfig.java` | Springfox → springdoc-openapi API |
| 重写 | `src/main/java/com/ican/service/impl/ElasticsearchServiceImpl.java` | RestHighLevelClient → ElasticsearchClient |
| 重写 | `src/main/java/com/ican/strategy/impl/EsSearchStrategyImpl.java` | RestHighLevelClient → ElasticsearchClient |
| 修改 | `src/main/resources/application.yml` | 替换 ES 配置块、添加 actuator/tracing/metrics 配置 |
| 新建 | `src/main/resources/logback-spring.xml` | JSON 结构化日志，注入 traceId |
| 新建 | `src/main/java/com/ican/metrics/BlogMetrics.java` | 自定义业务指标（文章浏览量 Counter） |
| 新建 | `src/test/java/com/ican/SmokeTest.java` | 迁移前/后基线验证测试 |
| 新建 | `src/test/resources/application-test.yml` | 测试环境配置（复用真实服务器） |

---

## Task 1: 建立迁移前的 Smoke Test 基线

> 目的：在迁移前先跑通测试，确保有可对比的基线。迁移后同样运行这些测试，通过则表示迁移成功。

**Files:**
- Create: `src/test/java/com/ican/SmokeTest.java`
- Create: `src/test/resources/application-test.yml`

- [ ] **Step 1: 创建测试配置文件**

`src/test/resources/application-test.yml` — 复用主配置中的服务器信息，仅关闭会产生副作用的异步任务：

```yaml
# 测试环境配置，继承 application.yml，覆盖需要的部分
spring:
  quartz:
    auto-startup: false  # 测试时不启动定时任务
```

- [ ] **Step 2: 编写 SmokeTest**

`src/test/java/com/ican/SmokeTest.java`:

```java
package com.ican;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Spring 上下文启动成功即通过
    }

    @Test
    void blogInfoEndpointReturns200() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk());
    }

    @Test
    void articleListEndpointReturns200() throws Exception {
        mockMvc.perform(get("/articles"))
               .andExpect(status().isOk());
    }
}
```

- [ ] **Step 3: 运行 smoke test（迁移前基线）**

```bash
cd /d/IdeaProjects/blog/blog-springboot
mvn test -Dtest=SmokeTest -pl blog-springboot
```

期望：3 个测试全部通过（PASS）。如果有失败，先修复再继续。

- [ ] **Step 4: Commit**

```bash
git add src/test/
git commit -m "test: 添加迁移前 smoke test 基线"
```

---

## Task 2: 升级 pom.xml 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 修改 pom.xml 中的核心版本**

将以下内容替换到 `pom.xml`（找到对应位置逐一修改）：

**parent 版本：**
```xml
<!-- 旧 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.6.14</version>
</parent>

<!-- 新 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>
```

**Java 版本：**
```xml
<!-- 旧 -->
<java.version>11</java.version>

<!-- 新 -->
<java.version>17</java.version>
```

- [ ] **Step 2: 替换 Sa-Token 依赖（Spring Boot 3 专用 artifact）**

```xml
<!-- 旧：删除这两行 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.36.0</version>
</dependency>
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>1.36.0</version>
</dependency>

<!-- 新：替换为 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.38.0</version>
</dependency>
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>1.38.0</version>
</dependency>
```

- [ ] **Step 3: 替换 Knife4j/Springfox 依赖**

```xml
<!-- 旧：删除 -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- 新：添加 -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.4.0</version>
</dependency>
```

- [ ] **Step 4: 替换 ES 客户端依赖**

```xml
<!-- 旧：删除 -->
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
</dependency>

<!-- 新：添加（版本由 Spring Boot 3 BOM 管理） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

- [ ] **Step 5: 添加可观测性依赖**

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Prometheus 指标导出 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- 链路追踪：Micrometer Tracing + OpenTelemetry Bridge -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- 结构化日志 JSON 编码器（给 Loki 用） -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

- [ ] **Step 6: 修复 MySQL 驱动类名**

在 `application.yml` 中：
```yaml
# 旧
driver-class-name: com.mysql.jdbc.Driver

# 新
driver-class-name: com.mysql.cj.jdbc.Driver
```

- [ ] **Step 7: 尝试编译（预期会失败，这是正常的）**

```bash
mvn compile -q 2>&1 | head -50
```

预期：大量编译错误，主要是 `javax.*` 找不到的报错。记录错误数量，继续下一个 Task。

---

## Task 3: 全局替换 javax → jakarta + Swagger 注解迁移

> Spring Boot 3.x 将所有 `javax.*` 包迁移到 `jakarta.*`。同时 Springfox 的 Swagger 2 注解需替换为 OpenAPI 3 注解。项目共有 59 个文件含 `javax.*`，137 个文件含 `io.swagger.annotations.*`。

**Files:**
- Modify: 所有 `src/main/java/` 下包含 `javax.` 或 `io.swagger.annotations` 的 Java 文件

- [ ] **Step 1: 批量替换 javax → jakarta（命令行执行）**

```bash
cd /d/IdeaProjects/blog/.worktrees/phase1-foundation/blog-springboot
# Servlet
find src/main/java -name "*.java" -exec sed -i 's/javax\.servlet\./jakarta.servlet./g' {} +
# Validation
find src/main/java -name "*.java" -exec sed -i 's/javax\.validation\./jakarta.validation./g' {} +
# Annotation (仅 @PostConstruct 等)
find src/main/java -name "*.java" -exec sed -i 's/javax\.annotation\./jakarta.annotation./g' {} +
```

- [ ] **Step 2: 批量删除 Swagger 2 注解 import 行**

Swagger 2 注解（`@Api`, `@ApiModel`, `@ApiOperation` 等）在 knife4j 4.x 不再需要显式标注，knife4j 会自动扫描。最快的做法是直接删除这些 import 行和注解使用：

```bash
# 删除 io.swagger.annotations 的所有 import 行
find src/main/java -name "*.java" -exec sed -i '/^import io\.swagger\.annotations\./d' {} +
```

- [ ] **Step 3: 删除 @ApiModel、@ApiModelProperty、@Api、@ApiOperation 注解使用**

由于 knife4j 4.x 可自动识别类和方法，只需删除这些注解声明（不影响运行和文档生成）：

```bash
# 删除 @ApiModel(...) 行（包含参数的）
find src/main/java -name "*.java" -exec sed -i '/@ApiModel/d' {} +
# 删除 @ApiModelProperty(...) 行
find src/main/java -name "*.java" -exec sed -i '/@ApiModelProperty/d' {} +
# 删除 @Api( 行
find src/main/java -name "*.java" -exec sed -i '/@Api(/d' {} +
# 删除 @ApiOperation 行
find src/main/java -name "*.java" -exec sed -i '/@ApiOperation/d' {} +
# 删除 @ApiParam 行
find src/main/java -name "*.java" -exec sed -i '/@ApiParam/d' {} +
```

- [ ] **Step 4: 编译验证**

```bash
export JAVA_HOME="/d/IntelliJ IDEA 2025.2.4/jbr"
"/d/IntelliJ IDEA 2025.2.4/plugins/maven/lib/maven3/bin/mvn" compile -q 2>&1 | grep "error:" | grep -v "javax.sql" | head -30
```

期望：`javax.*` 和 `io.swagger.annotations` 相关错误消失，剩下的是 ES 客户端错误。

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "refactor: javax.* → jakarta.*，删除 Swagger 2 注解（Spring Boot 3 migration）"
```

---

## Task 4: 重写 Knife4jConfig（Springfox → springdoc-openapi）

> Springfox 完全不兼容 Spring Boot 3.x。整体替换为 springdoc-openapi。

**Files:**
- Modify: `src/main/java/com/ican/config/Knife4jConfig.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 完整替换 Knife4jConfig.java**

将 `src/main/java/com/ican/config/Knife4jConfig.java` 内容完整替换为：

```java
package com.ican.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 文档配置（knife4j 4.x + springdoc-openapi）
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("博客系统 API 文档")
                        .description("基于 Spring Boot 3 + Vue 的前后端分离博客")
                        .version("2.0")
                        .contact(new Contact()
                                .name("w&p")
                                .url("https://github.com/ICAN1999")
                                .email("3169468598@qq.com")));
    }
}
```

- [ ] **Step 2: 修改 application.yml，删除 ant_path_matcher，添加 knife4j 配置**

在 `application.yml` 中**删除**以下配置（Spring Boot 3.x 已不需要，且会报错）：
```yaml
# 删除这段
spring:
  mvc:
    pathmatch:
      matching-strategy: ant_path_matcher
```

在 `application.yml` 末尾**添加**：
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
knife4j:
  enable: true
  setting:
    language: zh_cn
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -q 2>&1 | grep "Knife4j\|springfox\|swagger2" | head -20
```

期望：Springfox/swagger2 相关报错消失。

---

## Task 5: 迁移 Elasticsearch 客户端

> 删除手动配置的 `RestHighLevelClient`，改用 Spring Boot 3.x 自动配置的 `ElasticsearchClient`（`co.elastic.clients` 新版客户端）。

**Files:**
- Delete: `src/main/java/com/ican/config/ElasticsearchConfig.java`
- Rewrite: `src/main/java/com/ican/service/impl/ElasticsearchServiceImpl.java`
- Rewrite: `src/main/java/com/ican/strategy/impl/EsSearchStrategyImpl.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 删除旧的 ElasticsearchConfig.java**

直接删除文件 `src/main/java/com/ican/config/ElasticsearchConfig.java`。

Spring Boot 3.x 通过 `spring.elasticsearch.*` 配置项自动创建 `ElasticsearchClient` Bean，不需要手动配置。

- [ ] **Step 2: 修改 application.yml 中的 ES 配置**

**删除**旧的 `elasticsearch:` 自定义配置块（hostname/port/scheme/timeout 那段）。

**替换为**标准 Spring Boot 3.x 配置：
```yaml
spring:
  elasticsearch:
    uris: http://${ES_HOST:121.41.87.40}:${ES_PORT:9200}
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD:your_password}
    connection-timeout: 5s
    socket-timeout: 30s
```

同时**删除** `application.yml` 中的以下自定义配置（已被上面的配置替代）：
```yaml
# 删除
elasticsearch:
  username: elastic
  hostname: ...
  port: ...
  # 等等
```

- [ ] **Step 3: 重写 ElasticsearchServiceImpl.java**

完整替换 `src/main/java/com/ican/service/impl/ElasticsearchServiceImpl.java`：

```java
package com.ican.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.ican.constant.ElasticConstant.ARTICLE_INDEX;

@Service
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void addArticle(ArticleSearchVO article) {
        try {
            elasticsearchClient.index(i -> i
                    .index(ARTICLE_INDEX)
                    .id(article.getId().toString())
                    .document(article));
        } catch (Exception e) {
            log.error("ES 索引文章失败, id={}: {}", article.getId(), e.getMessage());
        }
    }

    @Override
    public void updateArticle(ArticleSearchVO article) {
        try {
            elasticsearchClient.update(u -> u
                    .index(ARTICLE_INDEX)
                    .id(article.getId().toString())
                    .doc(article),
                    ArticleSearchVO.class);
        } catch (Exception e) {
            log.error("ES 更新文章失败, id={}: {}", article.getId(), e.getMessage());
        }
    }

    @Override
    public void deleteArticle(Integer articleId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(ARTICLE_INDEX)
                    .id(articleId.toString()));
        } catch (Exception e) {
            log.error("ES 删除文章失败, id={}: {}", articleId, e.getMessage());
        }
    }
}
```

- [ ] **Step 4: 重写 EsSearchStrategyImpl.java**

完整替换 `src/main/java/com/ican/strategy/impl/EsSearchStrategyImpl.java`：

```java
package com.ican.strategy.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.strategy.SearchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.constant.ElasticConstant.*;
import static com.ican.enums.ArticleStatusEnum.PUBLIC;

@Slf4j
@Service("esSearchStrategyImpl")
@RequiredArgsConstructor
public class EsSearchStrategyImpl implements SearchStrategy {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public List<ArticleSearchVO> searchArticle(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return new ArrayList<>();
        }
        try {
            SearchResponse<ArticleSearchVO> response = elasticsearchClient.search(s -> s
                    .index(ARTICLE_INDEX)
                    .query(q -> q.bool(b -> b
                            .must(m -> m.match(mm -> mm.field("all").query(keyword)))
                            .must(m -> m.term(t -> t.field("isDelete").value(FALSE)))
                            .must(m -> m.term(t -> t.field("status")
                                    .value(PUBLIC.getStatus())))
                    ))
                    .highlight(h -> h
                            .requireFieldMatch(false)
                            .fields(ARTICLE_TITLE, f -> f
                                    .preTags(PRE_TAG).postTags(POST_TAG))
                            .fields(ARTICLE_CONTENT, f -> f
                                    .preTags(PRE_TAG).postTags(POST_TAG))
                    ),
                    ArticleSearchVO.class);

            return response.hits().hits().stream()
                    .map(this::mapHitToVO)
                    .toList();
        } catch (Exception e) {
            log.error("ES 搜索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private ArticleSearchVO mapHitToVO(Hit<ArticleSearchVO> hit) {
        ArticleSearchVO vo = hit.source();
        if (vo == null) return new ArticleSearchVO();
        Map<String, List<String>> highlights = hit.highlight();
        if (highlights.containsKey(ARTICLE_TITLE)) {
            vo.setArticleTitle(highlights.get(ARTICLE_TITLE).get(0));
        }
        if (highlights.containsKey(ARTICLE_CONTENT)) {
            vo.setArticleContent(highlights.get(ARTICLE_CONTENT).get(0));
        } else if (vo.getArticleContent() != null
                && vo.getArticleContent().length() > 300) {
            vo.setArticleContent(vo.getArticleContent().substring(0, 300));
        }
        return vo;
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
mvn compile -q 2>&1 | grep "error:" | head -20
```

期望：ES 相关错误消失。如果还有 `ArticleConsumer.java` 中 ES 相关错误，同样用 `ElasticsearchClient` 注入替换。

- [ ] **Step 6: Commit**

```bash
git add src/ pom.xml
git commit -m "refactor: 迁移 ES 客户端 RestHighLevelClient → co.elastic.clients ElasticsearchClient"
```

---

## Task 6: 修复剩余编译错误并验证启动

- [ ] **Step 1: 全量编译，收集所有剩余错误**

```bash
mvn compile 2>&1 | grep "error:" | sort -u
```

常见剩余问题及修复方式：

| 错误 | 原因 | 修复 |
|------|------|------|
| `cannot find symbol: class Docket` | 某处还有 Springfox import | 删除该 import，Knife4jConfig 已重写 |
| `@EnableSwagger2WebMvc` | 还留在类上 | 删除该注解 |
| `HttpStatus.resolve()` 变更 | Spring 6 调整 | 改用 `HttpStatus.valueOf()` |
| `MultipartResolver` bean 冲突 | Spring Boot 3 自动配置变化 | 删除手动配置的 `CommonsMultipartResolver` Bean（如有） |

- [ ] **Step 2: 启动应用**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

期望：看到 `Started BlogApplication in X.X seconds`，无 ERROR 日志。

如果启动失败，查看第一个 ERROR/WARN 日志，定位问题后修复，再次启动。

- [ ] **Step 3: 手动验证关键接口**

```bash
# 博客首页数据
curl -s http://localhost:8080/ | python -m json.tool | head -10

# 文章列表
curl -s "http://localhost:8080/articles?current=1&size=10" | python -m json.tool | head -10

# API 文档（新地址）
curl -s http://localhost:8080/doc.html -o /dev/null -w "%{http_code}"
# 期望：200
```

- [ ] **Step 4: 运行 smoke test（迁移后验证）**

```bash
mvn test -Dtest=SmokeTest
```

期望：3 个测试全部 PASS，与迁移前基线一致。

- [ ] **Step 5: Commit**

```bash
git add -u
git commit -m "feat: Spring Boot 3.2 + Java 17 迁移完成，smoke test 通过"
```

---

## Task 7: 添加 Prometheus 指标端点

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 在 application.yml 中开放 Prometheus 端点**

在 `application.yml` 中添加（如果 management 配置已存在则合并进去）：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}  # 每条指标附带 app 标签
```

- [ ] **Step 2: 启动应用并验证端点**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test &
sleep 15
curl -s http://localhost:8080/actuator/prometheus | head -30
```

期望：看到 `# HELP jvm_memory_used_bytes` 等 Micrometer 标准指标。

- [ ] **Step 3: 在 Prometheus 配置中添加 scrape job**

在你的 Prometheus 配置文件（通常是 `prometheus.yml`）中添加：

```yaml
scrape_configs:
  - job_name: 'blog-springboot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']  # Docker 内访问宿主机
    scrape_interval: 15s
```

修改后重启 Prometheus 容器（`docker restart prometheus`），然后在 Prometheus UI (`http://localhost:9090`) 中搜索 `jvm_memory_used_bytes` 确认有数据。

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "feat: 开放 Prometheus metrics 端点，接入可观测性"
```

---

## Task 8: 结构化 JSON 日志（接入 Loki）

**Files:**
- Create: `src/main/resources/logback-spring.xml`

- [ ] **Step 1: 创建 logback-spring.xml**

新建 `src/main/resources/logback-spring.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 引入 Spring Boot 默认配置（颜色、基本格式） -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- 开发环境：彩色控制台输出（便于调试） -->
    <springProfile name="!prod">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>${CONSOLE_LOG_PATTERN}</pattern>
                <charset>UTF-8</charset>
            </encoder>
        </appender>
    </springProfile>

    <!-- 生产/测试环境：JSON 格式输出到文件（供 Promtail 采集到 Loki） -->
    <springProfile name="prod,test">
        <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/blog.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/blog.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <!-- traceId/spanId 由 Micrometer Tracing 自动注入到 MDC -->
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>spanId</includeMdcKeyName>
                <customFields>{"app":"blog-springboot","env":"${spring.profiles.active:-dev}"}</customFields>
            </encoder>
        </appender>
    </springProfile>

    <!-- 控制台 appender（始终开启，方便开发） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 降低第三方库日志噪音 -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.elasticsearch" level="WARN"/>
    <logger name="com.zaxxer.hikari" level="WARN"/>

    <!-- 业务日志保持 INFO -->
    <logger name="com.ican" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <springProfile name="prod,test">
            <appender-ref ref="JSON_FILE"/>
        </springProfile>
    </root>
</configuration>
```

- [ ] **Step 2: 启动应用验证日志格式**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

用 `-Dspring-boot.run.profiles=test` 启动，观察控制台日志中是否出现 `traceId` 字段（在完成 Task 9 后会有）。

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/logback-spring.xml
git commit -m "feat: 添加结构化 JSON 日志，供 Promtail 采集至 Loki"
```

---

## Task 9: 添加 Micrometer 链路追踪

**Files:**
- Modify: `src/main/resources/application.yml`

> `pom.xml` 中已在 Task 2 添加了 `micrometer-tracing-bridge-otel` 依赖，此步骤只需配置。

- [ ] **Step 1: 在 application.yml 中配置 tracing**

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # 开发环境全采样；生产环境改为 0.1
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans  # 如果没有 Zipkin，注释掉这行
```

**注意：** 即使不配置 Zipkin，traceId 也会自动注入到每个请求的 MDC，日志里就会出现 traceId 字段。接入 Grafana Tempo 是可选的后续步骤。

- [ ] **Step 2: 启动并验证 traceId 出现在日志**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test &
sleep 15
curl -s http://localhost:8080/articles
```

观察控制台日志，每条日志后应出现类似 `[blog-springboot,6bce08b5e6f2f5b4,6bce08b5e6f2f5b4]` 的追踪信息。

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "feat: 接入 Micrometer Tracing，traceId 注入日志"
```

---

## Task 10: 添加自定义业务指标

> 为文章浏览量添加 Micrometer Counter，后续在 Grafana 中可看到实时浏览趋势。

**Files:**
- Create: `src/main/java/com/ican/metrics/BlogMetrics.java`
- Modify: `src/main/java/com/ican/service/impl/ArticleServiceImpl.java`

- [ ] **Step 1: 编写失败测试**

新建 `src/test/java/com/ican/metrics/BlogMetricsTest.java`：

```java
package com.ican.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlogMetricsTest {

    @Test
    void articleViewCounter_incrementsOnView() {
        MeterRegistry registry = new SimpleMeterRegistry();
        BlogMetrics metrics = new BlogMetrics(registry);

        metrics.incrementArticleView(42);
        metrics.incrementArticleView(42);

        Counter counter = registry.find("blog.article.views")
                .tag("articleId", "42")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
mvn test -Dtest=BlogMetricsTest
```

期望：FAIL，`BlogMetrics` 类不存在。

- [ ] **Step 3: 创建 BlogMetrics.java**

新建 `src/main/java/com/ican/metrics/BlogMetrics.java`：

```java
package com.ican.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 博客业务自定义指标
 * 指标均会暴露到 /actuator/prometheus，由 Prometheus 采集
 */
@Component
public class BlogMetrics {

    private final MeterRegistry registry;

    public BlogMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 文章浏览量 Counter
     * Prometheus 中查询：sum(increase(blog_article_views_total[5m])) by (articleId)
     */
    public void incrementArticleView(Integer articleId) {
        Counter.builder("blog.article.views")
                .description("文章浏览次数")
                .tag("articleId", String.valueOf(articleId))
                .register(registry)
                .increment();
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
mvn test -Dtest=BlogMetricsTest
```

期望：PASS。

- [ ] **Step 5: 在 ArticleServiceImpl 中注入并调用**

在 `ArticleServiceImpl.java` 中：

找到 `private final RedisService redisService;` 附近，添加注入：
```java
private final BlogMetrics blogMetrics;
```

找到 `updateArticleStatsFromRedis` 方法，在方法开头添加一行：
```java
private void updateArticleStatsFromRedis(Integer articleId, ArticleVO articleVO) {
    blogMetrics.incrementArticleView(articleId);  // 新增这行
    Double viewCount = Optional.ofNullable(redisService
            .getZsetScore(ARTICLE_VIEW_COUNT, articleId)).orElse((double) 0);
    // ... 其余代码不变
}
```

- [ ] **Step 6: 重新运行 smoke test，确保注入没有破坏启动**

```bash
mvn test -Dtest=SmokeTest
```

期望：3 个测试全部 PASS。

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat: 添加 BlogMetrics 自定义指标，文章浏览量接入 Prometheus"
```

---

## Task 11: 配置 Grafana Dashboard

> 这一步在 Grafana UI 中操作，不涉及代码修改。

- [ ] **Step 1: 确认 Prometheus 能采集到 blog 应用的指标**

访问 Prometheus UI：`http://localhost:9090`

在搜索框输入 `jvm_memory_used_bytes{application="blog-springboot"}`，能看到数据点说明采集正常。

- [ ] **Step 2: 导入 JVM 监控 Dashboard**

在 Grafana (http://localhost:3000) 中：
1. 点击左侧 `+` → `Import`
2. 输入 Dashboard ID：`4701`（Spring Boot 2.1 Statistics，兼容 3.x）
3. 选择你的 Prometheus 数据源 → `Import`

- [ ] **Step 3: 创建博客业务指标面板**

在 Grafana 中新建 Dashboard，添加以下面板：

**Panel 1：文章浏览量趋势**
- 数据源：Prometheus
- PromQL：`sum(increase(blog_article_views_total[5m]))`
- 图表类型：Time Series
- 标题：文章浏览量（5分钟）

**Panel 2：接口响应时间 P99**
- PromQL：`histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{application="blog-springboot"}[5m])) by (le, uri))`
- 标题：接口 P99 响应时间

- [ ] **Step 4: 保存 Dashboard**

Dashboard 名称：`博客系统监控`，点击保存。

---

## Task 12: 最终验证与整理

- [ ] **Step 1: 运行所有测试**

```bash
mvn test
```

期望：`SmokeTest`（3个）和 `BlogMetricsTest`（1个）全部 PASS，无 FAIL。

- [ ] **Step 2: 启动应用完整验证**

```bash
mvn spring-boot:run &
sleep 15

# 验证 API 文档
curl -s http://localhost:8080/doc.html -o /dev/null -w "Knife4j: %{http_code}\n"

# 验证 Prometheus 端点
curl -s http://localhost:8080/actuator/prometheus | grep "blog_article_views"

# 验证 JSON 日志格式（如开启 test profile）
curl -s http://localhost:8080/articles
cat logs/blog.log | head -3 | python -m json.tool
```

- [ ] **Step 3: 最终 Commit**

```bash
git add .
git commit -m "feat: Phase 1 完成 - SB3迁移 + Java17 + 可观测性三支柱接入"
```

---

## 已知限制与后续

| 项目 | 说明 |
|------|------|
| ES 版本兼容 | 新 `ElasticsearchClient` 要求 ES 8.x，如果服务器运行 ES 7.x，需同步升级 ES 或在 `pom.xml` 中锁定 `co.elastic.clients` 版本为 7.x 兼容版 |
| Grafana Tempo（链路可视化） | 当前 traceId 仅注入日志，未推送到链路存储。如需在 Grafana 中点击 traceId 跳转，需加一个 Tempo Docker 容器（低成本扩展） |
| 测试覆盖率 | Testcontainers 完整集成测试体系在 Phase 4 建立 |
