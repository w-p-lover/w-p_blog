# Phase5-公开读取撤回保护总结

日期：2026-10-17。分支：`codex/blog-quality-daily`。接续：`1107e60` 搜索同步与故障恢复。状态：BD-001 的详情/搜索发布与删除状态屏障已完成本地实现和隔离验证，Phase5整体进行中，未部署。

## 业务结果

作者撤回文章后，缓存/ES同步尚未完成时，公开详情和搜索出口会查询数据库当前发布/删除状态。已不再公开或已删除的候选不返回正文、标题和摘要；详情被拒绝时不增加浏览量。数据库状态不可确认则请求失败，不输出派生副本。

ES故障现在明确返回现有业务失败，真实无结果仍是空列表。发现搜索弹窗原先直接把响应data赋给数组、失败时null会导致 `.length` 报错，因此仅配套补了失败与重试承接、清除旧结果及过期响应保护。

“撤回”在此仅指 PUBLIC切换为SECRET/DRAFT、软删或物理删除。特殊账号、私人分类和私人标签规则保持原样，其跨入口差异仍未收口；不能把本轮当作完整权限系统完成。

## 文件与设计

源码路径相对Git根 `D:/IdeaProjects/blog`。

| 文件 | 改动 |
| --- | --- |
| `blog-springboot/src/main/java/com/ican/mapper/ArticleMapper.java` | 新增批量查询当前公开主键的方法 |
| `blog-springboot/src/main/resources/mapper/ArticleMapper.xml` | 按候选ID范围与PUBLIC/未删查询，空/null集合恒假，不取正文 |
| `blog-springboot/src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | 缓存/回源取得正文后、计数与输出前校验当前状态 |
| `blog-springboot/src/main/java/com/ican/strategy/context/SearchStrategyContext.java` | MySQL/ES共同出口一次批量校验，保持允许结果排序、高亮 |
| `blog-springboot/src/main/java/com/ican/strategy/impl/EsSearchStrategyImpl.java` | 检索失败传播业务错误，不再返回伪空结果 |
| `shoka-blog/src/components/Dialog/Search.vue` | 失败响应不写null列表，等待/错误/无结果分开，支持重试并丢弃旧响应 |
| `blog-springboot/src/test/java/com/ican/service/impl/ArticleServiceImplTest.java` | 详情撤回、回源期间撤回、恢复和数据库失败；释放测试执行器 |
| `blog-springboot/src/test/java/com/ican/strategy/context/SearchStrategyContextTest.java` | 两种策略共同出口的批量过滤、排序与状态失败 |
| `blog-springboot/src/test/java/com/ican/strategy/impl/EsSearchStrategyImplTest.java` | ES错误与真实无匹配的业务结果区分 |

维护原路线、台账、阶段计划和文档导航，新增[读取校验设计](../01-architecture/02-公开文章撤回读取校验设计.md)。原恢复文档补充读取屏障的边界；没有数据库迁移、新中间件、配置项或新业务模块。

## 实际验证

1. 后端聚焦测试：

   ```text
   mvn -q -Dtest=ArticleServiceImplTest,SearchStrategyContextTest,EsSearchStrategyImplTest,ElasticsearchServiceImplTest test
   ```

   20项通过，0失败/错误/跳过：详情5项、共同搜索出口6项、ES检索2项、上轮同步服务7项。Maven同时完成本次源码编译。数据库mapper和ES transport使用隔离桩，没有调用真实业务库或ES。
2. 新mapper XML经过实际MyBatis解析并生成绑定SQL；把同一SQL/参数在内存SQLite执行，公开未删、SECRET、DRAFT、软删、状态或删除标记null、物理不存在、空/null候选、撤回与恢复发布均符合预期。没有连接真实MySQL，不能替代其隔离级别/复制延迟验证。
3. 使用现有Vue编译器/esbuild和真实Vue renderer执行实际 Search.vue setup，受控API响应覆盖失败/null、重试成功、网络失败、迟到响应和清空查询失效。脚本在忽略的target目录，仅用于本次验证，不增加测试依赖或正式项目抽象。
4. `shoka-blog` 的 `npm run build` 通过。原有大图片/大chunk与CSS nesting告警保留；构建生成的无关 `components.d.ts` 变化已恢复。未执行全量测试、压测、真实浏览器或后端联调。

本轮SQL检查辅助程序第一次启动遇到Windows命令行长度限制，改为Java参数文件后执行通过；没有因此改变业务代码。原有服务未启停，没有写真实文章或调用付费模型。

## 边界与下一步

- 读取保护增加详情一次主键状态SQL、非空搜索一次批量SQL。数据库成为公开放行必要依赖，数据库不可用时拒绝返回旧内容；没有宣称性能改善。
- 承诺以状态SQL快照为界，未覆盖SQL之后发生的撤回、主从复制延迟、已传输/显示内容。仍公开文章的旧正文/AI摘要、新旧版本并发写入ES和旧缓存回填尚未解决。
- MySQL搜索/归档按私人分类过滤、首页按私人标签过滤、详情/ES缺少统一私人规则，这是本轮核实的既有差异。下一轮优先定向统一业务规则与相关公开入口；目前BD-001仍是部分完成。
- 详情沿用现有null未找到契约，现有文章页面缺少完整未找到/撤回提示，本轮没有扩为页面重做；这应作为后续可见性业务验收所需的承接事项。
- 真实MySQL/MQ/ES、浏览器和多实例业务验收仍未进行。搜索结果过滤后可能少于原候选数量，不自动补取；历史漏同步修复保持上轮边界。

提交标题：`fix: 为公开详情与搜索增加撤回状态校验`，对应本总结与本轮实现，可在当前分支按标题定位。用户已有ArticleController修改和未跟踪homa-xxl-job仍保留，不合并、推送或部署。
