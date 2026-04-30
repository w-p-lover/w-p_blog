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
              @click="showBookDetail(book)"
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
              <el-form-item label="封面 URL">
                <el-input v-model="newBook.cover" placeholder="图片链接（可选）" />
              </el-form-item>
              <el-form-item label="简介图 URL">
                <el-input v-model="newBook.briefImg" placeholder="图片链接（可选）" />
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

      <el-dialog
        title="书籍详情"
        v-model="showDetailDialog"
        width="1040px"
        class="book-detail-dialog"
        :modal-append-to-body="true"
        :close-on-click-modal="false"
      >
        <div v-if="currentBook" class="book-detail">
          <aside class="detail-side">
            <div class="book-3d preview">
              <div class="book-spine" :style="{ backgroundColor: getSpineColor(currentBook.tags) }">
                <div class="spine-text">{{ currentBook.title }}</div>
              </div>
              <div class="book-cover">
                <img :src="currentBook.cover || defaultCover" class="cover-img" :alt="currentBook.title">
                <div class="cover-reflection"></div>
              </div>
              <div class="book-edge"></div>
            </div>
            <div class="detail-status">
              <button
                v-for="item in statusTabs.slice(1)"
                :key="item.value"
                type="button"
                :class="{ active: currentBook.status === item.value }"
                @click="changeStatus(currentBook.id, item.value)"
              >
                {{ item.label }}
              </button>
            </div>
            <dl class="detail-side__meta">
              <div>
                <dt>资源</dt>
                <dd>{{ currentBook.resource.length }}</dd>
              </div>
              <div>
                <dt>添加</dt>
                <dd>{{ formatDate(currentBook.addTime) }}</dd>
              </div>
            </dl>
          </aside>

          <section class="detail-main">
            <header class="detail-heading">
              <div class="detail-title-group">
                <h2>{{ currentBook.title }}</h2>
                <p>作者：{{ currentBook.author || '未知' }}</p>
                <div class="tag-row detail-tags">
                  <span v-for="tag in splitTags(currentBook.tags)" :key="tag">{{ tag }}</span>
                  <span v-if="!splitTags(currentBook.tags).length">未分类</span>
                </div>
              </div>
              <span class="status-pill" :class="currentBook.status">{{ getStatusLabel(currentBook.status) }}</span>
            </header>

            <section class="detail-block">
              <h3>简介</h3>
              <p>{{ currentBook.brief || '暂无简介' }}</p>
            </section>

            <section class="detail-block">
              <div class="resource-head">
                <h3>书源链接</h3>
                <el-button size="small" @click="addResourceRow">添加资源</el-button>
              </div>
              <div v-if="currentBook.resource.length" class="resource-list">
                <div v-for="(item, index) in currentBook.resource" :key="index" class="resource-row">
                  <el-input v-model="item.name" placeholder="名称" class="resource-name" />
                  <el-input v-model="item.url" placeholder="URL" class="resource-url" />
                  <div class="resource-row__tools">
                    <el-select v-model="item.type" placeholder="类型" class="resource-type">
                      <el-option label="PDF" value="pdf" />
                      <el-option label="笔记" value="note" />
                      <el-option label="其他" value="other" />
                    </el-select>
                    <el-button @click="openLink(item.url)">访问</el-button>
                    <el-button type="danger" plain @click="removeResourceRow(index)">删除</el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无书源，可添加电子书、笔记或相关链接" />
              <div class="resource-actions">
                <el-button type="primary" :loading="savingResources" @click="saveResources">保存书源</el-button>
              </div>
            </section>
          </section>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from "vue";
import { Edit } from "@element-plus/icons-vue";
import type { TagProps } from "element-plus/dist/index.full.mjs";
import { addBook as apiAddBook, getBookList, updateBookStatus as apiChangeBookStatus, updateResource } from "@/api/book";
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
const showDetailDialog = ref(false);
const savingBook = ref(false);
const savingResources = ref(false);
const currentBook = ref<BookItem | null>(null);
const keyword = ref("");
const statusFilter = ref<BookStatusFilter>("all");
const sortType = ref("newest");
const gridLayout = ref<"standard" | "compact">("standard");
const loadError = ref(false);
const defaultCover = defaultCoverUrl;
const colorCache = new Map<string, string>();
const tagTypes: TagProps["type"][] = ["primary", "success", "info", "warning", "danger"];

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
    } as any);
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

const showBookDetail = (book: BookItem) => {
  currentBook.value = {
    ...book,
    resource: book.resource.map((item) => ({ ...item })),
  };
  showDetailDialog.value = true;
};

const changeStatus = async (id: number, status: BookStatusFilter) => {
  if (status === "all") {
    return;
  }
  await apiChangeBookStatus(id, status);
  if (currentBook.value) {
    currentBook.value.status = status;
  }
  await fetchBookList();
};

const addResourceRow = () => {
  currentBook.value?.resource.push({ name: "", url: "", type: "other" });
};

const removeResourceRow = (index: number) => {
  currentBook.value?.resource.splice(index, 1);
};

const saveResources = async () => {
  if (!currentBook.value) {
    return;
  }
  savingResources.value = true;
  try {
    const resources = currentBook.value.resource.filter((item) => item.name || item.url);
    await updateResource(currentBook.value.id, resources);
    currentBook.value.resource = resources;
    await fetchBookList();
    window.$message?.success("书源已保存");
  } finally {
    savingResources.value = false;
  }
};

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
