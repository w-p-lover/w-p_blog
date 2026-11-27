import {createRouter, createWebHistory, RouteRecordRaw} from "vue-router";

const routes: RouteRecordRaw[] = [
    {
        path: "/living",
        component: () => import("@/views/Living/index.vue"),
        meta: {title: "生活"},
    },
    {
        path: "/album",
        component: () => import("@/views/Album/Album.vue"),
        meta: {title: "相册"},
    },
    {
        path: "/album/:albumId",
        component: () => import("@/views/Album/Photo.vue"),
        meta: {title: "相册"},
    },
    {
        path: "/book",
        component: () => import("@/views/Book/index.vue"),
        meta: {title: "书架"},
    },
    {
        path: "/book/:id",
        component: () => import("@/views/Book/BookDetail.vue"),
        meta: {title: "书籍详情"},
    },
    {
        path: "/music",
        component: () => import("@/views/Music/index.vue"),
        meta: {title: "音乐"},
    },
    {
        path: "/music/:id",
        component: () => import("@/views/Music/MusicDetail.vue"),
        meta: {title: "音乐详情"},
    },
    {
        path: "/steam",
        component: () => import("@/views/Steam/index.vue"),
        meta: {title: "游戏"},
    },
    {
        path: "/life",
        component: () => import("@/views/Life/index.vue"),
        meta: {title: "成长路线"},
    },
    {
        path: "/weather",
        component: () => import("@/views/Weather/index.vue"),
        meta: {title: "天气"},
    },
    {
        path: "/trend",
        component: () => import("@/views/Trending/index.vue"),
        meta: {title: "开源发现"},
    },
    {
        name: "talk",
        path: "/talk",
        component: () => import("@/views/Talk/TalkList.vue"),
        meta: {title: "说说"},
    },
    {
        name: "talkInfo",
        path: "/talk/:id",
        component: () => import("@/views/Talk/Talk.vue"),
        meta: {title: "说说"},
    },
    {
        path: "/",
        component: () => import("@/views/Home/index.vue"),
        meta: {
            title: "首页",
        },
    },
    {
        path: "/about",
        component: () => import("@/views/About/index.vue"),
        meta: {
            title: "关于",
        },
    },
    {
        path: "/category/:categoryId",
        component: () => import("@/views/Category/ArticleList.vue"),
    },
    {
        path: "/tag/:tagId",
        component: () => import("@/views/Tag/ArticleList.vue"),
    },
    {
        name: "friend",
        path: "/friend",
        component: () => import("@/views/Friend/index.vue"),
        meta: {
            title: "友链",
        },
    },
    {
        path: "/archive",
        component: () => import("@/views/Archive/index.vue"),
        meta: {
            title: "归档",
        },
    },
    {
        path: "/user",
        component: () => import("@/views/User/index.vue"),
        meta: {
            title: "个人中心",
        },
    },
    {
        path: "/category",
        component: () => import("@/views/Category/index.vue"),
        meta: {
            title: "分类",
        },
    },
    {
        path: "/tag",
        component: () => import("@/views/Tag/index.vue"),
        meta: {
            title: "标签",
        },
    },
    {
        name: "article",
        path: "/article/:id",
        component: () => import("@/views/Article/Article.vue"),
        meta: {
            title: "文章",
        },
    },
    {
        path: "/404",
        component: () => import("@/views/404/index.vue"),
        meta: {
            title: "404",
        },
    },
    {
        path: "/oauth/login/gitee",
        component: () => import("@/components/Oauth/index.vue"),
    },
    {
        path: "/oauth/login/github",
        component: () => import("@/components/Oauth/index.vue"),
    },
    {path: "/:catchAll(.*)", redirect: "/404"},
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;
