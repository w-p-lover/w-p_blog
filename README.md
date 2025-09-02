 

<p align="center">    <a href="https://w-love-p.top" style="display: inline-block; text-align: center;">       <img src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img202410082058148.jpg" alt="一个比较大的博客" style="border-radius: 12px; width: 50%; height: auto;">    </a>   </p> <p align="center">    基于 <strong>SpringBoot + Vue3</strong> 开发的前后端分离博客 </p> <p align="center">    <a target="_blank" href="https://github.com/w-p-lover/w-p_blog">       <img src="https://img.shields.io/badge/JDK-11-green"/>       <img src="https://img.shields.io/badge/SpringBoot-2.6.14-brightgreen"/>       <img src="https://img.shields.io/badge/SaToken-1.34.0-blue"/>       <img src="https://img.shields.io/badge/Vue-3.x-green"/>       <img src="https://img.shields.io/badge/MySQL-8.0.27-orange"/>       <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.2-yellow"/>       <img src="https://img.shields.io/badge/Redis-6.2.6-red"/>       <img src="https://img.shields.io/badge/ElasticSearch-7.17.3-lightblue"/>       <img src="https://img.shields.io/badge/RabbitMQ-3.9.11-brightgreen"/>    </a> </p>

------



## 🔗 在线地址

- **博客链接：** [w-love-p.top](https://www.ttkwsd.top?utm_source=chatgpt.com)
- **测试账号：** `test@qq.com`        **密码：** `123456`
- **GitHub 地址：** [https://github.com/w-p-lover/w-p_blog](https://github.com/w-p-lover/w-p_blog)
- **接口文档：** [http://w-love-p.top:8080/doc.html](http://w-love-p.top:8080/doc.html?utm_source=chatgpt.com)

------



## ⚙️ 本地运行步骤

1. **环境要求**
   - MySQL：`8.0.27`
   - npm：`9.4.0`
   - node：`v16.18.0`
2. **导入数据库**
   - SQL 文件位于根目录下的 `blog.sql`，导入至本地数据库
3. **配置 Elasticsearch**
   - ES 映射文件位于 `deploy` 文件夹，可根据需要选择 ES 或 MySQL 搜索
4. **修改后端配置**
   - 修改数据库连接信息
   - 阿里云、腾讯云功能及第三方授权需自行开通
5. **前端注意事项**
   - 删除 `shoka-admin` 和 `shoka-blog` 的 `utils/token.ts` 文件中 `{ domain: domain }`，否则 Token 校验会卡死
6. **后台登录**
   - 账号：`admin@qq.com`
   - 密码：`123456`

------



## ✨ 项目特点

- 前台界面参考 **Hexo Shoka & Butterfly** 设计，**美观 + 响应式布局**
- 后台管理基于 **若依二次开发**，带侧边栏、历史标签、面包屑，支持 **EasyExcel 导出**
- **前后端分离**，支持 **Docker Compose 一键部署**
- **RABC 权限模型 + Sa-Token**，接入第三方登录，降低注册成本
- **说说、友链、相册、留言墙、音乐播放器**，评论、回复、表情全支持
- **聊天室**：支持图片、表情、文件发送
- 动态权限修改、动态菜单和路由
- **代码高亮、图片预览、黑夜模式、点赞、取消点赞**
- **HTML 邮件评论回复提醒**，异步实现
- **文章搜索**：关键字高亮分词
- **Markdown 编辑器**，支持文章目录、置顶、推荐
- **日志管理、定时任务、在线用户管理**
- 支持 **Elasticsearch/MySQL 搜索模式** & **OSS、COS、本地文件上传模式**
- **Restful API**，代码注释完善，遵循阿里巴巴开发规范

------



### ✅ 新增功能

- **缓存处理**，构建整体缓存架构
- **聊天室增强**，支持多功能互动
- **文章浏览模式**：最新、热度、默认排序
- **书籍管理模块**，书源管理 + 界面美化
- **Gitee 爬虫兼容**，实时爬取状态反馈
- **和风天气 + 高德地图 API 集成**，支持地图交互 & 天气展示

------



## 🛠 技术栈

### 🖥 前端技术栈

- **框架**：Vue 3
- **状态管理**：Pinia
- **路由**：Vue Router
- **语言**：TypeScript
- **网络请求**：Axios
- **UI 组件库**：Element Plus、Naive UI
- **图表可视化**：ECharts
- **轮播插件**：Swiper

### ⚙ 后端技术栈
- **框架**：Spring Boot
- **数据库**：MySQL
- **缓存**：Redis
- **任务调度**：Quartz
- **实时通信**：WebSocket
- **模板引擎**：Thymeleaf
- **容器化部署**：Docker + Nginx
- **权限认证**：Sa-Token
- **接口文档**：Swagger2
- **ORM**：MyBatis-Plus
- **搜索引擎**：ElasticSearch
- **消息队列**：RabbitMQ
- **数据同步**：Canal
- **Excel 处理**：EasyExcel

 **其他：** 接入 **QQ / Gitee / GitHub 第三方登录**，**TinyPng 图片压缩**

------



## 💻 运行环境

- **服务器：** 腾讯云 2 核 4G CentOS7.6
- **对象存储：** 阿里云 OSS、腾讯云 COS
- **最低配置：** 2 核 2G（关闭 Elasticsearch）

------



## 🧑‍💻 开发环境

| 工具                  | 说明               |
| --------------------- | ------------------ |
| IDEA                  | Java 开发工具      |
| VSCode                | Vue 开发工具       |
| DataGrip              | MySQL 远程连接工具 |
| Redis Desktop Manager | Redis 远程连接工具 |
| FinalShell            | Linux 远程工具     |

| 环境          | 版本            |
| ------------- | --------------- |
| OpenJDK       | 11              |
| MySQL         | 8.0.27          |
| Redis         | 6.2.6           |
| Elasticsearch | 7.15.3 - 7.17.3 |
| RabbitMQ      | 3.9.11          |

------



## 📌 后续计划

- - [ ] 第三方登录使用 JustAuth 
- - [ ] 移动端文章目录 
- - [ ] 图片瀑布流布局
- - [ ] 网页端图床
