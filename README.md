## 博客介绍
基于大佬的源码做些改造，部署踩坑实录

需要原版的去大佬的地址看



1、MySQL8.0安装很费劲，我依然用5.7，不支持窗口函数，需要改造代码



2、补充了适用MySQL5.7的脚本，可以直接执行；











## 本地运行

1. 

## 项目特点

- 前台界面参考 Hexo 的 Shoka 和 Butterfly 设计，页面美观，响应式布局
- 后台管理基于若依二次开发，含有侧边栏，历史标签，面包屑等
- 前后端分离，Docker Compose 一键部署
- 采用 RABC 权限模型，使用 Sa-Token 进行权限管理
- 支持动态权限修改、动态菜单和路由
- 说说、友链、相册、留言弹幕墙、音乐播放器
- 支持代码高亮、图片预览、黑夜模式、点赞、取消点赞等功能
- 发布评论、回复评论、表情包
- 发送 HTML 邮件评论回复提醒，内容详细
- 接入第三方登录，减少注册成本
- 文章搜索支持关键字高亮分词
- 文章编辑使用 Markdown 编辑器
- 含有最新评论、文章目录、文章推荐和文章置顶功能
- 实现日志管理、定时任务管理、在线用户和下线用户
- 代码支持多种搜索模式（Elasticsearch 或 MYSQL），支持多种文件上传模式（OSS、COS、本地）
- 采用 Restful 风格的 API，注释完善，代码遵循阿里巴巴开发规范，有利于开发者学习

## 技术介绍

**前端：** Vue3 + Pinia + Vue Router + TypeScript + Axios + Element Plus + Naive UI + Echarts + Swiper

**后端：** SpringBoot + Mysql + Redis + Quartz + Thymeleaf + Nginx + Docker + Sa-Token + Swagger2 + MyBatisPlus + ElasticSearch + RabbitMQ + Canal

**其他：** 接入 QQ、Gitee、Github 第三方登录

## 运行环境



| 开发工具              | 说明                    |
| --------------------- | ----------------------- |
| IDEA                  | Java 、vue 开发工具 IDE |
|                       |                         |
| Dbeaver               | MySQL 远程连接工具      |
| Redis Desktop Manager | Redis 远程连接工具      |
| Xshell                | Linux 远程连接工具      |
| Xftp                  | Linux 文件上传工具      |

| 开发环境      | 版本   |
| ------------- | ------ |
| OpenJDK       | 11     |
| MySQL         | 8.0.27 |
| Redis         | 6.2.6  |
| Elasticsearch | 7.17.3 |
| RabbitMQ      | 3.9.11 |



## 后续计划

## 项目总结

整个项目花费了大量的心血，开发过程中参考了很多优秀的开源项目，在这里感谢大家的开源项目，收获了很多，希望我的项目能给你带来收获。

鸣谢项目：

- [ 风丶宇 ](https://github.com/X1192176811/blog)
- **[hexo-theme-shoka](https://github.com/amehime/hexo-theme-shoka)**
- [A Hexo Theme: Butterfly](https://github.com/jerryc127/hexo-theme-butterfly)
- [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)
- [vue3-element-admin](https://github.com/youlaitech/vue3-element-admin)
- [基于 Vue.js 的弹幕交互组件](https://github.com/hellodigua/vue-danmaku)
- ...