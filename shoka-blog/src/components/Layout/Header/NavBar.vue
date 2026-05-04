<template>
  <div class="menu">
    <div class="menu-item title">
      <router-link to="/" class="menu-btn">
        {{ blog.blogInfo.siteConfig.siteName }}
      </router-link>
    </div>
    <template v-for="menu of menuList" :key="menu.name">
      <div v-if="!menu.children" class="menu-item" :class="{ active: isMenuGroupActive(menu, route.path, route.meta.title) }">
        <router-link :to="menu.path" class="menu-btn">
          <svg-icon :icon-class="menu.icon"></svg-icon>
          {{ menu.name }}
        </router-link>
      </div>
      <div v-else class="menu-item dropdown">
        <a class="menu-btn drop">
          <svg-icon :icon-class="menu.icon"></svg-icon>
          {{ menu.name }} </a>
        <ul class="submenu">
          <li class="subitem" v-for="submenu of menu.children" :key="submenu.name"
              :class="{ active: isMenuItemActive(submenu, route.path, route.meta.title) }">
            <router-link class="link" :to="submenu.path">
              <svg-icon :icon-class="submenu.icon"></svg-icon>
              {{ submenu.name }}
            </router-link>
          </li>
        </ul>
      </div>
    </template>
    <div class="menu-item">
      <a v-if="!user.id" @click="app.loginFlag = true" class="menu-btn">
        <svg-icon icon-class="user"></svg-icon>
        登录
      </a>
      <template v-else>
        <img class="user-avatar drop" :src="user.avatar"/>
        <ul class="submenu">
          <li class="subitem" :class="{ active: route.meta.title === '个人中心' }">
            <router-link to="/user" class="link">
              <svg-icon icon-class="author"></svg-icon>
              个人中心
            </router-link>
          </li>
          <li class="subitem">
            <a class="link" @click="logout">
              <svg-icon icon-class="logout"></svg-icon>
              退出 </a>
          </li>
        </ul>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import {isMenuGroupActive, isMenuItemActive, menuList} from "@/router/menu";
import useStore from "@/store";

const {user, app, blog} = useStore();
const router = useRouter();
const route = useRoute();
const logout = () => {
  if (route.path == "/user") {
    router.go(-1);
  }
  user.LogOut();
  window.$message?.success("退出成功");
};
</script>

<style lang="scss" scoped>
.user-avatar {
  display: inline-block;
  position: relative;
  top: 0.3rem;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
}

.menu {
  display: flex;
  align-items: center;
  height: 100%;
}

.menu-item {
  position: relative;
  display: inline-block;
  padding: 0 0.35rem;
  letter-spacing: 0;
  font-size: 0.95rem;
  text-align: center;
  color: var(--grey-7);

  &:not(.title) .menu-btn::before {
    content: none;
  }

  &:hover .submenu {
    display: block;
  }
}

.menu-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  min-height: 2.25rem;
  padding: 0 0.68rem;
  border-radius: 999px;
  transition: color 0.2s ease, background 0.2s ease;
}

.title .menu-btn {
  padding-left: 0;
  color: var(--grey-8);
  font-size: 1.18rem;
  font-weight: 700;
}

.menu-item:not(.title):hover .menu-btn,
.menu-item.active:not(.dropdown) .menu-btn {
  color: var(--home-accent);
  background: var(--home-accent-soft);
}

.menu-item.active:not(.dropdown) .menu-btn::before,
.menu-item:not(.dropdown):hover .menu-btn::before {
  width: 0;
}

.submenu {
  display: none;
  position: absolute;
  left: 7px;
  width: max-content;
  margin-top: 0.5rem;
  padding: 0.35rem;
  border: 1px solid var(--home-border);
  background: var(--home-surface-strong);
  box-shadow: var(--home-shadow);
  border-radius: 0.65rem;
  animation: slideUpIn 0.3s;

  &::before {
    position: absolute;
    top: -1.25rem;
    left: 0;
    width: 100%;
    height: 2.5rem;
    content: "";
  }
}

.subitem {
  display: block;
  font-size: 1rem;

  &:first-child {
    border-radius: 0.625rem 0 0 0;
  }

  &:last-child {
    border-radius: 0 0 0.625rem 0;
  }

  .link {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.3rem 0.7rem;
    width: 100%;
    text-shadow: none;
    border-radius: 0.45rem;
  }

  &:hover .link {
    transform: translateX(0.3rem);
  }
}

.submenu .subitem.active,
.submenu .subitem:hover {
  color: var(--grey-0);
  background: var(--home-accent);
  box-shadow: none;
}

.sub.menu .submenu {
  background-color: var(--grey-1);
}

.drop::after {
  content: "";
  display: inline-block;
  vertical-align: middle;
  border: 0.3rem solid transparent;
  border-top-color: currentColor;
  border-bottom: 0;
}

@media (max-width: 865px) {
  .menu {
    justify-content: center;
  }

  .menu .menu-item {
    display: none;
  }

  .menu .title {
    display: block;
  }
}
</style>
