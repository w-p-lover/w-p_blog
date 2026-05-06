<template>
  <div class="page-header book-header">
    <div class="book-header__title">
      <h1>书架</h1>
      <p>一本一本收起来，读过的、正在读的、想读的都放在这里。</p>
    </div>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="">
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="book-container">
      <section class="book-studio">
        <header class="studio-hero">
          <div class="studio-copy">
            <p class="studio-kicker">BOOK STUDIO</p>
            <h2>我的轻量书房</h2>
            <p class="studio-desc">用桌面端更舒服的密度管理书目、状态、标签和资源链接。</p>
            <div class="studio-markers">
              <span>桌面管理</span>
              <span>状态归档</span>
              <span>资源收纳</span>
            </div>
          </div>
          <div class="studio-stats" aria-label="书架统计">
            <div
              v-for="item in statItems"
              :key="item.key"
              class="stat-card"
            >
              <span class="stat-card__label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.hint }}</small>
            </div>
          </div>
        </header>

        <section class="book-toolbar">
          <div class="toolbar-search">
            <el-input
              v-model="keyword"
              clearable
              placeholder="搜索书名、作者或标签"
              @clear="keyword = ''"
            />
          </div>
          <div class="toolbar-actions">
            <div class="status-tabs" aria-label="阅读状态筛选">
              <button
                v-for="item in statusTabs"
                :key="item.value"
                type="button"
                :class="{ active: statusFilter === item.value }"
                @click="statusFilter = item.value"
              >
                {{ item.label }}
              </button>
            </div>
            <el-select v-model="sortType" class="sort-select" @change="changeSort" placeholder="排序">
              <el-option label="添加时间 新到旧" value="newest" />
              <el-option label="添加时间 旧到新" value="oldest" />
              <el-option label="书名 A 到 Z" value="nameAsc" />
            </el-select>
            <div class="density-switch" aria-label="网格密度">
              <button type="button" :class="{ active: gridLayout === 'standard' }" @click="gridLayout = 'standard'">标准</button>
              <button type="button" :class="{ active: gridLayout === 'compact' }" @click="gridLayout = 'compact'">紧凑</button>
            </div>
            <el-button type="primary" class="add-btn" @click="openAddDialog">
              <el-icon><Edit /></el-icon>
              添加书籍
            </el-button>
          </div>
        </section>

        <section v-if="loadError" key="error" class="book-state">
          <h3>书架加载失败</h3>
          <p>后端暂时没有返回书目，稍后可以再试一次。</p>
          <el-button type="primary" @click="fetchBookList">重新加载</el-button>
        </section>

        <section v-else-if="!visibleBooks.length" :key="`empty-${resultViewKey}`" class="book-state">
          <h3>{{ bookList.length ? '没有匹配的书' : '书架还是空的' }}</h3>
          <p>{{ bookList.length ? '换个关键词或状态试试。' : '先添加一本书，书房就有第一束光了。' }}</p>
          <div class="state-actions">
            <el-button v-if="bookList.length" @click="clearFilters">清空筛选</el-button>
            <el-button type="primary" @click="openAddDialog">添加书籍</el-button>
          </div>
        </section>

        <section v-else :key="`grid-${resultViewKey}`" class="books-grid" :class="`books-grid--${gridLayout}`">
          <div class="books-grid__inner">
            <article
              v-for="book in visibleBooks"
              :key="book.id"
              class="book-card"
              @click.capture="goBookDetail(book.id)"
              @mouseenter="book.hover = true"
              @mouseleave="book.hover = false"
            >
              <el-tooltip
                raw-content
                effect="light"
                placement="top"
                :hide-after="0"
                popper-class="book-tooltip"
                :enterable="false"
                :show-after="160"
              >
                <template #content>
                  <div class="book-label">
                    <img :src="book.briefImg || book.cover || defaultCover" alt="">
                    <div>
                      <strong :title="book.title">{{ book.title }}</strong>
                      <span>{{ book.author || '未知作者' }}</span>
                      <p>{{ book.brief || '暂无简介' }}</p>
                    </div>
                  </div>
                </template>

                <div class="book-3d-wrap">
                  <div class="book-3d" :class="{ hovered: book.hover }">
                    <div class="book-spine" :style="{ backgroundColor: getSpineColor(book.tags) }">
                      <div class="spine-text">{{ book.title }}</div>
                    </div>
                    <div class="book-cover">
                      <img :src="book.cover || defaultCover" :alt="book.title" class="cover-img">
                      <div class="cover-reflection"></div>
                    </div>
                    <div class="book-edge"></div>
                  </div>
                </div>
              </el-tooltip>

              <div class="book-card__meta">
                <div class="book-card__title-row">
                  <h3 :title="book.title">{{ book.title }}</h3>
                  <span class="status-pill" :class="book.status">{{ getStatusLabel(book.status) }}</span>
                </div>
                <p>{{ book.author || '未知作者' }}</p>
                <div class="tag-row">
                  <span v-for="tag in splitTags(book.tags).slice(0, 3)" :key="tag">{{ tag }}</span>
                  <span v-if="!splitTags(book.tags).length">未分类</span>
                </div>
                <div class="book-card__foot">
                  <span>{{ formatDate(book.addTime) }}</span>
                  <span>{{ book.resource.length }} 个资源</span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </section>

      <el-dialog
        title="添加书籍"
        v-model="showAddDialog"
        width="740px"
        class="book-add-dialog"
        :close-on-click-modal="false"
      >
        <div class="add-book-layout">
          <aside class="add-book-preview">
            <div class="add-cover">
              <img :src="newBook.cover || defaultCover" :alt="newBook.title || '书籍封面预览'">
            </div>
            <div class="add-preview-meta">
              <strong :title="newBook.title">{{ newBook.title || '未命名书籍' }}</strong>
              <span>{{ newBook.author || '未知作者' }}</span>
              <em class="status-pill" :class="newBook.status">{{ getStatusLabel(newBook.status) }}</em>
            </div>
          </aside>

          <el-form :model="newBook" label-position="top" class="book-form book-form--add">
            <div class="add-form-grid">
              <el-form-item label="书名" required class="add-field--title">
                <el-input v-model="newBook.title" placeholder="请输入书名" />
              </el-form-item>
              <el-form-item label="作者">
                <el-input v-model="newBook.author" placeholder="作者姓名" />
              </el-form-item>
              <el-form-item label="状态" class="add-field--status">
                <div class="add-status-switch">
                  <button
                    v-for="item in statusTabs.slice(1)"
                    :key="item.value"
                    type="button"
                    :class="{ active: newBook.status === item.value }"
                    @click="newBook.status = item.value"
                  >
                    {{ item.label }}
                  </button>
                </div>
              </el-form-item>
              <el-form-item label="标签" class="add-field--tags">
                <div class="tag-editor">
                  <el-tag
                    v-for="tag in tagInput"
                    :key="tag"
                    closable
                    effect="light"
                    :type="getTagType(tag)"
                    @close="removeTag(tag)"
                  >
                    {{ tag }}
                  </el-tag>
                  <el-input
                    v-if="tagInput.length < 5"
                    v-model="tagDraft"
                    class="tag-editor__input"
                    placeholder="回车添加"
                    @keyup.enter="addTag"
                    @blur="addTag"
                  />
                </div>
              </el-form-item>
              <el-form-item label="封面" class="add-field--image">
                <el-upload
                  drag
                  :show-file-list="false"
                  accept="image/*"
                  :http-request="uploadCoverImage"
                  :before-upload="beforeImageUpload"
                  class="book-image-uploader"
                  :class="{ 'is-uploading': uploadingImageField === 'cover' }"
                  :disabled="Boolean(uploadingImageField)"
                >
                  <div class="book-image-upload-content">
                    <img v-if="newBook.cover" :src="newBook.cover" alt="封面预览">
                    <div v-if="uploadingImageField === 'cover'" class="book-image-uploading">
                      <span>上传中...</span>
                    </div>
                    <div v-else-if="!newBook.cover">
                      <el-icon><Edit /></el-icon>
                      <span>上传封面</span>
                    </div>
                  </div>
                </el-upload>
              </el-form-item>
              <el-form-item label="简介图" class="add-field--image">
                <el-upload
                  drag
                  :show-file-list="false"
                  accept="image/*"
                  :http-request="uploadBriefImage"
                  :before-upload="beforeImageUpload"
                  class="book-image-uploader"
                  :class="{ 'is-uploading': uploadingImageField === 'briefImg' }"
                  :disabled="Boolean(uploadingImageField)"
                >
                  <div class="book-image-upload-content">
                    <img v-if="newBook.briefImg" :src="newBook.briefImg" alt="简介图预览">
                    <div v-if="uploadingImageField === 'briefImg'" class="book-image-uploading">
                      <span>上传中...</span>
                    </div>
                    <div v-else-if="!newBook.briefImg">
                      <el-icon><Edit /></el-icon>
                      <span>上传简介图</span>
                    </div>
                  </div>
                </el-upload>
              </el-form-item>
              <el-form-item label="简介" class="add-field--brief">
                <el-input v-model="newBook.brief" type="textarea" placeholder="请输入书籍简介" :rows="4" />
              </el-form-item>
            </div>
          </el-form>
        </div>
        <template #footer>
          <div class="add-dialog-footer">
            <el-button @click="showAddDialog = false">取消</el-button>
            <el-button type="primary" :loading="savingBook" @click="addBook">确认添加</el-button>
          </div>
        </template>
      </el-dialog>

    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from "vue";
import { useRouter } from "vue-router";
import { Edit } from "@element-plus/icons-vue";
import type { UploadRawFile, UploadRequestOptions } from "element-plus";
import type { TagProps } from "element-plus/dist/index.full.mjs";
import { addBook as apiAddBook, getBookList, uploadBookImage } from "@/api/book";
import defaultCoverUrl from "@/assets/images/bg.jpg";
import Waves from "@/components/Waves/index.vue";
import {
  filterBooks,
  getBookStats,
  getStatusLabel,
  joinTags,
  normalizeBook,
  sortBooks,
  splitTags,
  type BookItem,
  type BookStatus,
  type BookStatusFilter,
} from "@/views/Book/bookModel";

const showAddDialog = ref(false);
const savingBook = ref(false);
const uploadingImageField = ref<"cover" | "briefImg" | null>(null);
const keyword = ref("");
const statusFilter = ref<BookStatusFilter>("all");
const sortType = ref("newest");
const gridLayout = ref<"standard" | "compact">("standard");
const loadError = ref(false);
const defaultCover = defaultCoverUrl;
const colorCache = new Map<string, string>();
const tagTypes: TagProps["type"][] = ["primary", "success", "info", "warning", "danger"];
const router = useRouter();

const data = reactive({
  count: 0,
  queryParams: {
    sortType: "newest",
  },
  bookList: [] as BookItem[],
});

const newBook = ref({
  title: "",
  author: "",
  status: "wish" as BookStatus,
  cover: "",
  tags: "",
  brief: "",
  briefImg: "",
});

const tagInput = ref<string[]>([]);
const tagDraft = ref("");
const { count, queryParams, bookList } = toRefs(data);

const visibleBooks = computed(() => sortBooks(filterBooks(bookList.value, keyword.value, statusFilter.value), sortType.value));
const stats = computed(() => getBookStats(bookList.value));
const resultViewKey = computed(() => `${keyword.value.trim()}-${statusFilter.value}-${sortType.value}-${gridLayout.value}`);

const statusTabs = [
  { label: "全部", value: "all" as BookStatusFilter },
  { label: "想读", value: "wish" as BookStatusFilter },
  { label: "在读", value: "reading" as BookStatusFilter },
  { label: "已读", value: "read" as BookStatusFilter },
];

const statItems = computed(() => [
  { key: "total", label: "总藏书", value: count.value || stats.value.total, hint: "collection" },
  { key: "reading", label: "在读", value: stats.value.reading, hint: "reading" },
  { key: "read", label: "已读", value: stats.value.read, hint: "finished" },
  { key: "resources", label: "资源", value: stats.value.resources, hint: "links" },
]);

const openAddDialog = () => {
  resetNewBook();
  showAddDialog.value = true;
};

const goBookDetail = (id: number) => {
  router.push(`/book/${id}`);
};

const changeSort = async () => {
  queryParams.value.sortType = sortType.value;
  await fetchBookList();
};

const clearFilters = () => {
  keyword.value = "";
  statusFilter.value = "all";
};

const addBook = async () => {
  if (!newBook.value.title.trim()) {
    window.$message?.warning("请输入书名");
    return;
  }
  savingBook.value = true;
  try {
    newBook.value.tags = joinTags(tagInput.value);
    await apiAddBook({
      ...newBook.value,
      resource: JSON.stringify([]),
    });
    showAddDialog.value = false;
    await fetchBookList();
    window.$message?.success("已添加书籍");
  } finally {
    savingBook.value = false;
  }
};

const resetNewBook = () => {
  newBook.value = { title: "", author: "", cover: "", status: "wish", tags: "", brief: "", briefImg: "" };
  tagInput.value = [];
  tagDraft.value = "";
};

const addTag = () => {
  const value = tagDraft.value.trim();
  if (!value || tagInput.value.includes(value)) {
    tagDraft.value = "";
    return;
  }
  tagInput.value = [...tagInput.value, value].slice(0, 5);
  tagDraft.value = "";
};

const removeTag = (value: string) => {
  tagInput.value = tagInput.value.filter((tag) => tag !== value);
};

const beforeImageUpload = (file: UploadRawFile) => {
  if (!file.type.startsWith("image/")) {
    window.$message?.warning("请选择图片文件");
    return false;
  }
  const isUnderLimit = file.size / 1024 / 1024 < 5;
  if (!isUnderLimit) {
    window.$message?.warning("图片大小不能超过 5MB");
  }
  return isUnderLimit;
};

const uploadImageToField = async (options: UploadRequestOptions, field: "cover" | "briefImg") => {
  const formData = new FormData();
  formData.append("file", options.file);
  uploadingImageField.value = field;
  try {
    const { data } = await uploadBookImage(formData);
    if (data.flag && data.data) {
      newBook.value[field] = data.data;
      options.onSuccess?.(data);
      window.$message?.success("图片已上传");
      return;
    }
    throw new Error(data.msg || "图片上传失败");
  } catch (error) {
    options.onError?.(error as Error);
    window.$message?.error("图片上传失败");
  } finally {
    uploadingImageField.value = null;
  }
};

const uploadCoverImage = (options: UploadRequestOptions) => uploadImageToField(options, "cover");

const uploadBriefImage = (options: UploadRequestOptions) => uploadImageToField(options, "briefImg");

const fetchBookList = async () => {
  loadError.value = false;
  try {
    const { data } = await getBookList(queryParams.value);
    bookList.value = (data.data.recordList || []).map(normalizeBook);
    count.value = data.data.count;
  } catch {
    loadError.value = true;
    bookList.value = [];
  }
};

const openLink = (url: string) => {
  if (!url) {
    window.$message?.warning("请先填写链接");
    return;
  }
  window.open(url, "_blank", "noopener,noreferrer");
};

const getTagType = (value: string): TagProps["type"] => {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = value.charCodeAt(i) + ((hash << 5) - hash);
  }
  return tagTypes[Math.abs(hash) % tagTypes.length];
};

const getSpineColor = (tags?: string) => {
  const firstTag = splitTags(tags)[0] || "default";
  const tagColors: Record<string, string> = {
    科幻: "#25415f",
    文学: "#7c4d79",
    历史: "#8d3f34",
    技术: "#2f7058",
    小说: "#9c6a2f",
    default: "#49586f",
  };
  if (tagColors[firstTag]) {
    return tagColors[firstTag];
  }
  if (!colorCache.has(firstTag)) {
    const hue = Math.abs(firstTag.split("").reduce((sum, char) => sum + char.charCodeAt(0), 0)) % 360;
    colorCache.set(firstTag, `hsl(${hue}, 42%, 36%)`);
  }
  return colorCache.get(firstTag)!;
};

const formatDate = (value?: string) => {
  if (!value) {
    return "未记录";
  }
  return value.slice(0, 10);
};

watch(sortType, () => {
  queryParams.value.sortType = sortType.value;
});

onMounted(fetchBookList);
</script>

<style scoped>
@import "@/views/Book/css/book-info.scss";
@import "@/views/Book/css/base.scss";
@import "@/views/Book/css/book-3d.scss";
</style>
