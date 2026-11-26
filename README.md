# W-P Blog

个人技术博客。用于持续写作与发布技术文章，让读者搜索、阅读和交流。

当前运行范围：文章、分类/标签/归档、搜索、说说（配图/点赞/评论）、评论、关于/友链，以及账号、文件、权限和站点后台。首页以文章为中心，不承载 AI 问答、聊天、协作、娱乐工具或活动报名。

- `shoka-blog/`：Vue 3 读者端。`npm ci --legacy-peer-deps`、`npm run dev`；构建使用 `npm run build`。
- `shoka-admin/`：Vue 3 管理端。`npm ci --legacy-peer-deps`、`npm run dev`；构建使用 `npm run build`。
- `blog-springboot/`：Spring Boot 3 / Java 17 后端，启动和容器配置见[后端 README](blog-springboot/README.md)。

前后台可从 `.env.example` 复制 `.env.local`，配置 `VITE_API_BASE_URL`。本地默认 `http://localhost:8080`，正式部署可设为同域 `/api`，由反向代理去除前缀转发。当前界面保留原有照片/一言、推荐阅读、桌面宠物、音乐播放器、阅读动效和文章/评论点赞；没有重写前端或引入新的组件库。

[成品功能边界与旧代码恢复](blog-springboot/docs/00-项目总览/08-个人技术博客成品边界.md) · [本轮实施计划](blog-springboot/docs/02-阶段计划/06-个人技术博客精简计划.md) · [文档中心](blog-springboot/docs/README.md)

原版本留存在远端分支 [`留存/简历对应完整版本`](https://github.com/w-p-lover/w-p_blog/tree/%E7%95%99%E5%AD%98/%E7%AE%80%E5%8E%86%E5%AF%B9%E5%BA%94%E5%AE%8C%E6%95%B4%E7%89%88%E6%9C%AC)（`77bb6cc`），对应精简前最后一个已提交版本 `dd3030c`，用于简历过渡期的原功能查看与演示准备。该分支保留完整功能源码，并沿用远端既有的敏感信息清理；当前 `主分支` 继续维护精简博客。原始提交历史另保存在本地 `备份/精简前完整功能`，不推送清理前历史。

数据库、历史任务、附件和外部存储不删除。旧 AI/活动/工具文档保留为历史记录。仓库内其他独立服务不纳入博客当前运行与维护范围。
