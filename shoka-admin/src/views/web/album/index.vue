<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form @submit.native.prevent :inline="true" v-show="showSearch" class="search-form">
      <el-form-item label="相册名称">
        <el-input
            v-model="queryParams.keyword"
            style="width: 200px"
            placeholder="请输入相册名称"
            clearable
            @keyup.enter="handleQuery"
            class="search-input"
        />
      </el-form-item>
      <el-form-item>
        <el-button
            type="primary"
            icon="Search"
            @click="handleQuery"
            class="search-btn"
        >
          搜索
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb15">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            class="add-btn"
        >
          新建
        </el-button>
      </el-col>
      <right-toolbar
          v-model:showSearch="showSearch"
          @queryTable="getList"
          class="toolbar"
      />
    </el-row>

    <!-- 相册列表 -->
    <el-row :gutter="20" v-loading="loading" class="album-grid">
      <el-empty v-if="albumList.length === 0" description="暂无相册"/>

      <el-col
          v-for="album of albumList"
          :key="album.id"
          :xs="12"
          :sm="12"
          :md="8"
          :lg="6"
          :xl="4"
          class="album-col"
      >
        <div class="album-item" @click="checkPhoto(album.id)">
          <!-- 相册操作菜单 -->
          <div class="album-operation">
            <el-dropdown @command="handleCommand" effect="light">
              <el-icon class="more-icon">
                <MoreFilled/>
              </el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="'update' + album.id">编辑</el-dropdown-item>
                  <el-dropdown-item :command="'delete' + album.id" class="delete-item">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <!-- 相册封面 -->
          <div class="album-cover-container">
            <el-image
                class="album-cover"
                fit="cover"
                :src="album.albumCover || defaultCover"
            >
              <template #error>
                <div class="image-error">
                  <el-icon class="error-icon">
                    <PictureRounded/>
                  </el-icon>
                </div>
              </template>
            </el-image>

            <!-- 渐变遮罩 -->
            <div class="cover-overlay"></div>

            <!-- 照片数量 -->
            <div class="photo-count">
              <el-icon class="photo-icon">
                <PictureRounded/>
              </el-icon>
              <span>{{ album.photoCount }}</span>
              <el-icon v-if="album.status === 2" class="private-icon">
                <Lock/>
              </el-icon>
            </div>
          </div>

          <!-- 相册名称 -->
          <div class="album-name" :title="album.albumName">
            {{ album.albumName }}
          </div>

          <!-- 相册描述 -->
          <div class="album-desc" :title="album.albumDesc">
            {{ album.albumDesc || '无描述' }}
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 分页 -->
    <el-pagination
        class="pagination-container"
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :hide-on-single-page="true"
        layout="prev, pager, next"
        :total="count"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />

    <!-- 添加或修改对话框 -->
    <el-dialog
        :title="title"
        v-model="addOrUpdate"
        width="550px"
        append-to-body
        :before-close="handleDialogClose"
    >
      <el-form
          ref="albumFormRef"
          label-width="100px"
          :model="albumForm"
          :rules="rules"
          class="album-form"
      >
        <el-form-item label="相册名称" prop="albumName">
          <el-input
              placeholder="请输入相册名称"
              v-model="albumForm.albumName"
              style="width: 250px;"
              class="form-input"
          />
        </el-form-item>

        <el-form-item label="相册描述" prop="albumDesc">
          <el-input
              placeholder="请输入相册描述"
              v-model="albumForm.albumDesc"
              style="width: 250px;"
              class="form-input"
          />
        </el-form-item>

        <el-form-item label="相册封面" prop="albumCover">
          <el-upload
              drag
              :show-file-list="false"
              :headers="authorization"
              action="http://121.41.87.40:8080/admin/album/upload"
              accept="image/*"
              :before-upload="beforeUpload"
              :on-success="handleSuccess"
              class="cover-upload"
          >
            <el-icon class="el-icon--upload" v-if="albumForm.albumCover === ''">
              <upload-filled/>
            </el-icon>
            <div class="el-upload__text" v-if="albumForm.albumCover === ''">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <img v-else :src="albumForm.albumCover" width="360" class="preview-image" alt="相册封面预览图"/>
          </el-upload>
        </el-form-item>

        <el-form-item label="发布形式">
          <el-radio-group v-model="albumForm.status" class="radio-group">
            <el-radio :label="1" class="radio-item">公开</el-radio>
            <el-radio :label="2" class="radio-item">私密</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addOrUpdate = false" class="cancel-btn">取 消</el-button>
          <el-button type="primary" @click="submitForm" class="confirm-btn">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {addAlbum, deleteAlbum, editAlbum, getAlbumList, updateAlbum} from '@/api/album';
import {Album, AlbumForm, AlbumQuery} from '@/api/album/types';
import router from "@/router";
import {messageConfirm, notifySuccess} from '@/utils/modal';
import {getToken, token_prefix} from '@/utils/token';
import {AxiosResponse} from 'axios';
import {FormInstance, FormRules, UploadRawFile} from 'element-plus';
import * as imageConversion from 'image-conversion';
import {computed, onMounted, reactive, ref, toRefs} from 'vue';
import {MoreFilled, PictureRounded, Lock, UploadFilled} from '@element-plus/icons-vue';

// 默认封面图
const defaultCover = 'https://picsum.photos/400/300?random=1';

const albumFormRef = ref<FormInstance>();
const rules = reactive<FormRules>({
  albumName: [{required: true, message: "请输入相册名称", trigger: "blur"}],
  albumCover: [{required: true, message: "请上传相册封面", trigger: "blur"}],
});

const authorization = computed(() => {
  return {
    Authorization: token_prefix + getToken(),
  }
});

const data = reactive({
  count: 0,
  showSearch: true,
  loading: false,
  title: "",
  addOrUpdate: false,
  queryParams: {
    current: 1,
    size: 12, // 调整每页显示数量，视觉上更均衡
    keyword: '' // 添加keyword字段
  } as AlbumQuery,
  albumForm: {} as AlbumForm,
  albumList: [] as Album[],
});

const {
  count,
  showSearch,
  loading,
  title,
  addOrUpdate,
  queryParams,
  albumForm,
  albumList,
} = toRefs(data);

// 查看相册照片
const checkPhoto = (albumId: number) => {
  router.push({path: `/web/photo/${albumId}`});
};

// 图片上传前处理
const beforeUpload = (rawFile: UploadRawFile) => {
  return new Promise(resolve => {
    if (rawFile.size / 1024 < 200) {
      resolve(rawFile);
    }
    // 压缩到200KB
    imageConversion
        .compressAccurately(rawFile, 200)
        .then(res => {
          resolve(res);
        });
  });
};

// 上传成功处理
const handleSuccess = (response: AxiosResponse) => {
  albumForm.value.albumCover = response.data;
};

// 分页处理
const handleSizeChange = (size: number) => {
  queryParams.value.size = size;
  getList();
};

const handleCurrentChange = (current: number) => {
  queryParams.value.current = current;
  getList();
};

// 打开新增对话框
const handleAdd = () => {
  albumFormRef.value?.clearValidate();
  title.value = "新建相册";
  albumForm.value = {
    id: undefined,
    albumName: "",
    albumDesc: "",
    albumCover: "",
    status: 1,
  }
  addOrUpdate.value = true;
};

// 关闭对话框时清除验证
const handleDialogClose = () => {
  albumFormRef.value?.clearValidate();
};

// 处理编辑/删除命令
const handleCommand = (command: string) => {
  const type = command.substring(0, 6);
  const id = Number(command.substring(6));
  albumForm.value.id = id;

  if (type === "delete") {
    messageConfirm("确认删除该相册? 删除后不可恢复。").then(() => {
      deleteAlbum(id).then(({data}) => {
        if (data.flag) {
          notifySuccess(data.msg);
          getList();
        }
      });
    });
  } else {
    albumFormRef.value?.resetFields();
    editAlbum(id).then(({data}) => {
      if (data.flag) {
        albumForm.value = data.data;
        title.value = "修改相册";
        addOrUpdate.value = true;
      }
    })
  }
};

// 提交表单
const submitForm = () => {
  albumFormRef.value?.validate((valid) => {
    if (valid) {
      const submitFunc = albumForm.value.id !== undefined
          ? updateAlbum(albumForm.value)
          : addAlbum(albumForm.value);

      submitFunc.then(({data}) => {
        if (data.flag) {
          notifySuccess(data.msg);
          getList();
        }
        addOrUpdate.value = false;
      });
    }
  });
};

// 获取相册列表
const getList = () => {
  loading.value = true;
  getAlbumList(queryParams.value).then(({data}) => {
    albumList.value = data.data.recordList || [];
    count.value = data.data.count || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

// 搜索处理
const handleQuery = () => {
  queryParams.value.current = 1; // 重置为第一页
  getList();
};

// 页面挂载时加载数据
onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
// 全局样式变量
$primary-color: #409eff;
$hover-color: #66b1ff;
$text-color: #303133;
$text-color-secondary: #606266;
$text-color-placeholder: #909399;
$border-radius: 8px;
$shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
$transition: all 0.3s ease;

.app-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

// 搜索表单样式
.search-form {
  background-color: #fff;
  border-radius: $border-radius;
  margin-bottom: 15px;
  box-shadow: $shadow;
  padding: 15px 0 0 20px;

  .search-input {
    transition: $transition;

    &:focus {
      border-color: $primary-color;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }

  .search-btn {
    transition: $transition;

    &:hover {
      background-color: $hover-color;
      transform: translateY(-2px);
    }
  }
}

// 工具栏样式
.mb15 {
  margin-bottom: 15px;

  .add-btn {
    transition: $transition;

    &:hover {
      background-color: rgba(64, 158, 255, 0.1);
      color: $primary-color;
      transform: translateY(-2px);
    }
  }
}

// 相册网格布局
.album-grid {
  margin-bottom: 30px;
  padding: 10px 0;
}

.album-col {
  transition: $transition;
}

// 相册卡片样式
.album-item {
  position: relative;
  background-color: #fff;
  border-radius: $border-radius;
  overflow: hidden;
  box-shadow: $shadow;
  transition: $transition;
  height: 100%;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.12);
  }

  // 操作菜单
  .album-operation {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 10;
    opacity: 0;
    transition: $transition;

    .more-icon {
      color: #fff;
      background-color: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      padding: 4px;
      cursor: pointer;
      transition: $transition;

      &:hover {
        background-color: rgba(0, 0, 0, 0.7);
      }
    }

    .delete-item {
      color: #f56c6c;
    }
  }

  &:hover .album-operation {
    opacity: 1;
  }

  // 封面容器
  .album-cover-container {
    position: relative;
    width: 100%;
    height: 180px;
    overflow: hidden;
  }

  // 封面图片
  .album-cover {
    width: 100%;
    height: 100%;
    transition: $transition;

    &:hover {
      transform: scale(1.05);
    }
  }

  // 封面错误状态
  .image-error {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f5f7fa;

    .error-icon {
      font-size: 32px;
      color: $text-color-placeholder;
    }
  }

  // 封面遮罩
  .cover-overlay {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 60px;
    background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  }

  // 照片数量
  .photo-count {
    position: absolute;
    left: 15px;
    bottom: 15px;
    color: #fff;
    display: flex;
    align-items: center;
    font-size: 14px;

    .photo-icon, .private-icon {
      margin-right: 5px;
      font-size: 16px;
    }

    .private-icon {
      margin-left: 8px;
      color: #ffd700;
    }
  }

  // 相册名称
  .album-name {
    padding: 15px 15px 5px;
    font-weight: 500;
    color: $text-color;
    font-size: 16px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  // 相册描述
  .album-desc {
    padding: 0 15px 15px;
    color: $text-color-secondary;
    font-size: 13px;
    flex-grow: 1;
    display: -webkit-box;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

// 分页样式
.pagination-container {
  text-align: center;
  margin-top: 30px;
  padding: 10px;

  :deep(.el-pagination){
    display: inline-flex;
    align-items: center;
  }

  :deep(.el-pager li){
    margin: 0 5px;
    border-radius: 4px;
    transition: $transition;

    &:hover {
      color: $primary-color;
      transform: translateY(-2px);
    }

    &.active {
      background-color: $primary-color;
      color: #fff;

      &:hover {
        color: #fff;
      }
    }
  }
}

// 表单样式
.album-form {
  padding-top: 10px;

  .form-input {
    transition: $transition;

    &:focus {
      border-color: $primary-color;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }

  .cover-upload {
    margin-top: 5px;

    .preview-image {
      border-radius: 4px;
      transition: $transition;
      cursor: pointer;

      &:hover {
        transform: scale(1.02);
        box-shadow: $shadow;
      }
    }
  }

  .radio-group {
    margin-top: 5px;

    .radio-item {
      margin-right: 20px;
      transition: $transition;

      &:hover {
        color: $primary-color;
      }
    }
  }
}

// 对话框底部按钮
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;

  .cancel-btn {
    transition: $transition;

    &:hover {
      background-color: #f5f7fa;
    }
  }

  .confirm-btn {
    transition: $transition;

    &:hover {
      background-color: $hover-color;
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .app-container {
    padding: 10px;
  }

  .album-item {
    .album-cover-container {
      height: 150px;
    }
  }
}
</style>