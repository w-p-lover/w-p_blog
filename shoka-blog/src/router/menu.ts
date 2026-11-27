export interface MenuItem {
  name: string;
  icon: string;
  path: string;
  matchPaths?: string[];
}

export interface MenuGroup extends MenuItem {
  children?: MenuItem[];
}

export const lifeMenuItems = [
  { name: "相册", icon: "album", path: "/album", description: "照片里的日常与旅行" },
  { name: "书架", icon: "qizhi", path: "/book", description: "读过的书，想读的故事" },
  { name: "音乐", icon: "laba", path: "/music", description: "歌单、收藏与循环播放" },
  { name: "游戏", icon: "steam", path: "/steam", description: "游戏记录与收藏" },
  { name: "成长路线", icon: "fun", path: "/life", description: "一路走过的学习与生活" },
  { name: "天气", icon: "edit", path: "/weather", description: "看看今天的天空" },
  { name: "开源发现", icon: "search", path: "/trend", description: "感兴趣的项目与工具" },
];

export const menuList: MenuGroup[] = [
  { name: "首页", icon: "home", path: "/" },
  { name: "归档", icon: "archives", path: "/archive", matchPaths: ["/article"] },
  { name: "分类", icon: "category", path: "/category" },
  { name: "标签", icon: "tag", path: "/tag" },
  { name: "说说", icon: "talk", path: "/talk" },
  { name: "生活", icon: "flower", path: "/living", matchPaths: lifeMenuItems.map(item => item.path) },
  { name: "友链", icon: "friend", path: "/friend" },
  { name: "关于", icon: "plane", path: "/about" },
];

export const isMenuItemActive = (item: MenuItem | MenuGroup, routePath: string, routeTitle?: unknown) => {
  if (item.path === "/") return routePath === "/";
  return item.path === routePath
    || routePath.startsWith(`${item.path}/`)
    || item.matchPaths?.some((path) => routePath === path || routePath.startsWith(`${path}/`))
    || item.name === routeTitle;
};

export const isMenuGroupActive = (menu: MenuGroup, routePath: string, routeTitle?: unknown) =>
  isMenuItemActive(menu, routePath, routeTitle)
  || menu.children?.some((item) => isMenuItemActive(item, routePath, routeTitle));
