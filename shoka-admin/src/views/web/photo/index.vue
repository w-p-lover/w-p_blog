<template>
  <div class="app-container">
    <!-- 相册信息 -->
    <el-row :gutter="12" class="mb15">
      <el-col :span="1.5">
        <el-image fit="cover" class="album-cover" :src="albumInfo.albumCover">
        </el-image>
      </el-col>
      <el-col :span="12">
        <el-row align="bottom">
          <span class="album-name">{{ albumInfo.albumName }}</span>
          <span class="photo-count">{{ albumInfo.photoCount }}张</span>
        </el-row>
        <el-row class="album-desc">{{ albumInfo.albumDesc }}</el-row>
        <el-row class="select-count">已选择{{ selectPhotoIdList.length }}张</el-row>
      </el-col>
    </el-row>
    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb20">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Upload" @click="upload = true">上传</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Plus"
                   @click="handleRunSpider">壁纸脚本
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Promotion"
                   :disabled="selectPhotoIdList.length == 0">移动
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="selectPhotoIdList.length == 0"
                   @click="handleDelete">批量删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-checkbox v-model="checkAll" :indeterminate="isIndeterminate" @change="handleCheckAllChange">
          全选
        </el-checkbox>
      </el-col>
    </el-row>
    <!-- 照片列表 -->
    <el-checkbox-group v-model="selectPhotoIdList" @change="handleCheckedPhotoChange">
      <el-row class="picture-list" :gutter="10">
        <el-col :xs="12" :sm="6" :lg="4" v-for="photo of photoList" :key="photo.id" style="margin-bottom:1rem;">
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
    <el-pagination class="pagination-container" v-model:current-page="queryParams.current"
                   v-model:page-size="queryParams.size" :hide-on-single-page="true" layout=" prev, pager, next"
                   :total="count"
                   @size-change="handleSizeChange" @current-change="handleCurrentChange"></el-pagination>
    <!-- 修改对话框 -->
    <el-dialog title="修改照片" v-model="update" width="550px" append-to-body>
      <el-form ref="photoFormRef" label-width="100px" :model="photoForm" :rules="rules">
        <el-form-item label="照片名称" prop="photoName">
          <el-input placeholder="请输入照片名称" v-model="photoForm.photoName" style="width: 250px;"/>
        </el-form-item>
        <el-form-item label="照片描述" prop="photoDesc">
          <el-input placeholder="请输入照片描述" v-model="photoForm.photoDesc" style="width: 250px;"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="update = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 上传对话框 -->
    <el-dialog title="上传照片" v-model="upload" width="800px" append-to-body>
      <div class="upload-container">
        <el-upload v-show="uploadList.length > 0" :headers="authorization" class="avatar-uploader" multiple
                   action="http://121.41.87.40:8080/admin/photo/upload" :before-upload="beforeUpload"
                   :on-success="handleSuccess"
                   :on-remove="handleRemove" :on-preview="handlePictureCardPreview" list-type="picture-card"
                   :file-list="uploadList" accept="image/*">
          <img class="avatar"/>
          <el-icon class="avatar-uploader-icon">
            <Plus/>
          </el-icon>
        </el-upload>
        <div class="upload">
          <el-upload v-show="uploadList.length === 0" :headers="authorization" drag multiple
                     action="http://121.41.87.40:8080/admin/photo/upload" :before-upload="beforeUpload"
                     :show-file-list="false"
                     accept="image/*" :on-success="handleSuccess" style="width:360px;">
            <el-icon class="el-icon--upload">
              <upload-filled/>
            </el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <img width="360"/>
          </el-upload>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <div>
            共上传{{ uploadList.length }}张照片
          </div>
          <div>
            <el-button type="primary" :disabled="uploadList.length == 0" @click="handleAdd">确 定</el-button>
            <el-button @click="upload = false">取 消</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
    <!-- 图片预览 -->
    <el-dialog v-model="dialogVisible" append-to-body>
      <img :src="dialogImageUrl" style="max-width:100%"/>
    </el-dialog>
  </div>
  <div v-if="loading" class="spider-status-container">
    <!-- 状态图标：根据状态显示不同图标，增强直观性 -->
    <div class="spider-status-icon" :class="statusIconClass">
      <i v-if="spiderStatus === 'RUNNING'" class="el-icon-loading"></i>
      <i v-else-if="spiderStatus === 'COMPLETED'" class="el-icon-circle-check"></i>
      <i v-else-if="spiderStatus === 'FAILED'" class="el-icon-circle-exclamation"></i>
      <i v-else class="el-icon-spider"></i> <!-- 未开始用爬虫图标 -->
    </div>

    <!-- 文字区域：拆分主状态和详情，优化排版层次 -->
    <div class="spider-status-text">
      <h4 class="status-title" :class="statusTextClass">{{ spiderMessage }}</h4>
    </div>

    <!-- 进度条：优化尺寸、颜色渐变，增加过渡动画 -->
    <div class="spider-progress-wrapper">
      <el-progress
          type="line"
          :percentage="spiderPercentage"
          :status="statusProgressClass"
          :color="progressColor"
          :stroke-width="12"
      :gap-degree="30"
      ></el-progress>
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

import { Loading, CircleCheck, CircleClose, User } from '@element-plus/icons-vue'; // 这里换成你需要的图标


const props = defineProps({
  spiderStatus: String
});

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
  loading: false,
  upload: false,
  update: false,
  checkAll: false,
  isIndeterminate: false,
  dialogImageUrl: "",
  dialogVisible: false,
  queryParams: {
    current: 1,
    size: 18,
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
watch(photoList, () => {
  photoIdList.value = [];
  photoList.value.forEach(item => {
    photoIdList.value.push(item.id);
  });
});
const showSpiderNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
  ElNotification({
    title: '爬虫状态',
    message,
    type,
    duration: 5000, // 显示 5 秒后自动关闭
    offset: 60,     // 距离顶部 60px
    dangerouslyUseHTMLString: true
  })
}

const handleSizeChange = (size: number) => {
  queryParams.value.size = size;
  getList();
};
const handleCurrentChange = (current: number) => {
  queryParams.value.current = current;
  getList();
};
const handleCheckAllChange = (val: boolean) => {
  selectPhotoIdList.value = val ? photoIdList.value : [];
  isIndeterminate.value = false;
};
const handleCheckedPhotoChange = (value: number[]) => {
  const checkedCount = value.length;
  checkAll.value = checkedCount === photoIdList.value.length;
  isIndeterminate.value = checkedCount > 0 && checkedCount < photoIdList.value.length;
};
const handleCommand = (photo: Photo) => {
  photoFormRef.value?.resetFields();
  photoForm.value = photo;
  update.value = true;
};
const beforeUpload = (rawFile: UploadRawFile) => {
  return new Promise(resolve => {
    if (rawFile.size / 1024 < 200) {
      resolve(rawFile);
    }
    // 压缩到200KB,这里的200就是要压缩的大小,可自定义
    imageConversion
        .compressAccurately(rawFile, 200)
        .then(res => {
          resolve(res);
        });
  });
};
const handleSuccess = (response: AxiosResponse) => {
  uploadList.value.push({url: response.data});
};
const handleRemove = (file: UploadFile) => {
  uploadList.value.forEach((item, index) => {
    if (item.url == file.url) {
      uploadList.value.splice(index, 1);
    }
  });
};
const handlePictureCardPreview = (file: UploadFile) => {
  dialogImageUrl.value = file.url!;
  dialogVisible.value = true;
};
const handleMove = () => {

};
const handleDelete = () => {
  messageConfirm("确认删除已选中的数据项?").then(() => {
    deletePhoto(selectPhotoIdList.value).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
        selectPhotoIdList.value = [];
        isIndeterminate.value = false;
      }
    });
  }).catch(() => {
  });
};

const handleRunSpider = async () => {
  if (loading.value) return;
  loading.value = true;

  try {
    await axios.post('http://localhost:8080/photo/run', null, {});
    showSpiderNotification('info',
        `<div style="text-align: left; line-height: 1.6; margin-left: 40px">
                    <strong>🚀 爬虫任务已启动</strong><br>
                            📝 壁纸网站: <span style="color:#409EFF;">WallHaven</span><br>
                            </div>`);
    const startTime = Date.now();
    const pollStatus = async () => {
      try {
        const { data } = await axios.get('http://localhost:8080/photo/status');
        status.value = data.data.status?.trim()?.toUpperCase();
        spiderPercentage.value = data.data.spiderPercentage;
        spiderMessage.value = data.data.message || '';
        console.log('爬虫状态:', status);

        // 完成/失败或超时
        if (['COMPLETED', 'FAILED'].includes(status.value) || Date.now() - startTime > 180000) // 3分钟
        {
          loading.value = false;

          getList();
          console.log('爬虫完成，开始获取相册信息...');
          getAlbumInfo(Number(route.params.albumId)).then(({data}) => {
            albumInfo.value = data.data;
          });
          if (status.value === 'COMPLETED') {
            showSpiderNotification('success', '爬虫任务完成！');
          } else if (status.value === 'FAILED') {
            showSpiderNotification('error', '爬虫任务失败！');
          } else {
            showSpiderNotification('warning', '爬虫任务超时！');
          }

        } else {
          // 每次轮询间隔 5 秒
          setTimeout(pollStatus, 15000);
        }

      } catch (err) {
        console.error('轮询失败:', err);
        loading.value = false;
        showSpiderNotification('error', '轮询失败，请重试！');
      }
    };

    // 启动第一次轮询
    await pollStatus();

  } catch (err) {
    console.error('启动爬虫失败:', err);
    loading.value = false;
    showSpiderNotification('error', '启动爬虫失败！');
  }
};

const handleAdd = () => {
  let photoUrlList: string[] = [];
  if (uploadList.value.length > 0) {
    uploadList.value.forEach(item => {
      photoUrlList.push(item.url);
    });
  }
  addPhoto({albumId: Number(route.params.albumId), photoUrlList: photoUrlList}).then(({data}) => {
    if (data.flag) {
      notifySuccess(data.msg);
      uploadList.value = [];
      getList();
    }
    upload.value = false;
  })
};
const submitForm = () => {
  photoFormRef.value?.validate((valid) => {
    if (valid) {
      updatePhoto(photoForm.value).then(({data}) => {
        if (data.flag) {
          notifySuccess(data.msg);
          getList();
        }
        update.value = false;
      })
    }
  });
};
const getList = () => {
  loading.value = true;
  getPhotoList(queryParams.value).then(({data}) => {
    photoList.value = data.data.recordList;
    count.value = data.data.count;
    loading.value = false;
  });
};
onMounted(() => {
  getList();
  getAlbumInfo(Number(route.params.albumId)).then(({data}) => {
    albumInfo.value = data.data;
  });
});

const statusIconClass = computed(() => {
  const base = 'spider-icon';
  if (status.value === 'RUNNING') return `${base} icon-loading`;
  if (status.value === 'COMPLETED') return `${base} icon-success`;
  if (status.value === 'FAILED') return `${base} icon-failed`;
  return `${base} icon-default`;
});

const statusTextClass = computed(() => {
  if (status.value === 'COMPLETED') return 'text-success';
  if (status.value === 'FAILED') return 'text-failed';
  return 'text-default';
});

const statusDetailClass = computed(() => {
  return status.value === 'FAILED' ? 'detail-failed' : 'detail-default';
});

const statusProgressClass = computed(() => {
  if (status.value === 'COMPLETED') return 'success';
  if (status.value === 'FAILED') return 'exception';
  return '';
});

const progressColor = computed(() => {
  // 进度条渐变色：运行中用蓝紫渐变，成功用绿蓝渐变，失败用红橙渐变
  if (status.value === 'RUNNING') return ['#4096ff', '#6772e5'];
  if (status.value === 'COMPLETED') return ['#67c23a', '#52c41a'];
  if (status.value === 'FAILED') return ['#f56c6c', '#fa8c16'];
  return '#4096ff'; // 未开始用默认蓝色
});
</script>

<style lang="scss" scoped>
.album-cover {
  border-radius: 4px;
  width: 5rem;
  height: 5rem;
}

.album-name {
  font-size: 1.25rem;
}

.photo-count {
  font-size: 13px;
  margin: 0 0 0.1rem 0.5rem;
}

.album-desc {
  font-size: 15px;
  margin-top: 0.4rem;
}

.select-count {
  font-size: 13px;
  margin-top: 0.4rem;
}

.photo-item {
  position: relative;
  width: 100%;
  cursor: pointer;

  margin-bottom: 1rem;

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

.upload-container {
  height: 400px;

  .upload {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
  }
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.spider-status-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1px solid #f0f2f5;
  border-radius: 16px;
  padding: 28px 32px;
  margin-top: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.spider-status-container:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

/* 状态图标：控制大小、颜色，加载动画 */
.spider-icon {
  font-size: 28px;
  margin-bottom: 16px;
  transition: color 0.3s ease;
}

.icon-loading {
  color: #4096ff;
  animation: spin 1.5s linear infinite; /* 加载动画更流畅 */
}

.icon-success {
  color: #67c23a;
}

.icon-failed {
  color: #f56c6c;
}

.icon-default {
  color: #909399;
}

/* 文字区域：优化字体层级、间距、颜色 */
.spider-status-text {
  text-align: center;
  margin-bottom: 20px;
}

.status-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 8px;
  transition: color 0.3s ease;
}

.text-default {
  color: #303133;
}

.text-success {
  color: #67c23a;
}

.text-failed {
  color: #f56c6c;
}

.status-detail {
  font-size: 13px;
  margin: 0;
  transition: color 0.3s ease;
}

.detail-default {
  color: #909399;
}

.detail-failed {
  color: #f56c6c;
  /* 失败时增加轻微抖动动画，提醒用户 */
  animation: shake 0.5s ease-in-out;
}

/* 进度条容器：控制尺寸，增加内边距 */
.spider-progress-wrapper {
  width: 100%;
  max-width: 280px; /* 缩小仪表盘最大宽度，更精致 */
}

/* 加载动画：更平滑的旋转 */
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

</style>
