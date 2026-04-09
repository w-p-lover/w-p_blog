# Phase 2 优化要点（缓存 + 锁 + 限流）

## 1) 目标
- 统一文章详情缓存读写路径，减少重复逻辑。
- 解决缓存穿透/击穿/雪崩的基础问题。
- 为文章详情接口增加可配置限流保护。

## 2) 核心改动

### 缓存架构
- 新增 `MultiLevelCacheManager`：
  - L1：Caffeine 本地缓存
  - L2：Redis
  - BloomFilter：辅助防穿透
  - Redisson Lock：防击穿
  - 随机 TTL：缓解同一时刻大量过期
- 空值缓存使用统一哨兵值，避免字符串冲突。

### 接口限流
- 新增 `@RateLimit` 注解。
- 新增 `RateLimitAspect`，通过 Redis Lua 脚本做滑动窗口限流。
- 新增脚本：`src/main/resources/lua/rate_limit.lua`。

### 业务接入
- `ArticleServiceImpl` 详情查询改为走 `cacheManager.get(...)`。
- 文章写操作（删/改/置顶/推荐）统一执行缓存失效 `cacheManager.evict(...)`。
- 热点文章预热逻辑统一接入多级缓存管理器。

### 配置与依赖
- `pom.xml` 增加：Redisson、Caffeine。
- `application.yml` Redis 配置迁移到 `spring.data.redis.*`。
- 新增 `RedissonConfig`。
- 新增 `CacheController`（统计、清理、按 key 删除）。

## 3) 分批提交建议（本次已按此拆分）
1. 基础设施与配置（依赖、Redis 配置、Redisson 配置）。
2. 缓存与限流基础组件（缓存管理器、注解/切面/Lua、缓存管理接口）。
3. 业务层接入（ArticleServiceImpl 改造）。
4. 文档补充（本文件）。
