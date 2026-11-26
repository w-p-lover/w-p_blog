export interface MenuItem {
  name: string;
  icon: string;
  path: string;
  matchPaths?: string[];
}

export interface MenuGroup extends MenuItem {
  children?: MenuItem[];
}

export const menuList: MenuGroup[] = [
  { name: "首页", icon: "home", path: "/" },
  { name: "归档", icon: "archives", path: "/archive", matchPaths: ["/article"] },
  { name: "分类", icon: "category", path: "/category" },
  { name: "标签", icon: "tag", path: "/tag" },
  { name: "说说", icon: "talk", path: "/talk" },
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
