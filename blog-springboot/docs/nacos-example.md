# Nacos Example For This Project

This file gives a practical Nacos layout for the current `blog-springboot` project.

## Recommended layout

- Namespace: `blog-prod`
- Group: `BLOG_GROUP`
- Data ID: `blog-springboot-prod.yaml`
- Format: `YAML`

## Nacos config content example

Use this content in Nacos for production. Sensitive values can be plain environment placeholders or `ENC(...)` encrypted values.

```yaml
spring:
  datasource:
    username: ${BLOG_DB_USERNAME:}
    password: ${BLOG_DB_PASSWORD:}

  data:
    redis:
      password: ${BLOG_REDIS_PASSWORD:}

  rabbitmq:
    username: ${BLOG_RABBITMQ_USERNAME:}
    password: ${BLOG_RABBITMQ_PASSWORD:}

  mail:
    username: ${BLOG_MAIL_USERNAME:}
    password: ${BLOG_MAIL_PASSWORD:}

  ai:
    openai:
      api-key: ${BLOG_OPENAI_API_KEY:}
    dashscope:
      api-key: ${BLOG_DASHSCOPE_API_KEY:}

hefeng:
  api-key: ${BLOG_HEFENG_API_KEY:}

upload:
  oss:
    accessKeyId: ${BLOG_OSS_ACCESS_KEY_ID:}
    accesskeySecret: ${BLOG_OSS_ACCESS_KEY_SECRET:}

oauth:
  gitee:
    client-id: ${BLOG_GITEE_CLIENT_ID:}
    client-secret: ${BLOG_GITEE_CLIENT_SECRET:}
  github:
    client-id: ${BLOG_GITHUB_CLIENT_ID:}
    client-secret: ${BLOG_GITHUB_CLIENT_SECRET:}
```

If you want to store encrypted values directly in Nacos, use this style:

```yaml
spring:
  datasource:
    password: ENC(your-encrypted-password)
```

## Spring Boot side notes

The current project has not added a Nacos client dependency yet. If you want the app to read config from Nacos, add the Nacos config starter and import the remote config.

Typical shape:

```yaml
spring:
  config:
    import:
      - optional:nacos:blog-springboot-prod.yaml?group=BLOG_GROUP
```

## Docker Compose example for server deployment

Create `docker-compose.yml` on the server:

```yaml
services:
  nacos:
    image: nacos/nacos-server:v3.2.0
    container_name: nacos
    restart: unless-stopped
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_USER: nacos
      MYSQL_SERVICE_PASSWORD: change-me
      MYSQL_DATABASE_NUM: 1
      NACOS_AUTH_TOKEN: replace-with-base64-token-over-32-chars
      NACOS_AUTH_IDENTITY_KEY: serverIdentity
      NACOS_AUTH_IDENTITY_VALUE: security
      JVM_XMS: 512m
      JVM_XMX: 512m
      JVM_XMN: 256m
      NACOS_AUTH_ENABLE: "true"
    ports:
      - "8080:8080"
      - "8848:8848"
      - "9848:9848"
    depends_on:
      - mysql

  mysql:
    image: mysql:8.0
    container_name: nacos-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: change-root-password
      MYSQL_DATABASE: nacos
      MYSQL_USER: nacos
      MYSQL_PASSWORD: change-me
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    ports:
      - "3307:3306"
    volumes:
      - ./mysql-data:/var/lib/mysql
      - ./mysql-init:/docker-entrypoint-initdb.d
```

## Deployment steps

1. Download the Nacos MySQL initialization SQL from the official Nacos release or `nacos-docker` project.
2. Put the SQL file into `./mysql-init/`.
3. Run `docker compose up -d`.
4. Open `http://your-server-ip:8080/index.html`.
5. Initialize the `nacos` admin password on first login.

## Security reminders

- Do not expose Nacos directly to the public internet.
- Put it behind an internal network, VPN, or reverse proxy with IP restrictions.
- Keep `JASYPT_ENCRYPTOR_PASSWORD` in the deployment environment, not in Nacos.
