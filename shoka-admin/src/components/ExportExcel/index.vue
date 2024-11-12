<template>
  <el-drawer v-model="showSettings" :withHeader="false" direction="rtl" size="300px">
    <div class="drawer-container">
      <el-divider>信息导出配置</el-divider>
      <div class="drawer-item">
        <span>文章信息</span>
        <span class="comp-style">
          <el-switch v-model="exportSetting.articleInfo" class="drawer-switch"/>
        </span>
      </div>
      <div class="drawer-item">
        <span>说说信息</span>
        <span class="comp-style">
          <el-switch v-model="exportSetting.sayInfo" class="drawer-switch"/>
        </span>
      </div>
      <div class="drawer-item">
        <span>留言信息</span>
        <span class="comp-style">
          <el-switch v-model="exportSetting.messageInfo" class="drawer-switch"/>
        </span>
      </div>
      <div class="drawer-item">
        <span>用户信息</span>
        <span class="comp-style">
          <el-switch v-model="exportSetting.userInfo" class="drawer-switch"/>
        </span>
      </div>
      <div class="drawer-item">
        <span>访问信息</span>
        <span class="comp-style">
          <el-switch v-model="exportSetting.accessInfo" class="drawer-switch"/>
        </span>
      </div>
      <div class="button-container">
        <el-button type="primary" @click="exportExcel">导出信息</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import useStore from '@/store';

import {useDark, useToggle} from '@vueuse/core';
import {reactive, ref, toRefs, watch} from "vue";
import {messageConfirm} from "@/utils/modal";
import {getData} from "@/api/dict";
import {saveAs} from "file-saver";

const {setting} = useStore();
const showSettings = ref(false);
const isDark = useDark();
const toggleTheme = () => {
  const isDark = useDark();
  useToggle(isDark);
};
const openSetting = () => {
  showSettings.value = true;
};

const exportExcel = () => {
  messageConfirm("导出内容为动态配置数据").then(async () => {
    try {
      console.log(exportSetting)
      const response = await getData(exportSetting);
      const blob = new Blob([response.data], {type: 'application/vnd.ms-excel'});
      // 获取文件名
      const contentDisposition = response.headers['content-disposition'];
      let fileName = 'data.xlsx';
      if (contentDisposition && contentDisposition.indexOf('attachment') !== -1) {
        const matches = /filename="([^"]*)"/.exec(contentDisposition);
        if (matches != null && matches[1]) {
          fileName = matches[1];
        }
      }
      saveAs(blob, fileName);
    } catch (error) {
      console.error('导出数据时发生错误:', error);
    }
  }).catch(() => {
  });
};

// Reactive object to track the state of each switch
const exportSetting = reactive({
  articleInfo: false,
  sayInfo: false,
  messageInfo: false,
  userInfo: false,
  accessInfo: false,
});


defineExpose({
  openSetting,
})
</script>

<style lang="scss" scoped>
.drawer-container {
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;

  .drawer-item {
    font-size: 14px;
    padding: 12px 0;

    .comp-style {
      float: right;
      margin: -3px 8px 0 0;
    }
  }
}

::v-deep .el-switch__core {
  background: #b1c0c0;
}

.button-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;

  .el-button {
    background-color: #64b4c5 !important; /* 设置按钮为蓝色 */
    color: #fff !important; /* 设置字体为白色 */
  }
}

</style>
