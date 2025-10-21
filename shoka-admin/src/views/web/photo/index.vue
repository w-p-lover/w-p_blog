<template>
  <div class="photo-page-root">
    <div class="app-container">
      <!-- 相册信息卡片 -->
      <div class="album-info-card">
        <el-image
            fit="cover"
            class="album-cover"
            :src="albumInfo.albumCover || defaultCover"
        >
          <template #error>
            <div class="image-placeholder">
              <el-icon class="placeholder-icon">
                <PictureRounded/>
              </el-icon>
            </div>
          </template>
        </el-image>

        <div class="album-info-content">
          <div class="album-header">
            <h2 class="album-name">{{ albumInfo.albumName || '未命名相册' }}</h2>
            <span class="photo-count">
              <el-icon class="count-icon"><PictureRounded/></el-icon>
              {{ albumInfo.photoCount || 0 }}张照片
            </span>
          </div>

          <div class="album-desc">
            <el-icon class="desc-icon">
              <InfoFilled/>
            </el-icon>
            <span>{{ albumInfo.albumDesc || '该相册暂无描述' }}</span>
          </div>

          <div class="album-status">
            <el-tag :type="albumInfo.status === 1 ? 'success' : 'info'">
              {{ albumInfo.status === 1 ? '公开相册' : '私密相册' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 操作按钮工具栏 -->
      <div class="operation-toolbar">
        <el-button
            type="primary"
            icon="Upload"
            @click="upload = true"
            class="operation-btn upload-btn"
        >
          上传照片
        </el-button>

        <el-button
            type="warning"
            icon="Upload"
            @click="handleRunSpider"
            class="operation-btn spider-btn"
            :loading="spiderLoading"
        >
          壁纸爬虫
        </el-button>

        <el-button
            type="success"
            icon="SwitchButton"
            @click="handleMove"
            class="operation-btn move-btn"
            :disabled="selectPhotoIdList.length === 0"
        >
          移动照片
        </el-button>

        <el-button
            type="danger"
            icon="Delete"
            @click="handleDelete"
            class="operation-btn delete-btn"
            :disabled="selectPhotoIdList.length === 0"
        >
          批量删除
        </el-button>

        <div class="selection-info">
          <el-checkbox
              v-model="checkAll"
              :indeterminate="isIndeterminate"
              @change="handleCheckAllChange"
              class="check-all"
          >
            全选
          </el-checkbox>
          <span class="selected-count">已选择 {{ selectPhotoIdList.length }} 张</span>
        </div>
      </div>

      <!-- 照片列表 -->
      <el-checkbox-group v-model="selectPhotoIdList" @change="handleCheckedPhotoChange">
        <el-row class="picture-list" :gutter="18">
          <el-col :xs="8" :sm="6" :lg="4" v-for="photo of photoList" :key="photo.id" style="margin-bottom:1rem;">
            <el-checkbox :label="photo.id">
              <template #default>
                <div class="photo-item">
                  <div class="photo-operation">
                    <el-dropdown @command="handleCommand">
                      <el-icon style="color:#fff">
                        <MoreFilled/>
                      </el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item :command="photo">编辑</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                  <el-image class="photo-cover" fit="cover" :src="photo.photoUrl"
                            :preview-src-list="[photo.photoUrl]">
                  </el-image>
                  <div class="photo-name">{{ photo.photoName }}</div>
                </div>
              </template>
            </el-checkbox>
          </el-col>
        </el-row>
      </el-checkbox-group>

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

      <!-- 修改照片对话框 -->
      <el-dialog
          title="修改照片信息"
          v-model="update"
          width="550px"
          append-to-body
          :before-close="handleDialogClose"
      >
        <el-form
            ref="photoFormRef"
            label-width="100px"
            :model="photoForm"
            :rules="rules"
            class="photo-form"
        >
          <el-form-item label="照片名称" prop="photoName">
            <el-input
                placeholder="请输入照片名称"
                v-model="photoForm.photoName"
                style="width: 250px;"
                class="form-input"
            />
          </el-form-item>
          <el-form-item label="照片描述" prop="photoDesc">
            <el-input
                placeholder="请输入照片描述"
                v-model="photoForm.photoDesc"
                style="width: 250px;"
                class="form-input"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="update = false" class="cancel-btn">取 消</el-button>
            <el-button type="primary" @click="submitForm" class="confirm-btn">确 定</el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 上传照片对话框 -->
      <el-dialog
          title="上传照片到相册"
          v-model="upload"
          width="800px"
          append-to-body
          :before-close="handleUploadClose"
      >
        <div class="upload-container">
          <el-upload
              v-show="uploadList.length > 0"
              :headers="authorization"
              class="avatar-uploader"
              multiple
              action="http://121.41.87.40:8080/admin/photo/upload"
              :before-upload="beforeUpload"
              :on-success="handleSuccess"
              :on-remove="handleRemove"
              :on-preview="handlePictureCardPreview"
              list-type="picture-card"
              :file-list="uploadList"
              accept="image/*"
          >
            <el-icon class="avatar-uploader-icon">
              <Plus/>
            </el-icon>
          </el-upload>

          <div class="upload-drag-area" v-show="uploadList.length === 0">
            <el-upload
                :headers="authorization"
                drag
                multiple
                action="http://121.41.87.40:8080/admin/photo/upload"
                :before-upload="beforeUpload"
                :show-file-list="false"
                accept="image/*"
                :on-success="handleSuccess"
                class="drag-upload"
            >
              <el-icon class="upload-icon">
                <UploadFilled/>
              </el-icon>
              <div class="upload-text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <div class="upload-hint">支持 JPG、PNG 等格式，单张不超过 200KB</div>
            </el-upload>
          </div>
        </div>
        <template #footer>
          <div class="dialog-footer upload-footer">
            <div class="upload-count">
              已选择 {{ uploadList.length }} 张照片
            </div>
            <div class="upload-actions">
              <el-button @click="upload = false" class="cancel-btn">取 消</el-button>
              <el-button
                  type="primary"
                  :disabled="uploadList.length === 0"
                  @click="handleAdd"
                  class="confirm-btn"
              >
                确 定
              </el-button>
            </div>
          </div>
        </template>
      </el-dialog>

      <!-- 图片预览 -->
      <el-dialog
          v-model="dialogVisible"
          append-to-body
          class="image-preview-dialog"
          :show-close="false"
      >
        <img :src="dialogImageUrl" class="preview-image" alt=""/>
        <button class="close-preview" @click="dialogVisible = false">
          <el-icon class="close-icon">
            <Close/>
          </el-icon>
        </button>
      </el-dialog>
    </div>

    <!-- 爬虫状态面板 -->
    <div v-if="spiderLoading" class="spider-status-container">
      <div class="spider-status-icon" :class="statusIconClass">
        <el-icon v-if="status.value === 'RUNNING'">
          <Loading/>
        </el-icon>
        <el-icon v-else-if="status.value === 'COMPLETED'">
          <CircleCheckFilled/>
        </el-icon>
        <el-icon v-else-if="status.value === 'FAILED'">
          <CircleCloseFilled/>
        </el-icon>
        <el-icon v-else>
          <Upload/>
        </el-icon>
      </div>

      <div class="spider-status-text">
        <h4 class="status-title" :class="statusTextClass">{{ spiderMessage || '处理中...' }}</h4>
      </div>

      <div class="spider-progress-wrapper">
        <el-progress
            type="line"
            :percentage="spiderPercentage"
            :status="statusProgressClass"
            :color="progressColor"
            :stroke-width="10"
            :gap-degree="30"
        ></el-progress>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {addPhoto, deletePhoto, getAlbumInfo, getPhotoList, updatePhoto} from '@/api/photo';
import {AlbumInfo, Photo, PhotoForm, PhotoQuery} from '@/api/photo/types';
import {Picture} from '@/model';
import {messageConfirm, notifySuccess} from '@/utils/modal';
import {getToken, token_prefix} from '@/utils/token';
import axios, {AxiosResponse} from 'axios';
import {ElNotification, FormInstance, FormRules, UploadFile, UploadRawFile} from 'element-plus';
import * as imageConversion from 'image-conversion';
import {computed, onMounted, reactive, ref, toRefs, watch} from 'vue';
import {useRoute} from "vue-router";
import {
  MoreFilled, PictureRounded, UploadFilled, Plus,
  InfoFilled, Loading, Upload,
  CircleCheckFilled, CircleCloseFilled, Close
} from '@element-plus/icons-vue';

// 默认封面图
const defaultCover = 'https://picsum.photos/400/300?random=2';
const spiderMessage = ref('');
const status = ref('');
const spiderPercentage = ref(0.0);
const photoFormRef = ref<FormInstance>();
const rules = reactive<FormRules>({
  photoName: [{required: true, message: "请输入照片名称", trigger: "blur"}],
});

const authorization = computed(() => {
  return {
    Authorization: token_prefix + getToken(),
  }
});

const route = useRoute();
const data = reactive({
  count: 0,
  spiderLoading: false,
  loading: false,
  upload: false,
  update: false,
  checkAll: false,
  isIndeterminate: false,
  dialogImageUrl: "",
  dialogVisible: false,
  queryParams: {
    current: 1,
    size: 24,
    albumId: Number(route.params.albumId),
  } as PhotoQuery,
  photoForm: {} as PhotoForm,
  photoIdList: [] as number[],
  selectPhotoIdList: [] as number[],
  photoList: [] as Photo[],
  albumInfo: {} as AlbumInfo,
  uploadList: [] as Picture[],
});

const {
  spiderLoading,
  count,
  loading,
  upload,
  update,
  checkAll,
  isIndeterminate,
  dialogImageUrl,
  dialogVisible,
  queryParams,
  photoForm,
  photoIdList,
  selectPhotoIdList,
  photoList,
  albumInfo,
  uploadList,
} = toRefs(data);

// 监听照片列表变化，更新ID列表
watch(photoList, () => {
  photoIdList.value = [];
  photoList.value.forEach(item => {
    photoIdList.value.push(item.id);
  });
});

// 显示爬虫通知
const showSpiderNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
  ElNotification({
    title: '爬虫状态',
    message,
    type,
    duration: 5000,
    offset: 60,
    dangerouslyUseHTMLString: true
  })
}

// 分页处理
const handleSizeChange = (size: number) => {
  queryParams.value.size = size;
  getList();
};

const handleCurrentChange = (current: number) => {
  queryParams.value.current = current;
  getList();
};

// 全选处理
const handleCheckAllChange = (val: boolean) => {
  selectPhotoIdList.value = val ? [...photoIdList.value] : [];
  isIndeterminate.value = false;
};

// 选择变化处理
const handleCheckedPhotoChange = (value: number[]) => {
  const checkedCount = value.length;
  checkAll.value = checkedCount === photoIdList.value.length && checkedCount > 0;
  isIndeterminate.value = checkedCount > 0 && checkedCount < photoIdList.value.length;
};

// 照片操作命令
const handleCommand = (photo: Photo) => {
  photoFormRef.value?.resetFields();
  photoForm.value = {...photo};
  update.value = true;
};

// 图片上传前处理
const beforeUpload = (rawFile: UploadRawFile) => {
  return new Promise(resolve => {
    if (rawFile.size / 1024 < 200) {
      resolve(rawFile);
    } else {
      // 压缩到200KB
      imageConversion
          .compressAccurately(rawFile, 200)
          .then(res => {
            resolve(res);
          });
    }
  });
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

// 移动照片（待实现）
const handleMove = () => {
  ElNotification({
    title: '提示',
    message: '移动功能开发中',
    type: 'info'
  });
};

// 批量删除
const handleDelete = () => {
  messageConfirm(`确认删除已选中的 ${selectPhotoIdList.value.length} 张照片?`).then(() => {
    deletePhoto(selectPhotoIdList.value).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
        selectPhotoIdList.value = [];
        isIndeterminate.value = false;
        checkAll.value = false;
      }
    });
  });
};

// 运行爬虫
const handleRunSpider = async () => {
  if (spiderLoading.value) return;
  spiderLoading.value = true;

  try {
    const albumName = albumInfo.value.albumName;

    await axios.post('http://localhost:8080/photo/run', {
      albumName: albumName
    }, {});

    showSpiderNotification('info',
        `<div style="text-align: left; line-height: 1.6; margin-left: 40px">
        <strong>🚀 爬虫任务已启动</strong><br>
        📝 壁纸网站: <span style="color:#409EFF;">WallHaven</span><br>
        📁 专辑名称: <span style="color:#409EFF;">${albumName || '未指定'}</span><br>
      </div>`);

    const startTime = Date.now();
    const pollStatus = async () => {
      spiderLoading.value = true;
      try {
        const {data} = await axios.get(`http://localhost:8080/photo/status?albumName=${encodeURIComponent(albumName || '')}`);
        status.value = data.data.status?.trim()?.toUpperCase();
        spiderPercentage.value = data.data.spiderPercentage || 0;
        spiderMessage.value = data.data.message || '';

        // 完成/失败或超时
        if (['COMPLETED', 'FAILED'].includes(status.value) || Date.now() - startTime > 480000) {
          spiderLoading.value = false;
          getList();
          getAlbumInfo(Number(route.params.albumId)).then(({data}) => {
            albumInfo.value = data.data;
          });

          if (status.value === 'COMPLETED') {
            showSpiderNotification('success', `《${albumName}》专辑爬虫任务完成！`);
          } else if (status.value === 'FAILED') {
            showSpiderNotification('error', `《${albumName}》专辑爬虫任务失败！`);
          } else {
            showSpiderNotification('warning', `《${albumName}》专辑爬虫任务超时！`);
          }
        } else {
          // 继续轮询
          setTimeout(pollStatus, 10000);
        }
      } catch (err) {
        console.error('轮询失败:', err);
        spiderLoading.value = false;
        showSpiderNotification('error', '轮询失败，请重试！');
      }
    };

    await pollStatus();
  } catch (err) {
    console.error('启动爬虫失败:', err);
    loading.value = false;
    showSpiderNotification('error', '启动爬虫失败！');
  }
};

// 确认上传照片
const handleAdd = () => {
  const photoUrlList = uploadList.value.map(item => item.url);
  if (photoUrlList.length > 0) {
    addPhoto({
      albumId: Number(route.params.albumId),
      photoUrlList
    }).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
        uploadList.value = [];
        getList();
        // 更新相册信息
        getAlbumInfo(Number(route.params.albumId)).then(({data}) => {
          albumInfo.value = data.data;
        });
      }
      upload.value = false;
    });
  }
};

// 提交修改表单
const submitForm = () => {
  photoFormRef.value?.validate((valid) => {
    if (valid) {
      updatePhoto(photoForm.value).then(({data}) => {
        if (data.flag) {
          notifySuccess(data.msg);
          getList();
        }
        update.value = false;
      });
    }
  });
};

// 关闭对话框时清除验证
const handleDialogClose = () => {
  photoFormRef.value?.clearValidate();
};

// 关闭上传对话框时清空列表
const handleUploadClose = () => {
  uploadList.value = [];
};

// 获取照片列表
const getList = () => {
  loading.value = true;
  getPhotoList(queryParams.value).then(({data}) => {
    photoList.value = data.data.recordList || [];
    count.value = data.data.count || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

// 页面挂载时加载数据
onMounted(() => {
  getList();
  getAlbumInfo(Number(route.params.albumId)).then(({data}) => {
    albumInfo.value = data.data || {};
  });
});

// 状态样式计算属性
const statusIconClass = computed(() => {
  const base = 'status-icon';
  if (status.value === 'RUNNING') return `${base} running`;
  if (status.value === 'COMPLETED') return `${base} completed`;
  if (status.value === 'FAILED') return `${base} failed`;
  return `${base} default`;
});

const statusTextClass = computed(() => {
  if (status.value === 'COMPLETED') return 'text-success';
  if (status.value === 'FAILED') return 'text-failed';
  return 'text-default';
});

const statusProgressClass = computed(() => {
  if (status.value === 'COMPLETED') return 'success';
  if (status.value === 'FAILED') return 'exception';
  return '';
});

const progressColor = computed(() => {
  if (status.value === 'RUNNING') return 'linear-gradient(to right, #4096ff, #6772e5)';
  if (status.value === 'COMPLETED') return 'linear-gradient(to right, #67c23a, #52c41a)';
  if (status.value === 'FAILED') return 'linear-gradient(to right, #f56c6c, #fa8c16)';
  return '#4096ff';
});
</script>

<style lang="scss" scoped>
// 全局样式变量
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$text-color: #303133;
$text-color-secondary: #606266;
$text-color-placeholder: #909399;
$border-radius: 8px;
$shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
$transition: all 0.3s ease;

.photo-item {
  .photo-operation {
    position: absolute;
    top: 0.3rem;
    right: 0.5rem;
    z-index: 9;
  }

  .photo-cover {
    width: 100%;
    height: 7rem;
    border-radius: 4px;
  }

  .photo-name {
    font-size: 14px;
    margin-top: 0.3rem;
    text-align: center;
  }
}

.photo-page-root {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.app-container {
  max-width: 1400px;
  margin: 0 auto;
}

// 相册信息卡片
.album-info-card {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: $border-radius;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: $shadow;
  transition: $transition;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }

  .album-cover {
    width: 120px;
    height: 120px;
    border-radius: $border-radius;
    object-fit: cover;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  .image-placeholder {
    width: 120px;
    height: 120px;
    background-color: #f5f7fa;
    border-radius: $border-radius;
    display: flex;
    align-items: center;
    justify-content: center;

    .placeholder-icon {
      font-size: 32px;
      color: $text-color-placeholder;
    }
  }

  .album-info-content {
    margin-left: 20px;
    flex: 1;

    .album-header {
      display: flex;
      align-items: center;
      margin-bottom: 10px;

      .album-name {
        font-size: 22px;
        font-weight: 500;
        color: $text-color;
        margin: 0;
      }

      .photo-count {
        margin-left: 15px;
        color: $text-color-secondary;
        display: flex;
        align-items: center;
        font-size: 14px;

        .count-icon {
          margin-right: 5px;
          font-size: 16px;
        }
      }
    }

    .album-desc {
      display: flex;
      align-items: center;
      color: $text-color-secondary;
      margin-bottom: 10px;
      font-size: 14px;
      line-height: 1.5;

      .desc-icon {
        margin-right: 5px;
        color: $primary-color;
      }
    }

    .album-status {
      margin-top: 5px;
    }
  }
}

// 操作工具栏
.operation-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 15px;
  background-color: #fff;
  border-radius: $border-radius;
  margin-bottom: 20px;
  box-shadow: $shadow;
  align-items: center;

  .operation-btn {
    transition: $transition;
    display: flex;
    align-items: center;
    padding: 8px 16px;

    &:hover {
      transform: translateY(-2px);
    }
  }

  .upload-btn {
    background-color: $primary-color;

    &:hover {
      background-color: #66b1ff;
    }
  }

  .spider-btn {
    background-color: $warning-color;

    &:hover {
      background-color: #f0ad4e;
    }
  }

  .move-btn {
    background-color: $success-color;

    &:hover {
      background-color: #52c41a;
    }
  }

  .delete-btn {
    background-color: $danger-color;

    &:hover {
      background-color: #f78989;
    }
  }

  .picture-list {
    margin-top: 15px;
    // 减小列之间的间距
    --el-row-gutter: 12px !important;
  }

  .selection-info {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 15px;

    .check-all {
      color: $text-color-secondary;
      cursor: pointer;
    }

    .confirm-btn {
      transition: $transition;

      &:hover {
        transform: translateY(-2px);
      }
    }
  }

  // 上传对话框
  .upload-container {
    height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;

    :deep( .avatar-uploader) {
      width: 100%;
    }

    :deep(.el-upload-list--picture-card .el-upload-list__item) {
      width: 100px;
      height: 100px;
    }

    .upload-drag-area {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
    }

    .drag-upload {
      border: 2px dashed #ddd;
      border-radius: $border-radius;
      padding: 40px 20px;
      transition: $transition;

      &:hover {
        border-color: $primary-color;
        background-color: rgba(64, 158, 255, 0.05);
      }
    }

    .upload-icon {
      font-size: 48px;
      color: $primary-color;
      margin-bottom: 15px;
    }

    .upload-text {
      font-size: 16px;
      color: $text-color;
      margin-bottom: 8px;

      em {
        color: $primary-color;
        cursor: pointer;
      }
    }

    .upload-hint {
      font-size: 12px;
      color: $text-color-placeholder;
    }
  }

  .upload-footer {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .upload-count {
    color: $text-color-secondary;
    font-size: 14px;
  }

  .upload-actions {
    display: flex;
    gap: 10px;
  }

  // 图片预览对话框
  .image-preview-dialog {
    padding: 0;
    background-color: rgba(0, 0, 0, 0.9);

    :deep( .el-dialog__body) {
      padding: 0;
      overflow: hidden;
    }
  }

  .preview-image {
    display: block;
    max-width: 100%;
    max-height: 80vh;
    margin: 0 auto;
  }

  .close-preview {
    position: absolute;
    top: 15px;
    right: 15px;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background-color: rgba(0, 0, 0, 0.5);
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: $transition;

    &:hover {
      background-color: rgba(0, 0, 0, 0.8);
    }

    .close-icon {
      color: #fff;
      font-size: 18px;
    }
  }

  // 爬虫状态面板
  .spider-status-container {
    background: #ffffff;
    border-radius: $border-radius;
    padding: 25px 30px;
    margin-top: 20px;
    box-shadow: $shadow;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    max-width: 1400px;
    margin-left: auto;
    margin-right: auto;

    &:hover {
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
    }

    .spider-status-icon {
      display: flex;
      justify-content: center;
      margin-bottom: 18px;

      .status-icon {
        font-size: 32px;
      }

      .running {
        color: $primary-color;
        animation: spin 1.5s linear infinite;
      }

      .completed {
        color: $success-color;
      }

      .failed {
        color: $danger-color;
      }

      .default {
        color: $text-color-placeholder;
      }
    }

    .spider-status-text {
      text-align: center;
      margin-bottom: 20px;

      .status-title {
        font-size: 16px;
        font-weight: 500;
        margin: 0;
        transition: color 0.3s ease;
      }

      .text-default {
        color: $primary-color;
      }

      .text-success {
        color: $success-color;
      }

      .text-failed {
        color: $danger-color;
      }
    }

    .spider-progress-wrapper {
      width: 100%;
      max-width: 500px;
      margin: 0 auto;
    }
  }

  // 动画
  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  /* 失败抖动动画：轻微晃动，增强交互反馈 */
  @keyframes shake {
    0%, 100% {
      transform: translateX(0);
    }
    25% {
      transform: translateX(-3px);
    }
    75% {
      transform: translateX(3px);
    }
  }
}

:deep(.picture-list .el-checkbox__input) {
  position: absolute !important;
  top: 0.6rem;
  left: 0.8rem;
}
</style>