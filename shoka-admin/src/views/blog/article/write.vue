<template>
  <div class="app-container">
    <!-- 文章标题区域 -->
    <div class="operation-container">
      <el-input
          v-model="articleForm.articleTitle"
          placeholder="请输入文章标题"
          class="title-input"
          :class="{ 'title-empty': !articleForm.articleTitle.trim() }"
      ></el-input>
      <el-button
          type="primary"
          class="publish-btn"
          @click="openModel"
      >
        发布文章
      </el-button>
    </div>
    <!-- 文章内容编辑器 -->
    <div class="editor-wrapper">
      <umo-editor
          :editor-key="options.editorKey"
          ref="editorRef"
          v-model="articleForm.articleContent"
          @change="handleEditorChange"
          :theme="isDark ? 'dark' : 'light'"
          class="md-container"
          :toolbars="toolbars"
          @on-upload-img="uploadImg"
          placeholder="请输入文章内容..."
          previewTheme="smart-blue"
      >
        <template #defToolbars>
          <emoji-extension :on-insert="insert"/>
        </template>
      </umo-editor>
    </div>

    <!-- 发布或修改对话框 -->
    <el-dialog
        title="发布文章"
        v-model="addOrUpdate"
        width="600px"
        top="0.5vh"
        append-to-body
        class="custom-dialog"
    >
      <el-form
          ref="articleFormRef"
          label-width="80px"
          :model="articleForm"
          :rules="rules"
          class="article-form"
      >
        <!-- 文章分类 -->
        <el-form-item label="文章分类" prop="categoryName" class="form-item">
          <el-tag
              type="success"
              v-show="articleForm.categoryName"
              :disable-transitions="true"
              :closable="true"
              @close="removeCategory"
              class="selected-tag"
          >
            {{ articleForm.categoryName }}
          </el-tag>

          <!-- 分类选项 -->
          <el-popover
              v-if="!articleForm.categoryName"
              placement="bottom-start"
              width="460"
              trigger="click"
              popper-class="custom-popover"
          >
            <template #reference>
              <el-button type="success" plain class="add-btn">添加分类</el-button>
            </template>
            <div class="popover-title">分类</div>
            <!-- 搜索框 -->
            <el-autocomplete
                style="width: 100%"
                v-model="categoryName"
                :fetch-suggestions="searchCategory"
                placeholder="请输入分类名搜索,enter可添加自定义分类"
                :trigger-on-focus="false"
                @keyup.enter="saveCategory"
                @select="handleSelectCategory"
                class="search-input"
            >
              <template #default="{ item }">
                <div>{{ item.categoryName }}</div>
              </template>
            </el-autocomplete>
            <!-- 分类列表 -->
            <div class="popover-container">
              <div
                  v-for="item of categoryList"
                  :key="item.id"
                  class="category-item"
                  @click="addCategory(item.categoryName)"
              >
                {{ item.categoryName }}
              </div>
            </div>
          </el-popover>
        </el-form-item>

        <!-- 文章标签 -->
        <el-form-item label="文章标签" prop="tagNameList" class="form-item">
          <div class="tags-container">
            <el-tag
                v-for="(item, index) of articleForm.tagNameList"
                :key="index"
                :disable-transitions="true"
                :closable="true"
                @close="removeTag(item)"
                class="tag-item"
            >
              {{ item }}
            </el-tag>
          </div>

          <!-- 标签选项 -->
          <el-popover
              placement="bottom-start"
              width="460"
              trigger="click"
              v-if="articleForm.tagNameList.length < 3"
              popper-class="custom-popover"
          >
            <template #reference>
              <el-button type="success" plain class="add-btn">添加标签</el-button>
            </template>
            <div class="popover-title">标签</div>
            <!-- 搜索框 -->
            <el-autocomplete
                style="width: 100%"
                v-model="tagName"
                :fetch-suggestions="searchTag"
                placeholder="请输入标签名搜索,enter可添加自定义标签"
                :trigger-on-focus="false"
                @keyup.enter="saveTag"
                @select="handleSelectTag"
                class="search-input"
            >
              <template #default="{ item }">
                <div>{{ item.tagName }}</div>
              </template>
            </el-autocomplete>
            <!-- 标签列表 -->
            <div class="popover-container">
              <div class="section-title">推荐标签</div>
              <div class="tags-grid">
                <el-tag
                    v-for="(item, index) of tagList"
                    :key="index"
                    :class="tagClass(item.tagName)"
                    @click="addTag(item.tagName)"
                    class="selectable-tag"
                >
                  {{ item.tagName }}
                </el-tag>
              </div>
            </div>
          </el-popover>
        </el-form-item>

        <!-- 文章类型 -->
        <el-form-item label="文章类型" prop="articleType" class="form-item">
          <el-select
              v-model="articleForm.articleType"
              placeholder="请选择类型"
              class="form-select"
          >
            <el-option
                v-for="item in typeList"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            ></el-option>
          </el-select>
        </el-form-item>

        <!-- 缩略图 -->
        <el-form-item label="缩略图" prop="articleCover" class="form-item">
          <el-upload
              drag
              :show-file-list="false"
              :headers="authorization"
              action="http://121.41.87.40:8080/admin/article/upload"
              accept="image/*"
              :before-upload="beforeUpload"
              :on-success="handleSuccess"
              class="cover-uploader"
          >
            <el-icon class="el-icon--upload" v-if="articleForm.articleCover === ''">
              <upload-filled/>
            </el-icon>
            <div class="el-upload__text" v-if="articleForm.articleCover === ''">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <img
                v-else
                :src="articleForm.articleCover"
                width="360"
                class="preview-image"
                loading="lazy"
                alt="文章缩略图"
            />
          </el-upload>
        </el-form-item>

        <!-- 置顶 -->
        <el-form-item label="置顶" prop="isTop" class="form-item switch-item">
          <el-switch
              v-model="articleForm.isTop"
              :active-value="1"
              :inactive-value="0"
              class="form-switch"
          ></el-switch>
        </el-form-item>

        <!-- 推荐 -->
        <el-form-item label="推荐" prop="isRecommend" class="form-item switch-item">
          <el-switch
              v-model="articleForm.isRecommend"
              :active-value="1"
              :inactive-value="0"
              class="form-switch"
          ></el-switch>
        </el-form-item>

        <!-- 发布形式 -->
        <el-form-item label="发布形式" prop="status" class="form-item">
          <el-radio-group v-model="articleForm.status" class="radio-group">
            <el-radio :label="1" class="radio-option">公开</el-radio>
            <el-radio :label="2" class="radio-option">私密</el-radio>
            <el-radio :label="3" class="radio-option">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button
              v-if="articleForm.status != 3"
              type="primary"
              class="submit-btn"
              @click="submitForm"
          >
            发布文章
          </el-button>
          <el-button
              v-else
              type="primary"
              class="submit-btn"
              @click="submitForm"
          >
            保存草稿
          </el-button>
          <el-button
              class="cancel-btn"
              @click="addOrUpdate = false"
          >
            取 消
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 接口请求相关
import {
  addArticle,
  editArticle,
  getCategoryOption,
  getTagOption,
  updateArticle,
  uploadArticleCover
} from "@/api/article";
import {ArticleForm, CategoryVO, TagVO} from "@/api/article/types";

// 组件相关
import EmojiExtension from '@/components/EmojiExtension/index.vue';
import {toolbars} from '@/components/EmojiExtension/staticConfig';
import { UmoEditor } from '@umoteam/editor';

// 工具类/第三方库
import router from "@/router";
import useStore from "@/store";
import {notifySuccess} from "@/utils/modal";
import {getToken, token_prefix} from '@/utils/token';
import {useDark, useDateFormat} from '@vueuse/core';
import {AxiosError, AxiosResponse} from 'axios';
import {ElMessage, FormInstance, FormRules, UploadRawFile} from 'element-plus';
import * as imageConversion from 'image-conversion';
import {computed, onMounted, reactive, ref, toRefs} from "vue";
import {useRoute} from "vue-router";
import { UploadFilled } from '@element-plus/icons-vue'

// 编辑器Ref（修正类型：适配umo-editor）
const editorRef = ref<InstanceType<typeof UmoEditor>>();
const options = ref({
  editorKey: 'article-editor-' + Date.now(), // 编辑器唯一标识
});

// 路由/表单相关
const route = useRoute();
const articleId = route.params.articleId;
const articleFormRef = ref<FormInstance>();
const articleTitle = ref(useDateFormat(new Date(), "YYYY-MM-DD"));
const {tag} = useStore();

// 表单校验规则
const rules = reactive<FormRules>({
  categoryName: [{required: true, message: "文章分类不能为空", trigger: "blur"}],
  tagNameList: [{required: true, message: "文章标签不能为空", trigger: "blur"}],
});

// 请求头（token）
const authorization = computed(() => {
  return {
    Authorization: token_prefix + getToken(),
  }
});

// 暗黑模式
const isDark = useDark();

// 标签选中样式计算
const tagClass = computed(() => {
  return function (item: string) {
    const index = articleForm.value.tagNameList.indexOf(item);
    return index !== -1 ? "tag-item-select" : "tag-item";
  };
});

// 响应式数据
const data = reactive({
  addOrUpdate: false,
  typeList: [
    { value: 1, label: "原创" },
    { value: 2, label: "转载" },
    { value: 3, label: "翻译" },
  ],
  articleForm: {
    id: undefined,
    articleCover: "",
    articleTitle: articleTitle.value,
    articleContent: "",
    categoryName: "",
    tagNameList: [],
    articleType: 1,
    isTop: 0,
    isRecommend: 0,
    status: 1,
  } as ArticleForm,
  categoryList: [] as CategoryVO[],
  tagList: [] as TagVO[],
  categoryName: "",
  tagName: "",
});

// 解构响应式数据
const {
  addOrUpdate,
  typeList,
  articleForm,
  categoryList,
  tagList,
  categoryName,
  tagName,
} = toRefs(data);

// 编辑器图片上传
const uploadImg = async (files: Array<File>, callback: (urls: string[]) => void) => {
  try {
    const res = await Promise.all(
        files.map((file) => {
          return new Promise((resolve, reject) => {
            const form = new FormData();
            form.append('file', file);
            uploadArticleCover(form).then(({data}) => {
              if (data.flag) {
                resolve(data);
              } else {
                reject(new Error(data.msg || '图片上传失败'));
              }
            }).catch((error: AxiosError) => reject(error));
          });
        })
    );
    callback(res.map((item: any) => item.data));
  } catch (error) {
    ElMessage.error('图片上传失败，请重试');
    console.error('图片上传错误：', error);
  }
};

// 手动同步编辑器内容到表单（核心修复点）
const handleEditorChange = (content: string) => {
  articleForm.value.articleContent = content;
};

// 打开发布弹窗
const openModel = () => {
  // 主动获取编辑器最新内容（避免v-model同步延迟）
  const latestContent = editorRef.value?.getContent?.() || articleForm.value.articleContent;
  articleForm.value.articleContent = latestContent.trim();

  // 标题校验
  if (articleForm.value.articleTitle.trim() === "") {
    ElMessage.error("文章标题不能为空");
    return false;
  }
  // 内容校验（现在能拿到真实内容）
  if (articleForm.value.articleContent === "") {
    ElMessage.error("文章内容不能为空");
    return false;
  }

  // 清空表单校验、加载分类/标签列表
  articleFormRef.value?.clearValidate();
  getCategoryOption().then(({data}) => {
    categoryList.value = data.data;
  });
  getTagOption().then(({data}) => {
    tagList.value = data.data;
  });

  addOrUpdate.value = true;
};

// 移除标签
const removeTag = (item: string) => {
  const index = articleForm.value.tagNameList.indexOf(item);
  if (index > -1) {
    articleForm.value.tagNameList.splice(index, 1);
  }
};

// 选择标签
const handleSelectTag = (item: TagVO) => {
  addTag(item.tagName);
};

// 保存自定义标签
const saveTag = () => {
  if (tagName.value.trim() !== "") {
    addTag(tagName.value);
    tagName.value = "";
  }
};

// 添加标签
const addTag = (item: string) => {
  const trimItem = item.trim();
  if (trimItem && articleForm.value.tagNameList.indexOf(trimItem) === -1) {
    articleForm.value.tagNameList.push(trimItem);
  }
};

// 搜索标签
const searchTag = (keyword: string, cb: (arg: TagVO[]) => void) => {
  const results = keyword
      ? tagList.value.filter(createTagFilter(keyword))
      : tagList.value;
  cb(results);
};

// 标签过滤函数
const createTagFilter = (queryString: string) => {
  return (item: TagVO) => {
    return item.tagName.indexOf(queryString) !== -1;
  };
};

// 移除分类
const removeCategory = () => {
  articleForm.value.categoryName = "";
};

// 选择分类
const handleSelectCategory = (item: CategoryVO) => {
  addCategory(item.categoryName);
};

// 保存自定义分类
const saveCategory = () => {
  if (categoryName.value.trim() !== "") {
    addCategory(categoryName.value);
    categoryName.value = "";
  }
};

// 添加分类
const addCategory = (item: string) => {
  articleForm.value.categoryName = item.trim();
};

// 搜索分类
const searchCategory = (keyword: string, cb: (arg: CategoryVO[]) => void) => {
  const results = keyword
      ? categoryList.value.filter(createCategoryFilter(keyword))
      : categoryList.value;
  cb(results);
};

// 分类过滤函数
const createCategoryFilter = (queryString: string) => {
  return (item: CategoryVO) => {
    return item.categoryName.indexOf(queryString) !== -1;
  };
};

// 插入表情（适配umo-editor API）
const insert = (content: string) => {
  if (editorRef.value) {
    // 调用umo-editor插入内容方法
    editorRef.value.insertContent?.(content);
    // 插入后同步表单内容
    articleForm.value.articleContent = editorRef.value.getContent?.() || '';
  }
};

// 缩略图上传成功
const handleSuccess = (response: AxiosResponse) => {
  if (response.data.flag) {
    articleForm.value.articleCover = response.data.data;
  } else {
    ElMessage.error('缩略图上传失败：' + response.data.msg);
  }
};

// 缩略图上传前压缩
const beforeUpload = (rawFile: UploadRawFile) => {
  return new Promise((resolve) => {
    // 小于200KB直接上传
    if (rawFile.size / 1024 < 200) {
      resolve(rawFile);
      return;
    }
    // 压缩到200KB
    imageConversion
        .compressAccurately(rawFile, 200)
        .then(res => {
          resolve(res);
        })
        .catch(() => {
          resolve(rawFile); // 压缩失败则上传原文件
        });
  });
};

// 提交表单（发布/保存草稿）
const submitForm = () => {
  articleFormRef.value?.validate((valid) => {
    if (!valid) return;

    // 最终确认编辑器内容
    const finalContent = editorRef.value?.getContent?.() || articleForm.value.articleContent;
    articleForm.value.articleContent = finalContent;

    // 编辑/新增逻辑
    const request = articleForm.value.id
        ? updateArticle(articleForm.value)
        : addArticle(articleForm.value);

    request.then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
        // 关闭标签页并跳转列表
        const path = articleForm.value.id
            ? `/article/write/${articleForm.value.id}`
            : "/article/write";
        tag.delView({path});
        router.push({path: "/article/list"});

        // 重置表单
        articleForm.value = {
          id: undefined,
          articleCover: "",
          articleTitle: articleTitle.value,
          articleContent: "",
          categoryName: "",
          tagNameList: [],
          articleType: 1,
          isTop: 0,
          isRecommend: 0,
          status: 1,
        };
      } else {
        ElMessage.error(data.msg || '操作失败');
      }
      addOrUpdate.value = false;
    }).catch((error) => {
      ElMessage.error('网络异常，操作失败');
      console.error('提交表单错误：', error);
    });
  });
};

// 初始化（编辑文章时回显数据）
onMounted(() => {
  if (articleId) {
    editArticle(Number(articleId)).then(({data}) => {
      if (data.flag) {
        articleForm.value = data.data;
        // 主动给编辑器赋值（避免v-model同步问题）
        if (editorRef.value && data.data.articleContent) {
          editorRef.value.setContent?.(data.data.articleContent);
        }
      } else {
        ElMessage.error(data.msg || '获取文章信息失败');
        tag.delView({path: `/article/write/${articleId}`});
        router.push({path: "/article/list"});
      }
    }).catch(() => {
      ElMessage.error('网络异常，获取文章信息失败');
      tag.delView({path: `/article/write/${articleId}`});
      router.push({path: "/article/list"});
    });
  }
});
</script>

<style scoped>
.app-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 标题区域样式 */
.operation-container {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.title-input {
  flex: 1;
  height: 52px;
  font-size: 18px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.title-input:focus {
  box-shadow: 0 0 0 2px rgba(145, 163, 255, 0.3);
  border-color: #6b85ff;
}

.title-empty {
  border-color: #ff4d4f;
  animation: shake 0.5s ease;
}

.publish-btn {
  height: 52px;
  padding: 0 24px;
  font-size: 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 133, 255, 0.3);
}

/* 编辑器区域样式 */
.editor-wrapper {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: all 0.3s ease;
}

.editor-wrapper:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.md-container {
  min-height: 500px;
  height: calc(100vh - 220px);
  border: none !important;
}

/* 对话框样式 */
.custom-dialog {
  --el-dialog-border-radius: 12px;
  --el-dialog-box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.custom-dialog .el-dialog__header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f2f5;
}

.custom-dialog .el-dialog__title {
  font-size: 18px;
  font-weight: 600;
}

.custom-dialog .el-dialog__body {
  padding: 24px;
}

/* 表单样式 */
.article-form {
  margin-top: 8px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item .el-form-item__label {
  font-weight: 500;
  color: #4e5969;
}

.add-btn {
  margin-left: 8px;
  transition: all 0.2s ease;
}

.add-btn:hover {
  background-color: #5f92a6;
}

/* 标签样式 */
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  height: 32px;
}

.tag-item {
  background-color: #f0f5ff;
  color: #4096ff;
  height: 100%;
  border-color: #bfdbfe;
  transition: all 0.2s ease;
}

.tag-item:hover {
  background-color: #e6f0ff;
}

.selected-tag {
  margin-bottom: 8px;
}

/* 下拉弹窗样式 */
.custom-popover {
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 16px;
}

.popover-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1d2129;
  text-align: center;
}

.search-input {
  margin-bottom: 16px;
}

.popover-container {
  max-height: 260px;
  overflow-y: auto;
  padding-right: 8px;
}

.popover-container::-webkit-scrollbar {
  width: 6px;
}

.popover-container::-webkit-scrollbar-thumb {
  background-color: #ddd;
  border-radius: 3px;
}

.category-item {
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.category-item:hover {
  background-color: #f0f5ff;
  color: #4096ff;
}

.section-title {
  margin-bottom: 12px;
  font-weight: 500;
  color: #86909c;
  font-size: 14px;
}

.tags-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selectable-tag {
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-item-select {
  background-color: #f5f5f5;
  color: #d52828;
  border-color: #d9d9d9;
}

/* 表单元素样式 */
.form-select {
  width: 100%;
}

.cover-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.cover-uploader:hover {
  border-color: #4096ff;
}

.preview-image {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.preview-image:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.switch-item {
  display: flex;
  align-items: center;
}

.form-switch {
  --el-switch-on-color: #6b85ff;
}

.radio-group {
  display: flex;
  gap: 20px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 底部按钮样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f2f5;
}

.submit-btn {
  background-color: #6b85ff;
  border-color: #6b85ff;
  transition: all 0.2s ease;
}

.submit-btn:hover {
  background-color: #5573e8;
  border-color: #5573e8;
}

.cancel-btn {
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background-color: #f5f5f5;
}

/* 动画效果 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-5px); }
  40%, 80% { transform: translateX(5px); }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .app-container {
    padding: 16px;
  }

  .operation-container {
    flex-direction: column;
    align-items: stretch;
  }

  .title-input, .publish-btn {
    width: 100%;
  }

  .md-container {
    height: calc(100vh - 200px);
  }

  .custom-dialog {
    width: 90% !important;
  }

  .radio-group {
    flex-direction: column;
    gap: 12px;
  }
}
</style>