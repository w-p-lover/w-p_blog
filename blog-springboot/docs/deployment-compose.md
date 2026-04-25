# Docker Compose Deployment

这份文档对应当前仓库里的：

- `Dockerfile`
- `docker-compose.yml`
- `.env.example`

## 目标

用一套 `docker compose` 拉起：

- Spring Boot 应用
- MySQL 8
- Redis 7
- RabbitMQ 3 Management
- Elasticsearch 7.17
- Qdrant

## 1. 准备环境变量文件

在项目根目录准备 `.env`，内容可参考 `.env.example`。

> 说明：Compose 部署建议使用 `SPRING_PROFILES_ACTIVE=prod`。本地 IDEA 直启建议使用 `dev`。

最少需要确认这些值：

- `SPRING_PROFILES_ACTIVE=prod`
- `JASYPT_ENCRYPTOR_PASSWORD`
- `BLOG_DB_USERNAME`
- `BLOG_DB_PASSWORD`
- `BLOG_REDIS_PASSWORD`
- `BLOG_RABBITMQ_USERNAME`
- `BLOG_RABBITMQ_PASSWORD`
- `BLOG_MAIL_USERNAME`
- `BLOG_MAIL_PASSWORD`
- `BLOG_OPENAI_API_KEY`
- `BLOG_DASHSCOPE_API_KEY`
- `BLOG_HEFENG_API_KEY`
- `BLOG_HEFENG_BASE_URL`
- `BLOG_GITEE_CLIENT_ID`
- `BLOG_GITEE_CLIENT_SECRET`
- `BLOG_GITHUB_CLIENT_ID`
- `BLOG_GITHUB_CLIENT_SECRET`

## 2. 可选私有配置

如果你仍想保留一个服务器私有配置文件，可以在服务器的 `config/` 目录放：

- `config/application-private.yml`

容器会把 `./config` 挂载到 `/app/config`，并由 `application.yml` 自动读取。

## 3. 启动方式

```powershell
docker compose up -d --build
```

查看状态：

```powershell
docker compose ps
```

查看应用日志：

```powershell
docker compose logs -f app
```

## 4. 端口说明

- `8080`: Spring Boot API
- `3306`: MySQL
- `6379`: Redis
- `5672`: RabbitMQ
- `15672`: RabbitMQ 管理台
- `9200`: Elasticsearch
- `6333`: Qdrant HTTP API
- `6334`: Qdrant gRPC

## 5. 持久化目录

Compose 已经为以下服务声明 volume：

- `mysql-data`
- `redis-data`
- `rabbitmq-data`
- `es-data`
- `qdrant-data`
- `blog-static`
- `blog-upload`

其中：

- `/app/static` 用于 Python 爬虫相关静态资源
- `/data/blog/upload` 用于上传文件

## 6. 生产建议

- 应用前面再挂一层 Nginx / Traefik
- `doc.html` 和 `knife4j` 默认在 prod 关闭
- 不要把 MySQL / Redis / RabbitMQ 管理端口直接暴露公网
- AI Key、邮件授权码、OAuth Secret 建议放到 CI Secret

## 7. 常见问题

### Java 版本

项目当前基于 Java 17，容器镜像已固定为 JDK/JRE 17。

### 静态资源与爬虫目录

生产配置下：

- `spring.web.resources.static-locations=file:/app/static/`
- `spider.dir=/app/static/`

这样爬虫下载出来的文件不会写进 jar 包内部。
