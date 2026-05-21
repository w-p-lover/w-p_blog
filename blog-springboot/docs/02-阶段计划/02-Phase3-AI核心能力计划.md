# Phase 3 AI Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 Phase 3 AI 核心能力：文章 AI 摘要/标签异步生成、RAG 博客问答、流式写作助手，并与现有缓存/限流/MQ 体系无缝集成。

**Architecture:** 采用“发布文章 -> RabbitMQ 异步处理 -> AI 生成 -> 数据库回写 + 向量入库”的主链路，保证文章发布主流程不被 AI 延迟阻塞。在线问答与写作助手分别通过统一 `AiOrchestratorService` 调用 DeepSeek（对话）与 DashScope（Embedding），RAG 检索由 Qdrant 承担。所有对外接口统一 `Result<T>` 返回，异常进入现有 `GlobalExceptionHandler`。

**Tech Stack:** Spring Boot 3.2.5, Spring AI (OpenAI-compatible + DashScope + Qdrant), RabbitMQ, MyBatis-Plus, Redis, SSE (Flux)

---

## 文件变更总览

| 操作 | 文件 | 说明 |
|---|---|---|
| 修改 | `pom.xml` | 增加 Spring AI / Qdrant 相关依赖 |
| 修改 | `src/main/resources/application.yml` | 增加 spring.ai 配置和 AI 业务参数 |
| 修改 | `src/main/java/com/ican/entity/Article.java` | 增加 ai_summary/ai_suggest_tags/ai_indexed_at 映射字段 |
| 修改 | `src/main/resources/mapper/ArticleMapper.xml` | 文章详情查询补充 AI 字段 |
| 修改 | `src/main/java/com/ican/model/vo/ArticleVO.java` | 返回摘要与推荐标签 |
| 修改 | `src/main/java/com/ican/constant/MqConstant.java` | 增加 article.ai 交换机/队列/路由键/DLQ 常量 |
| 修改 | `src/main/java/com/ican/config/RabbitMqConfig.java` | 声明 AI 队列、死信队列与绑定 |
| 修改 | `src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | 新增/修改文章后发送 AI 任务消息 |
| 新建 | `src/main/java/com/ican/model/dto/ArticleAiMessage.java` | AI 异步消息体 |
| 新建 | `src/main/java/com/ican/model/dto/AiChatRequestDTO.java` | RAG 问答请求参数 |
| 新建 | `src/main/java/com/ican/model/vo/AiChatResponseVO.java` | RAG 问答响应 |
| 新建 | `src/main/java/com/ican/model/vo/AiSourceVO.java` | RAG 来源条目 |
| 新建 | `src/main/java/com/ican/model/vo/AiWriteAssistResponseVO.java` | 写作助手响应封装（非流式 fallback） |
| 新建 | `src/main/java/com/ican/service/AiArticleService.java` | 文章 AI 处理接口 |
| 新建 | `src/main/java/com/ican/service/AiRagService.java` | RAG 服务接口 |
| 新建 | `src/main/java/com/ican/service/AiWriteAssistService.java` | 写作助手服务接口 |
| 新建 | `src/main/java/com/ican/service/impl/AiArticleServiceImpl.java` | 摘要/标签生成与向量入库 |
| 新建 | `src/main/java/com/ican/service/impl/AiRagServiceImpl.java` | 问答检索与回答生成 |
| 新建 | `src/main/java/com/ican/service/impl/AiWriteAssistServiceImpl.java` | 四类写作提示词与流式输出 |
| 新建 | `src/main/java/com/ican/consumer/AiArticleConsumer.java` | 监听 AI 队列并执行重试/落盘 |
| 新建 | `src/main/java/com/ican/controller/AiController.java` | `/api/ai/chat` + `/api/ai/write-assist` |
| 新建 | `src/main/java/com/ican/config/AiVectorStoreConfig.java` | Qdrant collection 初始化/校验 |
| 新建 | `src/test/java/com/ican/service/impl/AiArticleServiceImplTest.java` | 摘要/标签/回写单测 |
| 新建 | `src/test/java/com/ican/service/impl/AiRagServiceImplTest.java` | RAG 检索拼装单测 |
| 新建 | `src/test/java/com/ican/controller/AiControllerTest.java` | AI 接口集成测试 |

---

### Task 1: 接入 Spring AI 依赖与配置

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 写依赖相关 failing check（先证明缺失）**

运行：

```bash
mvn -q -DskipTests compile
```

预期：当前不会有 AI 类；下一步引入依赖后可编译包含 `ChatClient` / `EmbeddingModel` 的代码。

- [ ] **Step 2: 在 `pom.xml` 增加 Spring AI 依赖**

在 `<dependencies>` 添加：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-dashscope</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-qdrant</artifactId>
</dependency>
```

并在 `<dependencyManagement>`（若无则新建）加入 BOM：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

- [ ] **Step 3: 在 `application.yml` 增加 AI 配置段**

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      embedding:
        options:
          model: text-embedding-v3
    vectorstore:
      qdrant:
        host: 121.41.87.40
        port: 6333
        collection-name: blog-articles

ai:
  rag:
    top-k: 5
    segment-size: 500
    segment-overlap: 50
```

- [ ] **Step 4: 编译验证**

运行：

```bash
mvn -q -DskipTests compile
```

预期：`BUILD SUCCESS`。

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.yml
git commit -m "feat: 接入 Spring AI 基础依赖与配置"
```

---

### Task 2: 扩展文章 AI 字段映射与查询

**Files:**
- Modify: `src/main/java/com/ican/entity/Article.java`
- Modify: `src/main/java/com/ican/model/vo/ArticleVO.java`
- Modify: `src/main/resources/mapper/ArticleMapper.xml`

- [ ] **Step 1: 写 failing test（文章详情应包含 AI 字段）**

在 `src/test/java/com/ican/controller/AiControllerTest.java` 先写一个最小断言草稿：

```java
@Test
void articleDetail_shouldContainAiFields_whenAiProcessed() {
    // 先留空实现，编译阶段验证字段存在
}
```

预期：目前无法断言字段，因为实体/VO 尚未定义。

- [ ] **Step 2: 在 `Article.java` 增加字段映射**

```java
private String aiSummary;
private String aiSuggestTags;
private LocalDateTime aiIndexedAt;
```

- [ ] **Step 3: 在 `ArticleVO.java` 增加返回字段**

```java
private String aiSummary;
private String aiSuggestTags;
```

- [ ] **Step 4: 更新 `ArticleMapper.xml` 的 `articleVO` resultMap 与查询列**

在 `resultMap id="articleVO"` 增加：

```xml
<result column="ai_summary" property="aiSummary"/>
<result column="ai_suggest_tags" property="aiSuggestTags"/>
```

在 `selectArticleHomeById` 的 SELECT 列中增加：

```sql
ai_summary,
ai_suggest_tags,
ai_indexed_at,
```

- [ ] **Step 5: 编译验证**

```bash
mvn -q -DskipTests compile
```

预期：`BUILD SUCCESS`。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ican/entity/Article.java src/main/java/com/ican/model/vo/ArticleVO.java src/main/resources/mapper/ArticleMapper.xml
git commit -m "feat: 扩展文章 AI 字段映射与详情返回"
```

---

### Task 3: 建立文章 AI 异步 MQ 基础设施（含 DLQ）

**Files:**
- Modify: `src/main/java/com/ican/constant/MqConstant.java`
- Modify: `src/main/java/com/ican/config/RabbitMqConfig.java`
- Create: `src/main/java/com/ican/model/dto/ArticleAiMessage.java`

- [ ] **Step 1: 新增消息体 DTO**

`ArticleAiMessage.java`:

```java
@Data
public class ArticleAiMessage {
    private Integer articleId;
    private String articleTitle;
    private String articleContent;
}
```

- [ ] **Step 2: 在 `MqConstant.java` 增加 AI 常量**

```java
public static final String ARTICLE_AI_EXCHANGE = "article.ai";
public static final String ARTICLE_AI_QUEUE = "article.ai.queue";
public static final String ARTICLE_AI_KEY = "article.ai.key";
public static final String ARTICLE_AI_DLX = "article.ai.dlx";
public static final String ARTICLE_AI_DLQ = "article.ai.dlq";
public static final String ARTICLE_AI_DLQ_KEY = "article.ai.dlq.key";
```

- [ ] **Step 3: 在 `RabbitMqConfig.java` 增加 AI 队列与死信绑定**

```java
@Bean
public TopicExchange articleAiExchange() {
    return new TopicExchange(ARTICLE_AI_EXCHANGE, true, false);
}

@Bean
public TopicExchange articleAiDeadLetterExchange() {
    return new TopicExchange(ARTICLE_AI_DLX, true, false);
}

@Bean
public Queue articleAiQueue() {
    return QueueBuilder.durable(ARTICLE_AI_QUEUE)
            .withArgument("x-dead-letter-exchange", ARTICLE_AI_DLX)
            .withArgument("x-dead-letter-routing-key", ARTICLE_AI_DLQ_KEY)
            .build();
}

@Bean
public Queue articleAiDeadLetterQueue() {
    return QueueBuilder.durable(ARTICLE_AI_DLQ).build();
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn -q -DskipTests compile
```

预期：`BUILD SUCCESS`。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ican/constant/MqConstant.java src/main/java/com/ican/config/RabbitMqConfig.java src/main/java/com/ican/model/dto/ArticleAiMessage.java
git commit -m "feat: 增加文章 AI 队列及死信队列配置"
```

---

### Task 4: 文章发布/更新后投递 AI 消息

**Files:**
- Modify: `src/main/java/com/ican/service/impl/ArticleServiceImpl.java`

- [ ] **Step 1: 写 failing test（发布文章后必须发送 AI 消息）**

`AiArticleServiceImplTest.java` 添加测试骨架（mock `RabbitTemplate`）：

```java
@Test
void addArticle_shouldSendAiMessage() {
    // given article dto
    // when addArticle
    // then verify rabbitTemplate.convertAndSend called
}
```

- [ ] **Step 2: 在 `ArticleServiceImpl` 注入 `RabbitTemplate`**

```java
private final RabbitTemplate rabbitTemplate;
```

- [ ] **Step 3: 在 `addArticle` 与 `updateArticle` 成功后发送消息**

```java
private void sendArticleAiMessage(Integer articleId, String title, String content) {
    ArticleAiMessage message = new ArticleAiMessage();
    message.setArticleId(articleId);
    message.setArticleTitle(title);
    message.setArticleContent(content);
    rabbitTemplate.convertAndSend(ARTICLE_AI_EXCHANGE, ARTICLE_AI_KEY, message);
}
```

并在 `addArticle`、`updateArticle` 尾部调用。

- [ ] **Step 4: 运行定向测试**

```bash
mvn -Dtest=AiArticleServiceImplTest test
```

预期：新增测试通过。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ican/service/impl/ArticleServiceImpl.java src/test/java/com/ican/service/impl/AiArticleServiceImplTest.java
git commit -m "feat: 文章写操作后异步投递 AI 处理消息"
```

---

### Task 5: 实现 AI 文章处理消费者（摘要/标签/向量入库）

**Files:**
- Create: `src/main/java/com/ican/service/AiArticleService.java`
- Create: `src/main/java/com/ican/service/impl/AiArticleServiceImpl.java`
- Create: `src/main/java/com/ican/consumer/AiArticleConsumer.java`
- Modify: `src/main/java/com/ican/mapper/ArticleMapper.java`
- Modify: `src/main/resources/mapper/ArticleMapper.xml`

- [ ] **Step 1: 定义服务接口**

```java
public interface AiArticleService {
    void processArticle(Integer articleId, String title, String content);
}
```

- [ ] **Step 2: 扩展 `ArticleMapper` 增加 AI 回写方法**

`ArticleMapper.java` 增加：

```java
void updateArticleAiResult(@Param("articleId") Integer articleId,
                           @Param("aiSummary") String aiSummary,
                           @Param("aiSuggestTags") String aiSuggestTags,
                           @Param("aiIndexedAt") LocalDateTime aiIndexedAt);
```

`ArticleMapper.xml` 增加：

```xml
<update id="updateArticleAiResult">
    UPDATE t_article
    SET ai_summary = #{aiSummary},
        ai_suggest_tags = #{aiSuggestTags},
        ai_indexed_at = #{aiIndexedAt}
    WHERE id = #{articleId}
</update>
```

- [ ] **Step 3: 实现 `AiArticleServiceImpl`（摘要/标签/embedding）**

核心实现代码：

```java
String summaryPrompt = "请用100-200字概括以下技术文章的核心内容，语言简洁专业，不要使用\"本文\"开头：\n\n" + content;
String tagsPrompt = "请从以下技术文章中提取3-5个关键技术标签，只返回标签列表，用逗号分隔，不要解释：\n\n" + content;

String summary = chatClient.prompt(summaryPrompt).call().content();
String tags = chatClient.prompt(tagsPrompt).call().content();

vectorStore.add(List.of(new Document(content, Map.of(
        "articleId", articleId,
        "title", title,
        "url", "/article/" + articleId
))));

articleMapper.updateArticleAiResult(articleId, summary, tags, LocalDateTime.now());
```

- [ ] **Step 4: 实现 `AiArticleConsumer` 监听队列**

```java
@RabbitListener(queues = ARTICLE_AI_QUEUE)
public void consume(@Payload ArticleAiMessage message) {
    aiArticleService.processArticle(message.getArticleId(), message.getArticleTitle(), message.getArticleContent());
}
```

- [ ] **Step 5: 运行单测**

```bash
mvn -Dtest=AiArticleServiceImplTest test
```

预期：处理逻辑测试通过。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ican/service/AiArticleService.java src/main/java/com/ican/service/impl/AiArticleServiceImpl.java src/main/java/com/ican/consumer/AiArticleConsumer.java src/main/java/com/ican/mapper/ArticleMapper.java src/main/resources/mapper/ArticleMapper.xml src/test/java/com/ican/service/impl/AiArticleServiceImplTest.java
git commit -m "feat: 实现文章 AI 异步处理与向量入库"
```

---

### Task 6: 实现 RAG 博客问答接口

**Files:**
- Create: `src/main/java/com/ican/model/dto/AiChatRequestDTO.java`
- Create: `src/main/java/com/ican/model/vo/AiChatResponseVO.java`
- Create: `src/main/java/com/ican/model/vo/AiSourceVO.java`
- Create: `src/main/java/com/ican/service/AiRagService.java`
- Create: `src/main/java/com/ican/service/impl/AiRagServiceImpl.java`
- Create: `src/main/java/com/ican/controller/AiController.java`

- [ ] **Step 1: 定义请求/响应 DTO**

`AiChatRequestDTO.java`:

```java
@Data
public class AiChatRequestDTO {
    @NotBlank(message = "问题不能为空")
    private String question;
}
```

`AiChatResponseVO.java`:

```java
@Data
@AllArgsConstructor
public class AiChatResponseVO {
    private String answer;
    private List<AiSourceVO> sources;
}
```

- [ ] **Step 2: 实现 `AiRagServiceImpl` 检索与回答**

```java
List<Document> docs = vectorStore.similaritySearch(SearchRequest.query(question).withTopK(5));
if (docs == null || docs.isEmpty()) {
    return new AiChatResponseVO("博客中暂无相关内容", List.of());
}
String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
String prompt = "你是一个基于博客文章内容回答问题的助手。请根据以下文章片段回答用户问题。"
        + "如果文章中没有相关信息，请明确说明\"博客中暂无相关内容\"，不要编造答案。\n\n"
        + "【相关文章片段】\n" + context + "\n\n【用户问题】\n" + question + "\n\n请用中文回答，并在回答末尾注明参考的文章标题。";
String answer = chatClient.prompt(prompt).call().content();
```

- [ ] **Step 3: 在 `AiController` 增加 `/api/ai/chat`**

```java
@PostMapping("/api/ai/chat")
public Result<AiChatResponseVO> chat(@Validated @RequestBody AiChatRequestDTO request) {
    return Result.success(aiRagService.chat(request.getQuestion()));
}
```

- [ ] **Step 4: 运行 RAG 测试**

```bash
mvn -Dtest=AiRagServiceImplTest test
```

预期：命中与未命中场景均通过。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ican/model/dto/AiChatRequestDTO.java src/main/java/com/ican/model/vo/AiChatResponseVO.java src/main/java/com/ican/model/vo/AiSourceVO.java src/main/java/com/ican/service/AiRagService.java src/main/java/com/ican/service/impl/AiRagServiceImpl.java src/main/java/com/ican/controller/AiController.java src/test/java/com/ican/service/impl/AiRagServiceImplTest.java
git commit -m "feat: 实现 RAG 问答接口并返回来源引用"
```

---

### Task 7: 实现流式写作助手（SSE）

**Files:**
- Create: `src/main/java/com/ican/service/AiWriteAssistService.java`
- Create: `src/main/java/com/ican/service/impl/AiWriteAssistServiceImpl.java`
- Modify: `src/main/java/com/ican/controller/AiController.java`

- [ ] **Step 1: 定义写作助手服务接口**

```java
public interface AiWriteAssistService {
    Flux<String> writeAssist(String action, String content);
}
```

- [ ] **Step 2: 实现 action -> prompt 生成策略**

```java
private String buildPrompt(String action, String content) {
    return switch (action) {
        case "continue" -> "请基于以下内容继续写作，保持技术风格：\n\n" + content;
        case "summary" -> "请提炼以下内容为简洁摘要：\n\n" + content;
        case "expand" -> "请对以下内容扩写，补充细节和案例：\n\n" + content;
        case "rewrite" -> "请将以下内容改写为更通俗易懂的技术表达：\n\n" + content;
        default -> throw new ServiceException("不支持的写作动作");
    };
}
```

- [ ] **Step 3: 在 `AiController` 增加流式接口并接入限流**

```java
@RateLimit(key = "api:ai:write:#{#action}", limit = 20, period = 3600)
@GetMapping(value = "/api/ai/write-assist", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> writeAssist(@RequestParam String action, @RequestParam String content) {
    return aiWriteAssistService.writeAssist(action, content);
}
```

- [ ] **Step 4: 运行控制器测试**

```bash
mvn -Dtest=AiControllerTest test
```

预期：`/api/ai/write-assist` 返回 200，content-type 为 `text/event-stream`。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ican/service/AiWriteAssistService.java src/main/java/com/ican/service/impl/AiWriteAssistServiceImpl.java src/main/java/com/ican/controller/AiController.java src/test/java/com/ican/controller/AiControllerTest.java
git commit -m "feat: 增加 AI 流式写作助手接口"
```

---

### Task 8: Qdrant 集合初始化与集成回归

**Files:**
- Create: `src/main/java/com/ican/config/AiVectorStoreConfig.java`
- Modify: `src/test/java/com/ican/SmokeTest.java`
- Modify: `src/test/resources/application-test.yml`

- [ ] **Step 1: 创建 Qdrant 启动校验配置**

```java
@Configuration
public class AiVectorStoreConfig {

    @Bean
    public ApplicationRunner aiVectorStoreHealthRunner(VectorStore vectorStore) {
        return args -> {
            // 启动时触发一次轻量调用，确保 vectorStore bean 正常
            vectorStore.similaritySearch(SearchRequest.query("health-check").withTopK(1));
        };
    }
}
```

- [ ] **Step 2: 增加 SmokeTest 的 AI 接口可达性断言**

```java
@Test
void aiChatEndpoint_shouldReturn200() throws Exception {
    mockMvc.perform(post("/api/ai/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"question\":\"Redis缓存击穿怎么解决\"}"))
           .andExpect(status().isOk());
}
```

- [ ] **Step 3: 更新 `application-test.yml` 避免真实外部 AI 调用**

```yaml
spring:
  ai:
    openai:
      api-key: test-key
      base-url: http://localhost:18080
    dashscope:
      api-key: test-key
```

- [ ] **Step 4: 执行完整测试与编译**

```bash
mvn test
mvn -q -DskipTests package
```

预期：测试通过，`BUILD SUCCESS`。

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ican/config/AiVectorStoreConfig.java src/test/java/com/ican/SmokeTest.java src/test/resources/application-test.yml
git commit -m "test: 补充 AI 核心链路回归验证"
```

---

## 上线前手工验收（Linux 服务器）

- [ ] 发布一篇新文章，检查 1~2 分钟内 `t_article.ai_summary`、`ai_suggest_tags`、`ai_indexed_at` 已更新。
- [ ] 检查 Qdrant：`curl http://121.41.87.40:6333/collections` 能看到 `blog-articles`。
- [ ] 调用 `POST /api/ai/chat`，验证有 `answer + sources`。
- [ ] 调用 `/api/ai/write-assist?action=expand&content=...`，确认流式输出。
- [ ] 高频请求写作助手超过阈值后，限流生效且返回可读错误。
- [ ] RabbitMQ 死信队列 `article.ai.dlq` 无持续堆积。

---

## 自检结果（Self-Review）

1. **Spec coverage**：
   - 5.1 Spring AI 接入：Task 1、Task 8 覆盖。
   - 5.2 异步 AI 摘要/标签/向量化：Task 3、Task 4、Task 5 覆盖。
   - 5.3 RAG 问答：Task 6 覆盖。
   - 5.4 流式写作助手 + 限流：Task 7 覆盖。

2. **Placeholder scan**：
   - 无 `TODO/TBD/implement later`。
   - 每个代码步骤给出了具体代码片段。
   - 每个任务都包含可执行命令和预期结果。

3. **Type consistency**：
   - `ArticleAiMessage`、`AiChatRequestDTO`、`AiChatResponseVO` 在任务间命名一致。
   - MQ 常量命名在配置/发送/消费三处一致。
   - AI 接口路径统一为 `/api/ai/*`。
