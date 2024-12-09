## 博客介绍

<p align="center"> 
  <a href="w-love-p.top" style="display: inline-block; text-align: left;">  
    <img src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img202410082058148.jpg" alt="一个比较大的博客" style="border-radius: 50%; width: 40%; height: auto; margin-left: 120px;"> 
  </a>  
</p>


<p align="center">
   基于Springboot + Vue3 开发的前后端分离博客
</p>

<p align="center">
   <a target="_blank" href="https://github.com/ttkican/Blog">
      <img src="https://img.shields.io/badge/JDK-11-green"/>
      <img src="https://img.shields.io/badge/springboot-2.6.14-green"/>
      <img src="https://img.shields.io/badge/saToken-1.34.0-green"/>
      <img src="https://img.shields.io/badge/vue-3.x-green"/>
      <img src="https://img.shields.io/badge/mysql-8.0.27-green"/>
      <img src="https://img.shields.io/badge/mybatis--plus-3.5.2-green"/>
      <img src="https://img.shields.io/badge/redis-6.2.6-green"/>
      <img src="https://img.shields.io/badge/elasticsearch-7.17.3-green"/>
      <img src="https://img.shields.io/badge/rabbitmq-3.9.11-green"/>
   </a>
</p>

## 在线地址

**项目链接：** [w-love-p.top](https://www.ttkwsd.top)

**测试账号：** test@qq.com，**密码**：123456

**Github 地址：**[http://w-love-p.top:8080/doc.html](api接口文档)

**接口文档：** [http://w-love-p.top:8080/doc.html](api接口文档)

## 本地运行

1. MySQL版本为`8.0.27`，npm版本为`9.4.0`，node版本为`v16.18.0`
2. SQL 文件位于根目录下的`blog.sql`，将其中的数据导入到自己本地数据库中
3. ES 映射文件位于`deploy`文件夹下，搜索策略可自主调整ES或SQL
4. 修改后端配置文件中的数据库等连接信息，项目中使用到的关于阿里云、腾讯云功能和第三方授权登录等需要自行开通
5. 一定要将前端`shoka-admin`和`shoka-blog`的 utils 下的 token.ts 中的`{ domain: domain }`给删除，要不然token会一直卡
6. 项目启动后，使用`admin@qq.com`管理员账号登录后台，密码为`123456`

## 项目特点

- 前台界面参考 Hexo 的 Shoka 和 Butterfly 设计，页面美观，响应式布局
- 后台管理基于若依二次开发，含有侧边栏，历史标签，面包屑等，支持EasyExcel导出
- 前后端分离，客户服务端分离，提供部署文件，支持Docker Compose 一键部署
- 采用 RABC 权限模型，使用 Sa-Token 进行权限管理，接入第三方登录，减少注册成本
- 说说、友链、相册、留言弹幕墙、音乐播放器，发布评论、回复评论、表情包
- 聊天室，支持图片，表情，文件发送
- 支持动态权限修改、动态菜单和路由
- 支持代码高亮、图片预览、黑夜模式、点赞、取消点赞等功能
- 发送 HTML 邮件评论回复提醒，内容详细，异步实现
- 文章搜索支持关键字高亮分词，文章编辑使用 Markdown 编辑器
- 含有最新评论、文章目录、文章推荐和文章置顶功能
- 实现日志管理、定时任务管理、在线用户和下线用户
- 代码支持多种搜索模式（Elasticsearch 或 MYSQL），支持多种文件上传模式（OSS、COS、本地）
- 采用 Restful 风格的 API，注释完善，代码遵循阿里巴巴开发规范，有利于开发者学习

## 技术介绍

**前端：** Vue3 + Pinia + Vue Router + TypeScript + Axios + Element Plus + Naive UI + Echarts + Swiper

**后端：** SpringBoot + Mysql + Redis + Quartz + WebSocket + Thymeleaf + Nginx + Docker + Sa-Token + Swagger2 + MyBatisPlus + ElasticSearch + RabbitMQ + Canal + EasyExcel

**其他：** 接入 QQ、Gitee、Github 第三方登录，TinyPng图片压缩

## 运行环境

**服务器：** 腾讯云 2 核 4G CentOS7.6

**对象存储：** 阿里云 OSS、腾讯云 COS

**最低配置：** 2 核 2G 服务器（关闭 ElasticSearch）

## 开发环境

| 开发工具                  | 说明            |
|-----------------------|---------------|
| IDEA                  | Java 开发工具 IDE |
| VSCode                | Vue 开发工具 IDE  |
| Datagrip              | MySQL 远程连接工具  |
| Redis Desktop Manager | Redis 远程连接工具  |
| Finalshell            | Linux 远程连接工具  |
| Xftp                  | Linux 文件上传工具  |

| 开发环境          | 版本              |
|---------------|-----------------|
| OpenJDK       | 11              |
| MySQL         | 8.0.27          |
| Redis         | 6.2.6           |
| Elasticsearch | 7.15.3 - 7.17.3 |
| RabbitMQ      | 3.9.11          |

## 后续计划

- [ ] 第三方登录使用 JustAuth
- [ ] 移动端文章目录
- [ ] 图片瀑布流布局
- [ ] 网页端图床
- [x] 缓存处理，构建整体的缓存结构
- [x] 聊天室，各种消息形式发送（图片，文件，文本等等）
- [x] 添加文章浏览方式，最新，热度，默认
