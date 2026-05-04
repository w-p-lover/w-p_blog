<template>
  <div class="app-container write-workspace">
    <div class="writing-toolbar">
      <div class="toolbar-left">
        <el-button class="icon-button" text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="toolbar-copy">
          <span class="toolbar-kicker">{{ articleForm.id ? "编辑文章" : "新文章" }}</span>
          <strong>{{ articleForm.articleTitle.trim() || "未命名草稿" }}</strong>
        </div>
      </div>

      <div class="toolbar-stats">
        <span>{{ writingStats.wordCount }} 字</span>
        <span>{{ writingStats.readMinutes }} 分钟阅读</span>
        <span>{{ statusText }}</span>
      </div>

      <div class="toolbar-actions">
        <el-button class="draft-btn" @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" class="publish-btn" @click="publishArticle">
          {{ submitButtonText }}
        </el-button>
      </div>
    </div>

    <div class="writing-board">
      <main class="writing-main">
        <section class="title-card">
          <el-input
              v-model="articleForm.articleTitle"
              placeholder="写下这篇文章的标题"
              class="title-input"
              :class="{ 'title-empty': !articleForm.articleTitle.trim() }"
          />
          <div class="title-meta">
            <span>{{ articleForm.categoryName || "未选择分类" }}</span>
            <span v-if="articleForm.tagNameList.length">
              {{ articleForm.tagNameList.join(" / ") }}
            </span>
            <span v-else>未添加标签</span>
          </div>
        </section>

        <section class="editor-shell">
          <MdEditor
              :editor-id="options.editorId"
              ref="editorRef"
              v-model="articleForm.articleContent"
              :theme="isDark ? 'dark' : 'light'"
              class="md-container"
              :style="{ height: '100%' }"
              :toolbars="toolbars"
              :footers="editorFooters"
              :preview="!isCompactWriting"
              language="zh-CN"
              previewTheme="vuepress"
              codeTheme="github"
              :showCodeRowNumber="true"
              :autoDetectCode="true"
              placeholder="从这里开始写..."
              @on-change="handleEditorChange"
              @on-save="saveDraft"
              @on-upload-img="uploadImg"
              @on-error="handleEditorError"
          >
            <template #defToolbars>
              <emoji-extension :on-insert="insert" />
            </template>
          </MdEditor>
        </section>
      </main>

      <aside class="publish-panel">
        <div class="panel-header">
          <div>
            <span class="panel-kicker">Publish</span>
            <h3>发布设置</h3>
          </div>
          <el-tag size="small" effect="plain" class="status-tag">{{ statusText }}</el-tag>
        </div>

        <el-form
            ref="articleFormRef"
            label-position="top"
            :model="articleForm"
            :rules="rules"
            class="article-form"
        >
          <section class="panel-section cover-section">
            <div class="section-heading">
              <el-icon><Picture /></el-icon>
              <span>文章封面</span>
            </div>
            <el-form-item prop="articleCover">
              <el-upload
                  drag
                  :show-file-list="false"
                  :http-request="uploadCover"
                  accept="image/*"
                  :before-upload="beforeUpload"
                  class="cover-uploader"
              >
                <div v-if="articleForm.articleCover === ''" class="cover-empty">
                  <el-icon class="upload-icon"><UploadFilled /></el-icon>
                  <span>拖拽或点击上传</span>
                </div>
                <img
                    v-else
                    :src="articleForm.articleCover"
                    class="preview-image"
                    loading="lazy"
                    alt="文章缩略图"
                />
              </el-upload>
            </el-form-item>
          </section>

          <section class="panel-section">
            <div class="section-heading">
              <el-icon><FolderOpened /></el-icon>
              <span>分类</span>
            </div>
            <el-form-item prop="categoryName">
              <div class="selected-line" v-if="articleForm.categoryName">
                <el-tag
                    type="success"
                    :disable-transitions="true"
                    :closable="true"
                    @close="removeCategory"
                    class="selected-tag"
                >
                  {{ articleForm.categoryName }}
                </el-tag>
              </div>

              <el-popover
                  v-if="!articleForm.categoryName"
                  placement="bottom-start"
                  width="320"
                  trigger="click"
                  popper-class="custom-popover"
              >
                <template #reference>
                  <el-button class="add-btn" plain @click="loadPublishOptions">添加分类</el-button>
                </template>
                <div class="popover-title">选择分类</div>
                <el-autocomplete
                    v-model="categoryName"
                    :fetch-suggestions="searchCategory"
                    placeholder="搜索或回车新建分类"
                    :trigger-on-focus="false"
                    @keyup.enter="saveCategory"
                    @select="handleSelectCategory"
                    class="search-input"
                >
                  <template #default="{ item }">
                    <span>{{ item.categoryName }}</span>
                  </template>
                </el-autocomplete>
                <div class="popover-container">
                  <button
                      v-for="item of categoryList"
                      :key="item.id"
                      type="button"
                      class="category-item"
                      @click="addCategory(item.categoryName)"
                  >
                    {{ item.categoryName }}
                  </button>
                </div>
              </el-popover>
            </el-form-item>
          </section>

          <section class="panel-section">
            <div class="section-heading">
              <el-icon><CollectionTag /></el-icon>
              <span>标签</span>
              <em>{{ articleForm.tagNameList.length }}/3</em>
            </div>
            <el-form-item prop="tagNameList">
              <div class="tags-container">
                <el-tag
                    v-for="item of articleForm.tagNameList"
                    :key="item"
                    :disable-transitions="true"
                    :closable="true"
                    @close="removeTag(item)"
                    class="tag-item"
                >
                  {{ item }}
                </el-tag>
              </div>

              <el-popover
                  placement="bottom-start"
                  width="320"
                  trigger="click"
                  v-if="articleForm.tagNameList.length < 3"
                  popper-class="custom-popover"
              >
                <template #reference>
                  <el-button class="add-btn" plain @click="loadPublishOptions">添加标签</el-button>
                </template>
                <div class="popover-title">选择标签</div>
                <el-autocomplete
                    v-model="tagName"
                    :fetch-suggestions="searchTag"
                    placeholder="搜索或回车新建标签"
                    :trigger-on-focus="false"
                    @keyup.enter="saveTag"
                    @select="handleSelectTag"
                    class="search-input"
                >
                  <template #default="{ item }">
                    <span>{{ item.tagName }}</span>
                  </template>
                </el-autocomplete>
                <div class="popover-container">
                  <div class="tags-grid">
                    <el-tag
                        v-for="item of tagList"
                        :key="item.id"
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
          </section>

          <section class="panel-section">
            <div class="section-heading">
              <el-icon><DocumentChecked /></el-icon>
              <span>文章属性</span>
            </div>
            <el-form-item label="类型" prop="articleType">
              <el-select v-model="articleForm.articleType" placeholder="请选择类型" class="form-select">
                <el-option
                    v-for="item in typeList"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>
            <div class="switch-grid">
              <label>
                <span>置顶</span>
                <el-switch v-model="articleForm.isTop" :active-value="1" :inactive-value="0" />
              </label>
              <label>
                <span>推荐</span>
                <el-switch v-model="articleForm.isRecommend" :active-value="1" :inactive-value="0" />
              </label>
            </div>
          </section>

          <section class="panel-section">
            <div class="section-heading">
              <el-icon><Timer /></el-icon>
              <span>发布形式</span>
            </div>
            <el-form-item prop="status">
              <el-radio-group v-model="articleForm.status" class="status-options">
                <el-radio-button :label="1">公开</el-radio-button>
                <el-radio-button :label="2">私密</el-radio-button>
                <el-radio-button :label="3">草稿</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </section>
        </el-form>

        <div class="panel-actions">
          <el-button class="draft-btn" @click="saveDraft">保存草稿</el-button>
          <el-button type="primary" class="publish-btn" @click="submitForm">
            {{ submitButtonText }}
          </el-button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  addArticle,
  editArticle,
  getCategoryOption,
  getTagOption,
  updateArticle,
  uploadArticleCover,
} from "@/api/article";
import { ArticleForm, CategoryVO, TagVO } from "@/api/article/types";
import EmojiExtension from "@/components/EmojiExtension/index.vue";
import { toolbars } from "@/components/EmojiExtension/staticConfig";
import router from "@/router";
import useStore from "@/store";
import { notifySuccess } from "@/utils/modal";
import { getArticleWritingStats } from "@/utils/article-writing.js";
import {
  ArrowLeft,
  CollectionTag,
  DocumentChecked,
  FolderOpened,
  Picture,
  Timer,
  UploadFilled,
} from "@element-plus/icons-vue";
import { useDark, useDateFormat, useMediaQuery } from "@vueuse/core";
import { AxiosError } from "axios";
import { ElMessage, FormInstance, FormRules, UploadRawFile } from "element-plus";
import * as imageConversion from "image-conversion";
import MdEditor, { type Footers, type InsertContentGenerator } from "md-editor-v3";
import "md-editor-v3/lib/style.css";
import { computed, onMounted, reactive, ref, toRefs } from "vue";
import { useRoute } from "vue-router";

const editorRef = ref<{ insert?: (generator: InsertContentGenerator) => void; focus?: () => void }>();
const options = ref({
  editorId: "article-editor-" + Date.now(),
});
const editorFooters: Footers[] = ["markdownTotal", "=", "scrollSwitch"];

const route = useRoute();
const articleId = route.params.articleId;
const articleFormRef = ref<FormInstance>();
const articleTitle = useDateFormat(new Date(), "YYYY-MM-DD");
const { tag } = useStore();
const isDark = useDark();
const isCompactWriting = useMediaQuery("(max-width: 900px)");
const publishOptionsLoaded = ref(false);

const rules = reactive<FormRules>({
  categoryName: [{ required: true, message: "文章分类不能为空", trigger: "change" }],
  tagNameList: [{ required: true, message: "文章标签不能为空", trigger: "change" }],
});

const data = reactive({
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

const {
  typeList,
  articleForm,
  categoryList,
  tagList,
  categoryName,
  tagName,
} = toRefs(data);

const writingStats = computed(() => getArticleWritingStats(articleForm.value.articleContent));

const statusText = computed(() => {
  if (articleForm.value.status === 2) return "私密";
  if (articleForm.value.status === 3) return "草稿";
  return "公开";
});

const submitButtonText = computed(() => {
  if (articleForm.value.status === 3) return "保存草稿";
  if (articleForm.value.status === 2) return "保存私密文章";
  return "发布文章";
});

const tagClass = computed(() => {
  return function (item: string) {
    return articleForm.value.tagNameList.indexOf(item) !== -1 ? "tag-item-select" : "tag-item";
  };
});

const syncEditorContent = () => {
  return articleForm.value.articleContent;
};

const loadPublishOptions = async () => {
  if (publishOptionsLoaded.value) return;

  try {
    const [categoryRes, tagRes] = await Promise.all([getCategoryOption(), getTagOption()]);
    categoryList.value = categoryRes.data.data || [];
    tagList.value = tagRes.data.data || [];
    publishOptionsLoaded.value = true;
  } catch (error) {
    ElMessage.error("分类和标签加载失败，请稍后重试");
  }
};

const uploadImg = async (files: Array<File>, callback: (urls: string[]) => void) => {
  try {
    const res = await Promise.all(
        files.map((file) => {
          return new Promise((resolve, reject) => {
            const form = new FormData();
            form.append("file", file);
            uploadArticleCover(form).then(({ data }) => {
              if (data.flag) {
                resolve(data);
              } else {
                reject(new Error(data.msg || "图片上传失败"));
              }
            }).catch((error: AxiosError) => reject(error));
          });
        })
    );
    callback(res.map((item: any) => item.data));
  } catch (error) {
    ElMessage.error("图片上传失败，请重试");
    console.error("图片上传错误：", error);
  }
};

const uploadCover = async (option: any) => {
  try {
    const form = new FormData();
    form.append("file", option.file);
    const { data } = await uploadArticleCover(form);

    if (data.flag) {
      articleForm.value.articleCover = data.data;
      option.onSuccess?.(data);
      return;
    }

    ElMessage.error(data.msg || "缩略图上传失败");
    option.onError?.(new Error(data.msg || "缩略图上传失败"));
  } catch (error) {
    ElMessage.error("缩略图上传失败，请重试");
    option.onError?.(error);
  }
};

const handleEditorChange = (content: string) => {
  articleForm.value.articleContent = content;
};

const handleEditorError = (error: { name: string; message: string }) => {
  console.error("Markdown 编辑器错误：", error);
  ElMessage.error(error.message || "编辑器加载异常，请刷新页面重试");
};

const removeTag = (item: string) => {
  const index = articleForm.value.tagNameList.indexOf(item);
  if (index > -1) {
    articleForm.value.tagNameList.splice(index, 1);
  }
};

const handleSelectTag = (item: TagVO) => {
  addTag(item.tagName);
};

const saveTag = () => {
  if (tagName.value.trim() !== "") {
    addTag(tagName.value);
    tagName.value = "";
  }
};

const addTag = (item: string) => {
  const trimItem = item.trim();
  if (!trimItem || articleForm.value.tagNameList.indexOf(trimItem) !== -1) return;
  if (articleForm.value.tagNameList.length >= 3) {
    ElMessage.warning("最多添加 3 个标签");
    return;
  }
  articleForm.value.tagNameList.push(trimItem);
};

const searchTag = (keyword: string, cb: (arg: TagVO[]) => void) => {
  const results = keyword ? tagList.value.filter(createTagFilter(keyword)) : tagList.value;
  cb(results);
};

const createTagFilter = (queryString: string) => {
  return (item: TagVO) => item.tagName.indexOf(queryString) !== -1;
};

const removeCategory = () => {
  articleForm.value.categoryName = "";
};

const handleSelectCategory = (item: CategoryVO) => {
  addCategory(item.categoryName);
};

const saveCategory = () => {
  if (categoryName.value.trim() !== "") {
    addCategory(categoryName.value);
    categoryName.value = "";
  }
};

const addCategory = (item: string) => {
  articleForm.value.categoryName = item.trim();
};

const searchCategory = (keyword: string, cb: (arg: CategoryVO[]) => void) => {
  const results = keyword ? categoryList.value.filter(createCategoryFilter(keyword)) : categoryList.value;
  cb(results);
};

const createCategoryFilter = (queryString: string) => {
  return (item: CategoryVO) => item.categoryName.indexOf(queryString) !== -1;
};

const insert = (generator: InsertContentGenerator) => {
  editorRef.value?.insert?.(generator);
};

const beforeUpload = (rawFile: UploadRawFile) => {
  return new Promise((resolve) => {
    if (rawFile.size / 1024 < 200) {
      resolve(rawFile);
      return;
    }
    imageConversion
        .compressAccurately(rawFile, 200)
        .then((res) => resolve(res))
        .catch(() => resolve(rawFile));
  });
};

const validateArticle = async () => {
  const content = syncEditorContent();

  if (articleForm.value.articleTitle.trim() === "") {
    ElMessage.error("文章标题不能为空");
    return false;
  }

  if (content.trim() === "") {
    ElMessage.error("文章内容不能为空");
    return false;
  }

  await loadPublishOptions();
  const valid = await articleFormRef.value?.validate().catch(() => false);
  return Boolean(valid);
};

const submitForm = async () => {
  const valid = await validateArticle();
  if (!valid) return;

  const request = articleForm.value.id
      ? updateArticle(articleForm.value)
      : addArticle(articleForm.value);

  request.then(({ data }) => {
    if (data.flag) {
      notifySuccess(data.msg);
      const path = articleForm.value.id
          ? `/article/write/${articleForm.value.id}`
          : "/article/write";
      tag.delView({ path });
      router.push({ path: "/article/list" });

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
      ElMessage.error(data.msg || "操作失败");
    }
  }).catch((error) => {
    ElMessage.error("网络异常，操作失败");
    console.error("提交表单错误：", error);
  });
};

const publishArticle = () => {
  if (articleForm.value.status === 3) {
    articleForm.value.status = 1;
  }
  submitForm();
};

const saveDraft = () => {
  articleForm.value.status = 3;
  submitForm();
};

const goBack = () => {
  router.push({ path: "/article/list" });
};

onMounted(() => {
  loadPublishOptions();

  if (articleId) {
    editArticle(Number(articleId)).then(({ data }) => {
      if (data.flag) {
        articleForm.value = data.data;
      } else {
        ElMessage.error(data.msg || "获取文章信息失败");
        tag.delView({ path: `/article/write/${articleId}` });
        router.push({ path: "/article/list" });
      }
    }).catch(() => {
      ElMessage.error("网络异常，获取文章信息失败");
      tag.delView({ path: `/article/write/${articleId}` });
      router.push({ path: "/article/list" });
    });
  }
});
</script>

<style scoped>
.write-workspace {
  --writer-paper: #fffdf8;
  --writer-paper-deep: #f7f1e6;
  --writer-line: rgba(126, 111, 88, 0.18);
  --writer-text: #40382e;
  --writer-muted: #8b7c69;
  --writer-primary: #5f8f86;
  --writer-primary-deep: #42766e;
  --writer-accent: #ba6a58;

  height: calc(100vh - 84px);
  min-height: 720px;
  padding: 14px 16px 24px;
  overflow-y: auto;
  overflow-x: hidden;
  background:
      linear-gradient(135deg, rgba(255, 253, 248, 0.96), rgba(247, 241, 230, 0.9)),
      radial-gradient(circle at 10% 12%, rgba(95, 143, 134, 0.08), transparent 28%),
      radial-gradient(circle at 88% 0%, rgba(186, 106, 88, 0.09), transparent 30%);
  color: var(--writer-text);
}

.writing-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto auto;
  align-items: center;
  gap: 16px;
  margin: 0 auto 16px;
  max-width: none;
}

.toolbar-left,
.toolbar-actions,
.toolbar-stats {
  display: flex;
  align-items: center;
}

.toolbar-left {
  min-width: 0;
  gap: 10px;
}

.icon-button {
  width: 36px;
  height: 36px;
  color: var(--writer-muted);
  border-radius: 50%;
}

.toolbar-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.toolbar-copy strong {
  overflow: hidden;
  color: var(--writer-text);
  font-size: 17px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-kicker,
.panel-kicker {
  color: var(--writer-muted);
  font-size: 12px;
  letter-spacing: 0;
}

.toolbar-stats {
  gap: 12px;
  color: var(--writer-muted);
  font-size: 13px;
  white-space: nowrap;
}

.toolbar-stats span {
  position: relative;
}

.toolbar-stats span + span::before {
  position: absolute;
  top: 50%;
  left: -7px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(139, 124, 105, 0.45);
  content: "";
  transform: translateY(-50%);
}

.toolbar-actions {
  gap: 10px;
}

.draft-btn,
.publish-btn {
  height: 36px;
  border-radius: 18px;
}

.draft-btn {
  border-color: rgba(95, 143, 134, 0.26);
  background: rgba(255, 253, 248, 0.7);
  color: var(--writer-primary-deep);
}

.publish-btn {
  border-color: var(--writer-primary);
  background: var(--writer-primary);
  color: #fff;
}

.publish-btn:hover,
.publish-btn:focus {
  border-color: var(--writer-primary-deep);
  background: var(--writer-primary-deep);
}

.writing-board {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 306px;
  align-items: stretch;
  gap: 0;
  max-width: none;
  min-height: 0;
  margin: 0 auto;
  padding: 10px 0 10px 10px;
  border: 1px solid rgba(126, 111, 88, 0.12);
  border-radius: 10px;
  background:
      linear-gradient(90deg, rgba(255, 253, 248, 0.72), rgba(247, 241, 230, 0.48)),
      rgba(255, 253, 248, 0.48);
  box-shadow: 0 18px 46px rgba(77, 65, 48, 0.07);
}

.writing-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  gap: 14px;
  padding-right: 12px;
}

.title-card,
.editor-shell {
  border: 1px solid var(--writer-line);
  background: rgba(255, 253, 248, 0.86);
  box-shadow: 0 10px 28px rgba(77, 65, 48, 0.035);
}

.title-card {
  padding: 14px 20px 12px;
  border-radius: 8px;
}

.title-input {
  width: 100%;
}

.title-input :deep(.el-input__wrapper) {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.title-input :deep(.el-input__inner) {
  height: 40px;
  color: var(--writer-text);
  font-size: 24px;
  font-weight: 700;
  line-height: 40px;
}

.title-input :deep(.el-input__inner::placeholder) {
  color: rgba(139, 124, 105, 0.56);
}

.title-empty :deep(.el-input__inner::placeholder) {
  color: rgba(186, 106, 88, 0.68);
}

.title-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  color: var(--writer-muted);
  font-size: 13px;
}

.title-meta span {
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(95, 143, 134, 0.08);
}

.editor-shell {
  height: max(920px, calc(100vh - 150px));
  min-height: 0;
  overflow: hidden;
  border-radius: 8px;
}

.md-container {
  --md-color: var(--writer-text);
  --md-hover-color: var(--writer-primary-deep);
  --md-bk-color: rgba(255, 253, 248, 0.94);
  --md-bk-color-outstand: rgba(247, 241, 230, 0.76);
  --md-bk-hover-color: rgba(95, 143, 134, 0.09);
  --md-border-color: rgba(126, 111, 88, 0.16);
  --md-border-hover-color: rgba(95, 143, 134, 0.34);
  --md-border-active-color: var(--writer-primary);
  --md-scrollbar-bg-color: rgba(247, 241, 230, 0.7);
  --md-scrollbar-thumb-color: rgba(126, 111, 88, 0.22);
  --md-scrollbar-thumb-hover-color: rgba(95, 143, 134, 0.38);
  --md-theme-link-color: var(--writer-primary-deep);
  --md-theme-link-hover-color: var(--writer-primary);

  width: 100%;
  height: 100%;
  min-height: 0;
  border: 0 !important;
  background: transparent !important;
  color: var(--writer-text);
  font-family: inherit;
}

.md-container :deep(.md-editor-toolbar-wrapper) {
  height: 40px;
  padding: 5px 8px;
  border-bottom-color: rgba(126, 111, 88, 0.14);
  background: rgba(247, 241, 230, 0.64);
}

.md-container :deep(.md-editor-toolbar-wrapper .md-editor-toolbar) {
  min-width: 780px;
}

.md-container :deep(.md-editor-toolbar-item) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  min-width: 28px;
  border-radius: 6px;
  color: rgba(64, 56, 46, 0.82);
}

.md-container :deep(.md-editor-toolbar-item:hover) {
  background: rgba(95, 143, 134, 0.1);
  color: var(--writer-primary-deep);
}

.md-container :deep(.md-editor-content) {
  background:
      linear-gradient(90deg, rgba(255, 253, 248, 0.96) 0 50%, rgba(252, 248, 241, 0.94) 50% 100%);
}

.md-container :deep(.md-editor-content:has(textarea.textarea-only)) {
  background: rgba(255, 253, 248, 0.96);
}

.md-container :deep(.md-editor-input-wrapper) {
  border-right: 1px solid rgba(126, 111, 88, 0.12);
}

.md-container :deep(.md-editor-content:has(textarea.textarea-only) .md-editor-input-wrapper) {
  flex: 1 1 100%;
  border-right: 0;
}

.md-container :deep(.md-editor-input-wrapper textarea) {
  padding: 26px 32px;
  color: var(--writer-text);
  font-size: 16px;
  line-height: 1.78;
  font-family: "Cascadia Code", "JetBrains Mono", Consolas, "PingFang SC", "Microsoft YaHei", monospace;
}

.md-container :deep(.md-editor-input-wrapper textarea::placeholder) {
  color: rgba(139, 124, 105, 0.58);
}

.md-container :deep(.md-editor-preview-wrapper) {
  background: rgba(255, 253, 248, 0.68);
}

.md-container :deep(.md-editor-preview),
.md-container :deep(.md-editor-html) {
  max-width: 820px;
  margin: 0 auto;
  padding: 28px 36px 48px;
  color: var(--writer-text);
  font-size: 16px;
  line-height: 1.9;
  word-break: break-word;
}

.md-container :deep(.vuepress-theme) {
  background: transparent;
}

.md-container :deep(.vuepress-theme h1),
.md-container :deep(.vuepress-theme h2),
.md-container :deep(.vuepress-theme h3),
.md-container :deep(.vuepress-theme h4),
.md-container :deep(.vuepress-theme h5),
.md-container :deep(.vuepress-theme h6) {
  color: var(--writer-text);
}

.md-container :deep(.vuepress-theme a) {
  color: var(--writer-primary-deep);
}

.md-container :deep(.vuepress-theme blockquote) {
  border-left-color: rgba(95, 143, 134, 0.45);
  background: rgba(95, 143, 134, 0.08);
  color: var(--writer-muted);
}

.md-container :deep(.md-editor-footer) {
  height: 28px;
  border-top-color: rgba(126, 111, 88, 0.14);
  background: rgba(247, 241, 230, 0.58);
  color: var(--writer-muted);
}

.md-container.md-editor-dark {
  --md-color: #d6cec1;
  --md-hover-color: #fff8ec;
  --md-bk-color: #211f1b;
  --md-bk-color-outstand: #2d2924;
  --md-bk-hover-color: rgba(118, 157, 150, 0.18);
  --md-border-color: rgba(226, 215, 198, 0.15);
  --md-border-hover-color: rgba(118, 157, 150, 0.46);
  --md-border-active-color: #7faea5;
  --md-scrollbar-bg-color: #2d2924;
  --md-scrollbar-thumb-color: rgba(226, 215, 198, 0.22);
}

.publish-panel {
  position: sticky;
  top: 10px;
  display: flex;
  min-height: 0;
  align-self: stretch;
  flex-direction: column;
  overflow: visible;
  border: 0;
  border-left: 1px solid rgba(126, 111, 88, 0.13);
  border-radius: 0;
  background:
      linear-gradient(90deg, rgba(126, 111, 88, 0.05), transparent 24px),
      linear-gradient(180deg, rgba(255, 253, 248, 0.18), rgba(247, 241, 230, 0.16));
  box-shadow: none;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px 16px 20px;
  border-bottom: 1px solid rgba(126, 111, 88, 0.1);
}

.panel-header h3 {
  margin: 2px 0 0;
  color: var(--writer-text);
  font-size: 16px;
}

.status-tag {
  border-color: rgba(95, 143, 134, 0.28);
  background: rgba(255, 253, 248, 0.34);
  color: var(--writer-primary-deep);
}

.article-form {
  flex: 1;
  min-height: 0;
  padding: 0 14px 12px 20px;
  overflow: visible;
}

.panel-section {
  padding: 16px 0;
  border-bottom: 1px solid rgba(126, 111, 88, 0.1);
}

.panel-section:last-child {
  border-bottom: 0;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 12px;
  color: rgba(64, 56, 46, 0.88);
  font-size: 14px;
  font-weight: 700;
}

.section-heading .el-icon {
  color: var(--writer-primary-deep);
}

.section-heading em {
  margin-left: auto;
  color: var(--writer-muted);
  font-style: normal;
  font-weight: 400;
}

.cover-section :deep(.el-form-item) {
  margin-bottom: 0;
}

.cover-uploader {
  width: 100%;
}

.cover-uploader :deep(.el-upload),
.cover-uploader :deep(.el-upload-dragger) {
  width: 100%;
}

.cover-uploader :deep(.el-upload-dragger) {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 138px;
  overflow: hidden;
  border-color: rgba(95, 143, 134, 0.22);
  border-radius: 8px;
  background:
      linear-gradient(135deg, rgba(255, 253, 248, 0.5), rgba(247, 241, 230, 0.36));
}

.cover-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--writer-muted);
  font-size: 13px;
}

.upload-icon {
  color: var(--writer-primary);
  font-size: 28px;
}

.preview-image {
  width: 100%;
  height: 138px;
  object-fit: cover;
}

.selected-line,
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.tags-container:empty {
  display: none;
}

.selected-tag,
.tag-item {
  border-color: rgba(95, 143, 134, 0.25);
  background: rgba(95, 143, 134, 0.08);
  color: var(--writer-primary-deep);
}

.add-btn {
  width: 100%;
  border-color: rgba(95, 143, 134, 0.2);
  background: rgba(255, 253, 248, 0.26);
  color: var(--writer-primary-deep);
}

.form-select {
  width: 100%;
}

.switch-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.switch-grid label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid rgba(126, 111, 88, 0.09);
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.24);
  color: var(--writer-muted);
  font-size: 13px;
  font-weight: 500;
}

.status-options {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(3, 1fr);
}

.status-options :deep(.el-radio-button__inner) {
  width: 100%;
  border-color: rgba(126, 111, 88, 0.16);
}

.panel-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 14px 14px 2px 20px;
  border-top: 1px solid rgba(126, 111, 88, 0.1);
  background: transparent;
}

.panel-actions .draft-btn,
.panel-actions .publish-btn {
  width: 100%;
}

:global(.custom-popover) {
  border-radius: 8px;
  box-shadow: 0 14px 34px rgba(77, 65, 48, 0.12);
}

.popover-title {
  margin-bottom: 12px;
  color: var(--writer-text);
  font-size: 15px;
  font-weight: 700;
}

.search-input {
  width: 100%;
  margin-bottom: 12px;
}

.popover-container {
  max-height: 230px;
  overflow-y: auto;
}

.category-item {
  display: block;
  width: 100%;
  margin: 0 0 4px;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--writer-text);
  cursor: pointer;
  text-align: left;
}

.category-item:hover {
  background: rgba(95, 143, 134, 0.08);
  color: var(--writer-primary-deep);
}

.tags-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selectable-tag {
  cursor: pointer;
}

.tag-item-select {
  border-color: rgba(186, 106, 88, 0.32);
  background: rgba(186, 106, 88, 0.1);
  color: var(--writer-accent);
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.el-form-item__label) {
  color: var(--writer-muted);
  font-weight: 600;
}

:deep(.el-switch.is-checked .el-switch__core) {
  border-color: var(--writer-primary);
  background-color: var(--writer-primary);
}

@media (max-width: 1280px) {
  .write-workspace {
    height: calc(100vh - 84px);
    min-height: 720px;
  }

  .writing-toolbar {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .toolbar-stats {
    grid-column: 1 / -1;
    order: 3;
  }

  .writing-board {
    height: auto;
    min-height: 0;
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 0;
    border: 0;
    background: transparent;
    box-shadow: none;
  }

  .writing-main {
    padding-right: 0;
  }

  .editor-shell {
    height: 860px;
  }

  .publish-panel {
    position: static;
    border: 1px solid var(--writer-line);
    border-radius: 8px;
    background: rgba(255, 253, 248, 0.76);
    box-shadow: 0 18px 45px rgba(77, 65, 48, 0.06);
  }

  .panel-header,
  .article-form,
  .panel-actions {
    padding-right: 18px;
    padding-left: 18px;
  }

  .panel-actions {
    padding-top: 14px;
    padding-bottom: 18px;
    background: rgba(255, 253, 248, 0.62);
  }
}

@media (max-width: 768px) {
  .write-workspace {
    height: calc(100vh - 84px);
    min-height: 680px;
    padding: 14px;
  }

  .writing-toolbar {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left,
  .toolbar-actions {
    justify-content: space-between;
  }

  .toolbar-stats {
    flex-wrap: wrap;
  }

  .title-card {
    padding: 14px;
  }

  .title-input :deep(.el-input__inner) {
    height: 38px;
    font-size: 22px;
    line-height: 38px;
  }

  .editor-shell {
    height: 760px;
  }

  .md-container :deep(.md-editor-content) {
    background: rgba(255, 253, 248, 0.96);
  }

  .md-container :deep(.md-editor-input-wrapper) {
    border-right: 0;
  }

  .md-container :deep(.md-editor-input-wrapper textarea) {
    padding: 24px 26px;
    font-size: 16px;
  }

  .switch-grid,
  .panel-actions {
    grid-template-columns: 1fr;
  }
}
</style>
