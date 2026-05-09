# Phase2 文章热度分与热点缓存预热总结

## 阶段目标

本阶段不是重写整个 Redis，而是在现有多级缓存体系上补齐文章主链路的热度分、热榜、热点预热、管理接口和指标。

核心目标是把“文章热度”从单一浏览量升级为可解释的综合分数，并让热点文章缓存预热变成可手动、可定时、可观测的能力。

## 升级内容

1. 统一了收藏表字段语义，把 `UserFavorite` 的 `doc_id` 纠正为 `favorite_id`，并同步修正读写 SQL。
2. 增加了文章热度分配置类和默认权重，支持浏览、点赞、评论、收藏、推荐、置顶和时间衰减。
3. 增加了 Redis ZSet 热榜封装，支持精确写入热度分和保持排序结果顺序。
4. 增加了文章热度分服务，负责聚合文章基础数据并刷新热榜。
5. 增加了热点文章预热服务，负责从热榜批量预热 `article:{id}` 详情缓存。
6. 增加了缓存管理接口和定时任务入口，方便手动刷新和定时调度。
7. 增加了热度分、预热和接口层的测试，确认不依赖真实 `.env`、Redis 或数据库。

## 修改文件清单

| 文件 | 做了什么 |
|---|---|
| `src/main/java/com/ican/entity/UserFavorite.java` | 保持实体字段与 `favorite_id` 语义一致 |
| `src/main/java/com/ican/mapper/UserFavoriteMapper.java` | 改为 `Integer favoriteId`，补充文章收藏统计查询 |
| `src/main/resources/mapper/UserFavoriteMapper.xml` | 全量替换 `doc_id`，增加文章收藏数聚合 SQL |
| `src/main/java/com/ican/mapper/DocMapper.java` | 收藏写入/删除 SQL 改为 `favorite_id` |
| `src/main/java/com/ican/service/impl/DocServiceImpl.java` | 直接返回 `List<Integer>` 收藏 ID，去掉字符串转整数 |
| `src/main/java/com/ican/model/vo/ArticleFavoriteCountVO.java` | 文章收藏统计 VO |
| `src/main/java/com/ican/cache/ArticleHotScoreProperties.java` | 文章热度分配置和默认权重 |
| `src/main/java/com/ican/constant/RedisConstant.java` | 新增热榜、预热锁、文章详情前缀常量 |
| `src/main/resources/application.yml` | 增加 `article.hot-score` 配置段 |
| `src/main/java/com/ican/model/vo/ArticleHotScoreSourceVO.java` | 热度分来源数据 VO |
| `src/main/java/com/ican/model/vo/ArticleHotScoreVO.java` | 热榜返回 VO |
| `src/main/java/com/ican/model/vo/ArticleHotScoreRefreshResultVO.java` | 刷新结果 VO |
| `src/main/java/com/ican/model/vo/HotArticleWarmupResultVO.java` | 预热结果 VO |
| `src/main/java/com/ican/service/RedisService.java` | 增加精确写入 ZSet score 的契约 |
| `src/main/java/com/ican/service/impl/RedisServiceImpl.java` | 实现 `setZsetScore`，保持 ZSet 返回顺序 |
| `src/main/java/com/ican/mapper/ArticleMapper.java` | 增加热度分来源查询 |
| `src/main/resources/mapper/ArticleMapper.xml` | 增加热度分来源 SQL |
| `src/main/java/com/ican/mapper/CommentMapper.java` | 增加文章评论数统计查询 |
| `src/main/resources/mapper/CommentMapper.xml` | 增加文章评论数统计 SQL |
| `src/main/java/com/ican/service/ArticleHotScoreService.java` | 热度分服务契约 |
| `src/main/java/com/ican/service/impl/ArticleHotScoreServiceImpl.java` | 计算并刷新 Redis 热榜 |
| `src/main/java/com/ican/service/HotArticleWarmupService.java` | 热点预热服务契约 |
| `src/main/java/com/ican/service/impl/HotArticleWarmupServiceImpl.java` | 从热榜预热文章详情缓存 |
| `src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | 把首页候选预热委托给预热服务 |
| `src/main/java/com/ican/controller/CacheController.java` | 增加热榜刷新、热榜查询、热点预热接口 |
| `src/main/java/com/ican/quartz/task/TimedTask.java` | 增加热榜刷新和预热任务入口 |
| `src/main/java/com/ican/metrics/BlogMetrics.java` | 增加热度分和预热指标 |
| `src/test/java/com/ican/mapper/UserFavoriteMapperContractTest.java` | 防止 `doc_id` 回流 |
| `src/test/java/com/ican/cache/ArticleHotScorePropertiesTest.java` | 验证热度分默认配置 |
| `src/test/java/com/ican/service/impl/RedisServiceImplTest.java` | 验证 ZSet 封装 |
| `src/test/java/com/ican/service/impl/ArticleHotScoreServiceImplTest.java` | 验证热度分计算和刷新 |
| `src/test/java/com/ican/service/impl/HotArticleWarmupServiceImplTest.java` | 验证热点预热逻辑 |
| `src/test/java/com/ican/controller/CacheControllerTest.java` | 验证管理接口 |
| `src/test/java/com/ican/quartz/task/TimedTaskTest.java` | 验证定时任务委托 |
| `src/test/java/com/ican/metrics/BlogMetricsTest.java` | 验证指标埋点 |
| `src/test/java/com/ican/service/impl/ArticleServiceImplTest.java` | 验证文章详情缓存相关构造变更 |

## 缓存体系变化

这阶段保留了现有 `MultiLevelCacheManager` 作为文章详情缓存基础设施，没有把全站 Redis 重构掉。

新的变化是把文章热度这条链路单独拆出来：

- 热度分用 Redis ZSet 维护。
- 热榜只负责排序，不直接承担详情缓存。
- 热点预热继续写入 `article:{id}`，仍然走多级缓存管理器。
- 管理接口和定时任务只做调度，不侵入业务查询逻辑。

这样缓存体系还是原来的多级缓存底座，但多了一条可解释、可治理的热点链路。

## 热度分模型

当前热度分公式如下：

```text
hotScore = viewCount * viewWeight
         + likeCount * likeWeight
         + commentCount * commentWeight
         + favoriteCount * favoriteWeight
         + recommendWeight
         + topWeight
         - timeDecay
```

默认权重：

| 信号 | 默认值 |
|---|---:|
| 浏览量 | 1.0 |
| 点赞量 | 5.0 |
| 评论量 | 8.0 |
| 收藏量 | 10.0 |
| 推荐 | 30.0 |
| 置顶 | 50.0 |
| 时间衰减 | 1.5 / 天 |

实现里会从文章表、评论表、收藏表和 Redis 统计中聚合这些信号，再把最终分数写入 `blog:article:hot_score`。

## 热点缓存预热流程

预热链路是：

1. 读取热榜 Top N。
2. 抽取文章 ID 去重。
3. 通过 `MultiLevelCacheManager.get()` 预热 `article:{id}`。
4. 统计 warmed、skipped、failed 和耗时。
5. 通过 `BlogMetrics` 记录指标。

预热入口有三处：

- 首页逻辑的候选预热委托。
- `CacheController` 手动触发。
- `TimedTask` 定时触发。

## 测试结果

验证命令：

```powershell
mvn test
```

结果：

```text
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

另外还跑过一组聚焦测试，用于确认热榜、预热、管理接口和定时任务：

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试环境没有依赖真实 `.env`、真实 Redis 或真实数据库连接，控制器测试通过 mock 和测试上下文隔离完成。

## Git 记录

阶段内主要提交如下：

```text
4f84aba fix: 修正收藏表字段映射
7da8f66 feat: 增加文章热度分配置
4a7f8e4 feat: 增加热榜ZSet操作
62bab3a feat: 增加文章热度分计算
46dda0f feat: 增加热点文章缓存预热和管理接口
```

阶段前置文档提交：

```text
56c34cc docs: 增加缓存体系分类和改造方案
201dd4a docs: 增加第二阶段实施计划
```

## 后续建议

1. 如果要继续做第三阶段，可以把缓存治理扩大到更多统计链路，但不要一次性重构所有 Redis key。
2. 可以考虑给热榜和预热补更细的失败分类日志，方便排查数据源缺失。
3. 如果后面要做 CI 集成，建议再补一层真实 Redis 的集成测试。
