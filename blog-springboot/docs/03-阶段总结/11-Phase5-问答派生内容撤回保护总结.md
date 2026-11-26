# Phase5-问答派生内容撤回保护总结

日期：2026-10-19。分支：`codex/blog-quality-daily`。接续提交：`9a8e098` 私人文章读取规则。本切片完成本地代码与隔离验证，BD-001和Phase5整体仍进行中，未部署。

## 业务触发与结果

读者访问已有 `/api/ai/chat` 时，原AiRagServiceImpl直接把向量候选正文交给模型，再从metadata输出引用；文章撤回或变更为私人后，旧向量不受上一轮详情/搜索读取屏障约束。AiArticleServiceImpl现有写入已携带articleId/title/url，因此无需改向量结构或新建权限平台即可补当前数据库判断。这里是既有派生输出的内容一致性修复，不扩展AI项目能力。

本轮生成前按候选articleId批量确认当前读者可见性，只让允许片段进入模型。匿名/普通/指定账号复用原规则：私人标签/分类限制普通读者，所有公开读取仍要求PUBLIC且未删除。未知归属不从URL猜测，普通标签不能抵消私人标签。

生成后重新确认当前账号、批量复查所有实际用过的文章。只要其中一篇已不可见，整个答案与sources均不返回；仅删引用无法清除生成文字中的敏感内容。拒绝后不自动重新生成，权限或发布状态恢复后用户可重新提问。原问题输入保留，前台请求开始时清除旧答案和引用，避免失败还展示上一轮答案。

检索故障与权限查询故障返回明确的可重试业务失败，不再把向量故障包装成“博客中暂无相关内容”。真实无结果或没有可见片段仍使用原空结果契约，不调用生成模型。查询Embedding可能已经发生，不能把“不调用生成模型”写成“零模型成本”。

## 设计与改变的文件

完整时序、取舍与承诺见[问答派生内容撤回读取约束](../05-AI体系/02-问答派生内容撤回读取约束.md)。文件路径相对Git根 `D:/IdeaProjects/blog`。

| 文件 | 变化及目的 |
| --- | --- |
| `blog-springboot/src/main/java/com/ican/service/impl/AiRagServiceImpl.java` | 注入原ArticleMapper/ArticleAccessService；生成前筛选与生成后整体验证；合法正整型articleId解析；引用按核验ID生成URL及去重；检索/校验异常明确失败 |
| `blog-springboot/src/test/java/com/ican/service/impl/AiRagServiceImplTest.java` | 覆盖混合来源提示词、异常归属、旧向量撤回/恢复、生成期间撤回/资格变化、两次校验失败、检索故障及真实HTTP业务包装拒绝契约 |
| `shoka-blog/src/views/AiChat/index.vue` | 专业问答请求前清空answer和sources，两行配套修改；原失败提示/重新提交入口保持 |
| `blog-springboot/docs/02-阶段计划/04-Phase5-内容变更一致性与可靠发布计划.md` | 实施前记录第四切片规则、失败恢复和验证边界；完成后更新状态 |
| `blog-springboot/docs/05-AI体系/02-问答派生内容撤回读取约束.md` | 长期领域设计：两个SQL校验阶段、来源归属、生成答案拒绝、模型已收到内容无法追回等边界 |
| `blog-springboot/docs/01-架构设计/02-公开文章撤回读取校验设计.md` | 在原可见性设计下接续既有问答出口，不改写前轮证据 |
| `blog-springboot/docs/00-项目总览/07-真实业务场景与深度升级路线.md`、`05-质量审查与升级台账.md` | 原BD-001/BQ-008记录新增子项证据和仍开放范围，保留整体进行中 |
| `blog-springboot/docs/05-AI体系/01-AI能力文档索引.md`、`docs/README.md` | 领域及阶段总结导航 |

沿用现有selectVisibleArticleIds SQL，没有新增表、字段、迁移、配置、角色、中间件或后台功能。向量topK仍为5，过滤后不另行补取候选，不保证召回数量；没有改写文章AI处理、任务投递、摘要/向量回写或自由对话流路径。

## 实际验证

1. 编译与隔离后端验证（IDE自带Maven，JBR21，目标Java17）：

   ```text
   mvn -q -Dtest=AiRagServiceImplTest,AiArticleServiceImplTest,ArticleAccessServiceTest test
   ```

   22项通过，0失败/错误/跳过：RAG读取与HTTP契约13项、既有向量写入3项、既有账号规则6项。Maven同时编译本轮主源码/测试源码，没有另做发布包；没有启动完整应用或连接真实依赖。
2. 业务验收使用受控VectorStore/ChatClient/Mapper和账号资格。捕获真实服务构造的提示词，确认混合候选中受限/未知归属文本未提交生成；保留允许片段顺序，重复文章只列一个规范引用。数值/字符串归属兼容与小数、溢出、非有限值、布尔、缺失主键拒绝均通过。
3. 同一旧向量候选先撤回再恢复；模型调用桩确定性触发文章撤回或指定账号资格收回，验证生成后重新查库并拒绝整段答案；资格恢复后新请求正常。任一校验阶段数据库失败均不能放行，向量故障是明确失败。没有用真实并发压测代替业务时序测试，也没有付费模型调用或隐性自动重试。
4. standalone MockMvc连接实际AiController、AiRagServiceImpl及GlobalExceptionHandler，仅外部依赖使用桩。模型生成后来源被撤回时，HTTP沿用200，Result.flag=false、data=null，响应体不含生成答案。这是隔离Controller/Service/handler调用证据，不是服务联调或完整认证链验证。
5. `target/check-rag-read-result.mjs` 使用现有Vue编译器/esbuild、真实Vue renderer，执行实际AiChat.vue setup，接口响应受控。验证请求前清旧答案/来源、撤回失败/null、恢复成功、网络失败、真实无结果及自由对话回归通过。脚本存于忽略target，仅本次验证使用；没有新增正式依赖、抽出生产组件或改布局样式。
6. `shoka-blog` 的 `npm run build` 通过。保留原CSS nesting/大chunk等告警；构建生成的无关 `src/types/components.d.ts` 变化已恢复。构建不等于完整TypeScript检查或浏览器交互验收。`git diff --check`及本轮文档相对链接检查通过。

原有用户ArticleController.java修改和未跟踪homa-xxl-job保留，未暂存、覆盖或清理；AI学习项目、PDM及其他参考目录未改。未跑无关全量测试、真实Qdrant/Embedding/LLM/MySQL/Redis/MQ/ES、多实例、浏览器或上线验证；未启停服务、改真实业务数据、推送或部署。

## 系统承诺与接续

承诺以两次SQL的可见快照为界：第一次校验已可见的撤回限制，不进入模型上下文；第二次校验已可见的任一实际来源不可见或账号资格变化，阻止整段答案返回。账号与文章查询不是原子快照，主从延迟、二次SQL之后发生的在途变化以及已传输/复制内容仍在保证之外。

第一次SQL后才撤回的上下文可能已经发给模型，第二次检查无法追回模型供应商收到的内容或费用。当前仍信任内部向量生产器的正文→articleId绑定；索引被篡改、旧公开正文/旧标题、撤回再恢复的历史版本、模型幻觉、历史向量物理清理、旧任务覆盖新摘要等未解决。不能将本轮称为全站权限闭环或可靠发布全完成。

目录名称/聚合数量、评论及其他派生入口仍待定向核实，详情页null/受限结果承接保留为配套待办。下一主线按BD-002/003/005，读取文章写事务→既有任务表→消息投递/ACK/重试→AI结果回写，选定持久任务待投递恢复或旧版本回写的最小纵向切片；不新增通用Outbox平台、不把afterCommit或状态字段当成业务保证完成。

本地提交标题：`fix: 阻止撤回文章经问答派生结果继续返回`。本总结和实现同一普通本地提交，可按标题定位；不合并、推送或部署。
