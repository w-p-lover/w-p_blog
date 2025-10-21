<template>
  <div class="app-container">
    <!-- 操作按钮 -->
    <el-row :gutter="10" class="operation-bar mb15">
      <el-col :span="1.5">
        <el-button
            type="primary"
            icon="Promotion"
            @click="openModel"
            class="create-btn"
        >
          发布说说
        </el-button>
      </el-col>
    </el-row>

    <!-- 说说状态筛选 -->
    <div class="status-filter-container mb15">
      <span class="status-label">状态筛选：</span>
      <div class="status-options">
        <span
            :class="isActive(undefined)"
            @click="changeStatus(undefined)"
            class="status-option"
        >
          全部
        </span>
        <span
            :class="isActive(1)"
            @click="changeStatus(1)"
            class="status-option"
        >
          <el-icon size="14" class="status-icon"><Hide/></el-icon> 公开
        </span>
        <span
            :class="isActive(2)"
            @click="changeStatus(2)"
            class="status-option"
        >
          <el-icon size="14" class="status-icon"><Lock/></el-icon> 私密
        </span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="talkList.length === 0" class="empty-state">
      <div class="empty-image">
        <svg-icon icon-class="message" size="3rem" color="#e5e7eb"/>
      </div>
      <p class="empty-text">暂无说说内容</p>
      <p class="empty-subtext">点击发布按钮，记录你的第一条动态吧～</p>
      <el-button
          type="primary"
          size="small"
          @click="openModel"
          class="empty-action"
      >
        发布第一条说说
      </el-button>
    </div>

    <!-- 时间轴流布局的说说列表 -->
    <div class="timeline-container" v-else>
      <!-- 时间轴主轴 -->
      <div class="timeline-axis">
        <!-- 主轴装饰点 -->
        <div class="axis-dot" v-for="(i) in 8" :key="i"></div>
      </div>

      <!-- 说说条目 - 左右交错排列 -->
      <div
          v-for="(talk, index) of talkList"
          :key="talk.id"
          :class="['timeline-item', index % 2 === 0 ? 'left' : 'right']"
      >
        <!-- 时间点标记（带状态色） -->
        <div class="timeline-dot">
          <div
              class="dot-inner"
              :class="[
              talk.isTop === 1 ? 'pinned-dot' : '',
              talk.status === 2 ? 'private-dot' : 'public-dot'
            ]"
          ></div>
        </div>

        <!-- 说说内容容器（加边框+背景层次） -->
        <div class="talk-content-wrapper">
          <!-- 顶部信息栏 -->
          <div class="talk-header">
            <div class="user-info">
              <img
                  class="user-avatar"
                  :src="talk.avatar || defaultAvatar"
                  alt="用户头像"
              >
              <div class="user-info-inner">
                <span class="user-name">{{ talk.nickname }}</span>
                <span class="user-tag" v-if="talk.isTop === 1">
                  <svg-icon icon-class="top" size="0.6rem"/> 置顶用户
                </span>
              </div>
            </div>

            <!-- 操作菜单 -->
            <el-dropdown
                trigger="click"
                @command="handleOperation"
                class="operation-menu"
            >
              <el-icon class="more-icon">
                <MoreFilled/>
              </el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="'update' + talk.id">编辑</el-dropdown-item>
                  <el-dropdown-item :command="'delete' + talk.id">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <!-- 元信息：时间 + 状态标签（加小图标） -->
          <div class="talk-meta">
            <span class="talk-time">
              <el-icon size="12" class="meta-icon"><Clock/></el-icon>
              {{ formatDateTime(talk.createTime) }}
            </span>

            <!-- 置顶标签 -->
            <el-tag
                v-if="talk.isTop === 1"
                size="small"
                color="#ff7242"
                class="status-tag"
            >
              <svg-icon icon-class="top" size="0.7rem"/>
              置顶
            </el-tag>

            <!-- 私密标签 -->
            <el-tag
                v-if="talk.status === 2"
                size="small"
                color="#909399"
                class="status-tag"
            >
              <el-icon size="12">
                <Lock/>
              </el-icon>
              私密
            </el-tag>
          </div>

          <!-- 说说内容（加前缀图标） -->
          <div class="talk-text">
            <span class="text-prefix">
              <el-icon size="14" :class="talk.status === 2 ? 'private-prefix' : 'public-prefix'">
                <Picture/>
              </el-icon>
            </span>
            <span v-html="talk.talkContent"></span>
          </div>

          <!-- 说说图片（加边框+hover阴影） -->
          <div class="talk-images" v-if="talk.imgList && talk.imgList.length">
            <div
                class="image-item"
                v-for="(img, imgIndex) of talk.imgList"
                :key="imgIndex"
                :style="getImageGridStyle(talk.imgList.length, imgIndex)"
            >
              <el-image
                  :src="img"
                  :preview-src-list="previewList"
                  fit="cover"
                  class="image"
                  :fallback="imageFallback"
              ></el-image>
            </div>
          </div>

          <!-- 底部互动标识（新增，填充底部空白） -->
          <div class="talk-footer" v-if="talk.imgList && talk.imgList.length">
            <span class="image-count">
              <el-icon size="12"><Picture/></el-icon>
              {{ talk.imgList.length }}张图片
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页（调整位置，减少底部空白） -->
    <el-pagination
        v-if="count > 0"
        class="pagination-container"
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        layout="prev, pager, next, ->, jumper, total"
        :total="count"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    ></el-pagination>

    <!-- 添加或修改对话框（保持不变） -->
    <el-dialog
        :title="title"
        v-model="addOrUpdate"
        @close="close"
        width="750px"
        append-to-body
        class="talk-dialog"
    >
      <el-form :model="talkForm" class="talk-form">
        <el-form-item prop="talkContent">
          <Editor
              ref="editorRef"
              v-model:text="talkForm.talkContent"
              placeholder="分享你的想法..."
              class="editor"
          ></Editor>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <el-row :gutter="10" align="middle" class="editor-toolbar">
        <el-col :span="1.5">
          <!-- 表情选择器 -->
          <el-popover
              placement="bottom-start"
              :width="460"
              trigger="click"
              class="emoji-popover"
          >
            <template #reference>
              <span class="tool-icon">
                <svg-icon icon-class="emoji" size="1.4rem" color="#606266"/>
              </span>
            </template>
            <div class="emoji-container">
              <span
                  class="emoji-item"
                  v-for="(value, key, index) of emojiList"
                  :key="index"
                  @click="addEmoji(key, value)"
              >
                <img :src="value" :title="key" class="emoji" width="24" height="24" alt=""/>
              </span>
            </div>
          </el-popover>
        </el-col>

        <el-col :span="1.5">
          <!-- 图片上传 -->
          <el-upload
              :headers="authorization"
              accept="image/*"
              multiple
              action="http://121.41.87.40:8080/admin/talk/upload"
              :before-upload="beforeUpload"
              :on-success="handleSuccess"
              :show-file-list="false"
              class="upload-trigger"
          >
            <span class="tool-icon">
              <svg-icon icon-class="album" size="1.5rem" color="#606266"/>
            </span>
          </el-upload>
        </el-col>

        <el-col :span="1.5" :offset="14">
          <el-switch
              style="margin-right:7px"
              v-model="talkForm.isTop"
              inactive-text="置顶"
              :active-value="1"
              :inactive-value="0"
              class="top-switch"
          />
        </el-col>

        <el-col :span="1.5">
          <el-dropdown trigger="click" @command="handleCommand" class="status-dropdown">
            <span class="el-dropdown-link">
              {{ statusTitle }}
              <el-icon class="el-icon--right">
                <arrow-down/>
              </el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                    v-for="(item, index) of statusList"
                    :key="index"
                    :command="item.value"
                >
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-col>

        <el-col :span="1.5">
          <el-button
              type="primary"
              :disabled="talkForm.talkContent === ''"
              @click="submitForm"
              class="submit-btn"
          >
            发布
          </el-button>
        </el-col>
      </el-row>

      <!-- 已上传图片 -->
      <div v-if="uploadList.length > 0" class="uploaded-images">
        <el-upload
            accept="image/*"
            action="/api/admin/talk/upload"
            :headers="authorization"
            list-type="picture-card"
            :file-list="uploadList"
            multiple
            :before-upload="beforeUpload"
            :on-success="handleSuccess"
            :on-remove="handleRemove"
            :on-preview="handlePictureCardPreview"
            class="upload-list"
        >
          <el-icon>
            <Plus/>
          </el-icon>
        </el-upload>
      </div>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog v-model="dialogVisible" append-to-body class="image-preview">
      <img :src="dialogImageUrl" style="max-width:100%" alt=""/>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 新增引入图标组件
import {MoreFilled, Hide, ArrowDown, Plus, Lock, Clock, Picture} from '@element-plus/icons-vue';
import {addTalk, deleteTalk, editTalk, getTalkList, updateTalk} from "@/api/talk";
import {Talk, TalkForm, TalkQuery} from "@/api/talk/types";
import Editor from "@/components/Editor/index.vue";
import {Picture as PictureModel} from "@/model";
import {formatDateTime} from "@/utils/date";
import emojiList from "@/utils/emoji";
import {messageConfirm, notifySuccess} from "@/utils/modal";
import {getToken, token_prefix} from "@/utils/token";
import {AxiosResponse} from "axios";
import {UploadFile, UploadRawFile} from 'element-plus';
import * as imageConversion from 'image-conversion';
import {computed, onMounted, reactive, ref, toRefs} from 'vue';

// 静态资源配置
const defaultAvatar = 'https://picsum.photos/200/200?grayscale&blur=2'; // 默认头像
const imageFallback = 'https://picsum.photos/300/300?blur=1&grayscale'; // 图片加载失败占位图

// 编辑器引用
const editorRef = ref();
// 图片预览相关
const dialogImageUrl = ref('');
const dialogVisible = ref(false);

// 状态选中样式计算
const isActive = computed(() => {
  return function (value: number | undefined) {
    return queryParams.value.status === value ? "active-status" : "";
  }
});

// 授权头计算
const authorization = computed(() => {
  return {
    Authorization: token_prefix + getToken(),
  }
});

// 状态标题计算
const statusTitle = computed(() => {
  let label = "公开";
  statusList.value.forEach(item => {
    if (item.value === talkForm.value.status) {
      label = item.label;
    }
  })
  return label;
})

// 响应式数据
const data = reactive({
  count: 0,
  title: "",
  addOrUpdate: false,
  queryParams: {
    current: 1,
    size: 6, // 增加每页条数，减少页面空白
    status: undefined,
  } as TalkQuery,
  statusList: [
    {value: 1, label: "公开"},
    {value: 2, label: "私密"}
  ],
  previewList: [] as string[],
  talkForm: {
    id: undefined,
    talkContent: "",
    images: "",
    isTop: 0,
    status: 1,
  } as TalkForm,
  talkList: [] as Talk[],
  uploadList: [] as PictureModel[],
});

const {
  count,
  title,
  addOrUpdate,
  queryParams,
  statusList,
  previewList,
  talkForm,
  talkList,
  uploadList,
} = toRefs(data);

/**
 * 获取图片网格布局样式（调整间距，减少空白）
 * @param imgCount 图片总数
 * @param index 当前图片索引
 */
const getImageGridStyle = (imgCount: number, index: number) => {
  const baseMargin = '6px';
  if (imgCount === 1) {
    return {
      width: 'auto',       // 宽度自适应内容
      maxWidth: '200px',   // 单张图片最大宽度（小尺寸）
      height: 'auto',
    };
  }
  // 2张及以上：每行2列，限制单张最大宽度
  return {
    width: `calc(50% - ${baseMargin})`,  // 2列布局
    maxWidth: '180px',                   // 单张最大宽度（更小）
    aspectRatio: '4/3',                  // 紧凑比例
    marginRight: index % 2 === 0 ? baseMargin : '0',
    marginBottom: baseMargin
  };
};

// 上传成功处理
const handleSuccess = (response: AxiosResponse) => {
  uploadList.value.push({url: response.data});
};

// 移除上传文件
const handleRemove = (file: UploadFile) => {
  uploadList.value = uploadList.value.filter(item => item.url !== file.url);
};

// 预览图片
const handlePictureCardPreview = (file: UploadFile) => {
  dialogImageUrl.value = file.url!;
  dialogVisible.value = true;
};

// 上传前处理（压缩）
const beforeUpload = (rawFile: UploadRawFile) => {
  return new Promise(resolve => {
    if (rawFile.size / 1024 < 200) {
      resolve(rawFile);
    }
    imageConversion
        .compressAccurately(rawFile, 200)
        .then(res => {
          resolve(res);
        });
  });
};

// 处理操作命令（编辑/删除）
const handleOperation = (command: string) => {
  const type = command.substring(0, 6);
  talkForm.value.id = Number(command.substring(6));

  if (type === "delete") {
    messageConfirm("确认删除这条说说吗?").then(() => {
      deleteTalk(talkForm.value.id).then(({data}) => {
        if (data.flag) {
          notifySuccess(data.msg);
          getList();
        }
      });
    }).catch(() => {
    });
  } else {
    editTalk(talkForm.value.id).then(({data}) => {
      if (data.flag) {
        talkForm.value = data.data;
        if (data.data.imgList) {
          uploadList.value = data.data.imgList.map(url => ({url}));
        }
        title.value = "修改说说";
        addOrUpdate.value = true;
      }
    })
  }
};

// 处理状态选择
const handleCommand = (command: number) => {
  talkForm.value.status = command;
};

// 添加表情
const addEmoji = (key: string, value: string) => {
  editorRef.value.addText(`<img src="${value}" width="24" height="24" alt="${key}" style="margin: 0 1px; vertical-align: text-bottom"/>`);
};

// 切换状态筛选
const changeStatus = (value: number | undefined) => {
  queryParams.value.current = 1;
  previewList.value = [];
  queryParams.value.status = value;
  getList();
};

// 分页大小变化
const handleSizeChange = (size: number) => {
  previewList.value = [];
  queryParams.value.size = size;
  getList();
};

// 当前页变化
const handleCurrentChange = (current: number) => {
  previewList.value = [];
  queryParams.value.current = current;
  getList();
};

// 提交表单
const submitForm = () => {
  if (uploadList.value.length > 0) {
    talkForm.value.images = JSON.stringify(uploadList.value.map(item => item.url));
  } else {
    talkForm.value.images = "";
  }

  if (talkForm.value.id !== undefined) {
    updateTalk(talkForm.value).then(({data}) => {
      if (data.flag) {
        uploadList.value = [];
        notifySuccess(data.msg);
        getList();
      }
      addOrUpdate.value = false;
    })
  } else {
    addTalk(talkForm.value).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
      }
      addOrUpdate.value = false;
    });
  }
};

// 重置表单
const reset = () => {
  talkForm.value = {
    id: undefined,
    talkContent: "",
    images: "",
    isTop: 0,
    status: 1,
  };
  uploadList.value = [];
  editorRef.value?.clear();
};

// 关闭对话框
const close = () => {
  reset();
  addOrUpdate.value = false;
};

// 打开发布对话框
const openModel = () => {
  reset();
  title.value = "发布说说";
  addOrUpdate.value = true;
};

// 获取说说列表
const getList = () => {
  getTalkList(queryParams.value).then(({data}) => {
    talkList.value = data.data.recordList || [];
    previewList.value = [];
    if (talkList.value.length) {
      talkList.value.forEach(item => {
        if (item.imgList) {
          previewList.value.push(...item.imgList);
        }
      });
    }
    count.value = data.data.count || 0;
  });
};

// 页面挂载时加载数据
onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
// 基础变量
$primary: #3b82f6;
$primary-light: #eff6ff;
$primary-dark: #2563eb;
$private-color: #909399;
$pinned-color: #ff7242;
$text-gray: #4b5563;
$text-light-gray: #9ca3af;
$border-light: #e5e7eb;
$bg-light: #f9fafb;
$spacing-sm: 8px;
$spacing-md: 16px;
$spacing-lg: 24px;

// 页面容器（减少顶部空白）
.app-container {
  padding: 16px 24px;
  max-width: 1200px;
  margin: 0 auto;
  background-color: $bg-light;
  min-height: 100vh;
}

// 页面标题（压缩间距）
.page-header {
  margin: 0 0 $spacing-md;
  padding-bottom: $spacing-sm;
  border-bottom: 1px solid $border-light;

  h2 {
    font-size: 22px;
    font-weight: 600;
    color: #111827;
    margin: 0 0 $spacing-sm;
  }

  p {
    font-size: 14px;
    color: $text-gray;
    margin: 0;
  }
}

// 操作按钮（保持不变）
.operation-bar {
  .create-btn {
    margin-top: 8px;
    background-color: $primary;
    border-color: $primary;
    transition: all 0.3s ease;

    &:hover {
      background-color: $primary-dark;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
    }
  }
}

// 状态筛选（增加内边距，填充空白）
.status-filter-container {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

  .status-label {
    color: $text-gray;
    font-weight: 500;
    margin-right: 16px;
  }

  .status-options {
    display: flex;
    gap: 12px;

    .status-option {
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;
      background-color: #f3f4f6;
      color: $text-light-gray;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background-color: #e5e7eb;
        color: $text-gray;
      }

      &.active-status {
        background-color: $primary-light;
        color: $primary;
        font-weight: 500;
      }

      .status-icon {
        margin-top: 1px;
      }
    }
  }
}

// 空状态（增加子文本，填充空白）
.empty-state {
  margin: 60px auto 80px;
  text-align: center;
  max-width: 400px;

  .empty-image {
    margin-bottom: $spacing-md;
  }

  .empty-text {
    color: $text-light-gray;
    margin-bottom: 8px;
    font-size: 16px;
  }

  .empty-subtext {
    color: $text-light-gray;
    font-size: 13px;
    margin-bottom: $spacing-md;
  }

  .empty-action {
    background-color: $primary;
    border-color: $primary;

    &:hover {
      background-color: $primary-dark;
    }
  }
}

// 时间轴布局核心样式（减少间距+增加装饰）
.timeline-container {
  position: relative;
  padding: 10px 0 20px;
  margin: 0 auto;
  max-width: 1000px;

  // 时间轴主轴（加装饰点，填充空白）
  .timeline-axis {
    position: absolute;
    left: 50%;
    top: 0;
    bottom: 0;
    width: 2px;
    background-color: $border-light;
    transform: translateX(-50%);
    z-index: 1;

    .axis-dot {
      position: absolute;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: $primary-light;
      left: 50%;
      transform: translateX(-50%);

      &:nth-child(1) {
        top: 10%;
      }

      &:nth-child(2) {
        top: 25%;
      }

      &:nth-child(3) {
        top: 40%;
      }

      &:nth-child(4) {
        top: 55%;
      }

      &:nth-child(5) {
        top: 70%;
      }

      &:nth-child(6) {
        top: 85%;
      }
    }
  }

  // 时间线条目（减少间距，更紧凑）
  .timeline-item {
    position: relative;
    margin-bottom: 30px; // 减少条目间距
    width: 50%;
    z-index: 2;

    // 左右交错布局
    &.left {
      padding-right: 30px;
      left: 0;
    }

    &.right {
      padding-left: 30px;
      left: 50%;
    }

    // 时间点标记（加状态色，更醒目）
    .timeline-dot {
      position: absolute;
      top: 14px;
      width: 22px;
      height: 22px;
      background-color: white;
      border: 2px solid $border-light;
      border-radius: 50%;
      z-index: 3;
      transition: all 0.3s;

      .dot-inner {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        margin: 4px auto;
      }

      // 状态色区分
      .public-dot {
        background-color: $primary;
      }

      .private-dot {
        background-color: $private-color;
      }

      .pinned-dot {
        background-color: $pinned-color;
      }
    }

    &.left .timeline-dot {
      right: 0;
      transform: translateX(50%);
    }

    &.right .timeline-dot {
      left: 0;
      transform: translateX(-50%);
    }

    &:hover {
      .timeline-dot {
        border-color: $primary;
      }

      &.left .timeline-dot {
        transform: translate(50%, -2px) scale(1.1);
      }

      &.right .timeline-dot {
        transform: translate(-50%, -2px) scale(1.1);
      }
    }

    // 内容容器（加边框+背景，减少空旷感）
    .talk-content-wrapper {
      background-color: white;
      border-radius: 12px;
      padding: 18px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      transition: all 0.3s;
      border: 1px solid $border-light; // 加边框，更饱满

      &:hover {
        box-shadow: 0 6px 16px rgba(59, 130, 246, 0.1);
        transform: translateY(-3px);
        border-color: $primary-light;
      }
    }

    // 头部信息（增加子标签，填充空白）
    .talk-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;

      .user-info {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .user-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        object-fit: cover;
        border: 2px solid $primary-light; // 加边框，更醒目
      }

      .user-info-inner {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .user-name {
        font-size: 15px;
        font-weight: 500;
        color: #111827;
      }

      .user-tag {
        font-size: 12px;
        color: $pinned-color;
        background-color: rgba(255, 114, 66, 0.1);
        padding: 2px 8px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .operation-menu {
        .more-icon {
          color: $text-light-gray;
          font-size: 18px;
          cursor: pointer;
          transition: color 0.2s;
          padding: 4px;
          border-radius: 50%;

          &:hover {
            color: $primary;
            background-color: $primary-light;
          }
        }
      }
    }

    // 元信息（加图标，更丰富）
    .talk-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 14px;
      font-size: 13px;
      color: $text-light-gray;

      .meta-icon {
        margin-right: 4px;
        vertical-align: middle;
      }

      .status-tag {
        height: 22px;
        line-height: 22px;
        padding: 0 8px;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    // 说说内容（加前缀图标，填充左侧空白）
    .talk-text {
      font-size: 14px;
      line-height: 1.7;
      color: #374151;
      margin-bottom: 18px;
      word-wrap: break-word;
      white-space: pre-line;
      padding-left: 4px;

      .text-prefix {
        margin-right: 6px;
        vertical-align: middle;
        display: inline-flex;
        align-items: center;
      }

      .public-prefix {
        color: $primary;
      }

      .private-prefix {
        color: $private-color;
      }
    }

    .talk-images {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 10px;

      .image-item {
        border-radius: 6px;
        overflow: hidden;
        cursor: pointer;
        border: 1px solid $border-light;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
        background-color: #f9fafb;

        .image {
          width: 100%;
          height: 100%;
          padding: 4px;
          transition: transform 0.3s;

          &:hover {
            transform: scale(1.05); // 轻微放大，不突兀
          }
        }
      }
    }

    // 底部互动标识（新增，填充底部空白）
    .talk-footer {
      margin-top: 12px;
      padding-top: 10px;
      border-top: 1px dashed $border-light;
      font-size: 12px;
      color: $text-light-gray;

      .image-count {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

// 分页（增加布局元素，减少空白）
.pagination-container {
  text-align: center;
  padding: $spacing-md 0 $spacing-lg;
  margin-top: 10px;

  :deep( .el-pagination) {
    .el-pager li {
      margin: 0 4px;
      border-radius: 4px;
      min-width: 32px;
      height: 32px;
      line-height: 32px;

      &.active {
        background-color: $primary;
        color: white;
      }
    }

    .el-pagination__total {
      margin-right: 12px;
      color: $text-gray;
    }

    .el-pagination__jump {
      color: $text-gray;
    }
  }
}

// 对话框样式（保持不变，微调间距）
.talk-dialog {
  :deep( .el-dialog__header) {
    padding: 20px 24px;
    border-bottom: 1px solid $border-light;

    .el-dialog__title {
      font-size: 18px;
      font-weight: 500;
    }
  }

  :deep( .el-dialog__body) {
    padding: 20px 24px;
  }

  .editor {
    min-height: 140px;
    border-radius: 8px;
  }

  .editor-toolbar {
    margin-top: $spacing-md;
    padding-top: $spacing-md;
    border-top: 1px solid $border-light;

    .tool-icon {
      cursor: pointer;
      padding: 6px;
      border-radius: 4px;
      transition: all 0.2s;

      &:hover {
        background-color: $primary-light;
        color: $primary;
      }
    }

    .submit-btn {
      background-color: $primary;
      border-color: $primary;

      &:hover {
        background-color: $primary-dark;
      }
    }
  }

  .uploaded-images {
    margin-top: $spacing-md;

    :deep( .el-upload-list__item) {
      border-radius: 8px;
      border: 1px solid $border-light;
    }
  }
}

// 图片预览
.image-preview {
  padding: 0;

  :deep( .el-dialog__body) {
    padding: 0;
  }

  img {
    border-radius: 8px;
    max-height: 80vh;
  }
}

// 响应式适配（保持紧凑，不拥挤）
@media (max-width: 768px) {
  .app-container {
    padding: 12px 16px;
  }

  // 时间轴在移动端调整
  .timeline-container {
    .timeline-axis {
      left: 24px;
    }

    .timeline-item {
      width: 100%;
      padding-left: 50px;
      padding-right: 0;
      margin-bottom: 24px;
      left: 0 !important;

      &.left, &.right {
        padding-left: 50px;
        padding-right: 0;
      }

      .timeline-dot {
        left: 24px;
        right: auto;
        transform: translateX(-50%);
      }

      // 内容容器调整内边距
      .talk-content-wrapper {
        padding: 14px;
      }

      // 隐藏置顶标签，避免拥挤
      .user-tag {
        display: none;
      }
    }
  }

  // 筛选栏换行显示
  .status-filter-container {
    flex-wrap: wrap;
    gap: 10px;

    .status-label {
      width: 100%;
      margin-right: 0;
    }

    .status-options {
      width: 100%;
      justify-content: flex-start;
    }
  }

  .talk-dialog {
    width: 95% !important;
  }
}

// 动画效果（保持流畅）
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.timeline-item {
  animation: fadeIn 0.5s ease forwards;
  opacity: 0;
}

.timeline-item:nth-child(1) {
  animation-delay: 0.1s;
}

.timeline-item:nth-child(2) {
  animation-delay: 0.2s;
}

.timeline-item:nth-child(3) {
  animation-delay: 0.3s;
}

.timeline-item:nth-child(4) {
  animation-delay: 0.4s;
}

.timeline-item:nth-child(5) {
  animation-delay: 0.5s;
}

.timeline-item:nth-child(6) {
  animation-delay: 0.6s;
}
</style>