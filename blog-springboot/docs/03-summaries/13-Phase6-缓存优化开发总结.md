# Phase6-缓存优化开发总结

最新接续2026-10-24：C5补齐现有缓存读/重建/失效的分段故障计量，删除本地被Git排除的Redis启动改配置钩子及遗留class；27项相关隔离验证通过，见第11节。本地缓存阶段范围收口，进入现有队列可靠性；持久版本迁移、真实依赖/多进程及运维有效配置仍未验收，不是生产缓存阶段全部完成。

历史接续2026-10-23：按用户要求集中交付C4热点读取批次，包含Bloom移除、详情导航合并、PV累计快照同步、临时负缓存恢复、热榜刷新代/原子发布和首页预热合并。7类54个不同用例分批通过，实际Lua受控执行通过，见第10节和[领域设计](../04-cache/07-热点统计与热榜快照设计.md)。缓存阶段仍未做真实迁移/服务验收，下一批集中收口运行配置边界及回源故障观测，然后按阶段验收进入队列。

持续维护的缓存阶段开发复盘，首轮日期2026-10-19；分支 `codex/blog-quality-daily`，改前基线 `45a5c09`。当前状态（2026-10-24）：P6-01/C1—C5本地代码与受控验证范围收口；迁移/统一切换/真实依赖的部署验收待补，下一阶段P6-02现有队列。用户最新顺序为缓存→队列→新功能，不提前进入报名。

历史接续2026-10-22：C3b拆开正文缓存与关联快照，返回前一次SQL取得当前可见版本/分类/标签，不做改名扇出更新；当前35个相关用例通过（分批执行），见第9节及[设计第9节](../04-cache/06-文章详情持久版本与失效故障隔离设计.md)。迁移、真实依赖与多进程仍待验收，缓存阶段未完成；下一入口C4a去除无阻断作用的Bloom热路径依赖。

历史接续2026-10-21：C3a持久正文版本与失效故障隔离完成本地实现/29项相关隔离验证，迁移文件未执行、独立关联写入未覆盖，详见第8节及[领域设计](../04-cache/06-文章详情持久版本与失效故障隔离设计.md)。下一入口C3b关联快照，缓存阶段仍进行中。

历史接续2026-10-20：C2提交后失效与旧回填保护已完成本地实现/隔离验证，详见本文第7节及[领域设计](../04-cache/05-文章缓存提交后失效与旧回填保护设计.md)。以下1—6节保留C1交付事实；当前仍不是完整缓存一致性或业务版本闭环。独立homa-xxl-job已由9277d80在blog根忽略，后续不触碰该项目。

## 1. 业务问题与承诺

热门文章集中访问时，原限流按文章共享额度且同毫秒请求合并计数；缓存未命中等锁失败会直接查库。目标是在保持现有撤回/私人内容校验的前提下，让访问者额度与请求计量正确，重建竞争和依赖失败不放大备用数据库装载，并让故障之后请求能恢复。

本轮只承诺：调用方正确产生独立成员时同毫秒请求分别计量；详情按认证用户/匿名连接IP区分；每实例cache.get重建区域受配置并发预算限制；锁超时/中断不执行备用loader。正文命中后SQL/统计开销、多实例全局预算、旧回填与权限快照之后的在途变化不在新增保证内。

## 2. 设计取舍与实现

- 限流保留已有滑动窗口，用UUID独立成员、Redis TIME统一同key窗口；StringRedisTemplate按普通字符串传参。没有增加新限流框架或改变所有接口作用域。
- `perClient`默认false，仅文章详情开启；登录用户按认证ID，匿名按remoteAddr。保留原10次/60秒作为每访问者每文章额度，未做容量校准；NAT/代理可能共享匿名额度，不能直接信任请求转发头来绕过。
- 只接受Redis明确返回1；null/数据访问失败拒绝，不在依赖故障时无保护放行。沿用ServiceException与Result失败响应，不全面改造HTTP状态契约。
- cache.get在L2缺失后立即申请本实例Semaphore，预算覆盖等锁和装载，默认8、可配置。L1/L2命中不占预算，预热共享预算。没有新增等待队列/线程池/全局分布式信号量。
- 锁超时最后检查一次L2，仍无结果则繁忙；中断保留标记并拒绝。正常、异常和中断均释放预算；仅持锁线程尝试unlock。没有把这一步当作内容版本fencing。

完整业务时序、失败结果和设计边界见[领域设计](../04-cache/04-热点读取限流与有界回源设计.md)。不采用“失败全部查库”是为避免热点反向放大；不采用本轮删除可见性SQL，是因为那会破坏已经交付的撤回保护。

## 3. 本轮文件与职责

| 文件 | 实际改动 |
| --- | --- |
| `src/main/resources/lua/rate_limit.lua` | Redis TIME作为score时间、独立请求成员，保持单脚本窗口处理 |
| `src/main/java/com/ican/aspect/RateLimitAspect.java` | 字符串脚本参数、UUID、访问者后缀、未知/失败拒绝 |
| `src/main/java/com/ican/annotation/RateLimit.java` | 增加必要的perClient作用域开关，默认保持原行为 |
| `src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | 仅详情注解开启perClient，不改撤回校验/导航/统计 |
| `src/main/java/com/ican/cache/MultiLevelCacheManager.java` | 重建Semaphore、锁超时再读L2/拒绝、中断拒绝、预算释放与持锁检查 |
| `src/main/java/com/ican/cache/MultiLevelCacheProperties.java`、`src/main/resources/application.yml` | 增加单实例重建并发参数及环境入口，默认8，启动校验正数 |
| `src/test/java/com/ican/aspect/RateLimitAspectTest.java` | 访问者、独立参数、拒绝/恢复和默认资源级作用域5项回归 |
| `src/test/java/com/ican/cache/MultiLevelCacheManagerTest.java` | 真线程屏障、锁/Redis/loader故障和恢复、负缓存6项回归 |
| `docs/04-cache/04-热点读取限流与有界回源设计.md` | 不变量、调用链、方案比较、参数和证据边界 |
| `docs/00-overview/06-日常质量开发约定.md`、`07-真实业务场景与深度升级路线.md` | v4顺序与完整复盘规则、当前切片与下一入口 |
| `docs/02-plans/05-Phase6-热点读取与限量报名计划.md` | 缓存→现有队列→报名，拆缓存C1—C4并明确阶段验收门槛 |
| `docs/04-cache/03-缓存与后端并发能力现状审查.md`、`docs/02-plans/04-Phase5-内容变更一致性与可靠发布计划.md`、`docs/03-summaries/12-后端并发能力审查与路线调整总结.md` | 审查进展和历史v3顺序的接续说明 |
| `docs/README.md`与本总结 | 领域/复盘入口及实际交付记录 |

原ArticleController.java修改与未跟踪homa-xxl-job未覆盖、暂存或清理；其他参考项目未改。定时任务通过应用工具更新，保留原任务ID、当前聊天、ACTIVE状态与每日北京时间10:00，不创建重复任务；不作为仓库文件提交。

## 4. 实际验证

命令（IDE自带Maven、JBR21、编译目标17）：

```text
mvn -q -Dtest=RateLimitAspectTest,MultiLevelCacheManagerTest,ArticleServiceImplTest,HotArticleWarmupServiceImplTest test
```

21项通过，0失败/错误/跳过。新限流5项、新缓存6项；既有详情8项和预热2项验证撤回校验、缓存对象隔离和预热失败承接继续成立。Mockito/受控RLock/Redis替身不证明真实中间件行为；并发预算使用实际线程与CountDownLatch屏障验证，不以固定sleep制造时序。

限流用例随后改为直接读取真实ArticleServiceImpl详情注解，避免仅验证测试中复制的作用域配置；只重跑 `mvn -q -Dtest=RateLimitAspectTest test`，5项通过，其余未变化用例不重复运行。

实际Lua通过Lupa执行源码，固定Redis时间与集合命令替身：复现原脚本同毫秒11次全放行、集合仅1成员；新脚本10次允许/第11次拒绝，独立key、窗口边界、过期与恢复通过。临时脚本/依赖仅在忽略target，未增加应用依赖。该证据不是真实Redis联调。

另完成本轮文件编码/换行核对、`git diff --check`、文档相对链接检查。Maven test同时编译主/测试源码，没有另做发布包，未运行全量测试。没有启动完整服务、连接真实MySQL/Redis/MQ/ES、做浏览器/多实例/压测、使用真实业务数据或付费模型，未推送/部署/合并。

## 5. 残留风险与阶段状态

| 缺口 | 本轮结论/接续 |
| --- | --- |
| 提交前失效、旧查询晚回填、固定租期失效 | C2下一入口；持锁检查只避免直接误解锁，不防止旧正文覆盖 |
| Pub/Sub遗漏、实例断线、Redis故障时的正文/权限/统计契约 | C3；本轮不建立持久广播或所有依赖的降级 |
| Bloom热路径额外操作但不拦截 | C4定向去除/改造，先明确存在集合维护价值，不称已解决穿透 |
| 每次正文命中仍有三次文章SQL和逐次异步SQL写入 | C4分别定义导航与统计时效、计数损失/去重预算后处理 |
| 热榜先删后逐条写、预热不是强刷新 | C4；不重新建设已有热榜 |
| 限流/缓存预算参数与监控 | 当前仅默认值和受控证据，未按真实负载校准；后续补必要回源/锁等待/拒绝观测 |
| 匿名共享IP、滚动版本混跑、真实依赖行为 | 保持未验证；旧应用实例仍执行旧Lua，必须在未来部署验收中确认切换 |

不能称为缓存优化阶段全部完成、全局数据库并发固定为8、完整Redis容灾或高并发生产实践。既有AI资源级额度保留；其任务可靠性留到队列阶段。

## 6. 开发复盘与下一步

本轮复盘日期2026-10-19，下次周复盘不晚于2026-10-26；切片完成或发现重大新缺口时提前复盘。依据本轮21项定向结果与真实Lua受控执行，C1可作为本地代码切片交付，但尚无性能提升数字。记录决定：

- 继续：C2围绕文章写事务→evict→loader回填，定义正文版本和当前权限的不同承诺，用并发屏障验证提交与旧回填，不只移动afterCommit。
- 接续：C3多实例/依赖故障，C4成本与热榜/预热/观测；每项以实际业务规则和实现验证收口，必要扩展可写明理由推迟。
- 推迟：所有现有队列代码优化到P6-02，报名到P6-03；本轮不提前搭报名表或端点。
- 不采用：全仓反复审计、无限回源/重试、为了简历数字增加平台或编造吞吐。

后续每个缓存切片更新本总结的设计取舍、文件职责、验证、风险与接续，不每日新建重复报告。阶段结束单列验收范围/证据和遗留项，再进入队列。

本地普通提交标题：`fix: 修正文章限流并限制缓存重建回源`。本总结与上述代码同一提交，可按标题定位；只有本轮文件进入提交。

## 7. C2提交后失效与旧回填保护复盘（2026-10-20）

### 7.1 业务触发、改前缺口与保证

作者编辑/撤回/删除时，原事务提交前删除缓存可能被旧DB值回填；即使改到提交后，慢loader仍可能在失效之后写旧值。旧L2读取还可能晚于通知重新安装L1。仅移动afterCommit或检查unlock没有覆盖这些并发窗口。

本次保护边界：通过manager登记的失效只在数据库成功提交后执行，回滚不切代；成功的Redis失效脚本切代并删正文后，之前观察旧代的loader不能跨代写L2；本实例已观察到失效的读取不能迟到安装L1。创建也失效，避免先前空值缓存隐藏新文章。详情原有当前数据库可见性屏障不变。

前提是Redis失效代保留、全部相关正文写入使用该协议、loader读取当前提交数据。UUID代是缓存栅栏，不是业务内容版本；广播遗漏、失效失败/提交后进程退出、陈旧DB事务快照或副本、旧代码混跑、Redis元数据丢失不受整体保证。

### 7.2 采用方案与取舍

1. 在manager集中识别Spring实际事务，同manager/同事务合并key并去重，成功commit后执行。单个key失败继续尝试其他key，最后明确返回“内容已提交，但缓存同步失败”；不假装能回滚已提交的数据库，不提示重复业务写入。
2. Redis原子切换每key UUID失效代并删除正文；loader先读代，再装载，Lua比较代与SET正文/TTL原子执行。未采用先GET再SET或仅延迟双删，因为仍有竞争窗口；未让写操作等待正文装载锁。
3. 保留失效代而不设TTL，避免过期/重置导致ABA；代数随曾失效key增加，清理要有停止在途装载的协议，Redis故障/淘汰恢复列入C3，不临时搭平台。
4. 本地全局epoch与短内存同步区保护L1安装，失效脚本前后均推进。无需每个随机key建永久对象表，但其他文章失效也可能保守拒绝在途填充；该可用性成本没有负载结论。
5. 复用原RedisTemplate值序列化，正文与负缓存TTL不变。当前SingleServer支持两key脚本，未为Redis Cluster引入跨槽key迁移。不增加数据库字段，不声称已完成派生结果版本保护。

### 7.3 文件改动与职责

| 文件 | 本切片内容 |
| --- | --- |
| `src/main/java/com/ican/cache/MultiLevelCacheManager.java` | 事务合并回调、成功提交后失效、切代脚本、回填CAS、本地epoch与失败结果；保留C1预算 |
| `src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | 创建文章也登记提交后失效，覆盖既有负缓存；不改消息发送/任务行为 |
| `src/main/resources/lua/cache_advance_and_evict.lua` | UUID切代与正文删除原子执行，正文缺失仍切代 |
| `src/main/resources/lua/cache_fill_if_current.lua` | 按装载前代比较后才写正文/空值与TTL，沿用实际序列化字节 |
| `src/test/java/com/ican/cache/CacheInvalidationConsistencyTest.java` | 11项事务、正常/回滚、双manager旧回填、迟到L2/通知、负缓存、失败/恢复与实际文章代理验证 |
| `src/test/java/com/ican/cache/MultiLevelCacheManagerTest.java` | 补CAS成功替身，继续验证原有6项预算与故障恢复 |
| `docs/04-cache/05-文章缓存提交后失效与旧回填保护设计.md` | 完整key/序列化协议、调用时序、取舍、失败恢复和未完成边界 |
| `docs/00-overview/07-真实业务场景与深度升级路线.md`、阶段计划05、本总结、`docs/README.md` | 更新C2实际交付、C3入口和领域导航，不新建每日报告 |

忽略target中的序列化/Lua验证程序只作本轮辅助，不提交应用依赖或临时工具。ArticleController.java原有构造器注入修改保留，未覆盖/暂存；homa-xxl-job、其他AI/PDM参考项目不动。

### 7.4 实际验证与修复过程

```text
mvn -q -Dtest=CacheInvalidationConsistencyTest,MultiLevelCacheManagerTest,ArticleServiceImplTest,HotArticleWarmupServiceImplTest test
```

IDE自带Maven、JBR21、目标Java17。初次加入真实创建代理用例时遇到隔离MyBatis缺少Category lambda元数据，按现有TableInfoHelper模式初始化后，26项通过。随后补“失效脚本期间开始的L2读取”用例，仅运行 `mvn -q -Dtest=CacheInvalidationConsistencyTest test`，11项通过；当前相关用例合计27项（11+6+8+2），0失败/错误/跳过。不重复运行未变化全量测试。

并发证据使用实际manager和Caffeine、真线程与CountDownLatch。两manager对象共享受控Redis状态，不自动投递广播：旧loader已读取old→另一manager切代并填new→旧loader继续被CAS拒绝，new保留。L2迟到、本地失效中重装、远程通知、旧负值、新建后恢复、回滚、提交后部分失效失败继续其他key、广播失败/再次失效均验证。真实ArticleServiceImpl创建/删除通过Spring注解事务代理；事务commit与Mapper为受控替身，不是MySQL提交/隔离证据。

序列化辅助程序通过实际RedisConfig生成UUID/哨兵/ArticleVO/Long参数字节并验证反序列化类型；Lupa执行实际两段Lua，验证无代首次填充、正负值跨代拒绝、连续失效、无正文也切代、重复/恢复、正文原字节与TTL。命令：IDE JBR的 `java @target/cache-c2-java.args`、`python target/check-cache-generation-lua.py`；脚本在忽略target，不引入正式依赖。Windows长classpath使用Java参数文件，未变更构建。实际Lua执行使用受控Redis命令，不是真实Redis网络/原子性/复制验收。

Maven test已编译主/测试源码，没有单独发布包、完整应用启动、真实MySQL/Redis/MQ/ES联调、浏览器、多进程或压测。未调用付费模型、修改真实业务数据、启停已有服务、推送、合并或部署。完成本轮文件差异/编码换行与文档相对链接检查。

### 7.5 残留风险、复盘决定与下一入口

| 事项 | 本次复盘决定 |
| --- | --- |
| 成功失效前后的旧装载回填、回滚与本地迟到安装 | 已交局部代码/隔离证据，正式环境验收未做 |
| commit后退出或脚本失效失败 | 仍可能保留旧缓存；有明确失败日志/结果和幂等再失效操作，没有持久待办/自动恢复 |
| 其他实例已有L1、Pub/Sub遗漏 | C3继续，不能因双manager对象测试就标多进程一致性完成 |
| Redis重启/元数据淘汰、Cluster和旧代码混跑 | 保持开放，不宣称支持；generation禁止无协议清理 |
| 业务内容版本、AI直接回写摘要、分类/标签关联写入覆盖 | 未解决，C3按需要接续权威轻量版本与写入覆盖，不用失效UUID代替业务版本 |
| Bloom/导航/逐次浏览量落库/热榜原子发布/观测 | 留C4，仍按实际问题分别设计，不顺手修改 |
| 队列、新报名 | 继续推迟，缓存阶段尚未验收完成 |

切片复盘日期2026-10-20，下一次周复盘仍不晚于2026-10-26。下一入口为C3“提交成功但失效失败/广播遗漏，公开读取如何约束旧正文时效”，定向核实详情已有权威SQL能否携带轻量版本证据，及相关写入口覆盖。未新增全仓扫描、事件平台、数据库迁移或性能数字；以这一新证据调整缓存接续，不提前进入队列。

本地普通提交标题：`fix: 提交后失效文章缓存并拒绝旧回填`；本节与实现同一提交，可按标题定位。缓存阶段保持进行中，不能将本切片记为完整正文版本或可靠失效闭环。

## 8. C3a持久正文版本与失效故障隔离复盘（2026-10-21）

### 8.1 触发、不变量与实际缺口

改前基线69a2673，开始时除用户ArticleController.java原修改外无其他未提交文件。C2解决成功失效后的旧回填，但DB提交后进程中断、Redis失效失败、远程Pub/Sub漏收时，L1/L2旧值仍可能继续命中。详情当前SQL只查可见主键，不识别正文变更。定向读实际写入链发现AI摘要使用独立SQL，不统一维护update_time；已有datetime也不足以区分所有同秒写入。

本切片不变量：创建/编辑/AI正文派生字段与持久UUID版本同写；详情按数据库已发布版本选缓存键、正文SQL固定版本，装载后按当前读者再核对可见版本。版本不同明确重读，不可见返回空，正常后才计数。成功正文变更不要求广播送达或旧键删除成功才能选择新缓存；旧loader不能覆盖新版本键。前提是所有相关写入维护标记、数据库提供当前提交数据，版本参考点为读后核对，不能保证核对后到收包之间无新变更。

### 8.2 正常、失败、恢复与并发设计

- 正常：选V→命中/固定V装载→核对同V与权限→复制响应、导航、统计；预热用同一版本键。前置选版内部允许私人文章，最终输出仍依赖当前账号/私人分类和标签约束。
- 提交后失效失败/漏广播：仍可能报“已提交但缓存同步失败”，但新的详情请求选V2而不访问旧V1；没有静默吞失效异常或自动重做文章写入。
- Redis状态丢失：从数据库当前版本装载，不信任旧generation；仍未提供Redis完全不可用时的HTTP降级、热榜/计数恢复或完整容灾。
- 旧loader暂停、编辑提交V2、新请求完成、旧loader恢复：旧值最多写V1键，读后核对拒绝旧请求，V2不被覆盖；没有无限重试。
- 选V后才编辑：正文SQL必须带V，不把新正文贴到旧键。负缓存也核对版本，新版本自然绕过旧负值。
- 权威查询失败拒绝旧内容；恢复后重新选版。撤回/私人权限收回继续受读后查询保护，不以减SQL为由删除屏障。

### 8.3 采用与未采用方案

| 方案 | 决定与理由 |
| --- | --- |
| 持久UUID+按版本键 | 采用，同语句/事务与正文绑定，比较相等即可，不要求时间精度或跨实例递增计数 |
| update_time直接当版本 | 不采用，datetime粒度和独立AI SQL无法提供完整变更证据 |
| 按次读取longtext/关联全文摘要 | 不采用，会把缓存应避免的大字段读取/计算带回命中路径 |
| 新建持久广播/Outbox平台 | 本切片不需要，已有逐次权威SQL可以先提供正文选版；消息持久投递仍在队列阶段处理 |
| 删除所有旧版本键/永久每版本generation | 不采用，旧值不再被新版本选择，先按原TTL退出；避免扫描与永久元数据 |
| 发生版本冲突自动循环查库 | 不采用，明确重读，保持C1预算与请求成本边界 |

代价明确保留：文章同步SQL从原可见性1+导航2=3次，变成前后版本2+导航2=4次；不存在ID也新增一次版本查询。登录资格、Redis统计与逐次异步浏览量写入另计。版本增多带来TTL窗口内多份正文，不给出命中率、容量或性能收益数字，后续C4以相同规则评估。

### 8.4 改动文件与职责

| 文件 | 职责 |
| --- | --- |
| `src/main/java/com/ican/entity/Article.java` | contentVersion持久字段，不加入详情VO/请求DTO，不冒充乐观锁 |
| `src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | 创建/编辑生成版本；详情数据库选版、版本键、固定版本装载、读后版本/权限核对；保留原失效 |
| `src/main/java/com/ican/mapper/ArticleMapper.java` | 轻量可见版本查询契约，正文SQL增加版本入参 |
| `src/main/resources/mapper/ArticleMapper.xml` | 复用私人规则的版本SQL，正文版本谓词，AI摘要同语句换UUID；浏览量SQL不换版本 |
| `src/main/java/com/ican/service/impl/HotArticleWarmupServiceImpl.java` | 与详情共用版本键和固定版本装载，无版本计skipped；warmed不代表完成时仍最新 |
| `src/test/java/com/ican/cache/CacheInvalidationConsistencyTest.java` | 原11项保留，新增失效失败/旧L1/Redis清空、慢旧查询、负缓存和真实重复编辑换版本4项；创建用例补版本断言 |
| `src/test/java/com/ican/service/impl/ArticleServiceImplTest.java` | 原8项适配版本/权限选版，新增负缓存装载中版本变化及初次权威失败2项 |
| `src/test/java/com/ican/service/impl/HotArticleWarmupServiceImplTest.java` | 原2项改验当前版本缓存键，保留去重/失败统计 |
| `src/test/java/com/ican/mapper/ArticleVersionSqlTest.java` | 真实XML/MP SQL绑定2项，验证权限/固定版本、正文与标记同语句、AI更新及浏览量不换版本 |
| `docs/07-database/03-文章详情持久版本标记迁移.sql` | 人工迁移脚本，加列/回填/非空，保持历史更新时间，维护窗口及回退说明；未执行 |
| `docs/04-cache/06-文章详情持久版本与失效故障隔离设计.md` | 完整协议、写入覆盖、场景、成本、迁移/验收与残留风险 |
| 缓存设计05、路线07、阶段计划05、`docs/README.md`、本总结 | 历史接续说明、实际进度、迁移/设计导航、复盘和下一入口 |

ArticleController.java原修改保持未覆盖/未暂存，homa-xxl-job及AI项目/PDM等参考目录不改。未改变ArticleVO/HTTP字段、既有全局缓存管理器或RabbitMQ任务协议，没有新增中间件/后台清理组件。

### 8.5 实际验证

```text
mvn -q -Dtest=ArticleServiceImplTest,HotArticleWarmupServiceImplTest,CacheInvalidationConsistencyTest,ArticleVersionSqlTest test
```

使用IDE Maven/JBR21、Java17目标。29项通过：详情10、预热2、一致性15、SQL绑定2，0失败/错误/跳过。随后加强SQL绑定用例，验证MyBatis-Plus实际insert/updateById也包含content_version及正文，同条更新；只重跑`mvn -q -Dtest=ArticleVersionSqlTest test`，2项通过。当前29个不同用例，不把两次执行相加；没有新增无关测试或重跑全量。

实际manager/Caffeine配共享受控Redis状态，两个manager对象无自动广播；服务真实读取及Spring事务代理使用真实代码，DB/Mapper和Redis命令为替身。并发用真实线程与CountDownLatch，让旧正文装载暂停、新版完成、再放行旧装载，确认旧响应拒绝、新键保留。SQL绑定来自实际XML与MP语句，不是复制SQL文本；但没有执行真实MySQL UUID、唯一性/隔离或DDL，也不是多进程验证。

Maven test已编译主/测试源码，未另构建发布包。迁移未执行，未启动完整应用、连接真实MySQL/Redis/MQ/ES、浏览器/压测/上线，无性能数字。未使用真实业务数据、付费模型或启停已有服务。最终检查本轮文件UTF-8/CRLF、文档相对链接及git diff --check。

### 8.6 残留风险、切片验收与复盘决定

| 事项 | 当前结论与决定 |
| --- | --- |
| 创建/编辑/AI同写版本，详情/预热选版 | 本地实现与受控验证完成，正式迁移/统一读写切换验收待补 |
| 分类/标签独立改名/删除、独立关联写入 | 未维护本文版本，可能同版本仍有旧关联名称或负值；C3b下一入口，不能称整份VO一致 |
| 旧程序或外部直接SQL修改正文 | 不保证维护版本，禁止新旧混跑后宣称协议成立；迁移须核实实际库/写入方 |
| 陈旧事务快照、滞后副本、检查之后的在途变更 | 保持边界，不宣称所有返回瞬间必为最新 |
| 旧编辑覆盖、旧AI任务晚写回 | UUID不是顺序或CAS，业务冲突/任务执行代尚未解决；任务治理仍在队列阶段 |
| Redis完全不可用、统计/热榜恢复、Bloom及读写成本 | C3/C4继续；版本键不是完整Redis容灾，也未减少DB负载 |
| 队列和报名 | 继续推迟，缓存阶段未收口 |

本次切片复盘2026-10-21，依据新增正文选版证据继续C3b关联快照，随后C4成本治理；没有新证据推翻缓存→队列→报名顺序，无新增取消业务项，不为复盘增加组件。下一次周复盘仍不晚于2026-10-26。下一代码入口是CategoryServiceImpl/TagServiceImpl及ArticleTag写入，先核实受影响文章、版本更新与文章编辑的事务/锁顺序，再实施最小覆盖方案。

本地普通提交标题：`fix: 按持久正文版本隔离文章缓存`，与本节同一提交，可按标题定位。C3a只验收上述局部实现/隔离证据，C3b、真实迁移/多进程与C4仍未完成，缓存阶段保持进行中。

## 9. C3b正文缓存与关联快照分离复盘（2026-10-22）

### 9.1 触发、调用链与方案选择

改前基线3a0942d，开始时仅用户ArticleController.java原修改。分类/标签可独立改名，正文content_version不变，原正文SQL却把关联名称一起放入版本缓存；旧JOIN还可能因缺分类/标签写入同版本负缓存。只改名发广播仍依赖交付成功，批量给文章换版本需要处理影响范围、并发新增关系与锁顺序。

定向核实：CategoryServiceImpl.updateCategory/TagServiceImpl.updateTag独立更新名称；deleteCategory/deleteTag有引用即拒绝删除，但检查/删除并发完整性不在本轮保证内；ArticleServiceImpl.saveArticleTag位于创建/编辑事务，正文已随其换版本。无需改写这些业务写入来解决读端长期陈旧。

采用方案：正文只缓存t_article标量；将原返回前版本/权限查询替换为一条SQL取得当前可见版本、分类和标签快照。版本相同才复制正文并装入本次关联/导航，查询失败不回退旧名称，关联缺失不输出/不计数。既有INNER JOIN有效分类/标签要求保留，不新增无标签公开规则。取消扇出维护文章版本的本切片实施项，以避免写放大和新事务/锁顺序；没有引入新中间件或关联版本表。

### 9.2 不变量、失败、恢复与并发

最终校验与关联名称来自同一条权威SELECT；已提交改名/关联修改不依赖正文失效、Pub/Sub或TTL才被后续查询看见。正文编辑期间前后版本不同仍拒绝；最终SQL之后的新变化允许以本次查询快照返回，在途响应无法追回，导航不是同一全局快照。权威读取仍要求当前主库/无旧事务快照。

新body键`article:id:v:V:body`隔离C3a可能产生的关联负缓存。缺关联时正文可以缓存但不输出，恢复有效关联后复用同body即恢复返回。旧键自然过期，不扫描、不无协议删generation。预热只计正文装载，warmed不代表关联有效或整份详情可见。管理精确key失效、C1预算与C2行为保持。

两个读者交错时分别复制正文、装各自的快照和导航，不能修改共享缓存或前一个已构造响应。普通标签改为私人标签时由最终SQL的私人标签排除规则决定访问；已有私人访问收回测试保留。关联故障明确失败，修复后重试取得当前快照，不把重试整个业务写入当缓存恢复。

### 9.3 文件职责

| 文件 | 本切片变更 |
| --- | --- |
| `src/main/java/com/ican/model/vo/ArticleDetailStateVO.java` | 内部可见版本/分类/标签快照，未新增HTTP字段或数据库列 |
| `src/main/java/com/ican/mapper/ArticleMapper.java` | 增加最终关联快照查询契约，初次轻量选版保留 |
| `src/main/resources/mapper/ArticleMapper.xml` | articleBody显式映射/正文无JOIN；最终快照复用私人限制、有效分类标签JOIN与嵌套结果映射 |
| `src/main/java/com/ican/service/impl/ArticleServiceImpl.java` | body键、最终版本/权限与关联一起核实，复制响应后用本次关联覆盖，保留统计与导航 |
| `src/main/java/com/ican/service/impl/HotArticleWarmupServiceImpl.java` | 与详情使用相同body键，不再缓存关联或关联缺失负值 |
| `src/test/java/com/ican/service/impl/ArticleServiceImplTest.java` | 既有10项适配最终快照，新增独立改名/响应隔离、缺失恢复、关联故障恢复3项 |
| `src/test/java/com/ican/cache/CacheInvalidationConsistencyTest.java` | 既有15项适配body键/最终快照，新增真实manager旧结构负缓存隔离与关联恢复1项 |
| `src/test/java/com/ican/service/impl/HotArticleWarmupServiceImplTest.java` | 原2项验证body键、去重与失败承接 |
| `src/test/java/com/ican/mapper/ArticleVersionSqlTest.java` | 原2项保留，新增正文/最终SQL约束及实际ResultSetHandler多标签组装2项 |
| 缓存设计06第9节、路线07、阶段计划05及本总结 | 持续维护协议、取舍、时序、文件、验证、成本、未验证边界及下一入口，不新增每日报告 |

未修改分类/标签写入方法、引用删除规则、RabbitMQ链路、ArticleVO接口字段或C3a迁移脚本；ArticleController.java原修改保留且未暂存，homa-xxl-job/其他项目不动。

### 9.4 实际验证与限制

```text
mvn -q -Dtest=ArticleServiceImplTest,HotArticleWarmupServiceImplTest,CacheInvalidationConsistencyTest,ArticleVersionSqlTest test
```

IDE Maven/JBR21、编译目标17，首轮33项通过（13+2+15+3），0失败/错误/跳过。随后明确正文结果映射并新增多标签实际映射验证，仅重跑`mvn -q -Dtest=ArticleVersionSqlTest test`，4项通过；再补实际manager旧结构负缓存与恢复用例，仅运行`mvn -q -Dtest=CacheInvalidationConsistencyTest#oldJoinedNegativeCacheIsIgnoredAndRelationRecoveryReusesOnlyTheBody test`，1项通过。当前35个不同用例分批通过，不重复计数，也不无变化重跑全量。

实际manager/Caffeine+受控Redis状态验证旧结构负缓存被绕开、关联缺失抑制返回、恢复复用正文且缓存中无分类/标签；服务测试验证名称更新、快照查询异常/恢复、响应隔离，既有真实线程屏障/慢正文/回滚/提交后失效故障验证继续通过。实际XML/MP绑定验证正文无JOIN、最终SQL版本/权限/有效关联；实际DefaultResultSetHandler配受控JDBC ResultSet把两标签行合成一个父快照。数据库仍未执行，不把Mapper/ResultSet替身当MySQL隔离、写入并发或多实例联调。

Maven test编译主/测试源码，没有单独发布包、真实数据库迁移、完整应用启动、真实MySQL/Redis/MQ/ES、多进程/浏览器、压测或上线证据；未付费模型、真实数据、启停服务或部署。最终核对本轮编码换行、文档相对链接和git diff --check。正常详情仍4次同步文章SQL，不新增次数，但最终JOIN结果行随标签数增加，不能称成本不变；正文回源不再按每标签行重复longtext。没有QPS/延迟收益数字，C1预算不覆盖全部权威/导航SQL。

### 9.5 验收与下一步复盘

本切片验收为正文缓存/当前关联快照分离的本地实现与受控证据。已处理独立名称/关系长期陈旧和旧关联负缓存隔离；未解决数据库引用删除竞争、目录聚合可见性、陈旧事务/副本、最终查询后的在途变化、编辑覆盖和旧任务摘要回写。C3a字段迁移未执行，新旧应用统一切换仍待验收，不能宣称生产已具备完整一致性或缓存阶段完成。

本次复盘2026-10-22：继续C4a无效Bloom热路径，入口MultiLevelCacheManager.init/get及原预算/失败测试；当前Bloom未命中仍继续回源，先去除无实际阻断作用的额外依赖，再推进导航/逐次统计、预热热榜原子更新与必要观测。取消关联版本扇出方案而不取消关联时效要求；其他Redis不可用契约/恢复仍需按实际调用点定向处理。队列/报名继续推迟，下次周复盘不晚于2026-10-26，不增加全仓审计或平台。

本地普通提交标题：`fix: 分离文章正文缓存与当前关联快照`，本节与实现同一提交，可按标题定位。缓存阶段保持进行中。

## 10. C4集中开发复盘：热点读取、统计同步与热榜发布

日期2026-10-23，基线`0d4e953`，同分支。用户明确原粒度过碎，要求关联优化集中推进，并提供回源锁、TTL抖动、多实例通知、编辑失效、热榜预热作为集中治理方向。C1—C3已有实现复用；本批连续完成剩余成本/快照链路，不以单独删除Bloom作为一天交付。后续采用同一业务目标的一组关联改动、批次验证/复盘，保留缓存→队列→新功能顺序。

### 10.1 不变量、缺口与采用方案

改前详情命中仍4次独立同步文章SQL，先取PV再加一存在并发显示旧数；每次阅读仍写一次DB且与预热共池。Bloom不拦截但可额外失败；热榜先删逐条写、并发无发布栅栏；首页为所有匹配文章反复排预热。

本批要求当前权限/版本/关联/导航一致读取、PV快照重试不放大/不回退、新阅读不被旧ACK抹去、热榜完整发布且旧执行不覆盖、可恢复正文不被临时负缓存长期隐藏。最终快照合并导航和DB统计下限；PV按Redis原子累计+首次时间pending+DB GREATEST+比较ACK；热榜临时键分批构建+UUID发布代+RENAME/PERSIST；首页仅当前页且单实例最多一个在途预热。

采用展示型PV容忍少计，明确不是UV、账本或实际阅读完成。未采用每PV发MQ/新增统计账本/通用Outbox，不提前挤入队列阶段；未采用GETDEL增量后写DB，因进程中断会丢增量、失败回补/重复处理又可能多计。累计快照省去跨资源精确一次协议，但Redis丢失/回滚/记录未知及恢复竞争不能保证无损；健康时30秒周期也不等于任何故障下只丢30秒。

未把Bloom改成否定拦截：缺少可维护存在集合/初始化/恢复协议，版本化键不断新增，误拦新内容的风险没有收益支持。未在正文缓存中缓存账号导航或删除权威校验换性能。热榜采用最新开始的刷新资格，后发刷新失败时继续保留上次正式榜；不是全信号同一事务快照或分布式互斥。首页合并是允许跳过的优化，不为预热新增持久任务。

### 10.2 正常、失败、恢复与并发时序

| 场景 | 实现及验证边界 |
| --- | --- |
| L1/L2正文命中 | 初始版本+最终快照2次独立文章SQL；内部仍有导航子查询/JOIN，未测真实扫描成本 |
| Bloom专用操作不可用 | 实际manager不接触它，初始化/L2/冷填正常；L2/锁本身仍可失败 |
| 正文装载期间撤回、随后同版本恢复 | loader的null抛重读，不写负缓存；实际manager验证下一请求恢复；旧body负缓存清除后也能恢复 |
| PV正常与并发/重复刷新 | 返回同一次Lua的累计值、pending保留首次时间；不是业务请求幂等，同刷新会再计数 |
| PV记录结果未知 | 不重试增量，DB下限兜底；结果可能已生效，指标不当作严格账本 |
| DB失败/提交成功但ACK丢失 | 同绝对快照可重试，GREATEST不放大；单篇失败继续其他篇 |
| 快照后发生新阅读/两实例重复同步 | 旧ACK比较失败保留pending；较旧SQL不回退DB；允许冗余SQL |
| Redis统计失败/点赞失败 | 数据访问异常下返回最终DB views/点赞0；不把权威/限流失败也伪装为可读 |
| 第101篇构建或依赖失败 | 不发布半成品，旧正式榜保持；每批100，临时键120秒TTL |
| 并发刷新/旧执行晚完成 | UUID在读取信号前登记，构建/发布均核对；旧代拒绝，实际Lua受控交错通过 |
| 临时快照过期/明确空集合 | 不完整拒绝；正常空源发布空榜，非空RENAME后PERSIST |
| 重复首页请求/预热任务失败 | 只当前页，单实例排队/执行合并；finally释放标记，后续请求可恢复 |

前5—9节的事务/旧回填/正文版本和关联保护仍保留，已有相关回归也通过。不把受控Redis操作或两个manager当实际多进程/网络故障证据。

### 10.3 本批文件与职责

路径相对`blog-springboot`；只提交本批文件，用户`ArticleController.java`未改未暂存。

| 文件 | 职责 |
| --- | --- |
| `cache/MultiLevelCacheManager.java` | 删除Bloom字段/初始化/读取/回填调用，纠正保护注释；原预算、锁、失效代CAS与epoch不变 |
| `cache/MultiLevelCacheProperties.java`、`resources/application.yml`、`.env.example` | 移除孤立Bloom配置入口，实际外部配置/位图未动 |
| `service/impl/ArticleServiceImpl.java` | 快照导航、正文不依赖统计、统计降级、拒绝临时正文负缓存/清旧负缓存、当前页首页预热合并 |
| `model/vo/ArticleDetailStateVO.java`、`mapper/ArticleMapper.xml` | 在最终当前快照合并DB views与上下篇，保留权限和多标签映射，无导航时null |
| `service/impl/ArticleViewService.java` | 新增展示PV记录及最多100篇/轮快照同步，DB失败/ACK未知可恢复 |
| `mapper/ArticleMapper.java` | 新增累计GREATEST checkpoint，不触发正文版本变化；旧增量方法保留但本链路不再调用 |
| `BlogApplication.java` | 开启应用内调度，使PV同步不依赖手工建Quartz任务；未启动应用 |
| `metrics/BlogMetrics.java` | 统计降级计数、最近待同步文章数、快照/失败/耗时指标 |
| `resources/lua/article_view_record.lua`、`article_view_snapshot.lua`、`article_view_ack.lua` | 原子累计/pending登记、有界快照、比较确认，字符串数值兼容既有Integer成员 |
| `service/impl/ArticleHotScoreServiceImpl.java` | 保留公式，登记刷新代、100篇分批构建、检查发布结果、清理临时键 |
| `resources/lua/article_hot_stage.lua`、`article_hot_publish.lua` | 构建代检查/TTL，完整性/刷新代检查，原子RENAME+PERSIST或明确空榜 |
| `service/impl/HotArticleWarmupServiceImpl.java` | 固定版本loader临时缺失明确失败，旧body负缓存清理；复用manager预算 |
| 7个相关测试类 | 见下表：原保护回归、统计恢复/序列化、映射、热榜构建失败、预热合并 |
| docs/04新07及03/04/06、docs/README | 新领域设计和入口；旧审查/数字标为历史，不删除演进记录 |
| 原路线、日常约定、Phase6计划、本总结 | 更新批次开发规则、已交内容、未验收边界与下一代码入口 |

Java路径前缀`src/main/java/com/ican/`，resources路径前缀`src/main/`；7个测试位于`src/test/java/com/ican/`。

### 10.4 实际验证命令与结果

IDE Maven/JBR21、目标Java17。按代码变化分批执行，没有无变化重复全仓测试：

```text
mvn -q -Dtest=MultiLevelCacheManagerTest,CacheInvalidationConsistencyTest,HotArticleWarmupServiceImplTest test
mvn -q -Dtest=ArticleServiceImplTest,ArticleViewServiceTest,ArticleVersionSqlTest,CacheInvalidationConsistencyTest test
mvn -q -Dtest=ArticleHotScoreServiceImplTest,ArticleServiceImplTest test
mvn -q -Dtest=ArticleServiceImplTest,CacheInvalidationConsistencyTest,HotArticleWarmupServiceImplTest test
python target/check-cache-batch-lua.py
```

第一条Bloom后25项通过；第二条新增测试首次Mockito泛型编译失败，改为匹配List/Long脚本泛型后38项通过；第三条热榜与首页预热20项通过；第四条临时负缓存恢复修改后34项通过。最终不同用例共54个，不把重跑叠加计成果：

| 类 | 最终用例 |
| --- | --- |
| MultiLevelCacheManagerTest | 7 |
| CacheInvalidationConsistencyTest | 17 |
| HotArticleWarmupServiceImplTest | 2 |
| ArticleServiceImplTest | 15 |
| ArticleViewServiceTest | 4 |
| ArticleVersionSqlTest | 4 |
| ArticleHotScoreServiceImplTest | 5 |

以上失败/错误/跳过最终均0。实际MyBatis XML/MP生成的SQL与受控JDBC验证多标签合并、DB views、上一篇映射与下一篇SQL NULL；实际RedisConfig序列化Integer成员与新字符串脚本成员兼容。SQL解析/映射不证明MySQL执行计划、事务隔离或性能。

Python用既有target/Lupa执行5个实际Lua源码，受控Redis命令验证PV累计/DB下限/INT_MAX、100篇上限、pending首次时间不后移、旧ACK/重复ACK、写前类型错误、丢失后的DB下限重建；热榜构建失败保留旧榜、发布去临时TTL、旧代拒绝、临时过期拒绝、明确空榜。未新增应用依赖。所有验证日志和脚本留在忽略target中。

Maven test完成主/测试源码编译，没有另跑发布包构建。未执行真实字段迁移、完整应用/真实调度、MySQL/Redis/MQ/ES、多进程、EXPLAIN、浏览器、负载或上线；未调用付费模型、生产数据或启停服务。没有QPS/延迟数值，2次SQL指往返次数，不等于内部工作量减半。

### 10.5 残留、阶段边界与接续

本批本地验收覆盖上述C4代码和故障/恢复协议，不是P6-01生产验收。C3a迁移未执行，新旧应用必须统一切换；旧实例继续每PV增DB、直接更新正式榜或忽略版本协议会破坏本批承诺。PV允许少计、INT上限及Redis同故障域，不适用于财务/计费；批量处理存在积压和多实例重复SQL。现有RedisStartupFix自动禁用RDB失败写保护需下一批移除/收口。首页全量查询/内存分页和全量ZSet读取仍在，不能当本轮已解决。

继续：下一缓存批次集中处理运行配置与剩余回源故障观测，代码入口`RedisStartupFix`、`MultiLevelCacheManager.init/get/clear/getStats`、现有`BlogMetrics`。聚焦不自动削弱Redis持久化保护、预算拒绝/锁超时/装载与依赖失败可区分，不引入新治理平台；结合已有回源锁/TTL/提交失效/多实例版本保护明确本地阶段验收及待联调项。迁移/真实依赖受限准确保留，不能用反复环境检查替代代码推进。

推迟：队列与报名仍按用户顺序排后，缓存本地重大缺口收口并记录阶段边界后进入现有RabbitMQ恢复/认领/版本，而非继续无限追求缓存完美。取消把每项无效调用清理单独作为一日交付的粒度；同目标批量开发且只验证实际风险。最近定向复盘2026-10-23，下次周复盘不晚于2026-10-26。

本地提交标题：`fix: 集中治理热点读取统计与热榜快照`，本节和代码同一提交，可按标题定位；不合并、不推送、不部署。缓存阶段整体仍进行中。

## 11. C5运行配置、回源诊断与缓存本地阶段收口

日期2026-10-24；改前`7710104`，分支`codex/blog-quality-daily`。接续第10节当前批次后再转队列的约定，不重做Bloom/PV/热榜。本节是持续总结的阶段收口，不新增每日复制报告。

### 11.1 触发、缺口与方案取舍

C4把展示PV的未同步证据放在Redis，源码却存在启动时自动将`stop-writes-on-bgsave-error`设为`no`的本地钩子；它既不修复RDB错误，又削弱保护。另外原manager只给L1统计字符串，故障时不能区分预算满、锁超时、装载业务拒绝、Redis失败和旧回填被拦截。

本批不变量：应用启动不自动修改Redis持久化写保护；诊断只增加固定维度计量，不放宽已有预算、提交/版本/可见性协议，不增加Redis失败直接查库旁路。已有版本/撤回的业务承诺依旧有第8—10节的权威快照和迁移边界。

采用删除本地钩子而非加开关/循环CONFIG SET：服务端配置应在授权环境检查、修复和维护，应用不能用“可写”掩盖持久化失败。采用现有BlogMetrics而非监控平台：在实际执行点记录事件和耗时，保留错误传播；不增加高基数文章维度、队列式指标缓冲或通用拦截框架。回填null现在明确提示结果未知，0仍是内容已变；二者与异常均不把装载值安装L1。

### 11.2 正常、失败、恢复和并发证据

| 时序 | 结果/实际证据 |
| --- | --- |
| L1/L2命中；冷装载后回填 | 分别计实际命中、loader、fill与request；命中不占重建预算。通用负缓存返回null，分别计loader/null、L1/negative和request/null |
| 线程屏障阻塞一个loader占满预算，另一个miss与L2命中并发 | active=1、另miss只增budget/rejected；L2命中仍返回，释放后active=0且新miss可重建 |
| 等锁timeout/中断/运行时异常 | 超时只末次读L2，中断保留线程标记，运行时异常保留原异常；均不执行备用loader，预算释放后请求恢复，各结果可区分 |
| loader抛错或锁内L2读取失败 | 分别计loader/error与l2/error，不把Redis读失败算成DB装载失败；锁/预算按原finally释放，下一请求能继续 |
| 回填返回null、0、抛异常，之后恢复1 | 前3次拒绝且未留下成功L1；第4次重新装载、成功缓存，第5次命中；unknown/stale/error/accepted各1，不猜测Redis是否实际执行 |
| 失效异常/null，之后删除成功但广播失败，再次重试 | 前2次不发布；区分evict/error、unknown与publish/error；重试可以发布。返回0订阅者仍只计publish/sent，不声称收到 |
| 提交/回滚、晚旧loader、双manager漏广播和旧L1、撤回再恢复 | 原17项一致性回归通过，未用指标替代正文版本与权限保障；双manager受控替身不是网络/真实多进程证据 |

所有新增缓存metric的标签仅stage/outcome，计量测试用真实SimpleMeterRegistry。L2是操作次数，request是manager调用，active是已取得本实例预算的调用；都不是完整接口请求量/DB总并发。loader错误可来自业务版本检查；request耗时也不含调用方最终权限SQL。发布sent与通知received不能证明持久投递。完整口径、排查方法及本地验收矩阵见[领域设计第7节](../04-cache/07-热点统计与热榜快照设计.md)。

### 11.3 文件与职责

| 路径（相对blog-springboot） | 改动 |
| --- | --- |
| `src/main/java/com/ican/cache/MultiLevelCacheManager.java` | 实际L1/L2命中、预算拒绝、锁/装载/回填/失效/广播的固定结果计量；3段耗时和预算Gauge绑定；明确回填未知；更正clear只清本实例的注释 |
| `src/main/java/com/ican/metrics/BlogMetrics.java` | 复用Micrometer的缓存事件、分段Timer与预算active Gauge；无新依赖 |
| `src/test/java/com/ican/cache/MultiLevelCacheManagerTest.java` | 既有7项补计量/预算断言，新增3项相关故障恢复；实际SimpleMeterRegistry |
| `src/test/java/com/ican/cache/CacheInvalidationConsistencyTest.java` | 只补构造器依赖，原17项提交/并发/版本保护回归 |
| `src/main/java/com/ican/config/RedisStartupFix.java`、对应target/class | 本地源码与遗留class删除；源码被本机`.git/info/exclude`排除，当前HEAD未跟踪，因此不在提交中；不清理其他target文件 |
| 缓存领域07/现状审查03、原Phase6计划、总路线与本总结 | 精确口径、运行保护、阶段本地验收/未验收矩阵、下一队列入口与完整复盘 |

`ArticleController.java`用户原修改保留且不暂存；未修改homa-xxl-job、其他项目、队列实现或前端。删除钩子不恢复实际Redis配置、不删除其他检出或旧包的同名类；部署前需检查干净构建与所有实例。

### 11.4 实际验证与未验证

IDE Maven/JBR21、编译目标17，实际命令：

```text
mvn -q -Dtest=MultiLevelCacheManagerTest,CacheInvalidationConsistencyTest test
```

退出0，MultiLevelCacheManagerTest 10项、CacheInvalidationConsistencyTest 17项，合计27，失败/错误/跳过均0。日志`target/cache-c5-test.log`和本次Surefire XML保存在忽略target；受控故障产生的异常日志不等于测试失败。编译主/测试源码，没有另跑package或全量测试，没有无变化重跑C4的Lua/统计/热榜验证。源码定向搜索确认当前main中没有RedisStartupFix、setConfig、stop-writes-on-bgsave-error；增量编译后对应class仍不存在。

未连接实际MySQL/Redis/MQ/ES、执行迁移、启动应用/定时调度、读Prometheus端点、压测/多进程、部署/推送/合并或调用付费模型。没有吞吐/尾延迟数字，没有声称新增指标已经完成监控告警。后续上线必须确认内容版本迁移/统一切换、权威读快照、真实脚本/事务/调度、Redis持久化配置与积压恢复。

### 11.5 阶段验收、残留与复盘决定

P6-01本地范围验收：C1计量/作用域/有界重建、C2提交后失效与旧回填、C3正文版本/当前关联快照、C4热点统计/完整热榜/预热合并、C5运行保护与分段观测已有实现和各自受控验证。此次只重跑直接改动的manager和一致性测试，不将历史所有用例重新相加作为本日成果。可以进入P6-02代码开发；缓存真实环境/上线验收未通过，不能将迁移受限写成整体完成。

保留：多实例总预算、Redis复制/Cluster/故障切换、首页全量SQL/内存分页/全量ZSet、PV少计和INT饱和、权限最终快照之后的在途变化、无持久失效补偿、锁租期不等于全局唯一装载。锁释放异常仍可能遮盖原错误，本批仅观测。指标不修复这些边界；不无限追加缓存优化阻塞下一阶段。

继续：定向已读`AiTaskServiceImpl.createArticleTask/executeTask/retryTask`、`AiArticleConsumer`、`RabbitMqConfig`和`ArticleSearchSyncConfig`。现有任务插入后仍在文章事务内convertAndSend；executeTask先查再无条件markRunning；retryTask可重置RUNNING。AI消费者抛AmqpRejectAndDontRequeueException，队列声明DLX，但这不是数据库提交可见、投递确认或业务重试闭环。搜索已有独立3次重试/confirm故障转存，不能照搬成AI已经有该保障。

下一代码入口：先补读AiTask实体/mapper/建表SQL、实际全局listener配置和AiArticleService回写，再集中设计“已有持久任务作为待办→提交后唤醒→有界扫描/重投→原子认领/人工重试边界”；明确confirm/ACK未知与重复消息，随后执行代/正文版本条件回写。不能只改afterCommit便结束，也不能自动重投正在执行的模型请求而没有执行代/旧结果约束。验证仅用受控处理器，不调用付费模型；真实表元数据未知时提供迁移并保留未执行状态。

推迟：报名新功能，直至队列本地阶段验收及其真实DB容量事务验证边界明确；缓存性能参数/全量首页成本留到有负载证据时，不按猜想增组件。取消：自动关闭Redis写保护作为修复、以发送成功/指标存在推导可靠投递、按日拆分无关微调。最近批次复盘2026-10-24，下次周复盘不晚于2026-10-26；周复盘基于相关变化，不全仓重审。

本地提交标题：`fix: 收口缓存运行保护与回源故障观测`，本节与代码同一提交，按标题可定位；只提交本批跟踪文件，无推送/合并/部署。


## 12. 当前配置库版本字段迁移已执行


2026-10-26执行状态补充：用户授权后，当前配置库`121.41.87.40:3306/blog`已执行01—05及必要的AI字段类型修正06，并经完整备份/新连接读回确认。34篇文章原有字段与正文均保持一致，0条AI任务；作者/缓存版本和任务恢复列、扫描索引已建立。原文“未执行”是此前交付时记录，不代表该库当前状态；其他库、应用部署/统一版本、真实依赖/并发与负载仍未验收。详见[数据库迁移执行记录](../06-deployment/06-数据库迁移执行记录.md)。

本次只补真实MySQL版本字段和读回；缓存实例统一、Redis/Lua/多进程、指标采集和负载门槛仍保留，不重做C1—C5。
