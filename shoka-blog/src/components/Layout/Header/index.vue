<template>
  <header class="header-wrapper" :class="fixedClass">
    <!-- 切换按钮 -->
    <Toggle></Toggle>
    <!-- 菜单 -->
    <NavBar :class="{ sub: y > 0 }"></NavBar>
    <!-- 右侧按钮 -->
    <ul class="right">
      <li class="item">
        <svg-icon style="cursor: pointer;" :icon-class="isDark ? 'moon' : 'sun'" @click="toggle()"></svg-icon>
      </li>
      <li class="item">
        <svg-icon style="cursor: pointer;" icon-class="search" @click="app.searchFlag = true"></svg-icon>
      </li>
    </ul>
  </header>
</template>

<script setup lang="ts">
import useStore from "@/store";
import {useDark, useScroll} from "@vueuse/core";
import {useToggle} from '@vueuse/shared';

const {app} = useStore();
const {y} = useScroll(window);
const isDark = useDark({
  selector: 'html',
  attribute: 'theme',
  valueDark: 'dark',
  valueLight: 'light',
})
const toggle = useToggle(isDark);
const fixedClass = ref("");
watch(y, (newValue, oldValue) => {
  if (newValue > 0) {
    if (newValue < oldValue) {
      fixedClass.value = "show up";
    } else {
      fixedClass.value = "show down";
    }
  } else {
    fixedClass.value = "";
  }
});
</script>

<style lang="scss" scoped>
.header-wrapper {
  position: fixed;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 3.75rem;
  padding: 0 1.9rem;
  border-bottom: 1px solid var(--nav-border);
  background: var(--nav-bg-initial);
  backdrop-filter: blur(12px);
  text-shadow: none;
  color: var(--text-color);
  transition: all 0.2s ease-in-out 0s;
  z-index: 9;
}

.show {
  background: var(--nav-bg);
  box-shadow: var(--nav-shadow);
  text-shadow: none;
  color: var(--text-color);
}

.up {
  transform: translateY(0);
}

.down {
  transform: translateY(-100%);
}

.right {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 100%;

  .item {
    padding: 0.625rem 0.5rem;
    color: var(--grey-6);
    transition: color 0.2s ease;

    &:hover {
      color: var(--home-accent);
    }
  }
}

@media (max-width: 991px) {
  .header-wrapper {
    padding: 0;
  }

}
</style>
