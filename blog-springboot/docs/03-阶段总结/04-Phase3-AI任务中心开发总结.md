# Phase 3 AI任务中心开发总结

## 本阶段目标

本阶段按照 `docs/00-项目总览/03-能力提升与跳槽AI发展路线.md` 的第三阶段要求，将已有 AI 能力从“接口调用 + MQ 消费”升级为“可追踪、可重试、可观测”的 AI 任务中心。

## 升级内容

- 新增 `t_ai_task` 任务模型，用于记录 AI 任务的业务类型、业务 ID、任务类型、状态、失败次数、模型名称、Prompt 版本、耗时和错误原因。
- 文章发布和更新后不再直接投递完整文章内容到 MQ，而是先创建 AI 任务，再向 MQ 发送只包含 `taskId` 的消息。
- AI 消费者根据 `taskId` 执行任务，任务执行前更新为 `RUNNING`，成功后更新为 `SUCCESS`，失败后记录错误并进入 `RETRYING` 或 `FAILED`。
- 新增后台任务列表接口和手动重试接口，便于后续后台页面接入和人工补偿。
- 补充 AI 任务中心建表和权限 SQL，方便线上环境落库。
- 补充服务、消费者和控制器测试，测试不依赖真实 `.env`、Redis、RabbitMQ、Qdrant 或外部 AI 服务。

## 修改文件

- `src/main/java/com/ican/entity/AiTask.java`
  - 新增 AI 任务实体，映射 `t_ai_task` 表。

- `src/main/java/com/ican/enums/AiTaskStatusEnum.java`
  - 新增 AI 任务状态枚举，包含 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`、`RETRYING`、`CANCELED`。

- `src/main/java/com/ican/mapper/AiTaskMapper.java`
  - 新增 AI 任务 Mapper，支持基础 CRUD 和后台分页查询。

- `src/main/resources/mapper/AiTaskMapper.xml`
  - 新增后台列表数量统计和分页查询 SQL。

- `src/main/java/com/ican/service/AiTaskService.java`
  - 新增 AI 任务中心服务接口，包含创建文章任务、查询任务列表、执行任务和重试任务。

- `src/main/java/com/ican/service/impl/AiTaskServiceImpl.java`
  - 实现任务创建、状态流转、失败记录、手动重试和 MQ 投递。

- `src/main/java/com/ican/model/dto/ArticleAiMessage.java`
  - 新增 `taskId` 字段，让 MQ 消息体支持只传任务 ID。

- `src/main/java/com/ican/model/dto/AiTaskQueryDTO.java`
  - 新增后台查询条件。

- `src/main/java/com/ican/model/vo/AiTaskBackVO.java`
  - 新增后台任务列表返回视图。

- `src/main/java/com/ican/controller/AiTaskController.java`
  - 新增 `/admin/ai/task/list` 和 `/admin/ai/task/{taskId}/retry` 接口。

- `src/main/java/com/ican/consumer/AiArticleConsumer.java`
  - 改为消费 `taskId` 并委托 `AiTaskService` 执行任务。

- `src/main/java/com/ican/service/impl/ArticleServiceImpl.java`
  - 文章新增和更新后改为创建 AI 任务，不再直接发送完整文章内容。

- `src/test/java/com/ican/service/impl/AiTaskServiceImplTest.java`
  - 覆盖任务创建、成功执行、失败记录和成功任务幂等跳过。

- `src/test/java/com/ican/consumer/AiArticleConsumerTest.java`
  - 覆盖消费者按 `taskId` 执行、空消息忽略和失败进入死信逻辑。

- `src/test/java/com/ican/controller/AiTaskControllerTest.java`
  - 覆盖后台列表和手动重试控制器委托。

- `src/test/java/com/ican/service/impl/ArticleServiceImplTest.java`
  - 更新构造依赖，适配文章服务改为依赖 `AiTaskService`。

- `docs/07-数据库变更/02-Phase3-AI任务中心建表与权限SQL.sql`
  - 新增 AI 任务中心建表和权限 SQL。

## 注意事项

- 当前阶段没有接入自动延迟重试队列，失败任务会记录为 `RETRYING` 或 `FAILED`，可通过后台接口手动重试。
- `request_payload` 只记录文章 ID 和标题，不保存完整正文，避免任务表过度膨胀。
- AI 任务中心目前聚焦文章 AI 摘要、标签和向量入库。后续第六阶段可以把缓存预热、ES 重建、历史向量重建等统一抽象为通用任务平台。
