<template>
  <div :class="{ 'has-logo': showLogo }" class="sidebar-container">
    <!-- 网站Logo -->
    <logo v-if="showLogo" :collapse="isCollapse" class="sidebar-logo"/>
    <!-- 侧边栏 -->
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
          :default-active="activeMenu"
          :unique-opened="true"
          :collapse="isCollapse"
          :collapse-transition="false"
          :background-color="variables.menuBg"
          :text-color="variables.menuText"
          :active-text-color="variables.menuActiveText"
          class="sidebar-menu"
      >
        <sidebar-item
            v-for="route in routes"
            :item="route"
            :key="route.path"
            :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import variables from '@/assets/styles/variables.module.scss';
import useStore from "@/store";
import {computed} from "vue";
import {useRoute} from 'vue-router';
import Logo from './Logo.vue';
import SidebarItem from './SidebarItem.vue';

const {app, setting, permission} = useStore();
const route = useRoute();
const isCollapse = computed(() => app.isCollapse);
const showLogo = computed(() => setting.sidebarLogo);
const routes = computed(() => permission.routes);
const activeMenu = computed(() => route.path);
</script>

<style scoped>
.sidebar-container {
  width: 200px;
  height: 100vh;
  transition: width 0.3s ease;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  z-index: 10;
  position: relative;
  overflow: visible !important; /* 关键：允许子菜单完整显示 */
}

/* 折叠状态适配 */



/* Logo样式 */

.has-logo .sidebar-menu {
  margin-top: 10px;
}


/* 菜单项基础样式 */
:deep(.el-menu-item),
:deep(.el-sub-menu__title){
  position: relative;
  height: 48px;
  line-height: 48px;
  padding: 0 16px !important;
  margin: 0 8px 4px !important;
  border-radius: 8px;
  transition: all 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 图标样式 */
:deep(.el-sub-menu .el-sub-menu__title .el-icon) {
  margin-left: 12px;
}
/* 一级子菜单项 */
:deep(.el-sub-menu .el-menu-item) {
  padding-left: 40px !important;
  width: calc(100% - 16px) !important;

}
:deep(.el-menu-item .el-menu-tooltip__trigger) {
  left : -6px
}

</style>
