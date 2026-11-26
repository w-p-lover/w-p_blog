# W-P Blog 后端

面向个人技术博客的 Spring Boot 3 / Java 17 后端。当前成品围绕文章写作、发布、检索、阅读、说说图文动态和评论；功能边界见 [个人技术博客成品边界](docs/00-项目总览/08-个人技术博客成品边界.md)。

保留 MySQL、Redis/Caffeine 缓存、RabbitMQ 邮件与文章搜索同步、Elasticsearch 搜索、Sa-Token 权限和必要监控。已移出 AI/Qdrant、活动报名、聊天、协作文档和娱乐工具。旧实现保存在 `备份/精简前完整功能`（本地保留，不推送旧历史），不需要为退出模块设置开关。

## 本地运行

需要 Java 17+、Maven、MySQL、Redis、RabbitMQ；默认搜索模式使用 Elasticsearch。先准备已有业务 schema 与本机配置，不把历史迁移当作可重复执行的初始化程序。

```powershell
Copy-Item .env.example .env
Copy-Item config\application-private.example.yml config\application-private.yml
mvn spring-boot:run
```

配置示例只使用占位值。默认 API 端口 8080，接口文档 `/doc.html`，健康检查 `/actuator/health`。运行配置从环境变量和本机私有配置读取，不要求模型密钥或向量库。

## 容器运行

在本目录配置 `.env` 后执行 `docker compose up -d --build`。本目录 Dockerfile 使用 Java 17；Compose 包含应用、MySQL、Redis、RabbitMQ 和 Elasticsearch，持久卷按原约定保留。Compose 不创建或迁移业务表，也不删除旧 Qdrant 卷或任何业务数据。

前后台分别在 `../shoka-blog`、`../shoka-admin` 构建。两端 `VITE_API_BASE_URL` 统一控制请求地址；同域部署可设置 `/api`，并在反向代理中将该前缀移除后转发至后端。必须同时保留验证码与上传路径的代理。

## 开发与验证

使用 npm 与各自的 package-lock.json 构建前后台。后端有隔离单元/Mapper 契约用例；默认不以真实数据库或模型进行测试。详细结果见 [精简总结](docs/03-阶段总结/16-个人技术博客精简总结.md)。

[文档中心](docs/README.md)保留旧阶段设计和证据；其 AI/报名内容是历史记录，不代表当前成品仍提供这些业务。当前开发优先修复实际写作、阅读、部署中的问题，不继续按旧阶段扩展功能。
