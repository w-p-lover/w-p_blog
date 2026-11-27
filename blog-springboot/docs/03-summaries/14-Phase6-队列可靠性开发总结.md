# Phase6-队列可靠性开发总结

持续维护的P6-02开发复盘，首轮2026-10-25；分支`codex/blog-quality-daily`，改前基线`3dfaf44`。用户要求缓存→现有队列→新功能，关联代码集中交付。缓存C1—C5本地范围已收口，真实迁移/依赖验收仍待补。Q1—Q3已交本地批次；2026-10-28 Q4修复真实MySQL恢复饥饿并补实际Java/依赖证据，当前进度以第10节为准。队列整体未完成，报名未开发。

## 1. Q1业务触发与改前证据

作者保存文章时，既有AiTaskServiceImpl直接在文章事务内convertAndSend，消费者可能在提交前读取不到任务；提交后发送/进程失败没有扫描恢复。executeTask先select再无条件markRunning，双消费者可同时启动同一任务；人工retryTask可把RUNNING重置，再次触发副作用。全局listener有3次内存retry，但失败状态/消息拒绝与业务重试语义没有闭合。摘要按articleId更新、向量随机ID追加，旧内容与部分完成恢复仍无完整版本协议。

本批不变量：提交/回滚决定持久任务存在；MQ只作唤醒，PENDING不因发送/confirm而失去恢复资格；同任务只有PENDING条件更新获胜者执行；RUNNING不能被人工重置；任务完成只由本执行token落状态。正常、失败、恢复、重复、并发按同一链路实现，没有把afterCommit当可靠投递结束。

## 2. 实现与取舍

- 复用t_ai_task加投递次数/到期时间/执行token与扫描索引；不新建通用Outbox、任务中心或中间件。任务创建/人工重试使用REQUIRED短事务，文章调用时加入原事务；插入不成功明确回滚。
- 提交后唤醒先REQUIRES_NEW预约再发送，避免afterCommit资源仍绑定旧事务造成预约未提交。漏回调、预约后崩溃或发送未知都由持久PENDING到期恢复，不写“发送成功后永久跳过”的标记。
- 每轮最多100个到期任务，固定完成后30秒扫描；DB时间、30/60/120/240/300秒封顶退避、条件预约。恢复可一直按此速率持续，界限是单轮/每任务间隔，不是有限总尝试数；没有全局在途上限或吞吐实测。
- 专用发布器复用现有连接/转换器，自持有mandatory RabbitTemplate，不注册Bean替换共享邮件template；消息PERSISTENT，correlation观察ack/nack/returned/unknown。10秒是确认future观测超时，不取消实际发送，也不是网络send总上限。
- 消费PENDING→RUNNING为数据库条件UPDATE，随后读任务并验证UUID；完成/失败匹配RUNNING和token。executeTask NOT_SUPPORTED，不把模型调用放进长事务。原任务列表/控制器/消息taskId格式保持。
- 重试只允许未达失败上限的失败状态，匹配已观察失败次数；清空旧执行字段但保留累计模型失败预算。PENDING只再次尝试到期唤醒；RUNNING/已结束拒绝。RETRYING沿用枚举表示失败未耗尽，Q1不自动重跑。
- AI专用AUTO factory、单消费者/实例、prefetch1、无内存retry、不重入队；现有DLX仅是broker故障副本路线，不能证明可靠转存。消费日志明确需核查持久状态与死信。未改变搜索的独立3次重试/confirm转存或邮件配置。

不采用RUNNING超时直接重置、失败状态定时自动重跑：原处理器的摘要/向量副作用没有完整版本/恢复协议，自动重放会扩大重复模型调用和旧结果风险。Q1的token只保护任务行，不保护文章结果或向量；Q2不能省略。方案/状态机/排查和完整场景矩阵见[领域设计](../01-architecture/03-持久任务投递恢复与原子认领设计.md)。

## 3. 本轮文件与职责

路径相对blog-springboot，Java生产前缀`src/main/java/com/ican/`。

| 文件 | 职责 |
| --- | --- |
| `entity/AiTask.java` | 映射投递预约次数、下次投递时间、执行token |
| `mapper/AiTaskMapper.java`、`src/main/resources/mapper/AiTaskMapper.xml` | 到期100条查询、预约CAS/退避、原子认领、token完成和条件人工重试 |
| `service/impl/AiTaskDispatchService.java`（新增） | 提交后唤醒、独立短事务预约、定时有界恢复、持久mandatory发送及确认未知诊断 |
| `service/impl/AiTaskServiceImpl.java` | 事务创建/重试、原子认领和token条件落状态，移除直接发送和无条件markRunning |
| `config/ArticleAiQueueConfig.java`（新增） | AI专用ACK/并发/prefetch及无内存retry，不创建额外共享template Bean |
| `consumer/AiArticleConsumer.java` | 显式选AI factory，纠正“异常必然入DLQ”的日志口径 |
| `metrics/BlogMetrics.java` | 固定stage/outcome的投递/确认/扫描失败事件，无业务ID标签 |
| `src/test/java/com/ican/service/impl/AiTaskServiceImplTest.java` | 改旧服务断言为真实新协议，线程屏障/失败不自动重跑/人工重试边界 |
| `src/test/java/com/ican/service/impl/AiTaskDispatchServiceTest.java`（新增） | 实际Spring事务代理/同步/挂起，受控发送/确认、回调遗漏和恢复8项 |
| `src/test/java/com/ican/config/ArticleAiQueueConfigTest.java`、`mapper/AiTaskSqlTest.java`（新增） | Boot配置覆盖与共享template保留、实际MyBatis SQL解析/参数/实体字段 |
| `docs/07-database/04-文章任务投递恢复与认领迁移.sql`（新增） | 3列/1复合索引、目标元数据/历史任务与统一切换前置说明；未执行 |
| `docs/09-tooling/verify_ai_task_sql.py`（新增） | 读取实际mapper的临时SQLite两连接并发验证，只适配时间函数，不连接业务库 |
| 领域设计03、本总结14、原路线/Phase6/Phase5/日常约定、docs README | 完整业务时序与边界、交付证据和Q2接续；持续维护而非每日复制 |

用户原ArticleController.java修改未改未暂存。homa-xxl-job与其他参考项目未动，AiArticleServiceImpl摘要/向量、AiRagServiceImpl和前端未改；这些未改部分的旧版本风险保持明确。

## 4. 实际验证与结果

IDE自带Maven、JBR21、目标Java17，按变化分批：

```text
mvn -q -Dtest=AiTaskServiceImplTest,AiTaskDispatchServiceTest,AiArticleConsumerTest test
mvn -q -Dtest=AiTaskServiceImplTest,AiTaskDispatchServiceTest,AiArticleConsumerTest,ArticleAiQueueConfigTest test
mvn -q -Dtest=AiTaskDispatchServiceTest,AiTaskSqlTest,AiArticleConsumerTest test
python docs/09-tooling/verify_ai_task_sql.py
```

第一条初次16项中1项测试桩重新设定失败：Mockito的when触发此前设定的受控扫描异常；改为doReturn覆盖桩后第二条18项通过。随后增加NOT_SUPPORTED事务挂起和实际MyBatis绑定验证，第三条12项通过。最终20个不同用例：AiTaskServiceImplTest7、AiTaskDispatchServiceTest8、AiArticleConsumerTest3、ArticleAiQueueConfigTest1、AiTaskSqlTest1，失败/错误/跳过均0；重复执行不累计成果。最后两次输出分别在忽略target/queue-q1-test.log、queue-q1-final-test.log和对应Surefire XML。

| 场景 | 实际证据/限制 |
| --- | --- |
| 提交/回滚和提交后独立预约 | 真实Spring事务代理/同步，确认事务内不发送、回滚无预约、提交后独立预约提交；持久DB由受控mapper代替 |
| 前序缓存回调失败 | 注册真实afterCommit前序异常，即时唤醒跳过，随后扫描恢复；不冒充原缓存错误已消除 |
| send失败/returned/nack/unknown/ACK | 受控RabbitTemplate和真实CorrelationData future/指标，ACK不改待办；unknown主动注入TimeoutException，没有真实等10秒模拟网络 |
| 预约后崩溃/恢复 | 受控reserve输赢与重扫，加临时SQL到期时间推进；没有杀真实进程 |
| 双消费者与运行中重试 | 真实线程屏障、受控原子mapper，只一次processor调用；NOT_SUPPORTED测试确认处理器不在调用方事务中 |
| SQL竞争/失败预算/token | 实际XML语句在临时SQLite两连接执行，预约/认领/重试只有一个影响1行；错误token/终态/运行中/上限拒绝；MyBatis真实解析和参数顺序另验 |
| 配置隔离 | ApplicationContextRunner只启动配置上下文，验证共享RabbitTemplate仍一个，AI覆盖全局retry/manual/concurrency而非启动服务/消费者 |

Python脚本只为SQLite适配NOW/TIMESTAMPADD/LEAST/POW，未将SQL条件重新实现成Python判断；通过不证明MySQL方言、事务隔离/锁和查询计划。Maven test完成主/测试编译，未另跑package、全仓测试或历史缓存/Lua测试。没有实际迁移、应用启动/真实调度、MySQL/RabbitMQ/Redis/ES、多进程、压测、部署、生产数据、付费模型调用。未将发送量当成功量，未报告性能提升数值。

## 5. Q1验收边界、残留与当次复盘

Q1本地范围：提交后唤醒+持久PENDING恢复、预约/认领条件更新、任务行token回写、人工重试竞争与预算、专用ACK配置和必要确认诊断，已有相关代码/受控验证。P6-02整体仍进行中，不能写消息零丢失、精确一次副作用、失联RUNNING自动恢复或完整发布闭环。

未解决：不同任务同文章的旧摘要覆盖、向量随机ID重复/旧版本读取、结果部分成功后的重试、撤回中的派生回写和失联RUNNING恢复。任务状态成功也不说明业务结果仍适用于当前内容；任务状态写入未知可留RUNNING，即使业务副作用已发生。现有DLX不是带确认故障转存，积压年龄/全局在途预算与负载未完成；DB备份/复制丢记录不在本协议承诺内。

继续Q2：从createArticleAiTask的当前持久版本传递、AiTask request/实体、AiArticleServiceImpl.processArticle/updateArticleAiResult、buildChunkedDocuments与AiRagServiceImpl来源校验进入。先定义作者内容版本与AI摘要导致content_version变化的关系，方案需同时覆盖摘要CAS、向量稳定身份/版本元数据及读取过滤、成功与业务过期/部分完成，随后设计RUNNING执行代/租约恢复和自动业务重试。不能只给update加WHERE就把向量闭环算完成，也不能为避免问题无限保留未知RUNNING而声称高可用。

推迟：报名新功能直到队列阶段边界收口；真实压测待协议正确、测试环境/资源授权明确。取消：仅afterCommit、仅手动ACK、盲目RUNNING重置、把声明DLQ等同可靠恢复。新发现的前序缓存提交回调可阻断即时唤醒，已由持久扫描覆盖，不另改缓存主线。最近定向复盘2026-10-25，下次周复盘不晚于2026-10-26，按相关变化评估，不反复全仓审计。

## 6. 迁移与提交

[迁移04](../07-database/04-文章任务投递恢复与认领迁移.sql)未执行。未来先核实目标表列/索引、备份/历史任务与费用预算，再审批迁移、统一切换所有旧生产者/消费者；不可混跑旧无条件认领/重试协议。历史PENDING在新应用启动后会被扫描，不可未经核查就启动；本轮没有启动/停止任何已有服务。旧RUNNING/失败行不自动重置，旧DLQ不自动重放。

本地提交标题：`fix: 补齐持久任务投递恢复与原子认领`，本总结与本批代码同一提交，可按标题定位。只提交本轮文件，不合并/推送/部署。缓存实际依赖验收仍沿第13份总结保留，不能因转阶段而消失。


## 7. Q2集中交付与2026-10-26周复盘

本批承接cf6b691，解决“作者B已保存，旧任务A才回写摘要”“向量第二批失败后重试不断新增随机点”“文章仍公开却在问答引用旧正文”。不变量是派生结果只能发布到自己绑定的当前作者正文；未完成向量批次和旧源不能成为问答上下文。完整正常/失败/恢复/并发时序和方案取舍见[领域设计第8节](../01-architecture/03-持久任务投递恢复与原子认领设计.md)。

### 7.1 实际改动与职责

| 文件 | 本批职责 |
| --- | --- |
| entity/Article.java | 独立sourceVersion和indexedSourceVersion，保留详情contentVersion |
| service/impl/ArticleServiceImpl.java | 每次作者保存新源身份；编辑同事务清除旧摘要/标签/索引时间/向量发布凭据 |
| entity/AiTask.java | 任务源身份与generatedPayload检查点，失败/人工重试不清检查点 |
| service/impl/AiTaskServiceImpl.java | 同事务读源并核对标题/正文；执行前排除旧/无身份/非公开任务；当前发布SUCCESS，过期/撤回CANCELED并记录outcome |
| service/AiArticleService.java、impl/AiArticleServiceImpl.java | 接受已认领任务/源正文；先持久摘要标签再写向量；稳定UUID点ID、源版本metadata；分批全返回后条件发布。历史回填遵循相同身份/发布条件 |
| mapper/AiTaskMapper.java、resources/mapper/AiTaskMapper.xml | RUNNING/token条件写检查点，既有认领/失败重试/完成不清该列 |
| mapper/ArticleMapper.java、resources/mapper/ArticleMapper.xml | 最终摘要/缓存版本/向量发布源同SQL；源/公开/删除/token条件；作者编辑清理；问答查询当前已发布源与权限；历史向量独立条件发布 |
| impl/AiRagServiceImpl.java | 首次与生成后按当前已发布源过滤所有实际片段，保留私人可见性/合法ID/规范URL，生成期间变更整体拒绝 |
| AiArticleServiceImplTest、AiTaskServiceImplTest、AiTaskDispatchServiceTest | 生成检查点、部分批次/相同点ID、过期/历史取消、失败预算及原投递事务回归 |
| AiRagServiceImplTest、mapper/ArticleVersionSqlTest、AiTaskSqlTest | 历史/旧源/未发布/生成中编辑拒绝，原撤回/资格/错误输出契约，以及实际MyBatis语句/字段绑定 |
| docs/09-tooling/verify_ai_task_sql.py | 扩展原临时SQLite验证，执行实际Q2发布/检查点XML，不另建测试平台 |
| docs/07-database/05-文章源版本与派生发布迁移.sql | 4列、历史文章新身份/摘要与缓存处理、旧任务不猜版本、统一切换与回填门槛；未执行 |
| 原领域设计/问答约束、路线/Phase6计划、持续总结、README | 更新当前契约、迁移影响、证据/残留与下次入口；未每日复制报告 |

本表Java路径均相对于src/main/java/com/ican，XML在src/main/resources/mapper，测试在src/test/java/com/ican。没有修改或暂存用户ArticleController.java；homa-xxl-job与其他项目未动。没有新中间件、报名功能或额外前端调整。

### 7.2 方案与业务恢复

作者源UUID独立于会被AI改变的缓存UUID，避免任务/向量自己失效；不以仅摘要WHERE条件代替向量读取保护。任务保留source身份而不复制完整正文，过期就取消，不把受理时旧任务升级成处理最新版。编辑清掉旧AI展示，避免新正文继续显示历史摘要；新增源身份每次保存都会换，包括内容回改，防止旧任务复活。

摘要标签均生成后先持久检查点；向量第2批失败不发布，人工重试读取检查点，使用相同源/块索引的UUID重新upsert。当前Qdrant依赖源码已核对按Document ID执行upsertAsync(...).get()，未连真实服务。旧任务A晚于B写向量仍只能写A点，最终SQL不能覆盖B；问答只采用当前源且有全批次发布凭据的片段。历史无版本任务CANCELED且不增加失败计数，无版本向量拒绝，迁移后召回会降低，须授权回填。

未采用新通用Outbox/向量平台、内容历史表或每篇全局串行锁；既有任务与Qdrant已能表达当前协议。相同源的不同任务仍可分别调用模型并覆盖同源摘要；稳定ID只约束向量点身份，不代表模型/Embedding费用一次。检查点前崩溃仍可能重新生成，检查点后进程崩溃仍可能留下RUNNING，本批没有擅自自动重置。

### 7.3 实际验证

IDE Maven/JBR21、Java17编译目标，命令：

```text
mvn -q -Dtest=AiArticleServiceImplTest,AiTaskServiceImplTest,AiTaskDispatchServiceTest,AiRagServiceImplTest,ArticleVersionSqlTest,AiTaskSqlTest test
python docs/09-tooling/verify_ai_task_sql.py
```

首轮43项通过，补齐3项源身份/过期取消的任务服务用例及SQL条件断言后最终46项通过：处理器7、任务服务10、投递恢复8、问答16、文章SQL4、任务SQL1，0失败/错误/跳过；不累加重跑。最终日志target/queue-q2-final-test.log与对应Surefire XML（忽略目录）。最后仅调整本批import分组/文档，无行为变化，不再重复全套。

处理器用受控ChatClient/VectorStore，模拟第2批故障、任务从持久检查点重新读取，验证重试不重新生成摘要/标签、前10点ID相同、13个合成分块无追加重复，且失败前不发布。问答保留实际MockMvc失败输出契约，新增文章仍公开但生成期间换正文源、未完成发布和历史无版本拒绝。真实MyBatis XML解析/参数/实体字段映射校验通过。Python读取实际XML在临时SQLite执行Q1两连接竞争与Q2新旧源发布、错误token、撤回/删除和检查点跨失败重试保留；仅适配时间/UUID函数，不是MySQL行锁/隔离或Qdrant网络联调证据。

Maven test包含主/测试编译，未另package/全仓测试。没有执行迁移、连接真实MySQL/Qdrant/Redis/MQ/ES、启动应用/调度、调用付费模型/Embedding、进行负载测试、部署或操作真实业务数据。没有规模/性能提升数字。

### 7.4 当前验收边界与本周决策

Q2本地批次验收：任务源绑定→检查点→稳定向量点→条件派生发布→问答两次当前源过滤，以及历史任务/向量拒绝协议已有代码和受控证据。只说明源版本隔离与失败后人工重试的可重复副作用；不代表队列阶段全部完成或RUNNING已可自动恢复。真实数据库/向量可见性、服务重启、多实例与统一切换仍是门槛。迁移05未执行，旧写入者/消费者/回填进程不得混跑；历史摘要清空和召回下降需要在实际迁移前核查并安排授权回填。

基于2026-10-25之后相关调用链的周复盘：

| 决策 | 当前证据与理由 | 下一入口 |
| --- | --- | --- |
| 继续Q3执行恢复 | Q1持久PENDING已可恢复，Q2防旧/检查点已具备；RUNNING失联仍可能永久悬挂，模型前/后未知成本不同 | executeTask的startedAt/token、generatedPayload、文章当前发布源；先区分可安全接续与模型结果未知 |
| 继续有限自动业务重试/积压与故障诊断 | RETRYING仍人工，生产confirm/ACK未知和DLX故障未真实验证；不能靠快速重投积累重复待办 | 既有dispatch扫描索引/预算、失败状态/次数、队列积压年龄与持久任务核查 |
| 保留缓存部署门槛，停止新增缓存小项 | C1—C5本地收口无新增反证；真实迁移/统一实例、Redis保护配置/指标/负载仍未验收 | 缓存总结13第11节；取得授权环境再补真实证据 |
| 推迟报名与Redis预扣 | 队列本地阶段仍未收口，失联执行与恢复影响受理承诺 | Q3完成且记录队列验收范围后进入P6-03 |
| 暂缓向量物理清理/全库接续工具 | 已可拒绝旧源；旧点可能挤占topK导致召回下降，但没有容量数据支持新增平台 | 先以既有有限回填核查数据分布；不降低版本门槛 |
| 取消盲目RUNNING重置、仅凭token/ACK宣称精确一次 | 同源模型重复/费用未知仍存在，稳定点不能撤销模型请求 | 不增加自动重跑或新中间件数量来掩盖未知 |

本周复盘完成日2026-10-26；下次周复盘不晚于2026-11-02，新重大缺口或下一集中批次完成时提前复盘，只读上次后的相关变化。没有反复全仓审计，不以文档替代本批代码。

### 7.5 提交与接续

本地提交标题`fix: 隔离文章源版本与异步派生结果发布`，本节与代码同一提交，可按标题定位；仅本批文件，不合并/推送/部署。下一Q3优先把检查点后可幂等接续与检查点前模型未知分开，验证执行代/租约与旧执行回写的真实竞争，再引入有预算的自动失败恢复/积压治理。不得将所有RUNNING直接改PENDING，不回放旧DLQ，不因Q2完成跳过队列进入报名。


## 8. 用户授权后的真实数据库迁移


2026-10-26执行状态补充：用户授权后，当前配置库`121.41.87.40:3306/blog`已执行01—05及必要的AI字段类型修正06，并经完整备份/新连接读回确认。34篇文章原有字段与正文均保持一致，0条AI任务；作者/缓存版本和任务恢复列、扫描索引已建立。原文“未执行”是此前交付时记录，不代表该库当前状态；其他库、应用部署/统一版本、真实依赖/并发与负载仍未验收。详见[数据库迁移执行记录](../06-deployment/06-数据库迁移执行记录.md)。

本次没有变更Java业务代码。SQL06修正真实库ai_indexed_at的int/LocalDateTime冲突及摘要100字容量；音乐权限脚本补6行，AI任务脚本不重复建表/菜单。逐语句、备份、表锁别名与本地空结果验证器修正、完整字段读回及未验证内容均记入上述执行记录。本批提交标题`fix: 执行博客数据库迁移并对齐AI字段类型`；源码bb69eb4不因此成为已部署事实。Q3顺序保持，未做付费回填/服务启停。

## 9. Q3集中交付、验证与批次复盘（2026-10-27）

本节为当前接续，Q1/Q2及迁移执行记录保持历史事实。本批基于f5fc156后的实际调用链，不重做缓存或迁移、不调用付费模型。业务不变量：同一任务的摘要/向量发布凭据与终态同一事务提交；旧执行被回收后不能发布；无持久检查点的结果未知不能自动重跑模型。

### 9.1 改前缺口、方案与文件职责

改前：RUNNING永久悬挂；摘要UPDATE与SUCCESS独立提交；Q2的EXISTS token没有共享回收锁观察点；RETRYING只有人工入口，缺少积压年龄观测。采用既有任务表/索引和检查点：原字段按状态区分投递/租约/业务到期，15分钟无进度租约，检查点和向量批次前续租，检查点后的有限自动恢复，UNKNOWN隔离模型结果未知，任务行锁内完成文章发布和终态。

不采用所有RUNNING改PENDING、无预算自动重试、把单消费者当并发保证、通用Outbox/租约平台、新心跳线程或新中间件。没有供应商调用幂等，检查点前重跑可能重复收费；向量稳定点只去重身份，Embedding也可能重新计费。完整正常/失败/回收/重复/旧新执行竞争时序见[领域设计第10节](../01-architecture/03-持久任务投递恢复与原子认领设计.md)。

| 文件（Java路径相对src/main/java/com/ican） | 本批职责 |
| --- | --- |
| service/impl/AiTaskExecutionService.java（新增） | 通过Spring独立短事务锁任务；最终发布与终态同事务；失败重新读持久检查点；提交后记录状态事件 |
| service/impl/AiTaskServiceImpl.java | 编排外部准备和最终事务；异常委托持久失败裁决，移除分裂的完成/失败写入；人工重试错误说明 |
| service/impl/AiArticleServiceImpl.java、service/AiArticleService.java | 处理器只准备向量；每批前条件续租；坏/空检查点禁止悄悄重生成；历史有限回填逻辑保留 |
| mapper/AiTaskMapper.java、src/main/resources/mapper/AiTaskMapper.xml | 任务行锁、15分钟租约与到期CAS、checkpoint重试预算/退避、到期接续及状态积压聚合；原子清token |
| entity/AiTask.java、enums/AiTaskStatusEnum.java | 时间字段状态语义、UNKNOWN结果未知状态；没有新增持久列 |
| service/impl/AiTaskDispatchService.java | 每轮先恢复最多100个到期执行/退避，再投递最多100个PENDING，快照观测；沿用Q1消息/confirm协议 |
| metrics/BlogMetrics.java | 五个有限状态数量/最老年龄、成功快照时间和复用有限事件；不加taskId标签 |
| src/test/java/com/ican/service/impl/AiTaskExecutionServiceTest.java（新增） | 真实Spring代理/事务同步及回滚决定、发布顺序、旧代拒绝、DB检查点胜过旧对象、UNKNOWN和共享预算6项 |
| 既有AiTaskServiceImplTest、AiTaskDispatchServiceTest、AiArticleServiceImplTest | 更新处理器/事务边界；受控并发/恢复编排、批次续租失败、空白/坏JSON检查点不调用模型；修正独立测试的元数据初始化 |
| 既有mapper/AiTaskSqlTest、metrics/BlogMetricsTest | 人工重试保留检查点而非清除；有限积压状态与快照时间断言 |
| docs/09-tooling/verify_ai_task_sql.py | 直接读取XML，新增到期边界、两连接回收/接续竞争、旧token拒绝、预算耗尽、UNKNOWN阻断及文章/终态事务提交回滚 |
| 领域设计03、原路线07、日常约定06、Phase6计划05、本总结14、docs README | 当前协议、业务边界、运行门槛和下批入口同步；不每天新建报告 |

ArticleController.java仍是用户原有未提交修改，本轮未改/未暂存；homa-xxl-job与其他项目、前端、共享邮件/搜索消费者均未改。搜索链路已有自己的重试/故障转存，不能把本批文章任务的恢复保证扩展到它。

### 9.2 实际验证与失败修正

环境：Windows，本机IDE自带Maven，JAVA_HOME为`D:/IntelliJ IDEA 2025.2.4/jbr`（JBR21，项目Java17目标）；不启动Spring应用，不连接真实MySQL/Redis/RabbitMQ/Qdrant，不请求模型/Embedding。命令在blog-springboot目录执行：

```text
mvn -q -Dtest=AiTaskExecutionServiceTest,AiTaskServiceImplTest,AiTaskDispatchServiceTest,AiArticleServiceImplTest,AiTaskSqlTest,ArticleVersionSqlTest,BlogMetricsTest,AiArticleConsumerTest,ArticleAiQueueConfigTest test
python docs/09-tooling/verify_ai_task_sql.py
```

44个不同Java用例通过：执行事务6、任务10、投递恢复9、文章处理7、任务SQL1、文章SQL4、指标3、消费者3、专用配置1。构建证据为Maven test对主/测试源码编译，没有package或可部署包验收。受控Spring事务管理器验证事务是否建立、顺序和回滚决定，不伪装成数据库行锁；SQLite真实执行XML语句和两连接竞争/显式提交回滚，仍不是MySQL隔离或行锁证据。

首轮36项中3处断言失败、1处测试错误：新增恢复场景投递13号任务碰到原测试桩固定12；两处Mockito重新when触发了要求事务内执行的旧Answer；历史回填独立运行缺Article的MyBatis元数据缓存。这些均修正测试隔离/桩后通过，未将首次失败隐去，也未为过测试放宽生产条件。复核另发现纯空白持久检查点会落入旧生成分支，已改成仅NULL允许首次生成；空串/空白/坏JSON/错误源均受控验证不调用模型。

后续只针对新改动补跑`mvn -q -Dtest=BlogMetricsTest test`（快照时间）和`mvn -q -Dtest=AiArticleServiceImplTest test`（坏检查点），不把重复执行计为新增用例。日志位于Git忽略的target/queue-q3-test.log、queue-q3-metrics-test.log、queue-q3-checkpoint-test.log；SQL脚本输出Q3 deadline fencing / two-connection expire-resume / checkpoint-only budget / UNKNOWN gate / atomic publication rollback-commit通过。没有压力吞吐、生产事故或真实费用证据。

### 9.3 本地阶段边界与残留

| 范围 | 当前依据/剩余边界 |
| --- | --- |
| 提交后唤醒、投递确认未知、PENDING恢复和重复消息 | Q1持久凭据/条件认领继续保留；真实broker确认/断线/ACK丢失未验证 |
| 检查点后的异常/失联接续 | Q3按DB期限、有限预算、旧token隔离和稳定向量点；一次外部调用可能在租约外继续并重复Embedding |
| 检查点前结果未知 | UNKNOWN可查、计入积压/年龄并阻断普通重跑；供应商核账/幂等与确认重跑接口尚未实现，不自动重新收费 |
| 摘要发布与任务完成 | 新短事务/行锁协议及受控回滚决定、SQLite事务证据；真实MySQL行锁、隔离/锁超时/死锁及跨进程竞争未验收 |
| 向量和公开问答 | Q2源身份/完整发布和问答前后校验保留；真实Qdrant响应未知/持久性、旧点topK挤占与物理清理、授权付费回填未验收 |
| 迁移与协议切换 | 本批无新增DDL；原01—06在当前配置库已于2026-10-26执行，不重复ALTER；旧next_dispatch_at不是租约，旧RUNNING/RETRYING需上线前逐行核查，不能混跑 |
| 观测与容量 | 持久状态数量/创建年龄/成功快照时间及有限事件已实现；指标采集、告警阈值、实际扫描成本、积压排空/端到端时延与入站预算无实测 |

因此Q1—Q3的文章持久任务本地代码与受控恢复范围已交付，P6-02队列整体仍进行中。缓存实际依赖/统一切换门槛仍保留；不能因代码/脚本测试通过转为“线上队列可靠性完成”或进入报名。

### 9.4 批次复盘、提交与接续

本次按bb69eb4/f5fc156之后相关任务/发布路径复盘，没有全仓重复审计。继续：先完成队列阶段Q4验收与必要缺口收口，重点是真实MySQL最终发布与到期回收争同一任务行、提交/ACK未知后的唯一业务结果、MQ缺失时的待办恢复、向量分批失败/响应未知和旧线程晚到；用隔离表/合成正文、受控模型处理器，不调用付费模型，不操作业务表或启停既有服务。先检查已有隔离依赖与授权范围；缺运行条件时保留未验证，不以重复单测替代。

下一代码/验证入口为`AiTaskExecutionService.finish/fail`、`AiTaskMapper.expireExecution/renewExecution`、`AiTaskDispatchService.recoverPending`和现有搜索故障转存的恢复入口；只在实际证据暴露缺口时补相关代码，不扩成AI平台或重建搜索队列。Q4需明确文章任务与搜索各自验收范围，再决定队列阶段是否满足进入报名条件。

推迟：报名/Redis预扣、向量清理和全库游标工具。取消：所有失联任务自动重跑、普通重试UNKNOWN、假设模型/Embedding费用一次、用单消费者替代数据库竞争保证。另保留用户关于真实Redis业务故障证据的要求：后续在隔离环境围绕撤回/旧回填和Redis故障/有界DB回源做成对故障与恢复演练，不宣称已发生生产事故，不为了故事制造无必要组件。

本地提交标题`fix: 补齐检查点任务租约恢复与原子发布`，本节与代码同一提交，可按标题定位；只提交本批文件，不合并/推送/部署。最近周复盘2026-10-26，本批于2026-10-27补充复盘；下次不晚于2026-11-02，重大新缺口提前更新。

## 10. Q4恢复饥饿修复与真实依赖/Java链路批次（2026-10-28）

### 10.1 业务触发、改前缺口与不变量

本批从Q3发布/回收路径进入，先核实已有远程依赖和本地配置。此前“环境没准备好/找不到”缺少充分依据：本次MySQL 5.7.36、Redis 6.2.6、RabbitMQ 3.9.11和Qdrant REST版本1.17.1均可访问。没有本机Docker不代表无法使用已有开发依赖；环境可连通也不能自动证明业务协议正确。

真实MySQL复现：100条到期但无检查点/已耗尽预算的RETRYING占据恢复SELECT的LIMIT100，正常第101条一直不被选中。每轮执行了扫描，业务仍无法恢复。不变量是“不合格的历史重试记录不能永久阻塞后续合格待办”，并保留“检查点前UNKNOWN不自动重跑、过期旧token不能发布、文章发布和终态同事务”。

修复在SELECT前置既有RETRYING接续条件，保留RUNNING分流，未新增中间件/表/状态，也未重置任何业务行。增大LIMIT只能推迟复现；无界遍历扩大DB成本；SQL解析检查点会增加协议耦合。因此只修候选筛选，坏检查点仍按既有处理边界拒绝。没有以单消费者替代数据库竞争保证。

### 10.2 文件与职责

| 文件 | 本批职责 |
| --- | --- |
| `src/main/resources/mapper/AiTaskMapper.xml` | 恢复候选排除不可接续RETRYING，保留到期RUNNING |
| `docs/09-tooling/verify_ai_task_sql.py` | 新增100条不合格记录及后续合格任务回归，保留UNKNOWN候选 |
| `docs/09-tooling/verify_queue_dependencies.py` | 显式主机/执行开关；唯一隔离库/vhost/集合，实际XML SQL与真实依赖场景，finally清理；可选实际Java入口，密码只经子进程环境传递 |
| `src/test/java/com/ican/service/impl/AiTaskDependencyIntegrationTest.java` | 默认跳过，仅接受唯一隔离schema；真实MyBatis、事务管理器/代理、投递服务、任务服务、消费者及专用AUTO工厂，处理器受控且不创建模型客户端 |
| `docs/06-deployment/07-队列隔离依赖验收.md` | 执行资源/权限、准确命令、证据等级、清理与运行记录 |
| 领域设计03第11节、Phase6计划v9、路线v9、开发约定/文档索引 | 协议修复、当前验收范围与下批接续统一 |

用户ArticleController.java既有改动未改写/暂存，homa-xxl-job未触碰。没有改pom或全局Python依赖；pika1.3.2放在Git忽略的target/q4-python。生产修改只有恢复SQL谓词；其他新增代码服务于本次必要的真实并发/故障验收，不运行付费AI。

### 10.3 实际时序、执行与结果

1. MySQL真实SQL显式回滚后摘要/发布凭据和任务均未完成；发布先持任务行锁，到期回收观察到InnoDB锁等待，发布提交后回收0行。反向回收先持锁，旧finish等待后无法取得所有权。两个连接接续同任务结果为0/1。无检查点到UNKNOWN、预算耗尽到FAILED，新连接读回已提交结果，重复认领0行。
2. 同依赖中的实际Java链路用真实事务代理与SqlSessionTemplate。隔离库触发器在任务SUCCESS更新时SIGNAL，文章UPDATE已经执行，最终整事务回滚，任务仍RUNNING；移除故障后成功发布。任务创建外层回滚时无持久行和消息；提交后独立预约，RabbitTemplate捕获mandatory无路由返回而保留PENDING。恢复绑定、仅使合成待办到期，实际扫描再投递，专用AUTO消费者收到2条同身份消息，只执行1次受控处理并发布。
3. 独立AMQP场景在数据库提交后关闭未ACK连接，新连接收到真实redelivered；重复消息不能再认领已完成任务。成功发布回复刻意忽略后重发，验证重复安全，不声称确认报文真实丢失。没有停止broker、清现有队列或修改服务端配置。
4. Qdrant独立集合使用合成2维向量和同源稳定UUID：10点部分批次中断，完成余3点/忽略回复后重写仍13点；新源13点后旧源晚写，总26个隔离点。不是实际Embedding/问答或gRPC Java证据。
5. 改前100条不可接续记录确实挤掉后续合格任务；改后正常候选入选并接续1条。该百行集EXPLAIN显示ALL/Using where/Using filesort，不将LIMIT返回上限当作扫描成本上限或性能提升数字。

实际命令（工作目录blog-springboot）：

```text
python docs/09-tooling/verify_ai_task_sql.py
mvn -q -Dtest=AiTaskSqlTest test
python docs/09-tooling/verify_queue_dependencies.py --execute --expect-host 121.41.87.40
python docs/09-tooling/verify_queue_dependencies.py --execute --expect-host 121.41.87.40 --java
```

前两项通过；AiTaskSqlTest为1项、0失败/错误/跳过，Maven编译主/测试源码，未package/全量测试。纯协议成功运行`blog_q4_f7501264014349e8a100d623c67208df`，11个场景检查；追加Java后运行`blog_q4_c9907f232a46462492e3ec958131ce8c`，2026-10-28本机11:24:37—11:25:03，11个协议场景和1个Java整合方法通过，后者JUnit 1项/0失败/错误/跳过。没有把每个内部断言或重复运行累计为新增测试。Java使用本机JBR和IDE Maven，临时设置JAVA_HOME/PATH，脚本未硬编码IDE路径。

证据在Git忽略`logs/queue-acceptance/<运行名>.json`和`<运行名>-java.log`，Java报告在target/surefire-reports。三个独立资源finally删除均成功，随后用新连接/管理接口只读核实本次精确资源名均不存在。未向blog业务表写数据，原01—06迁移执行事实不变，无新DDL作用于业务库、部署、服务启停或付费调用。

首次运行`blog_q4_3129132a2b16432ab04f020eecdf2f1f`故意复现扫描饥饿并以失败退出。反向锁验证初版没有等工作连接就绪，观察窗口内未发现锁等待而断言失败；改用连接begin完成后的Event同步后，两向锁等待均实测通过。未改生产锁/事务协议以迁就验证器，失败日志保留，不把先前失败隐藏为全程成功。

### 10.4 阶段边界、复盘与接续

Q4本批交付的是扫描活性缺口修复与有限真实依赖/实际Java证据。尚未完成：完整应用/HTTP配置装配、真实模型及Embedding、Qdrant Java/gRPC/RAG运行、多进程调度、broker重启/磁盘故障、真实confirm丢包、数据库故障/死锁恢复、代表性积压排空/尾延迟/扫描成本、指标采集及统一新旧协议切换。不能据此写成线上事故解决、生产精确一次或吞吐提升。

搜索链路核查：现有ArticleConsumer整批校验后逐ID同步，专用3次重试及带confirm故障转存已存在；运维05中的重放目前是手工协议说明，并没有可直接运行的受控重放客户端/API。AI待办恢复不能覆盖搜索故障副本。下一集中批次从ArticleSearchSyncConfig、ArticleConsumer、运维05进入，在新隔离vhost/合成批次中验证转存确认/失败和修复后的重放，再明确队列阶段验收边界；不读取或回放现有错误队列，不改成第二套平台。ES当前状态快照并发晚写仍独立保留，不因MQ重放通过自动宣称版本一致。

继续：现有队列故障恢复的必要收口、随后隔离Redis撤回/旧回填和故障有界回源演练。推迟：报名直到队列边界明确，真实负载等有代表性环境/资源预算后开展，旧点清理/全库回填等数据证据。取消：无检查点自动重跑、盲加索引/扩大LIMIT、基于连通性就称联调完成、以文档/无变化全量测试代替实现。最近周复盘2026-10-26，本批复盘2026-10-28，下次周复盘不晚于2026-11-02；新重大缺口提前调整。

本地提交标题`fix: 修复任务恢复扫描饥饿并验证隔离依赖链路`，本节与代码同提交，按标题可定位；仅本批文件，不合并/推送/部署。

## 11. Q4搜索故障受控重放客户端（2026-10-29）

### 11.1 业务触发、不变量与实际开发

用户指出上一批主要是“一个问题修复和之前开发验证”，要求继续交付实际能力。本批交付新的可执行恢复入口：ES/DB故障解决后，操作员可以将保留的文章故障批次按小批次重新送入原消费链。改前只有运维05的手工协议，没有可运行客户端；已有3次重试/故障转存不重建。业务不变量是“未确认成功重入MQ之前，不确认移除故障来源”，而不是测试通过或新增状态字段。

正常：默认被动计数→明确主机/vhost/execute→逐条手动ACK获取→整批文章主键校验/审计→持久mandatory发布确认→审计确认→发送源ACK。失败：坏消息/return/NACK/连接未知/审计失败即停止，关闭连接恢复未ACK源；恢复：修路由/依赖后同客户端重入原链；重复：确认后ACK前中断可重复，原消费者按ID重读当前事实；并发/乱序：不宣称basic_get可排他，也不解决旧快照向ES晚写，保留原公开权威屏障。确认发布只代表重入MQ，索引完成必须另读回。

采用已有pika、一次性CLI和固定目标拓扑，保留原body/错误头，只省略旧expiration/user_id；消息ID/哈希供追溯不做伪去重。未采用自动确认导出、直写ES、无界重放、通用平台、无认证管理端点或定时自动回灌，避免新增丢失窗口、费用和无关装配。执行默认1条、最多100条，父子进程120秒期限与逐阶段fsync审计一起提供明确的操作边界，不把socket_timeout当发布确认总期限。

### 11.2 改动文件与职责

| 文件 | 职责 |
| --- | --- |
| docs/09-tooling/replay_article_search.py | 实际运维客户端：默认只计数、显式目标/执行、整批校验、确认后ACK、有界停止、审计与监督期限 |
| docs/09-tooling/test_replay_article_search.py | 6项直接相关离线边界回归，假channel不连接任何服务 |
| docs/09-tooling/verify_search_replay.py | 只创建新Rabbit vhost、运行搜索实际Java链/CLI，追加确认后ACK前中断场景并独立清理读回；本批未成功进入远程场景 |
| src/test/java/com/ican/config/ArticleSearchRecoveryIntegrationTest.java | 显式新vhost才运行，实际专用重试/恢复/消费者和CLI；ES/DB事实受控，默认跳过 |
| 运维05、运维07、领域设计03第12节 | 客户端用法、协议/保证、真实运行失败和待验收范围 |
| 路线v10、Phase6计划v10、日常约定v9、README | 当前状态/接续入口一致，避免继续写“只有手工协议” |

没有修改生产Java消费/恢复协议、pom、业务DDL/数据或既有配置；用户ArticleController改动保持未暂存，homa-xxl-job未触碰。本批生产能力是恢复客户端，验证代码是配套，不将预备整合场景记作已完成能力。

### 11.3 实际验证、失败与未验证

工作目录blog-springboot：

```text
python docs/09-tooling/test_replay_article_search.py
mvn -q -Dtest=ArticleConsumerTest,ArticleSearchSyncConfigTest,ArticleSearchRecoveryIntegrationTest test
python docs/09-tooling/verify_search_replay.py --execute --expect-host 121.41.87.40
```

离线6项通过：确认→ACK、body/错误头/持久属性与去旧TTL、坏尾行整批拒绝、return/NACK/连接未知不ACK、确认后审计失败保留源、默认只计数及120秒监督（部分在同一方法中验证，不拆成虚增用例）。Java主/测试编译通过，ArticleConsumerTest 9项、ArticleSearchSyncConfigTest 7项通过；新整合1项默认跳过，不能记为联调通过。没有package/全量测试，也没有重跑昨日无变化AI链路。初次本地Maven命令仅传子进程PATH但用了未解析mvn.cmd，Windows CreateProcess未找到命令；改用shutil.which(path=子进程PATH)解析绝对路径后通过，未更改系统PATH/JDK。

远程新入口运行`blog_search_ee59e59e8a4d40c9a080a26022b7021e`（本机2026-10-29 10:01:56—10:02:04）在只读Rabbit管理API查询超时，URLError/TimeoutError；未创建vhost、未运行真实Java/确认间隙场景。随后本机到配置相同目标5672和15672独立TCP探测均TimeoutError。证据保存在Git忽略logs/search-replay/<运行名>.json，无凭据/正文。这是当时网络到两端口不通的观测，不是推断服务停止、配置失效或环境不存在；昨日可用事实保留，未启停服务/改网络或消费现有故障消息。

未验证：新客户端/搜索专用恢复的真实Rabbit整链、真实ES/DB同步与refresh、真实confirm丢包/持久化崩溃、多实例旧快照覆盖、代表性积压恢复/尾延迟。新Java用例使用受控索引边界，即使未来通过也不能扩大成真实ES联调。审计写盘故障/ACK未知可能无法给出完整最后阶段，必须核查MQ和持久日志，不能承诺精确一次。尚无业务库修改、生产部署、付费模型或旧故障队列回放。

### 11.4 复盘、阶段边界与下一入口

继续：网络恢复后只运行新的verify_search_replay入口，按失败证据修正必要缺口并收口队列本地阶段验收；不重复旧AI单测/真实依赖套件。下一代码入口仍为客户端replay的发布/源ACK边界与ArticleSearchSyncConfig故障副本确认，预备场景未实际执行前保持未验收。不反复仅检查环境，若端口持续不可达，保留阻塞事实并推进不依赖网络的必要实现。

推迟：报名直到搜索恢复关键路径明确；完整部署/统一切换、服务崩溃、多进程/负载、Redis故障演练作为分开的验收门槛，不把所有生产级演练当进入新功能的前提。取消：以重复测试/文档代替开发、自动清队列、坏消息自动重写/跳过、确认发送等于索引成功、无检查点付费重跑。旧索引快照覆盖有独立风险，不因此次CLI完成就写成解决。缓存→队列→新功能顺序保留；当前Q4及P6-02仍部分完成。最近周复盘10月26日，本批10月29日，下次周复盘不晚于11月2日。

本地提交标题`feat: 增加文章搜索故障消息受控重放客户端`，与本节同提交；仅本批文件，不合并/推送/部署。

## 12. 2026-10-29业务开发接续决定

用户再次明确不接受只测试。Q1—Q3队列代码、Q4缺口修复和搜索受控恢复客户端作为已交本地基线保留；新搜索MQ整链/ES并发旧快照与部署/故障验收仍未完成，不把这些事实改成通过。按照最新要求已交P6-03/A1限量活动报名后端，具体见[报名总结15](15-Phase6-限量活动报名开发总结.md)，下一开发前后台活动/报名流程，不再把旧链路验收作为唯一日常工作。该决定区分实现进展与运行验收，未完成风险仍记录，不是队列生产闭环声明。
