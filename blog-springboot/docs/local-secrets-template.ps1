# 本地开发建议先启用 dev 配置
$env:SPRING_PROFILES_ACTIVE = "dev"

# Jasypt 主密钥（仅在你使用 ENC(...) 时需要）
$env:JASYPT_ENCRYPTOR_PASSWORD = "replace-with-your-master-password"

# MySQL
$env:BLOG_DB_URL = "jdbc:mysql://127.0.0.1:3306/blog?serverTimezone=Asia/Shanghai&allowMultiQueries=true&rewriteBatchedStatements=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8"
$env:BLOG_DB_HOST = "127.0.0.1"
$env:BLOG_DB_PORT = "3306"
$env:BLOG_DB_NAME = "blog"
$env:BLOG_DB_USERNAME = "root"
$env:BLOG_DB_PASSWORD = "replace-with-your-db-password"

# Redis / RabbitMQ
$env:BLOG_REDIS_HOST = "127.0.0.1"
$env:BLOG_REDIS_PASSWORD = "replace-with-your-redis-password"
$env:BLOG_RABBITMQ_HOST = "127.0.0.1"
$env:BLOG_RABBITMQ_USERNAME = "guest"
$env:BLOG_RABBITMQ_PASSWORD = "replace-with-your-rabbitmq-password"

# 邮箱
$env:BLOG_MAIL_USERNAME = "replace@example.com"
$env:BLOG_MAIL_PASSWORD = "replace-with-your-mail-password"

# AI / 第三方服务
$env:BLOG_OPENAI_API_KEY = "replace-with-your-openai-compatible-key"
$env:BLOG_DASHSCOPE_API_KEY = "replace-with-your-dashscope-key"
$env:BLOG_HEFENG_API_KEY = "replace-with-your-hefeng-key"
$env:BLOG_GITEE_CLIENT_ID = "replace-with-your-gitee-client-id"
$env:BLOG_GITEE_CLIENT_SECRET = "replace-with-your-gitee-client-secret"
$env:BLOG_GITHUB_CLIENT_ID = "replace-with-your-github-client-id"
$env:BLOG_GITHUB_CLIENT_SECRET = "replace-with-your-github-client-secret"
