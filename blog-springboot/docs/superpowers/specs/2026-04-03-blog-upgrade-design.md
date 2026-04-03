# 博客系统全面升级设计文档

- **日期**：2026-04-03
- **项目**：blog-springboot（Spring Boot 2.6 → 3.x 全面升级）
- **目标**：提升系统工程化质量 + 集成 AI 能力，增强求职竞争力与技术深度

---

## 一、背景与目标

### 现状

| 维度 | 现状 |
|------|------|
| 技术栈 | Spring Boot 2.6、Java 8/11、MyBatis-Plus、Redis、RabbitMQ、ES、Sa-Token、Quartz |
| 缓存 | Caffeine + Redis 两级缓存已有雏形，但逻辑散落，无统一抽象 |
| 分布式锁 | 手写 `setIfAbsent` 自旋，存在异常时锁泄漏风险 |
| 可观测性 | Docker 中已有 Prometheus + Grafana + Loki，应用侧尚未对接 |
| AI 能力 | 无 |
| 测试 | 无体系 |

### 目标

1. **工程化深度**：可观测性、多级缓存、分布式锁、限流，达到生产可用标准
2. **AI 能力**：集成 Spring AI + DeepSeek，实现文章智能处理、RAG 问答、写作助手
3. **技术栈现代化**：升级至 Spring Boot 3.x + Java 17，使用 Spring AI 框架
4. **文档完整**：每个阶段有独立的技术说明，可用于面试展示与日常参考

---

## 二、整体架构

### 技术栈全景

```
┌─────────────────────────────────────────────────────────┐
│                      前端（Vue）                          │
│         shoka-blog（博客）/ shoka-admin（管理后台）        │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP / WebSocket / SSE
┌────────────────────────▼────────────────────────────────┐
│                Spring Boot 3.x (Java 17)                 │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │Controller│  │ Service  │  │  Mapper  │  │Consumer │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                    基础设施层                         │ │
│  │  多级缓存   分布式锁   限流   Spring AI   可观测性     │ │
│  └─────────────────────────────────────────────────────┘ │
└──────┬──────┬──────┬──────┬──────┬──────┬───────────────┘
       │      │      │      │      │      │
    MySQL  Redis  RabbitMQ  ES  Qdrant  Prometheus
                                        /Grafana/Loki
```

### 新增 / 升级组件对照

| 组件 | 变化 | 作用 |
|------|------|------|
| Spring Boot 2.6 → 3.x | 升级 | Spring AI 必须，Java 17 支持 |
| Java 8/11 → Java 17 | 升级 | LTS，虚拟线程，Records，Switch 表达式 |
| Caffeine + Redis（散落） → MultiLevelCacheManager | 重构 | 统一缓存抽象，@Cacheable 无感知 |
| 手写 setNX → Redisson | 替换 | 安全的分布式锁，自动续期 |
| 无 → @RateLimit + Redis Lua | 新增 | 接口限流，防刷 |
| 无 → Micrometer Tracing | 新增 | 链路追踪，对接 Grafana |
| 无 → Spring AI + DeepSeek | 新增 | AI 文章处理、RAG 问答 |
| 无 → Qdrant | 新增 | 向量数据库，RAG 检索 |
| ES 关键词搜索 → 混合搜索 | 升级 | BM25 + kNN 语义检索融合 |
| 无 → JUnit 5 + Testcontainers | 新增 | 单元 + 集成测试体系 |

---

## 三、阶段一：地基

> **交付物**：Spring Boot 3.x 运行成功 + 应用指标/日志/链路接入 Grafana

### 3.1 Spring Boot 3.x 迁移

#### 主要破坏性变更

| 变更类型 | 旧写法 | 新写法 |
|----------|--------|--------|
| 包名全局替换 | `javax.*` | `jakarta.*` |
| Security 配置 | 继承 `WebSecurityConfigurerAdapter` | 注册 `SecurityFilterChain` Bean |
| Actuator 端点 | 默认全部开放 | 仅默认开放 health、info，需显式配置 |
| MyBatis-Plus | 3.4.x | 升级至 3.5.5+ |
| Sa-Token | 需验证兼容版本 | 升级至 1.37+ |
| ES 客户端 | `elasticsearch-rest-high-level-client`（已废弃） | `elasticsearch-java` + Spring Data ES 5.x |

#### 迁移步骤

1. 新建 `feature/spring-boot-3` 分支，保留回滚能力
2. 升级 `pom.xml`：parent → 3.2.x，Java 版本 → 17
3. 全局替换 `javax` → `jakarta`（IDEA 一键重构）
4. 逐一修复编译报错（Security、Actuator 配置重写）
5. 跑通所有接口，回归测试

#### Java 17 新特性使用策略

不为用而用，仅在有明确收益时使用：

| 特性 | 使用场景 | 示例 |
|------|----------|------|
| `record` | 纯数据 DTO/VO | `record ArticleInfoDTO(String title, String content) {}` |
| Text Block | 多行 Prompt 模板 | AI 提示词模板 |
| Switch 表达式 | 枚举状态判断 | 文章状态、搜索模式分支 |
| `instanceof` 模式匹配 | 去掉强制类型转换 | 异常处理、类型判断 |

### 3.2 可观测性深度接入

#### 三个维度

```
指标 (Metrics)
  ├── Micrometer 自动采集：JVM 堆内存、GC、HTTP 请求耗时、连接池
  ├── 自定义业务指标：
  │     - 文章浏览量 Counter
  │     - AI API 调用次数 Counter + 耗时 Histogram
  │     - 限流触发次数 Counter
  └── 暴露 /actuator/prometheus → Prometheus 定时抓取

日志 (Logs)
  ├── Logback 输出 JSON 结构化日志（logstash-logback-encoder）
  ├── 包含字段：timestamp、level、logger、message、traceId、spanId
  ├── Promtail 采集日志文件 → Loki
  └── Grafana 中可通过 traceId 从日志跳转到链路

链路追踪 (Traces)
  ├── Micrometer Tracing + OpenTelemetry Bridge
  ├── traceId 自动注入每个请求（HTTP、MQ、@Async 均传播）
  └── 后续可低成本接入 Grafana Tempo（加一个 Docker 容器）
```

#### Grafana Dashboard 规划

| 面板 | 核心指标 |
|------|----------|
| JVM 健康 | 堆内存使用率、GC 频率、线程数、类加载数 |
| 接口质量 | 各接口 QPS、P50/P99 响应时间、4xx/5xx 错误率 |
| 缓存效果 | L1 命中率、L2 命中率、缓存穿透次数（Phase 2 后补充） |
| AI 监控 | LLM 调用次数、Token 消耗趋势、平均响应时间（Phase 3 后补充） |

#### 关键配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0  # 开发环境全采样，生产环境调低至 0.1

logging:
  pattern:
    # 结构化日志由 logback-spring.xml 配置，此处仅示意
    level: INFO
```

---

## 四、阶段二：工程质量

> **交付物**：多级缓存统一抽象 + Redisson 分布式锁 + 接口限流注解

### 4.1 多级缓存体系

#### 架构

```
请求
 │
 ▼
L1 Caffeine（进程内，纳秒级，200 条上限，5 分钟过期）
 │ miss
 ▼
L2 Redis（分布式，毫秒级，TTL 动态计算）
 │ miss
 ▼
数据库
 │
 └── 回填 L2 → 回填 L1
```

#### MultiLevelCacheManager 设计

```java
// 伪代码，说明接口语义
public class MultiLevelCacheManager implements CacheManager {

    // L1: Caffeine，L2: Redis
    // get(): L1 → L2 → DB（由 @Cacheable 的 loader 触发）
    // put(): 同时写 L1 + L2
    // evict(): 同时清 L1 + L2（需广播 L1 失效，多实例场景）
    // L2 TTL = baseTTL + log(viewCount) * 因子 + 随机抖动（复用现有 getDynamicTTL 逻辑）
}
```

**多实例 L1 失效广播**：通过 Redis Pub/Sub，某实例更新数据后广播 `cache:evict:{key}`，其他实例监听后清除本地 L1。

#### 缓存三大问题

| 问题 | 触发场景 | 解决方案 |
|------|----------|----------|
| 穿透 | 查询不存在的文章 ID，绕过缓存打到 DB | Redisson 布隆过滤器（文章发布时写入）+ 空值缓存双重保险 |
| 击穿 | 热门文章缓存刚好过期，大量并发同时重建 | Redisson 分布式锁保护重建过程（见 4.2） |
| 雪崩 | 大量 key 同时过期，DB 瞬间压力暴增 | TTL = base + random(0 ~ base*30%)，错开过期时间 |

#### 缓存 Key 规范

```
格式：{模块}:{资源类型}:{唯一标识}

示例：
  blog:article:1024          # 文章详情
  blog:article:hot           # 热点文章 hash
  blog:site:config           # 站点配置
  blog:user:1:profile        # 用户信息
```

### 4.2 Redisson 分布式锁

#### 替换现有 setNX 方案

```java
// 现有方案：手写自旋 + 手动释放，有泄漏风险
for (int i = 0; i < 3; i++) {
    if (redisService.setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS)) { ... }
    Thread.sleep(200);
}

// 新方案：Redisson RLock，自动续期 + 可重入 + finally 保证释放
RLock lock = redissonClient.getLock(lockKey);
boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
if (acquired) {
    try { /* 临界区 */ } finally { lock.unlock(); }
}
```

#### @DistributedLock 注解封装

```java
@DistributedLock(key = "'article:rebuild:' + #articleId", waitTime = 3, leaseTime = 5)
public ArticleVO rebuildArticleCache(Integer articleId) { ... }
```

AOP 拦截注解，SpEL 解析 key，失败快速返回或降级查库，业务代码零侵入。

#### 主要使用场景

| 场景 | Key 示例 | 说明 |
|------|----------|------|
| 缓存击穿重建 | `article:rebuild:{id}` | 防并发重建 |
| 浏览量刷库 | `article:viewflush` | Quartz 任务防重 |
| AI 摘要生成 | `article:ai:{id}` | 防重复触发 AI 调用 |

### 4.3 接口限流

#### 算法选型：滑动窗口

- **固定窗口**：简单，但窗口边界有 2 倍突刺问题
- **滑动窗口**（选用）：流量平滑，Lua 脚本保证原子性，实现适中
- **令牌桶**：适合精细流控，复杂度更高，本项目暂不需要

#### Lua 核心逻辑（Redis 原子执行）

使用 ZSET 实现真正的滑动窗口，避免固定窗口在边界处 2 倍突刺问题：

```lua
-- KEYS[1]: rate_limit:{ip/userId}:{接口}
-- ARGV[1]: 当前时间戳(ms)  ARGV[2]: 窗口大小(ms)  ARGV[3]: 限制次数
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local key = KEYS[1]

-- 删除窗口外的旧记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
-- 统计窗口内请求数
local count = redis.call('ZCARD', key)
if count < limit then
    -- 将当前请求加入窗口（score=时间戳，member=时间戳+随机数保证唯一）
    redis.call('ZADD', key, now, now .. math.random())
    redis.call('PEXPIRE', key, window)
    return 1  -- 允许
end
return 0  -- 拒绝
```

#### @RateLimit 注解

```java
// 登录接口：5次/分钟
@RateLimit(limit = 5, window = 60, key = "#request.remoteAddr", message = "操作过于频繁，请稍后再试")
public Result login(...) { }

// AI 问答：20次/小时
@RateLimit(limit = 20, window = 3600, key = "@stpUtil.getLoginId()")
public Result aiChat(...) { }
```

#### 分级策略

| 接口类型 | 限制 | Key 维度 |
|----------|------|----------|
| 登录/注册 | 5 次/分钟 | IP |
| 评论发布 | 10 次/分钟 | 用户 ID |
| AI 问答 | 20 次/小时 | 用户 ID |
| AI 写作助手 | 20 次/小时 | 用户 ID |
| 普通查询 | 100 次/分钟 | IP |

超限响应：HTTP 429 + `{"code": 429, "message": "操作过于频繁，请稍后再试"}`

---

## 五、阶段三：AI 核心

> **交付物**：文章 AI 摘要/标签 + RAG 博客问答 + 流式写作助手

### 5.1 Spring AI 框架接入

#### 模型分工

| 用途 | 服务商 | 模型 | 理由 |
|------|--------|------|------|
| 文本生成 / 对话 | DeepSeek | `deepseek-chat` | 中文理解强，API 兼容 OpenAI，价格极低 |
| 向量嵌入 | 阿里云 DashScope | `text-embedding-v3` | DeepSeek 无专用 embedding 模型，DashScope 中文向量质量好 |

#### 核心配置

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      embedding:
        options:
          model: text-embedding-v3
    vectorstore:
      qdrant:
        host: localhost
        port: 6333
        collection-name: blog-articles
```

#### 向量数据库选型：Qdrant

- 项目已有 ES，但 ES 向量搜索升级较重（ES 客户端需切换），放到 Phase 4
- Qdrant：轻量 Docker 容器，Spring AI 原生支持，开发阶段零运维成本
- Docker 启动：`docker run -p 6333:6333 qdrant/qdrant`

### 5.2 文章发布 → AI 异步处理

#### 数据流

```
POST /article (新增/更新文章)
    └── ArticleServiceImpl.addArticle() / updateArticle()
            └── 发送 MQ 消息：ArticleAiMessage { articleId, title, content }
                    └── RabbitMQ: exchange=article.ai, queue=article.ai.queue

AiArticleConsumer.consume(ArticleAiMessage)
    ├── 调用 DeepSeek：生成 100~200 字中文摘要
    ├── 调用 DeepSeek：推荐 3~5 个技术标签
    ├── 将摘要和标签写入 article 表（ai_summary, ai_suggest_tags 字段）
    └── 调用 DashScope：生成文章内容向量 → 存入 Qdrant
```

#### Prompt 设计

```
摘要 Prompt：
"请用100-200字概括以下技术文章的核心内容，语言简洁专业，不要使用"本文"开头：\n\n{content}"

标签 Prompt：
"请从以下技术文章中提取3-5个关键技术标签，只返回标签列表，用逗号分隔，不要解释：\n\n{content}"
```

#### 数据库变更

```sql
ALTER TABLE t_article
    ADD COLUMN ai_summary     VARCHAR(500)  NULL COMMENT 'AI生成摘要',
    ADD COLUMN ai_suggest_tags VARCHAR(200) NULL COMMENT 'AI推荐标签（逗号分隔）',
    ADD COLUMN ai_indexed_at  DATETIME      NULL COMMENT 'AI向量索引时间';
```

#### 异常处理

- 失败自动重试（RabbitMQ 重试策略：3 次，间隔指数退避）
- 3 次失败后进入死信队列 `article.ai.dlq`，记录日志，不影响文章正常发布
- 管理后台可手动触发重新生成

### 5.3 RAG 博客知识库问答

#### 整体流程

```
[数据准备 - 离线/异步]
文章内容 → 按段落分块（500 token，50 token overlap）
    └── DashScope embedding → float[] 向量
            └── 存入 Qdrant（附带 metadata：articleId, title, url）

[用户提问 - 在线]
用户问题
    └── DashScope embedding → 问题向量
            └── Qdrant 相似度检索（Top 5，cosine similarity）
                    └── 拼装 Prompt（问题 + 5 段上下文）
                            └── DeepSeek 生成回答（含来源引用）
                                    └── 返回前端展示
```

#### Prompt 模板

```
你是一个基于博客文章内容回答问题的助手。请根据以下文章片段回答用户问题。
如果文章中没有相关信息，请明确说明"博客中暂无相关内容"，不要编造答案。

【相关文章片段】
{context}

【用户问题】
{question}

请用中文回答，并在回答末尾注明参考的文章标题。
```

#### API 设计

```
POST /api/ai/chat
Request:  { "question": "Redis 缓存击穿怎么解决？" }
Response: {
  "answer": "缓存击穿是指热点 key 过期后...",
  "sources": [
    { "title": "Redis 缓存问题总结", "url": "/article/42" }
  ]
}
```

### 5.4 流式写作助手

#### 功能设计

编辑器右侧 AI 面板，支持四种操作：

| 操作 | 触发方式 | Prompt 策略 |
|------|----------|-------------|
| 续写 | 选中段落末尾，点击续写 | 基于上下文预测后续内容 |
| 摘要 | 选中全文或段落 | 压缩提炼核心观点 |
| 扩写 | 选中简短段落 | 补充细节、案例、解释 |
| 改写 | 选中文字，选择风格 | 技术风格 / 通俗易懂 |

#### 流式 SSE 实现

```java
// 后端：Spring AI 流式输出
@GetMapping(value = "/ai/write-assist", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> writeAssist(@RequestParam String action,
                                 @RequestParam String content) {
    String prompt = buildPrompt(action, content);
    return chatClient.prompt(prompt).stream().content();
}
```

```javascript
// 前端：EventSource 逐字展示
const source = new EventSource(`/api/ai/write-assist?action=expand&content=${encodeURIComponent(text)}`);
source.onmessage = (e) => { outputText.value += e.data; };
```

限流：接入 Phase 2 的 `@RateLimit(limit=20, window=3600)`，超限提示"今日 AI 使用次数已达上限"。

---

## 六、阶段四：高级特性

> **交付物**：ES 混合搜索 + 完整测试体系

### 6.1 混合搜索升级

#### 现状 vs 目标

| | 现状 | 目标 |
|--|------|------|
| 搜索方式 | BM25 关键词匹配 | BM25 + kNN 向量检索，RRF 融合排序 |
| 能找到 | "Redis 缓存击穿" | "Redis 缓存击穿" + "redis 并发问题" |
| ES 客户端 | 已废弃的 rest-high-level-client | elasticsearch-java（Spring Boot 3.x 对应） |

#### 技术实现

```
ES 文档结构新增字段：
  content_vector: dense_vector(dims=1536)  # DashScope embedding 维度

搜索请求（ES 8.x Hybrid Search）：
  BM25: match query on title/content
  kNN:  k=10, num_candidates=100, field=content_vector
  融合: RRF (Reciprocal Rank Fusion)，ES 8.8+ 原生支持
```

向量来源复用 Phase 3 DashScope embedding，文章索引时同步写入，不增加额外调用成本。

### 6.2 测试体系建设

#### 分层策略

```
单元测试（Unit Tests）
  工具：JUnit 5 + Mockito
  目标：Service 层核心逻辑，mock 所有外部依赖
  重点：
    - MultiLevelCacheManager 命中/穿透/降级逻辑
    - @RateLimit 边界：第 N 次通过，第 N+1 次拒绝
    - AiArticleConsumer 重试逻辑
    - RAG 检索流程（mock 向量检索结果）

集成测试（Integration Tests）
  工具：Testcontainers（自动启动真实容器）
  目标：Controller → Service → DB/Redis/MQ 完整链路
  容器：MySQL 8、Redis 7、RabbitMQ 3
  重点：
    - 文章 CRUD + 缓存一致性验证
    - 文章发布后 MQ 消息是否正确投递
    - 限流注解在真实 Redis 下的并发行为

覆盖率（Coverage）
  工具：JaCoCo
  目标：核心模块（service/impl）行覆盖率 ≥ 60%
  报告：`mvn test jacoco:report`，输出 target/site/jacoco/index.html
```

#### 测试命名规范

```java
// 格式：方法名_场景_预期结果
@Test
void getArticleById_whenCacheHit_shouldNotQueryDatabase() { }

@Test
void getArticleById_whenCacheExpired_shouldRebuildWithLock() { }

@Test
void rateLimit_whenExceedLimit_shouldReturn429() { }
```

---

## 七、文档规划

每个阶段完成后产出对应文档，供面试展示与日常参考：

| 文档 | 内容要点 |
|------|----------|
| `docs/phase1-migration.md` | Spring Boot 3.x 迁移全过程，before/after 代码对比，踩坑记录 |
| `docs/phase2-cache.md` | 多级缓存设计原理，三大问题解决方案，性能对比数据 |
| `docs/phase2-ratelimit.md` | 限流算法对比，滑动窗口 Lua 实现，压测结果 |
| `docs/phase3-spring-ai.md` | Spring AI 快速上手，DeepSeek 接入，模型选型决策 |
| `docs/phase3-rag.md` | RAG 架构详解，向量化流程，Prompt 工程实践 |
| `docs/phase4-search.md` | 混合搜索原理，ES kNN 配置，效果对比 |
| `docs/phase4-testing.md` | 测试分层策略，Testcontainers 使用，覆盖率报告解读 |

---

## 八、面试亮点索引

完成后可在面试中展开讲述的技术点：

| 技术点 | 能讲的深度 |
|--------|-----------|
| 多级缓存 | 设计思路 → 缓存三大问题 → 布隆过滤器原理 → L1 失效广播实现 |
| 分布式锁 | setNX 方案缺陷 → Redisson 如何解决 → 锁续期（Watch Dog）原理 |
| 限流 | 算法对比 → 滑动窗口 Lua 原子性 → 生产配置经验 |
| 可观测性 | 三支柱（指标/日志/链路）→ traceId 传播机制 → Grafana 面板搭建 |
| Spring AI + RAG | RAG vs Fine-tuning 选型 → 分块策略 → Prompt 工程 → 幻觉处理 |
| 混合搜索 | BM25 局限性 → 语义搜索原理 → RRF 融合算法 |
| Testcontainers | 为什么不 mock DB → 容器生命周期管理 → CI 集成方案 |
