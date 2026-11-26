# Phase6-限量活动报名开发总结

## 1. A1交付与方向修正（2026-10-29）

用户指出“为什么还是只测试，我不用开发了吗”。最近两批虽补了问题/运维恢复工具，但业务功能推进不足。本批沿已批准的限量技术活动方案，实际新增活动管理、公开活动接口、登录报名受理/结果/取消、名额数据库确认、独立MQ持久唤醒/扫描恢复和管理失败恢复。不是再次测试旧AI/搜索链路，也没有搭另一个通用平台。缓存/队列本地代码基线与剩余真实部署/故障验收明确分开；P6-02完整运行验收仍开放，最新用户要求优先实际开发，不再把所有生产演练视为转阶段前置。

场景：管理员发布100名额的技术交流，读者集中报名；接口先给已受理，后给确认/售罄，断消息不丢持久请求，同用户重复不多占，取消与确认竞争不多释放。100仅业务样例，无运营/负载数据。不变量及全部接口见[领域设计04](../01-架构设计/04-限量技术活动报名设计.md)。

## 2. 代码与取舍

| 文件 | 本批职责 |
| --- | --- |
| entity/Activity.java、ActivityRegistration.java | 活动容量/时间和唯一报名身份、持久待办/结果/投递代 |
| dto/ActivitySaveDTO、ActivityRegistrationDTO、ActivityRegistrationMessage | 管理输入校验、纯requestId报名输入、带投递代唤醒 |
| vo/ActivityVO、ActivityRegistrationVO | 对外活动与本人/管理结果；不返回用户身份、内部错误/token |
| mapper/ActivityMapper.java、resources/mapper/ActivityMapper.xml | 唯一插入/当前锁读、活动条件容量/待办、按代恢复/失败和分页 |
| service/impl/ActivityService.java | 草稿/编辑/发布、公开列表、受理去重、本人/管理查询 |
| service/impl/ActivityRegistrationExecutionService.java | 请求→活动固定锁序下确认、取消、独立失败记录和人工恢复 |
| service/impl/ActivityRegistrationDispatchService.java | afterCommit后台唤醒、独立预约、扫描/退避/confirm观测，不跨网络持锁 |
| config/ActivityRegistrationConfig.java、consumer/ActivityRegistrationConsumer.java | 独立拓扑/AUTO/prefetch1、1线程100等待发送池、失败分流，默认不启用 |
| controller/ActivityController.java | 11个实际业务端点，认证本人/管理权限、复用活动级Redis限流 |
| docs/07-数据库变更/07-限量技术活动与报名建表.sql | 新2表/索引/7条管理菜单按钮声明，未执行，不自动分配普通角色 |
| ActivityRegistrationServiceTest、verify_registration_sql.py | 仅新业务直接相关行为/SQL与容量事务边界验证 |
| 领域设计04、路线/Phase6计划/约定/文档索引、本总结 | 业务规则、API、失败恢复、启用与尚未验证证据 |

采用持久PENDING作待办和MySQL条件余量作最终裁决，MQ削峰后段容量竞争；请求表不复用AI任务。未用Redis预扣裁决、独立outbox平台、支付/候补/活动关闭扩展。报名每次仍需DB插入和活动待办预算UPDATE，初版存在热点，不虚称完全异步无DB瓶颈或任意吞吐。

## 3. 正常、失败、恢复、重复与并发时序

受理：登录认证取得userId，活动100次/秒保护→唯一身份插入→窗口内/已发布/pending<1000条件加待办，同提交后后台投递，返回PENDING。重复唯一冲突用当前锁读返回同一身份；跨用户/活动复用requestId拒绝。预算失败回滚新行，不发有效消息。

确认：锁匹配PENDING/token请求→活动余量>0条件扣减与待办-1→写CONFIRMED，同提交/回滚；无余量则待办-1与SOLD_OUT。取消同锁序：PENDING只减待办、CONFIRMED只回补一次容量，再转CANCELED。已售罄/取消不自动候补或重新报名。接收窗口结束后原已受理请求仍处理，没有绝对先到先得保证。

投递/故障：提交后仅enqueue有界线程，不在HTTP线程发送；预约独立REQUIRES_NEW提交token/30—300秒退避再发MQ，扫描每5秒最多100个候选，多实例条件预约。confirm不移除PENDING，重复/旧投递不能扣库存；提交未知重新看当前状态/token，记录失败到3转FAILED并释放待办预算，管理员可按原身份恢复。记录失败也遇DB故障时仍有原PENDING，不依赖DLX副本可靠性；终态已提交后错误不转成失败或补库存。扫描上限不是查询成本保证，MQ发送阻塞不被10秒future取消，待办上限1000为初始预算，需真实容量证据后定容。

默认开关关闭，避免尚无07表时启动新scanner/consumer或声明活动拓扑；待迁移和配置核对后启用。没有覆盖用户ArticleController、homa-xxl-job，未改AI/PDM/其他参考目录、既有队列协议、pom或线上数据，没有服务启停/部署/付费调用。

## 4. 实际验证与边界

工作目录blog-springboot，IDE自带Maven/JBR只作为本机本次进程环境，不改系统环境：

```text
mvn -q -DskipTests compile
mvn -q -Dtest=ActivityRegistrationServiceTest test
python docs/09-模板与脚本/verify_registration_sql.py
```

编译通过，直接相关Java 7项/0失败/错误/跳过，包含XML真实解析、受理后唤醒、身份冲突、拒绝无唤醒、旧投递不扣、取消及旧错误不影响新代/终态等分支。没有启动整个应用，没有重跑旧AI/搜索套件或全量测试。日志在Git忽略logs/activity-build.log、activity-tests.log；JUnit在target/surefire-reports/TEST-com.ican.service.impl.ActivityRegistrationServiceTest.xml。

实际mapper的条件扣减/释放/结果SQL在临时SQLite（仅NOW方言替换）验证：两用户最后名额结果一成功一售罄；重复已确认不再次扣；确认后取消恢复一次；任务终态触发器故意失败时容量和待办/状态一起回滚；PENDING取消不加库存；活动用户唯一约束拒绝第二身份。SQLite采用BEGIN IMMEDIATE写者串行，不是MySQL行锁证据，临时表是所需列/约束合成结构，不冒称执行正式MySQL迁移。首次脚本业务断言已通过但Python SQLite context只提交未close导致Windows临时清理失败；修正closing后完整通过并清理本次精确残留路径，没有改生产代码迁就验证。

未验证：正式迁移07、管理角色权限分配/会话刷新、完整应用装配/HTTP、真实MySQL唯一冲突/行锁/回滚及锁预算、活动MQ/Redis链路、跨进程调度/消息未知、代表性请求/积压/尾延迟/DB成本。10月26日已执行01—06不等于07执行。此前Rabbit连接失败保持历史，不再次把环境问题作为停止实际代码开发的理由。本批没有连接/操作真实业务库，07也未授权为无条件上线动作。

## 5. 开发复盘与接续

A1为后端代码首版完成、运行验收未完成，不是完整秒杀系统。继续：以前后台真实可用流程为下一集中开发目标，活动列表/详情、报名状态轮询与重复/取消、后台草稿发布/失败恢复页面接入这些接口；随后迁移/配置就绪时跑最小新业务真实链路。旧搜索真实恢复和缓存依赖故障/统一切换独立列出，不无限扩测试阻断新业务。若页面接入暴露API缺口直接补，而不是先写一轮新报告。

推迟：Redis Lua预扣，等基线可用和容量压力证据；支付/候补/重报/开始后改容量等另有业务需求再定。取消：继续只测试旧链路、虚构QPS/上线经历、简单Redis扣减→MQ伪闭环、未经核查启用新功能或重跑旧迁移。每个切片继续维护本总结，不每日复制。最近周复盘2026-10-26、本批2026-10-29，下次周复盘不晚于2026-11-02。

本地提交标题`feat: 实现限量技术活动报名后端业务链路`，本总结和代码同提交，按标题定位；仅本轮文件，不合并/推送/部署。

## 6. A2前后台实际业务开发（2026-10-30）

本批把A1的业务端点接成可操作的前后台流程，非旧链路重复测试。公开活动列表/详情、登录报名、持久请求号、有限结果轮询、取消、后台草稿/编辑/发布、报名状态/失败恢复均已实现。新后端端点没有缺口，沿用既有11个，不新增平台/全局配置/支付/候补。A2前端代码范围完成，P6-03完整运行验收仍未完成。

| 文件 | 职责 |
| --- | --- |
| shoka-blog/src/api/activity/index.ts、types.ts | 公开5类请求/分页/结果状态类型、北京时间解释和状态标签 |
| shoka-blog/src/router/index.ts、menu.ts | 列表/详情路由及动态导航入口，保留既有导航 |
| shoka-blog/src/views/Activity/index.vue、detail.vue、activity.scss | 公开列表分页、详情、加载/错误/重载、窄屏与主题；17px正文、现有青绿/暖色小幅加强，不重设全站 |
| shoka-blog/src/views/Activity/RegistrationPanel.vue、useRegistration.ts | 本人报名交互；按账号/活动持久UUID、未知响应接续、20次串行轮询、取消、异步响应代及KeepAlive生命周期 |
| shoka-admin/src/api/activity/index.ts、types.ts | 管理活动与报名恢复API/输入输出类型 |
| shoka-admin/src/views/activity/index.vue | 活动管理列表、开始前编辑/发布、权限按钮和刷新 |
| shoka-admin/src/views/activity/ActivityForm.vue | 受校验的草稿/编辑表单、ISO LocalDateTime、未知保存防重复提交 |
| shoka-admin/src/views/activity/RegistrationDrawer.vue | 按状态分页查报名、FAILED原请求恢复/确认/刷新 |
| docs/07-数据库变更/07-限量技术活动与报名建表.sql | 修正未执行菜单为可见C路由和5个权限按钮，总6条，绑定现有动态路由模式 |
| 领域设计04、前端接入02、路线/计划/约定、本总结 | 业务边界、失败/接续、真实验证和迁移未执行状态 |

正常路径：匿名看列表/详情→既有登录弹窗→先查本人→持久UUID→提交→PENDING等待→有限查询CONFIRMED/SOLD_OUT等；已接受请求即使窗口结束仍可继续查。失败路径：加载失败保留明确重载入口；受理/取消响应未知先查当前事实，原身份保留；FAILED不开放新报名，由后台恢复。取消先确认不可重报；登出/换账号清本人展示、忽略旧请求返回。并发路径：按钮busy和响应sequence只防客户端误操作，最终跨窗口/跨实例幂等仍靠MySQL。KeepAlive离开不等于卸载，本批显式在deactivated停轮询/计时、使在途响应失效，回到页面重新查。

没有引入全局报名store或通用轮询框架，业务状态只在页面所需组合函数中维护。说明按纯文本渲染，不给活动说明增加未经清洗的HTML；时间统一标北京时间，浏览器时间只提示窗口，后端仍裁决。余量为详情/列表读取时快照，不能承诺实时保留；无库存自动预扣。后台创建暂无服务端幂等键，网络未知禁止直接在原表单重按保存；关闭后须先刷新核对已有草稿。

## 7. A2实际验证、执行尝试与未验证边界

构建（两前端各自工作目录）：

```text
shoka-blog: npm.cmd run build
shoka-admin: npm.cmd run build
shoka-admin: npx.cmd vite build
shoka-blog: npx.cmd vue-tsc --noEmit
```

最终公开Vite构建与管理Vite单独构建通过，存在既有大包警告。管理标准build在vue-tsc步骤返回1（28条，collab/doc、tool/book、web/photo等未改路径），公开类型检查返回1（37条，Archive、Music、Collab、Life等未改路径），本批活动API/页面路径均无诊断；不扩大为全项目编译/类型全面通过，不顺手修这些旧模块。日志：Git忽略logs/activity-public-build.log、activity-admin-build.log、activity-admin-vite-build.log、activity-public-types.log。没有重跑后端/AI/搜索无变化测试。

浏览器：按development-workflow、karpathy、vue-best-practices、impeccable及playwright约定复用实际页面与API客户端，只启动本轮专用127.0.0.1:5183/5184 Vite预览（open:false），所有localhost:8080请求由Playwright合成处理，其他外网请求全部阻止。运行`npx.cmd --yes --package @playwright/cli playwright-cli -s=blog-activity-a2 run-code --filename=logs/activity-ui-*.js`及snapshot/click。实际通过：受理POST回应被故意丢弃后localStorage保留同UUID、刷新读取PENDING、离开KeepAlive至少3.5秒无后续本人GET、返回重查、CONFIRMED显示成功、确认取消后无重报、登出清旧结果；后台创建请求ISO时间正确、PUT编辑、POST发布和FAILED原requestId恢复路径；390宽无水平溢出、列表失败后重载、暗色标题跟随正文色。20次预算/全部终态呈现为代码边界，未完整等待20次、未声称覆盖所有分支。

截图检查修正了暗色活动标题受全局a样式影响和手机底部详情入口与既有AI浮钮重叠。首次截图遇页面入场动画半透明，截图只是当时帧，不作为对比结论；CSS热更新后立即取色一次早于生效，稍后重取标题/正文均rgb(170,170,170)。合成运行中外部Live2D/CDN被阻止，引发已有全局装饰报错；没有当作报名业务通过或顺手改全站装饰。截图及命令记录保存在本机Git忽略logs/activity-ui/。所有预览/浏览器仅本轮新建，结束后关闭，未启停已有服务。

迁移：依据用户“将没有运行的脚本执行”的持续授权，只尝试07前置只读核查，核对配置解析目标为121.41.87.40:3306/blog；PyMySQL connect_timeout=5返回2003/timed out，连接尚未建立，执行SQL=0、没有备份或DML/DDL。记录logs/activity-migration07-attempt.json；没有盲重跑01—06，没有把当次连接超时说成服务器停机/环境不存在。07仍未执行，新业务仍默认关闭，没有权限分配/启用/部署、真实业务写入或付费调用。

未验证：真实登录会话与服务端HTTP完整装配、迁移07/角色与动态菜单读回、新活动MySQL/MQ/Redis事务和消息恢复、跨进程/积压/尾延迟/DB成本。页面的受控响应不能证明真实抢号成功、服务可运营或吞吐改进；不以本批UI工作宣称缓存/队列历史门槛通过。

## 8. A2复盘与下一入口

继续：A2业务页面已交代码，下一集中目标是把报名链路跑通并交付必要修正：仅核对一次实际目标/可达性，07执行前备份t_menu并核对新表/菜单不存在或精确匹配，避免IF NOT EXISTS掩盖不兼容；按已有授权执行后新连接读回。权限分配/开关/完整应用运行分别记录，未经授权不启停或部署现有服务。依赖可用时以合成活动在隔离资源验证A1+A2最小受理→确认→取消与原请求恢复；无连接时保留阻塞，不把每天重查端口当工作。

推迟：Redis资格预扣，直到基线运行且有DB热点证据；旧点物理清理/topK、旧搜索整链、缓存故障与统一切换仍独立开放，不反复全套回归阻断新业务。取消：重复实现A1/A2、为页面重建全局框架或篡改无关类型错误、伪造生产事故/性能数字。新业务代码边界明确，下一不继续纯样式或重复测试；若新业务联调确实不可执行且无证据支持功能缺口，准确保留状态。

本批复盘2026-10-30，下次周复盘不晚于2026-11-02。普通本地提交标题`feat: 接入限量活动前后台报名管理流程`，本总结与代码同提交，按标题定位；只提交本轮文件，ArticleController用户修改不纳入，不合并/推送/部署。

## 9. A3恢复实际开发、迁移与消息积压修正（2026-11-23）

### 9.1 业务触发、缺口与方案

11月19日用户恢复任务后只更新自动化，后续日期确认未执行开发；本批明确纠正，按A2下一入口实际完成07迁移，补报名运行链路的源码缺口，不以ACTIVE或日期回复冒充交付。没有重做A1/A2、旧AI/搜索全套或前端样式。

正常场景仍为持久PENDING→MQ唤醒→数据库CONFIRMED/SOLD_OUT，取消/失败恢复维护同一身份与容量。发现两个直接缺口：①扫描预约每次换token，broker积压超过重投间隔时，已排队消息可能不断被作废；②实际MySQL会话SYSTEM/服务器UTC，前端输入北京时间LocalDateTime，原NOW(6)窗口/到期可能偏移8小时。两项是源码与实际时区事实，不伪造生产事故或性能提升。

采用：同一处理轮保留持久token，预约和读token同一REQUIRES_NEW事务；每次发送使用独立confirm相关ID。正常迟到消息仍有资格处理，重复由请求行锁/终态挡住；失败清token，后续新代拒绝上一失败代晚到。固定activityNow SQL片段统一本域窗口/持久时间/投递退避为数据库UTC+8；JDBC LocalDateTime契约不变。不采用猜更长退避、删除token、全局改数据库/共享连接时区或新增通用队列平台。新表执行后为空，无历史活动时间转换。

### 9.2 本批文件职责

| 文件 | 职责 |
| --- | --- |
| ActivityRegistrationDispatchService.java | 预约锁内取持久代，重投保留处理代，每次confirm使用独立ID |
| ActivityMapper.xml | reserveDispatch用COALESCE保留代；活动域全部业务时间引用固定北京时间数据库表达式 |
| ActivityRegistrationServiceTest.java | 调整既有窗口SQL断言，保留7项直接相关行为/XML验证 |
| ActivityRegistrationDependencyIntegrationTest.java | 显式隔离库/vhost才启用；真实MyBatis/事务代理/专用投递/AUTO消费者，合成报名验证 |
| verify_registration_dependencies.py | 凭据沿既有解析；创建唯一隔离2表结构与vhost、运行对应Java、finally清理/读回；不启动完整应用 |
| verify_registration_sql.py | 原SQLite验证器适配实际mapper时间include；仍只声称SQLite证据 |
| 迁移07/运维06 | 只调整SQL注释契约，记录已执行事实、备份、菜单/结构读回；不重跑旧迁移 |
| 设计04、路线/计划/约定、本总结 | 当前协议、阶段状态、实际命令/失败/边界与下一入口 |

### 9.3 实际迁移与验证

目标121.41.87.40:3306/blog恢复可达，MySQL5.7.36。已按持续授权执行07：新增t_activity/t_activity_registration空表，菜单115→121，原115行逐字段不变，仅新增ID145—150的活动C菜单/5按钮；不分配角色。完整备份/逐语句记录及新连接结构/空行读回在`logs/database-backups/activity-migration07-20261123-081557/`，运维06第6节详述；01—06不重跑。应用开关仍默认false，没有真实活动/报名数据或其他业务表写入。

工作目录blog-springboot，本次使用IDE自带Maven/JBR，仅设置子进程JAVA_HOME/PATH：

```text
python target/execute_activity_migration07.py
mvn -q -Dtest=ActivityRegistrationServiceTest,ActivityRegistrationDependencyIntegrationTest test
python docs/09-模板与脚本/verify_registration_dependencies.py --execute --expect-host 121.41.87.40
python target/verify_activity_a3_sql.py
python docs/09-模板与脚本/verify_registration_sql.py
git diff --check
```

最终Java运行`blog_activity_c3e8094380324dcc852772e827c75017`：既有相关7项+新整合1方法，8项/0失败/错误/跳过，包含主/测试编译。实际MyBatis和事务代理验证UTC/+08会话相同北京时间输入与持久时间、开始前拒绝受理；活动创建/发布，受理外事务回滚无请求；mandatory无路由保留PENDING，修复绑定后扫描重发；人为将该合成请求的退避设为到期模拟积压，2条消息保留同token，专用AUTO消费后确认一次；另一用户得到SOLD_OUT；重复取消回库存一次，取消后新UUID仍返回原身份；触发器阻断CONFIRMED验证库存/待办/结果同回滚；3次独立记录失败释放待办，人工恢复仍原requestId，新token拒绝旧失败代、最终确认。此方法是一个整合用例，不能将每个断言重复计数。没有生成模型/Embedding客户端。

实际XML静态SQL另在唯一隔离MySQL库`blog_activity_sql_b22f4278f3814598a41fcb8a84a3b5ae`验证4组协议：UTC/+08窗口和持久时间、超时重投token不变、两个InnoDB连接竞争最后名额一确认一售罄及重复处理、取消回补一次；不是HTTP压测。临时脚本在target，仅作本机证据不提交；记录在logs/activity-acceptance。原SQLite入口适配include后通过，不将其充当真实MySQL证据。

实际失败及修正保留：首次旧SQL断言受MyBatis include空白影响而失败，规范化断言空白后既有7项通过；新runner一次因函数内import urllib.error触发局部变量遮蔽，在访问网络前失败，修正为模块级导入。3轮真实Java初始catalog连接超时（1bd4…、6a34…、fc348…），均清理读回；独立Java连接information_schema成功，隔离新库直接认证的PyMySQL也曾同样超时，不能报告服务停机或密码错。实验观察先认证information_schema再setCatalog到唯一隔离库可用，测试数据源以此选择隔离目标，不修改生产数据源或全局配置，不推断服务端根因。之后Java已进入真实业务，但测试用get(counter)在异步指标尚未注册时立即抛MeterNotFound（150af…）；改为find+断言存在使等待遵守异步契约，最终整链通过。没有改生产代码掩盖连接失败，所有失败报告保留，不重复算通过数。

全部本轮隔离MySQL库/vhost在finally删除，独立新连接/HTTP404读回不存在，最终report.json记录deleted_verified。只在隔离vhost创建/消费活动队列；没有访问原业务消息、启停既有服务/部署/付费调用。未提交私有配置、日志、备份或用户ArticleController修改。

### 9.4 阶段边界、复盘与下一入口

A3是实际数据库迁移+两处生产业务代码修正+新报名最小MySQL/MQ链路通过，P6-03仍部分完成。尚无完整应用/真实HTTP登录和SaToken权限/Redis限流装配证据、角色菜单分配/会话刷新、功能显式启用或部署；没有代表性负载/尾延迟/积压定容、多进程扫描、broker崩溃/磁盘故障或ACK真实丢包，不能称报名已上线或性能优化已达标。UTC+8是活动域现有北京时间契约，其他旧域时区没有顺手修改。

继续：下一集中入口为ActivityController的实际HTTP认证/权限/RateLimit与前后台客户端接续，补完整应用装配暴露的必要缺口；只用隔离合成活动，不自动授予真实角色或启用生产功能。已验证的本轮事务/MQ协议不无变化重复全套。Redis资格预扣仍推迟至基线可用且有DB热点证据，旧缓存/搜索/统一切换等门槛独立开放；取消为日报凑数的日期确认、反复端口核查和纯测试轮次。暂停期间无开发不补造历史复盘，本次只针对报名相关变化复盘，下次周复盘不晚于2026-11-30。

本批普通本地提交标题`fix: 修复报名积压重投与时区并完成迁移接续`，本总结与代码同提交，按标题定位；不合并/推送/部署。
## 10. A4 HTTP入口修正与本轮交付（2026-11-24）

### 10.1 业务触发、缺口与实现

用户连续要求恢复开发、说明每日新增，执行却再次停在环境/日期确认。这属于执行失误，既有委托始终要求推进实际代码；不能用更新任务描述替代交付。本轮沿A3指定的ActivityController入口完成HTTP契约修正，没有重做迁移或旧AI/搜索验收。

正常入口仍为登录身份→活动级保护→持久受理，PENDING不等于确认；管理恢复必须有对应权限并保留原标准UUID。发现UUID.fromString接受短分组输入，恢复接口与报名DTO约束不一致；此外坏JSON、非整数路径和方法参数约束没有统一的参数错误，兜底异常直接返回e.getMessage，可能向客户端暴露数据库/中间件细节。

| 文件 | 本批职责 |
| --- | --- |
| src/main/java/com/ican/controller/ActivityController.java | 恢复接口先校验标准UUID，再规范小写，避免宽松解析创造另一种输入身份 |
| src/main/java/com/ican/handler/GlobalExceptionHandler.java | 路径约束/类型与请求体解析错误统一Result400；系统异常固定对外消息、服务端保留堆栈 |
| src/test/java/com/ican/controller/ActivityHttpContractTest.java | 独立回环HTTP装配实际认证/权限拦截、MVC校验和限流切面，业务/权限/Redis受控，无远程数据 |
| 领域设计04、前端接入02、路线/计划/日常约定、本总结 | 更新入口协议、当前执行事实、任务继续规则、验证边界和下一入口 |

复用MVC异常处理器而不新增错误平台或前端协议。保留已有业务异常消息及Result字段/HTTP200契约；合法大写UUID仍恢复原小写身份。新系统错误文案适用于共享处理器覆盖的其他接口，未做全站回归；不把已有IllegalArgumentException业务分支也称为全部脱敏。没有UI/数据库协议/消息ACK和库存逻辑改动。

### 10.2 正常、失败、恢复与并发边界

公开读取不要求登录，匿名报名402；普通读者访问管理接口被拒绝，伪造body.userId不能覆盖会话身份。受理成功仍返回PENDING。非法路径/JSON/身份在业务写入前拒绝；限流拒绝或Redis故障不进入submit，错误对外不包含本次合成依赖原文。有权限恢复校验标准身份后调用原retry，不改其失败预算/投递代。

运行中系统错误或网络未知不代表事务未提交，前台A2的先查本人/保留requestId规则保持；不增加自动新号、自动提交或重复补偿。HTTP入口没有建立新的并发保证，本轮没有新增锁/容量算法，已有A3的真实MySQL/MQ竞争和回滚证据独立保留，不在本轮重复计数。

### 10.3 实际命令、结果与失败

工作目录blog-springboot，使用既有IDE Maven/JBR，仅本次子进程设置JAVA_HOME/PATH：

```text
mvn -q -Dtest=ActivityHttpContractTest test
git diff --check
```

最终主/测试编译通过；3个HTTP测试方法，0失败/错误/跳过。实际启动本轮专用127.0.0.1随机端口Tomcat，HttpClient发送真实HTTP，装配现有SaServletFilter、SaInterceptor、ActivityController、方法/DTO校验、RateLimitAspect和全局异常处理器。验证活动级键api:activity:register:1及100/1秒参数、匿名/权限隔离、会话身份、PENDING响应、标准UUID恢复、坏路径/JSON、限流拒绝/Redis故障不受理和系统错误不透传原文。测试结束DirtiesContext关闭本轮服务器，未启停已有服务。

业务服务与权限提供器采用受控替身，StringRedisTemplate也是替身；SaToken采用测试内存会话。不是实际Redis Lua容量/TTL、数据库角色会话、完整应用/审计异步写库、前后台浏览器到真实DB/MQ的同次整链证据。没有连接业务库、重跑01—07、分配真实权限、开启默认功能、写真实活动/报名、付费调用或部署。

首次HTTP测试装配因同时声明SaBeanRegister默认配置与自建SaTokenConfig产生两个配置Bean失败；移除测试重复Bean、改用既有配置属性绑定后通过，没有改生产认证配置迁就测试。前一条PowerShell未正确引用逗号-Dtest的命令在解析时失败；随后一次仅既有7项运行，不算本次新增验证。最终以上述ActivityHttpContractTest的JUnit3项为准，未写入的两个早期拟议测试名不算成果。日志为Git忽略logs/activity-http-contract-tests.log，报告target/surefire-reports/TEST-com.ican.controller.ActivityHttpContractTest.xml。

### 10.4 阶段复盘、提交与下一入口

A4是入口参数/错误契约代码修正和受控真实HTTP装配通过，P6-03仍部分完成。A3迁移01—07已执行/读回、新表为空、默认开关关闭、未分配角色等事实保留；前端接入02修正旧“07未执行/所有后端均未验证”的当前表述。前端源码未变，不重复构建/视觉验收。

继续：下一集中入口为完整应用范围的活动链路装配与客户端接续，在独立合成资源上串起HTTP→实际业务服务→MySQL/MQ与Redis保护，并按新证据修补代码；关注真实角色/会话、配置开关、异常/操作日志切面装配和HTTP未知结果恢复，不重复本轮受控HTTP或A3无变化协议测试。真实角色授权、生产启用/部署另守授权；不能以模拟权限推断真实菜单可用。没有新功能缺口证据时不人为扩功能凑提交，也不以端口循环代替开发。

推迟Redis资格预扣和容量数字，仍需基线可用及DB热点证据；缓存/搜索/统一切换等旧门槛独立保留。取消日期确认、仅改自动化配置、复述昨日提交作为今日交付。最新环境上下文或用户追问不取消尚未完成的开发，简短回应后继续；只有用户明确暂停/取消才停。本批定向复盘2026-11-24，下次周复盘不晚于2026-11-30，重大新证据提前。普通本地提交标题`fix: 修复活动入口校验与异常响应契约`，本总结与代码同提交，按标题定位；不合并/推送/部署。用户ArticleController修改未触碰或暂存。


## 11. A5业务响应不被日志故障改写与真实HTTP链路（2026-11-25）

### 11.1 本轮实现与业务不变量

延续A4下一入口，实际接入认证/角色、Redis限流、业务事务、Rabbit消费者及日志，避免重复受控HTTP测试或把任务配置当开发。本轮新增的生产修正是日志故障隔离：管理员草稿/编辑/发布/恢复可能已提交，辅助日志失败不能把业务成功改为失败；业务异常也不能被“记录异常失败”覆盖。持久身份、PENDING与CONFIRMED分离、请求→活动锁序、同轮投递token和容量条件事务保持。

改前：OptLogAspect AfterReturning在事务后同步构造日志并向AsyncManager提交，任何RuntimeException都能覆盖返回；startTime仅末尾remove，业务失败或uploadTalkFile提前返回不清理。ExceptionLogAspect记录自身失败时抛新异常，可能遮住原始错误。两处直接getArgs()[0]还不兼容空参数。

改后：操作日志准备/提交失败warning并保留返回，finally和业务异常通知清理ThreadLocal；异常日志自身失败warning并保留原异常；空参数安全记录。选择复用现有日志切面/异步服务，不改变业务事务去迁就日志，不引入审计平台或新线程池配置。日志是当前辅助记录，仍可能丢失，不称持久可靠审计；只捕获RuntimeException，不宣称能屏蔽JVM严重错误。

| 文件 | 本轮职责 |
| --- | --- |
| src/main/java/com/ican/aspect/OptLogAspect.java | 操作日志构造/调度失败不覆盖业务返回，成功/失败/提前返回清理耗时状态，空参兼容 |
| src/main/java/com/ican/aspect/ExceptionLogAspect.java | 日志失败不覆盖原业务异常，空参兼容 |
| src/test/java/com/ican/controller/ActivityRegistrationHttpIntegrationTest.java | 实际HTTP应用切片，真实MySQL/MQ/Redis会话与权限/限流、业务和日志，隔离合成身份/数据 |
| docs/09-模板与脚本/verify_registration_dependencies.py | 新--http模式、8表仅克隆结构/唯一vhost/Redis前缀、单选本轮整合、清理读回与失败退出；Maven/JVM进程树期限和新鲜JUnit报告核对 |
| 领域设计04、前端接入02、路线/阶段计划、docs/README与本总结 | 当前证据、日志边界、命令/文件职责、下一开发入口；更正入口中旧07未执行状态 |

### 11.2 实际正常、失败、恢复与重复时序

在唯一隔离库插入合成角色/菜单关联，不复制业务行。测试代码通过SaToken生成合成用户会话，真实SaTokenDaoRedis和RedisConfig存/取会话，StpInterfaceImpl通过实际Role/Menu Mapper加载权限；匿名不能写管理、普通读者不能管理/恢复。不是密码/OAuth登录。HTTP依次创建/编辑/发布活动；操作日志异步实际写入t_operation_log，重复发布的异常日志写入t_exception_log。

仅合成活动的开始时间由隔离SQL设为窗口内；报名POST返回PENDING，GET可查，换新UUID重复仍原身份。实际监听器启动后Rabbit→真实执行事务→CONFIRMED；第二用户SOLD_OUT，确认后取消两次只回补一次，取消后再POST仍CANCELED。并非FIFO或多实例容量实测，A3两InnoDB连接证据不重复计数。

隔离触发器阻断CONFIRMED写入，实际消费者捕获事务失败并独立记录；每轮仅将合成请求退避设为到期再显式调用recoverPending，3次到FAILED。删除隔离触发器，普通读者无权恢复，管理员HTTP按原requestId恢复并最终CONFIRMED；数据库余量0、待办0、3条唯一报名身份。结果/库存和恢复都未用模拟业务服务。

故障隔离：仅本轮前缀下的活动限流键写为错误类型，让实际Redis Lua失败，HTTP明确“访问保护暂不可用”，DB身份数量不变；删除该合成键后同用户仍返回原SOLD_OUT。此项是Redis命令错误，不是Redis停机/网络超时演练。最后shutdown本轮测试日志executor，HTTP新建草稿仍成功、DB只增加一条；再次发布已开始活动仍保留原ServiceException文案，日志拒绝以warning可见。没有测试HTTP响应丢失/重启、多进程或代表性负载，不能称整体运行验收完成。

### 11.3 实际命令、结果、环境与清理

工作目录blog-springboot，沿既有本机IDE Maven/JBR，仅对本次子进程设置JAVA_HOME/PATH：

```text
mvn -q -DskipTests test-compile
python docs/09-模板与脚本/verify_registration_dependencies.py --http --execute --expect-host 121.41.87.40
git diff --check
```

2026-11-25一次有界预核查：MySQL只读表结构、Redis PING、Rabbit管理API均可用；不再沿用旧超时为当前判断。最终运行`blog_activity_a128db14680b4502b0fbf87dd74e91e1`，JUnit1个整合方法、0失败/错误/跳过，耗时18.094秒仅为该测试方法执行时间，不是业务尾延迟/吞吐。主/测试编译通过，实际JBR Java21.0.8、Tomcat10.1.20；远程共享开发依赖，单测试进程/一个消费者/测试连接池最多4连接，合成容量1，未做负载定容。新测试默认无BLOG_ACTIVITY_SCHEMA时跳过，绝不默认连依赖。

运行器--http只选新整合，不重跑A3服务/AI/搜索。MySQL唯一库仅CREATE TABLE LIKE克隆8表结构，vhost唯一；Redis值使用现有生产序列化，键仅在测试BeanPostProcessor加运行前缀。测试结束DirtiesContext关闭本轮回环服务器、连接池/消费者/日志线程，finally删除本轮库/vhost/前缀键并通过新MySQL连接、Rabbit404、新Redis连接读回不存在，report的三个cleanup均deleted_verified。没有FLUSHDB、删除业务键、写原业务表/角色、消费原队列、启停已有服务、执行01—07、启用/部署或付费调用。期限分支会终止本轮Maven/JVM进程树后清理，正常路径已实际运行，240秒超时分支未故意触发，不能声称演练过。

报告与Java日志在Git忽略logs/activity-acceptance/<运行名>.json及<运行名>-java.log，JUnit位于target/surefire-reports/TEST-com.ican.controller.ActivityRegistrationHttpIntegrationTest.xml。最终日志含3次故意确认事务失败、1次故意Lua错误、各1次操作/异常日志调度拒绝告警，均是设计内故障，不写成运行仍失败。

实际失败保留：首轮`0f82950bb97447458118f9ff176e5b08`测试装配未沿用mybatis-plus的t_表前缀，异步日志插入未进克隆表，日志数量断言超时；补测试GlobalConfig.DbConfig表前缀并沿现有ThreadUtils输出异步异常，不修改生产表名。第二轮`99901a3f5e324851bd531b254f7743cf`确认失败断言误用列名failures，实际为failure_count；修测试SQL后最终通过。首轮尚未执行到的会话键断言也改为通过实际会话API读取Role_List，避免猜键名。两轮失败的三类资源均删除并读回，报告保留；不重复统计为通过。独立编译与首轮runner一度重叠，本轮没有依据该重叠作通过结论，最终单独runner包含编译与执行，后续必须等构建返回完成再跑共享target验证。

### 11.4 阶段边界、复盘与下一开发入口

A5交付日志故障隔离生产修正及同一HTTP进程内的报名实际MySQL/MQ/Redis/角色/日志链路，补足A4业务/Redis替身边界。不是整个博客启动/全部配置或前台浏览器真实依赖验收：测试仅显式导入报名相关生产类，旧消费者/AI不装配，周期调度不开启，恢复显式调用真实scanner；RedisService和UserService仅作为既有拦截器/handler的未用依赖替身，其余所述链路真实。密码/OAuth登录、权限修改后的会话刷新/菜单生成、完整应用启动、HTTP未知结果/进程退出、跨实例/积压排空/尾延迟/DB成本仍未验收。P6-03仍部分完成，默认开关仍关；迁移已执行、实际切片、完整应用和部署分别记录。

本轮故障暴露下一开发需求：现有管理报名列表只返回requestId/status/时间，PENDING时看不到投递预约次数、失败次数或下一恢复时间，FAILED时也看不到持久故障摘要；管理者无法从原页面判断等待和恢复依据。下一沿现有ActivityRegistrationVO/mapper listRegistrations/ActivityService.registrations与RegistrationDrawer补管理专用的投递/失败诊断及原身份恢复接续，公开本人接口不泄露内部字段，不返回dispatch_token/底层异常或新增平台；所有显示只表达数据库已知事实，不能把预约次数叫投递成功/ACK。实现和必要验证成批完成，不再反复执行本轮整链充当新开发。

继续以业务可操作性和必要后端修正为主。推迟Redis资格预扣直到DB热点/容量证据，不扩支付/候补/新中间件；缓存/搜索/统一切换旧门槛独立开放，不因本轮通过而抹掉，也不阻断已有批准功能。此次按A4以来的相关变化复盘，最近周复盘2026-11-23/本批11-25，下次不晚于11-30；不重审全仓。普通本地提交标题`fix: 隔离报名日志故障并接通真实HTTP依赖链路`，本总结与代码同提交，按标题定位。不合并/推送/部署；用户ArticleController改动未触碰/暂存。


## 12. A6后台诊断与恢复原身份接续（2026-11-26）

### 12.1 业务触发、不变量与实现

沿A5的下一开发入口：管理员能看到FAILED/PENDING但不知道是否预约、已记录几次失败和何时再次有资格扫描；只能盲目刷新或恢复。原抽屉恢复在确认框结束后才锁操作，且网络未知/业务拒绝后没有统一读回当前事实。本批实际实现管理诊断API与可操作面板、恢复读回保护，不重复A5整链测试或新增任务平台。

不变量：PENDING仅受理、CONFIRMED才占名额；requestId不变；MySQL唯一约束、请求→活动锁序、条件容量事务及投递token/失败预算保持。公开本人结果不包含内部诊断；管理页面也不能得到token/原始SQL/连接错误。预约是持久预约次数、失败是已记录确认事务失败，均不证明MQ发送或ACK；人工恢复按既有retryFailed清零两项计数，面板明确说明。

| 文件 | 职责 |
| --- | --- |
| model/vo/AdminActivityRegistrationVO.java | 独立管理响应，既有5字段+预约/失败次数、扫描资格时间、安全摘要；公共VO未改 |
| controller/ActivityController.java、service/impl/ActivityService.java、mapper/ActivityMapper.java | 仅管理报名分页使用管理类型，权限/路径及retry响应保持 |
| resources/mapper/ActivityMapper.xml | 管理列表单条分页SELECT投影诊断，PENDING才返回扫描资格，PENDING/FAILED按last_error存在映射固定摘要，不返回底层原文/token |
| shoka-admin/src/api/activity/types.ts、index.ts | 管理列表AdminRegistration类型，恢复响应仍Registration |
| shoka-admin/src/views/activity/RegistrationDrawer.vue | 当前行诊断、确认前锁操作、旧GET作废、成功/拒绝/未知统一查回、读回失败禁止再次恢复 |
| shoka-admin/src/views/activity/RegistrationDiagnostics.vue | 原抽屉内只读面板、次数/北京时间秒级扫描资格/安全摘要，必要说明、主题/窄屏和键盘按钮 |
| controller/ActivityHttpContractTest.java | 新增1个定向方法验证管理权限/诊断序列化、公开/取消/恢复仍为原5字段且无内部数据 |
| 领域设计04、前端接入02、阶段计划/路线、docs/README、本总结 | API/界面契约、实际验证和后续入口 |

采用独立管理投影而非扩公共VO或返回持久实体，避免未来误用泄漏。摘要固定映射而非展示last_error原文，不把当前固定错误文本永远安全当假设。诊断读取复用原分页、排序与总数查询，无逐行回查、无新平台/指标标签/DDL。面板派生自当前rows，关闭/筛选排除即不再呈现旧记录；沿既有Element Plus与局部15px字号，不重设全站样式。没有支付/候补/Redis预扣或修改管理角色授权。

### 12.2 正常、失败、恢复与并发时序

具有activity:registration:list权限→原列表按活动/状态分页→处理记录显示持久快照。终态不展示旧自动投递时间/摘要，PENDING到期只表示可被scanner发现，不保证准时发布/消费；确认失败摘要不是根因诊断，必要时仍按requestId查服务日志。

恢复按钮先设置retrying并sequence++使旧列表GET失效，再确认；取消仅解锁，不调用POST。确认后按原requestId调用一次retry，结果成功/业务拒绝/网络未知均GET当前事实。读取失败保留needsRefresh，不允许拿旧FAILED再次点击；手动刷新成功才解除，若实际已PENDING/终态则不再有FAILED按钮。原数据库状态检查仍是最终防线，客户端busy/sequence不等于跨页并发或网络请求取消。未新增自动POST/轮询；没有改变旧token晚到、取消补偿或库存协议。

### 12.3 实际验证与失败记录

backend目录使用既有IDE Maven/JBR，只在本次子进程设置JAVA_HOME/PATH；与前端独立构建可并行，未并发两个Maven访问同target：

```text
mvn -q -Dtest=ActivityHttpContractTest#diagnosticsAreAdminOnlyAndPublicResultsRetainTheirOriginalFields,ActivityRegistrationServiceTest#mapperParsesAndAdmissionAndCapacitySqlKeepTheirDistinctTimeBoundaries test
python target/verify_activity_a6_diagnostics.py
shoka-admin: npx.cmd vue-tsc --noEmit
shoka-admin: npx.cmd vite build
npx.cmd --yes --package @playwright/cli playwright-cli -s=blog-a6 run-code --filename=logs/activity-a6-ui-fixture.js
npx.cmd --yes --package @playwright/cli playwright-cli -s=blog-a6 run-code --filename=logs/activity-a6-ui-check.js
npx.cmd --yes --package @playwright/cli playwright-cli -s=blog-a6 run-code --filename=logs/activity-a6-ui-recovery.js
npx.cmd --yes --package @playwright/cli playwright-cli -s=blog-a6 run-code --filename=logs/activity-a6-ui-final.js
git diff --check
```

Java2个方法/0失败/错误/跳过，包含主/测试编译。实际回环HTTP/权限拦截证明普通读者不能调用管理诊断，有权限可获得新字段；持久实体中放合成私有token/错误后，经公共VO转换的受理/本人GET/取消/管理retry均仍5字段，没有userId/诊断/token/lastError。业务服务/权限/Redis替身，非真实数据库权限证据。另一个既有方法只因XML/结果类型变化定向运行，用于真实MyBatis XML解析，不重跑其余旧业务或A5整链。

临时脚本执行实际mapper listRegistrations的SELECT，按实际`status != null`条件组装，无生产SQL方言替换，在内存SQLite验证PENDING/FAILED/三种终态、故意残留终态时间/错误、跨活动隔离、各状态过滤、排序分页，以及合成私有原文/dispatch_token不出现在投影中。它不是实际MySQL映射、行锁或性能验收；脚本在target不提交，结果logs/activity-a6-sql.log。首次临时脚本引号生成错误导致SyntaxError，在任何SQL前失败，改Python字面量后通过，未修改业务SQL迁就验证。

管理端最终Vite构建通过，保留既有大包提示。全项目vue-tsc返回2，28条诊断均未改路径（collab/doc等），活动路径0诊断；不冒称全项目类型通过。时间显示改为秒后仅重跑必要Vite构建，不无变化重复全量类型检查。Vite生成的components.d.ts/auto-imports.d.ts无关变化在关闭本轮预览后恢复原始内容，不提交扫描全仓产生的无关声明。

实际浏览器使用原动态路由/请求客户端/权限指令，合成API拦截所有localhost:8080请求、其他外网阻止；仅本轮新建127.0.0.1:5186 Vite（open:false）和命名浏览器blog-a6。验证FAILED/PENDING/终态诊断、秒级时间、键盘Enter展开/收起、恢复确认取消无POST、成功及业务拒绝读回、POST已受理但响应丢弃且GET同时失败后的禁重发/手动接续、状态过滤。未知路径仅1次POST；另1次业务拒绝/1次正常恢复，总3次预期POST，无自动重发。390宽页面scrollWidth=390，面板无水平溢出；检查桌面/手机/暗色截图，新面板暗色文本rgb(229,234,243)/背景rgb(38,39,39)。旧管理外壳/表格暗色不完全一致未顺手重设，不称全站主题验收。

浏览器恢复脚本一次把旧Element Plus下拉项按option查找而超时；真实快照显示listitem，改定位后继续完成过滤/最终截图，没有把该次脚本称通过。故意abort的两个请求产生既有网络错误提示/控制台错误，属于本次未知响应夹具，不当作真实后端错误。原始CLI记录/截图已归入Git忽略logs/activity-a6-browser/，另有fixture/check/recovery/final脚本和logs/activity-a6-{http,sql,admin-types,admin-build}.log。关闭本轮浏览器和5186预览，不启停已有服务。没有连接业务库、创建新真实活动、分配权限、执行迁移、付费模型或部署。

### 12.4 阶段范围、复盘与下一入口

A6完成管理诊断和恢复接续代码/必要受控验证；A1—A6是当前报名本地业务基线，P6-03完整运行验收仍部分完成。A5实际MySQL/MQ/Redis链路证据保留，本轮未无变化重跑；新诊断尚无真实MySQL/Redis/浏览器到实际后端的同次证据。密码/OAuth登录、角色变更会话刷新/动态菜单、全博客启动、多实例/网络未知/进程退出、积压/尾延迟/DB成本仍待验收；默认开关关、迁移01—07已执行状态不变，不把页面模拟响应说成上线。

下一集中开发入口来自现有ActivityForm.create未知响应：仅本窗口uncertain禁按保存，关闭重开/跨窗口仍可能重复创建，ActivitySaveDTO/ActivityService.create/INSERT没有持久创建身份。沿现有管理草稿新增业务的“创建请求幂等与未知结果查回”，先定义创建专用UUID、同身份同载荷返回同草稿、冲突载荷拒绝、身份与草稿同事务及原创建身份查询，再接表单持久原号/读回；不复用读者报名身份、不按标题猜重复、不新增通用Outbox/任务平台。编辑与创建输入分开核对，DDL如需新增迁移按既有授权目标/备份规则执行，不能改写已执行07。此项是已读代码缺口，不是线上事故，也不在本批假称实现。

继续聚焦可操作业务和后端恢复深度，不按每日一个小项凑交付；推迟Redis预扣直到DB热点/容量证据，旧缓存/搜索/统一切换门槛独立保留。本批定向复盘11月26日，下一周复盘不晚于11月30日，无新证据不重审全仓。普通本地提交标题`feat: 增加报名管理诊断与恢复结果接续`，本总结与代码同提交，按标题定位；不合并/推送/部署。用户ArticleController未改写、暂存或清理。
