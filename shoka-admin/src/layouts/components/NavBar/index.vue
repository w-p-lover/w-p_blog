<template>
  <div class="navbar">
    <!-- 折叠按钮 -->
    <hamburger class="hamburger-container"></hamburger>
    <!-- 面包屑 -->
    <breadcrumb class="breadcrumb-container"></breadcrumb>
    <div class="right-menu">
      <template v-if="device !== 'mobile'">
        <!-- 博客首页 -->
        <el-tooltip content="博客首页" effect="dark" placement="bottom">
          <div class="right-menu-item hover-effect">
            <svg-icon @click="openHome" icon-class="home" size="1.2rem"/>
          </div>
        </el-tooltip>
        <!-- 修改密码 -->
        <el-tooltip content="修改密码" effect="dark" placement="bottom">
          <password class="right-menu-item hover-effect"></password>
        </el-tooltip>
        <!-- 全屏 -->
        <screenfull class="right-menu-item hover-effect"></screenfull>
        <!-- 布局大小 -->
        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select class="right-menu-item hover-effect"/>
        </el-tooltip>
      </template>
      <el-dropdown @command="handleCommand" class="avatar-container right-menu-item hover-effect" trigger="click">
        <!-- 头像 -->
        <div class="avatar-wrapper">
          <img :src="user.avatar" class="user-avatar"/>
          <el-icon class="el-icon-caret-bottom">
            <caret-bottom/>
          </el-icon>
        </div>
        <!-- 选项 -->
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="setLayout">
              <span>布局设置</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="getData">
              <span>导出数据</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import breadcrumb from "@/components/Breadcrumb/index.vue";
import {getData} from "@/api/dict";
import hamburger from "@/components/Hamburger/index.vue";
import password from "@/components/Password/index.vue";
import Screenfull from '@/components/Screenfull/index.vue';
import SizeSelect from '@/components/SizeSelect/index.vue';
import useStore from "@/store";
import {saveAs} from 'file-saver';
import {messageConfirm} from "@/utils/modal";
import {computed} from "vue";

const {app, user} = useStore();
const device = computed(() => app.device);
const openHome = () => {
  window.open("http://w-love-p.top/");
};
const handleCommand = (command: string) => {
  switch (command) {
    case "setLayout":
      setLayout();
      break;
    case "logout":
      logout();
      break;
    case "getData":
      exportExcel();
      break;
    default:
      break;
  }
};
const logout = () => {
  messageConfirm("确定注销并退出系统吗？").then(() => {
    user.LogOut().then(() => {
      location.href = "/login";
    });
  }).catch(() => {
  });
};

const emits = defineEmits(['setLayout', 'exportExcel']);
const setLayout = () => {
  emits('setLayout');
};

const exportExcel = () => {
  emits('exportExcel');
};

</script>

<style lang="scss" scoped>
.navbar {
  height: 56px;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  z-index: 100;

  .hamburger-container {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 100%;
    cursor: pointer;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: #f3f4f6;
    }
  }

  .breadcrumb-container {
    padding: 0 16px;
    height: 100%;
    display: flex;
    align-items: center;
  }

  .right-menu {
    margin-left: auto;
    height: 100%;
    display: flex;
    align-items: center;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 100%;
      color: #6b7280;
      transition: all 0.2s ease;

      &.hover-effect {
        cursor: pointer;

        &:hover {
          color: #2563eb;
          background-color: #f3f4f6;
        }
      }
    }

    .avatar-container {
      position: relative;
      padding: 0 16px;
      width: auto;

      .avatar-wrapper {
        display: flex;
        align-items: center;

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 50%;
          object-fit: cover;
          border: 2px solid transparent;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          transition: all 0.2s ease;

          &:hover {
            transform: scale(1.05);
            border-color: rgba(37, 99, 235, 0.2);
          }
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          margin-left: 8px;
          font-size: 14px;
          color: #9ca3af;
          transition: all 0.2s ease;
        }
      }

      &:hover .el-icon-caret-bottom {
        color: #2563eb;
      }
    }
  }
}

/* 下拉菜单样式优化 */
::v-deep .el-dropdown-menu {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 4px 0;
  overflow: hidden;
  animation: dropdownFadeIn 0.2s ease-out;
}

::v-deep .el-dropdown-item {
  padding: 8px 16px;
  font-size: 14px;
  color: #374151;
  transition: all 0.2s ease;

  &:hover {
    background-color: #f3f4f6;
    color: #2563eb;
  }

  &.is-disabled {
    color: #d1d5db;
    cursor: not-allowed;
  }
}

::v-deep .el-dropdown-item.divided {
  border-top: 1px dashed #e5e7eb;
}

/* 动画效果 */
@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 面包屑样式优化 */
::v-deep .el-breadcrumb {
  font-size: 14px;

  .el-breadcrumb__item {
    .el-breadcrumb__inner {
      color: #6b7280;
      font-weight: 400;
      transition: color 0.2s ease;

      &:hover {
        color: #2563eb;
      }

      &.is-link {
        color: #2563eb;
      }
    }

    .el-breadcrumb__separator {
      color: #d1d5db;
      margin: 0 8px;
    }
  }
}

/* tooltip样式优化 */
::v-deep .el-tooltip__popper {
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
</style>