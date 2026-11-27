# Phase5-私人内容可见性统一总结

日期：2026-10-18。分支：`codex/blog-quality-daily`。接续：`bef8aa1` 公开读取撤回状态校验。状态：本切片已完成本地实现和隔离验证；BD-001与Phase5整体仍进行中，未部署。

## 业务触发与结果

文章同时带有普通标签和私人标签时，原首页按“存在一个普通标签”放行；MySQL搜索与归档又只排除分类60/61，详情缓存和ES出口缺少私人规则。指定账号曾读过的缓存详情还可能包含私人上下篇。以上为源码可复现的规则差异，未认定线上已发生泄露。

本轮以现有文章限制条件的并集收紧：分类60/61，或关联任一“红红专属~”“猫猫出没”“w&p~”标签，即视为私人文章标记。匿名和普通账号不能读取这些文章；原有两个指定邮箱账号可读取，但仍要求PUBLIC且未删除。没有扩展成角色、租户或权限管理平台。

覆盖的文章返回入口为：首页、缓存详情、MySQL/ES共同搜索出口、归档数量与列表、推荐、上下篇、分类/标签下文章列表。数据库当前分类/标签关系决定是否允许返回；缓存或旧索引只提供正文/候选。混合普通标签不能抵消私人标签，指定账号也不能越过SECRET/DRAFT/删除状态。

## 设计与取舍

- ArticleAccessService每次从Sa-Token登录ID查询当前邮箱，复用既有两个账号常量；匿名不查用户，缺失/空邮箱或查询失败按普通权限处理。文档不记录邮箱值，不能用前端传入的布尔值取得资格。
- ArticleMapper.xml复用excludePrivateArticles片段，NOT EXISTS检查文章的全部私人标签。批量selectVisibleArticleIds只返回候选范围内的允许ID，空/null集合恒假；状态SQL失败仍传播失败，不降级输出旧内容。category_id为null不命中私人分类标记，依然执行私人标签和发布状态检查。
- 首页在既有普通SQL中改为排除私人条件；归档数量与列表使用相同谓词，推荐和导航在LIMIT前过滤。分类/标签列表先分页文章，再连接完整标签，避免同一文章多个或重复标签关联挤占页容量；保留原有必须存在有效分类/标签关联的要求。没有顺带改首页全量查询后的内存分页。
- 详情共享缓存继续保存与读者无关的文章；校验后浅复制独立ArticleVO，在当前响应中覆盖上下篇和统计，不把读者导航写回共享缓存。即使命中旧缓存里残留的私人导航也会重新查询替换；嵌套分类/标签在本链路只读。
- 账号与文章SQL都依赖数据库当前快照，没有新增状态缓存、行锁、长事务、中间件、数据库迁移或配置项。代价是登录请求多一次账号查询，普通可见性查询需要检查标签关联；没有性能提升或容量承诺。

长期设计见[读取校验设计](../01-architecture/02-公开文章撤回读取校验设计.md)，实施范围见[Phase5计划](../02-plans/04-Phase5-内容变更一致性与可靠发布计划.md)。

## 改变的文件

源码路径相对Git根 `D:/IdeaProjects/blog`。

| 文件 | 职责与变化 |
| --- | --- |
| `blog-springboot/src/main/java/com/ican/service/ArticleAccessService.java` | 提取本轮文章读取入口使用的指定账号确认，异常收紧权限 |
| `blog-springboot/src/main/java/com/ican/mapper/ArticleMapper.java` | 当前读者可见ID、归档数量和各列表/导航的privateAccess参数 |
| `blog-springboot/src/main/resources/mapper/ArticleMapper.xml` | 共用私人排除谓词、混合标签修复、分页/数量及导航筛选 |
| `blog-springboot/src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | 首页/详情/归档/推荐接入，详情复制响应并隔离导航 |
| `blog-springboot/src/main/java/com/ican/service/impl/CategoryServiceImpl.java` | 分类下文章使用当前读者资格，目录元数据逻辑保留 |
| `blog-springboot/src/main/java/com/ican/service/impl/TagServiceImpl.java` | 标签下文章使用当前读者资格，目录元数据逻辑保留 |
| `blog-springboot/src/main/java/com/ican/strategy/context/SearchStrategyContext.java` | 两种搜索共同按当前账号及文章关系批量过滤 |
| `blog-springboot/src/main/java/com/ican/strategy/impl/MysqlSearchStrategyImpl.java` | 移除策略内旧分类/账号判断，权限收口到共同出口，原高亮实现未改 |
| `blog-springboot/src/test/java/com/ican/service/ArticleAccessServiceTest.java` | 匿名/普通/两个指定账号、缺失账号及查询失败 |
| `blog-springboot/src/test/java/com/ican/service/impl/ArticleServiceImplTest.java` | 旧缓存撤回/恢复、资格变更、交错请求导航隔离及归档契约 |
| `blog-springboot/src/test/java/com/ican/strategy/context/SearchStrategyContextTest.java` | 两种搜索的资格收回/恢复与原状态失败回归 |

文档更新原路线、台账、阶段计划、读取校验设计和docs/README，新增本总结。用户已有ArticleController.java修改及未跟踪homa-xxl-job保留，未暂存；前端和其他项目没有改动。

## 实际验证

1. 后端编译与定向单测：使用IDE自带Maven及JBR21（项目目标Java17）执行：

   ```text
   mvn -q -Dtest=ArticleAccessServiceTest,ArticleServiceImplTest,SearchStrategyContextTest,EsSearchStrategyImplTest,HotArticleWarmupServiceImplTest test
   ```

   最终26项通过，0失败/错误/跳过。账号6项、详情与归档8项、两种搜索8项、ES失败/空结果2项、既有热点预热2项。构建阶段完成主源码/测试源码编译，没有额外打发布包。Mapper、缓存、Redis、ES均使用隔离桩，不是实际服务联调。
2. 权限与恢复：匿名/普通/两个指定账号、空邮箱/缺失用户/账号查询失败；每次缓存命中与搜索重新确认资格，指定→普通→指定的收回/恢复使用同一旧副本。详情拒绝时不计浏览量；文章状态数据库失败不能返回缓存。用受控重入调用确定性交错两次详情返回，分别保持各自导航，原共享缓存对象不被修改；这不是多实例或真实并发压力测试。
3. SQL：实际MyBatis XMLMapperBuilder解析当前ArticleMapper.xml，生成44组绑定SQL，并将同一SQL及参数在内存SQLite执行。覆盖两类账号、三个私人标签、两个私人分类、普通/私人混合及重复标签、SECRET/DRAFT/软删/null状态、空/null候选、首页普通/私人标签和日期筛选、归档数量/页窗口、推荐、上下篇、分类/标签条件与第二页。额外4次当前标签/分类加入限制及解除限制的状态转换检查通过。真实数据与业务库未访问；SQLite不能替代MySQL方言、执行计划、事务隔离和复制延迟验收。
4. SQL辅助程序与报告保存在忽略的target目录：`RenderPrivateVisibility.java`、`verify_private_visibility.py`、`phase5-private-sql-verification.json`；未引入正式测试依赖或测试平台。首轮账号单测暴露隔离环境未初始化MyBatis-Plus的User lambda元数据，已仅在测试夹具初始化并校验实际Mapper调用；扩展SQL辅助程序时出现可空三目表达式自动拆箱错误，也已仅修正辅助程序。最终验证均通过。

没有跑全仓测试、前端构建、浏览器、真实MySQL/MQ/ES、多实例或付费模型；没有启停服务、部署或改真实文章。

## 承诺、未验证与接续

文章撤回/私人关系变更已提交且在本次可见性SQL快照中可见时，旧缓存/索引不能单独决定放行。账号资格以账号SQL读取时点为准，账号查询与文章SQL不是一个原子快照；查询后才收回资格或撤回文章的在途响应不属于零延迟保证。归档数量和列表条件一致，但两个SQL之间的并发变更仍可能暂时改变数量。

仍公开文章的旧正文、旧标签显示与旧AI摘要、解除私人标记后读到更早缓存版本、搜索索引写入乱序、数据库复制延迟和已经传出的内容仍未解决。本轮不把资格确认当成内容版本保护。

分类目录按名称隐藏“宝宝~”“不开心”、标签/分类目录聚合计数、评论、热度榜管理输出及独立AI检索等没有统一验收；分类和标签的名称也仍可能由原目录接口返回。不能将“列出的文章返回入口已收紧”表述为“全站私人信息已完全隔离”。硬编码分类ID与按标签名称识别延续原项目规则，没有虚构数据库迁移完成。

下一轮优先补文章详情null/受限结果的必要页面承接，定向核实剩余公开派生入口；再接续BD-002/003/005的事务投递、执行代/旧版本回写与缓存并发保护。真实服务验证须独立记录，保持BD-001和Phase5部分完成。

本地提交标题：`fix: 统一私人文章读取规则并隔离缓存导航`。本总结与代码同一普通本地提交，可按标题定位；不合并、推送或部署。
