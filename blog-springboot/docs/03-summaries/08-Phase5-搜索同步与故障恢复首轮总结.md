# Phase5-搜索同步与故障恢复首轮总结

日期：2026-10-16。分支：`codex/blog-quality-daily`。状态：BD-004 首个切片代码及隔离验证完成，Phase5 整体进行中，未部署。

## 业务问题与结果

作者批量更新或撤回文章时，一条 Canal 消息可携带多行；原实现仅处理第一行。ES 写入异常又被吞掉，消息消费可能正常返回，导致其余文章或失败文章长期停留在旧搜索状态。

本轮将消息定义为“这些主键的搜索结果需要同步”的提示，权威内容来自 MySQL 当前文章；不直接把可能过期的 CDC 正文当作最新内容。所有有效行逐条处理，任一失败向上传递，整批重试；3次仍失败则带确认转存原始消息，供依赖恢复后重放。转存失败保留原消息，不将故障当作成功。

## 设计取舍

- 使用固定文章 ID 的全文索引覆盖，替代只更新已存在文档；首次 INSERT 漏过后，UPDATE/重放也能重建。当前行是私密/草稿/软删除或不存在时删除索引，避免顺序重放旧公开事件重新写入其旧正文。
- 不使用 Canal `es` 作为 ES 外部版本。它在原 DTO 使用 Integer，无法正常承接13位时间戳；本轮改为 Long，仅解决消息解析，不冒充内容版本。
- 专用监听工厂隔离搜索重试；原队列参数不变。采用确认转存而不是给已有队列硬加 DLX 参数，减少队列重声明冲突。恢复器和模板不注册为全局 bean。
- 保留至少一次语义：部分成功后整批重试、转存确认与原 ACK 之间退出都可能重复。索引覆盖和删除允许重放；本轮没有承诺跨实例乱序写入或严格撤回的完整保证。

## 修改文件

以下源码路径均相对 `blog-springboot/`。

| 文件 | 改动 |
| --- | --- |
| `src/main/java/com/ican/consumer/ArticleConsumer.java` | 校验文章表与整批主键，处理所有数据行，使用专用监听工厂，失败上抛 |
| `src/main/java/com/ican/service/ElasticsearchService.java` | 内部服务收口为按主键同步当前状态；原三个方法仅供旧消费者使用，没有外部 HTTP 接口变更 |
| `src/main/java/com/ican/service/impl/ElasticsearchServiceImpl.java` | 回源文章，公开未删时全文覆盖；其余删除；ES异常上抛 |
| `src/main/java/com/ican/model/dto/CanalDTO.java` | `es` 改为 Long |
| `src/main/java/com/ican/config/ArticleSearchSyncConfig.java` | 新增搜索专用 AUTO ACK、3次重试/3秒间隔、确认转存与失败重新入队 |
| `src/main/java/com/ican/config/RabbitMqConfig.java` | 新增故障交换机、持久化队列及绑定，原文章队列参数不变 |
| `src/main/java/com/ican/constant/MqConstant.java` | 新增故障路由常量 |
| `src/main/resources/application.yml` | 启用 correlated publisher confirms 和 returns，供确认转存使用 |
| `src/test/java/com/ican/consumer/ArticleConsumerTest.java` | 批量三类事件、时间戳、坏批次、部分失败重放及表过滤 |
| `src/test/java/com/ican/service/impl/ElasticsearchServiceImplTest.java` | 使用真实 ES 客户端请求构造与受控 transport，验证当前状态、重放、撤回、删除和错误传播 |
| `src/test/java/com/ican/config/ArticleSearchSyncConfigTest.java` | 实际 Spring Retry 拦截器的重试/恢复边界；确认、NACK、return、连接失败 |

文档：新增真实业务路线、Phase5计划、本总结与[故障恢复说明](../06-deployment/05-文章搜索同步故障恢复.md)；更新既有后端路线、日常约定、台账和文档中心。日常自动化 `blog` 已改名“Blog 业务深度升级”，保持每天北京时间10:00与原聊天接续；不再以页面小修作为默认主线。

## 实际验证

使用本机已有 IDEA JBR 21 与 Maven，编译目标仍是项目 Java17。`mvn -q -DskipTests compile` 通过。

针对本轮消息/搜索范围执行：

```text
mvn -q -Dtest=ArticleConsumerTest,ElasticsearchServiceImplTest,ArticleSearchSyncConfigTest,AiArticleConsumerTest test
```

结果：26项通过、0失败、0错误、0跳过；其中包含已有 AI 消费者3项回归。模拟了批次第二篇写入失败后重放到完整索引、旧事件使用新正文或当前撤回状态、物理删除重放、3次失败转存、短暂失败恢复，以及确认失败时原消息重新投递所需的异常传播。

初次验证的测试代理误把容器拦截器应用到单参数业务方法，耗尽重试时出现数组越界；已将测试修正为实际容器的 Channel/Message 调用边界后重跑通过。没有为这次测试适配改变生产逻辑。

这里 MySQL mapper、ES transport、RabbitTemplate confirm/return 是隔离桩；实际 Spring Retry 和 ES 请求构造被执行。未启动完整应用、未写真实业务数据、未操作真实 MQ/ES、未调用付费模型。没有运行无关全量测试或压测。

## 未完成与接续

1. 真实依赖联调：核对 Canal 业务库与应用数据源、新故障路由声明权限、最终生效的 confirms/returns 配置，再按运维说明验证实际 ACK/转存/重放。这次没有部署或启停已有服务。
2. BD-001：公开详情与搜索输出在缓存/索引滞后时仍需依据权威可见性判断；私人标签/分类语义需从现有业务确认。下一轮优先在这条主线继续。
3. BD-004：读取当前行不能阻止两个实例在回源与写 ES 之间乱序。内容版本与条件写入、历史丢失事件的有界补偿、搜索故障返回空结果尚未解决，不能将 BD-004 或 Phase5 标为完成。
4. BD-002/003/005：事务内发消息、任务原子认领与旧结果回写、多实例缓存旧回填仍是已定位缺口，本轮没有顺手改动。

本轮没有修改用户既有 `ArticleController.java` 改动、未跟踪 `homa-xxl-job/`，没有改 AI 项目、PDM、其他参考目录，也没有合并、推送或操作线上文章。

## 提交记录

本总结与相关实现使用普通本地提交 `feat: 完善批量文章搜索同步与故障恢复`，提交号可按该标题在当前分支定位。此前 `4fb9f4d` 为 Phase4 列表维护记录，不归入本轮深度业务能力。
