export interface MenuItem {
  name: string;
  icon: string;
  path: string;
  matchPaths?: string[];
}

export interface MenuGroup {
  name: string;
  icon: string;
  path: string;
  matchPaths?: string[];
  children?: MenuItem[];
}

export const articleMenuItems: MenuItem[] = [
  {
    name: "归档",
    icon: "archives",
    path: "/archive",
  },
  {
    name: "分类",
    icon: "category",
    path: "/category",
  },
  {
    name: "标签",
    icon: "tag",
    path: "/tag",
  },
];

export const activityMenuItems: MenuItem[] = [
  {
    name: "说说",
    icon: "talk",
    path: "/talk",
  },
  {
    name: "相册",
    icon: "album",
    path: "/album",
  },
  {
    name: "音乐库",
    icon: "laba",
    path: "/music",
  },
  {
    name: "留言板",
    icon: "message",
    path: "/message",
  },
  {
    name: "友链",
    icon: "friend",
    path: "/friend",
  },
];

export const labMenuItems: MenuItem[] = [
  {
    name: "实验室",
    icon: "flower",
    path: "/lab",
  },
  {
    name: "AI问答",
    icon: "ai",
    path: "/ai-chat",
  },
  {
    name: "共享",
    icon: "trumpet",
    path: "/collab",
  },
  {
    name: "天气",
    icon: "edit",
    path: "/weather",
  },
  {
    name: "项目",
    icon: "search",
    path: "/trend",
  },
  {
    name: "游戏",
    icon: "steam",
    path: "/steam",
  },
  {
    name: "路线",
    icon: "fun",
    path: "/life",
  },
  {
    name: "书目",
    icon: "qizhi",
    path: "/book",
  },
  {
    name: "聊天",
    icon: "friend",
    path: "/chat",
  },
];

export const labNavItems: MenuItem[] = labMenuItems.filter((item) =>
  ["实验室", "AI问答", "共享", "项目"].includes(item.name)
);

export const menuList: MenuGroup[] = [
  {
    name: "首页",
    icon: "home",
    path: "/",
  },
  {
    name: "文章",
    icon: "article",
    path: "/archive",
    matchPaths: ["/article", "/archive", "/category", "/tag"],
    children: articleMenuItems,
  },
  {
    name: "动态",
    icon: "talk",
    path: "/talk",
    matchPaths: ["/talk", "/album", "/message", "/friend", "/music"],
    children: activityMenuItems,
  },
  {
    name: "实验室",
    icon: "flower",
    path: "/lab",
    matchPaths: ["/lab", "/ai-chat", "/collab", "/weather", "/trend", "/steam", "/life", "/book", "/chat"],
    children: labNavItems,
  },
  {
    name: "关于",
    icon: "plane",
    path: "/about",
  },
];

export const isMenuItemActive = (item: MenuItem | MenuGroup, routePath: string, routeTitle?: unknown) => {
  if (item.path === "/") {
    return routePath === "/";
  }
  const matchesPath = item.matchPaths?.some((path) => routePath === path || routePath.startsWith(`${path}/`));
  return item.path === routePath || matchesPath || item.name === routeTitle;
};

export const isMenuGroupActive = (menu: MenuGroup, routePath: string, routeTitle?: unknown) => {
  if (isMenuItemActive(menu, routePath, routeTitle)) {
    return true;
  }
  if (menu.children) {
    return menu.children.some((item) => isMenuItemActive(item, routePath, routeTitle));
  }
  return false;
};
