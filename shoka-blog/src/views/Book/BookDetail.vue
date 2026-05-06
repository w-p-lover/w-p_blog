<template>
  <div class="bg book-detail-page">
    <main class="page-container book-detail-container">
      <section v-if="loading" class="book-detail-state">
        <div class="state-orbit"></div>
        <p>正在翻开这本书...</p>
      </section>

      <section v-else-if="loadError" class="book-detail-state">
        <h2>书籍加载失败</h2>
        <p>{{ loadError }}</p>
        <div class="detail-state-actions">
          <el-button @click="goBack">返回书架</el-button>
          <el-button type="primary" @click="fetchBook">重新加载</el-button>
        </div>
      </section>

      <section v-else-if="currentBook" class="book-detail-paper">
        <header class="detail-page-head">
          <button type="button" class="back-link" @click="goBack">返回书架</button>
          <div>
            <span>BOOK DETAIL</span>
            <h1>{{ currentBook.title }}</h1>
          </div>
        </header>

        <aside class="detail-cover-rail">
          <div class="detail-cover-frame">
            <img :src="currentBook.cover || defaultCover" :alt="currentBook.title">
          </div>
          <div class="detail-status-switch">
            <button
              v-for="item in statusOptions"
              :key="item.value"
              type="button"
              :class="{ active: currentBook.status === item.value }"
              @click="changeStatus(item.value)"
            >
              {{ item.label }}
            </button>
          </div>
          <dl class="detail-mini-stats">
            <div>
              <dt>资源</dt>
              <dd>{{ availableResourceCount }}/{{ currentBook.resource.length }}</dd>
            </div>
            <div>
              <dt>添加</dt>
              <dd>{{ formatDate(currentBook.addTime) }}</dd>
            </div>
          </dl>
        </aside>

        <section class="detail-content">
          <header class="detail-hero-card">
            <div class="detail-summary-grid">
              <section class="detail-intro-section">
                <div class="detail-section-title">
                  <span>Book Note</span>
                  <h2>书本介绍</h2>
                </div>
                <p class="detail-brief">{{ currentBook.brief || "暂无简介。等你给它补上一段读书缘起，这页就会更完整。" }}</p>
              </section>

              <aside class="detail-side-notes">
                <section class="detail-meta-panel">
                  <div class="detail-section-title detail-section-title--compact">
                    <span>Info</span>
                    <h3>基础信息</h3>
                  </div>
                  <dl class="detail-meta-list">
                    <div>
                      <dt>作者</dt>
                      <dd>{{ currentBook.author || "未知作者" }}</dd>
                    </div>
                    <div>
                      <dt>状态</dt>
                      <dd><span class="status-pill" :class="currentBook.status">{{ getStatusLabel(currentBook.status) }}</span></dd>
                    </div>
                    <div>
                      <dt>添加时间</dt>
                      <dd>{{ formatDate(currentBook.addTime) }}</dd>
                    </div>
                    <div>
                      <dt>标签</dt>
                      <dd class="tag-row detail-tags">
                        <span v-for="tag in splitTags(currentBook.tags)" :key="tag">{{ tag }}</span>
                        <span v-if="!splitTags(currentBook.tags).length">未分类</span>
                      </dd>
                    </div>
                  </dl>
                </section>

                <figure v-if="currentBook.briefImg" class="detail-image-note">
                  <img :src="currentBook.briefImg" :alt="`${currentBook.title} 详情图`">
                </figure>
              </aside>
            </div>
          </header>

          <section class="detail-block">
            <div class="detail-block__head">
              <div>
                <span>Resources</span>
                <h3>书源与资料</h3>
              </div>
              <el-button size="small" @click="addResourceRow">添加资源</el-button>
            </div>

            <div v-if="currentBook.resource.length" class="resource-list">
              <article
                v-for="(item, index) in currentBook.resource"
                :key="index"
                class="resource-card"
                :class="{ editing: editingResourceIndex === index }"
              >
                <template v-if="editingResourceIndex === index">
                  <div class="resource-row__fields">
                    <label>
                      <span>名称</span>
                      <el-input v-model="item.name" placeholder="资源名称" class="resource-name" />
                    </label>
                    <label class="resource-field--url">
                      <span>链接</span>
                      <el-input v-model="item.url" placeholder="https://..." class="resource-url" />
                    </label>
                    <label>
                      <span>类型</span>
                      <el-select v-model="item.type" placeholder="类型" class="resource-type">
                        <el-option label="PDF" value="pdf" />
                        <el-option label="笔记" value="note" />
                        <el-option label="网页" value="web" />
                        <el-option label="其他" value="other" />
                      </el-select>
                    </label>
                  </div>
                  <div class="resource-row__actions">
                    <el-button @click="editingResourceIndex = null">收起</el-button>
                    <el-button type="danger" plain @click="removeResourceRow(index)">删除</el-button>
                  </div>
                </template>

                <template v-else>
                  <span class="resource-type-pill">{{ getResourceTypeLabel(item.type) }}</span>
                  <div class="resource-card__body">
                    <strong>{{ item.name || "未命名资源" }}</strong>
                    <span>{{ getResourceHost(item.url) }}</span>
                  </div>
                  <div class="resource-card__actions">
                    <el-button size="small" @click="openLink(item.url)">访问</el-button>
                    <el-button size="small" plain @click="editingResourceIndex = index">编辑</el-button>
                  </div>
                </template>
              </article>
            </div>
            <div v-else class="resource-empty">
              <h4>还没有书源</h4>
              <p>可以添加电子书、在线阅读地址、笔记或相关资料链接。</p>
            </div>

            <div class="resource-save">
              <el-button type="primary" :loading="savingResources" @click="saveResources">保存书源</el-button>
            </div>
          </section>
        </section>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getBook, updateBookStatus as apiChangeBookStatus, updateResource } from "@/api/book";
import defaultCoverUrl from "@/assets/images/bg.jpg";
import {
  getAvailableResourceCount,
  getStatusLabel,
  normalizeBook,
  splitTags,
  type BookItem,
  type BookStatus,
} from "@/views/Book/bookModel";

const route = useRoute();
const router = useRouter();
const defaultCover = defaultCoverUrl;

const currentBook = ref<BookItem | null>(null);
const loading = ref(false);
const savingResources = ref(false);
const editingResourceIndex = ref<number | null>(null);
const loadError = ref("");

const statusOptions = [
  { label: "想读", value: "wish" as BookStatus },
  { label: "在读", value: "reading" as BookStatus },
  { label: "已读", value: "read" as BookStatus },
];

const bookId = computed(() => Number(route.params.id));
const availableResourceCount = computed(() => currentBook.value ? getAvailableResourceCount(currentBook.value.resource) : 0);

const fetchBook = async () => {
  if (!Number.isFinite(bookId.value)) {
    loadError.value = "书籍编号无效";
    return;
  }
  loading.value = true;
  loadError.value = "";
  try {
    const { data } = await getBook(bookId.value);
    if (data.flag) {
      currentBook.value = normalizeBook(data.data);
      editingResourceIndex.value = null;
      document.title = `${currentBook.value.title} | 书目`;
    } else {
      loadError.value = data.msg || "书籍不存在";
    }
  } catch {
    loadError.value = "网络异常，暂时无法加载书籍。";
  } finally {
    loading.value = false;
  }
};

const changeStatus = async (status: BookStatus) => {
  if (!currentBook.value || currentBook.value.status === status) {
    return;
  }
  await apiChangeBookStatus(currentBook.value.id, status);
  currentBook.value.status = status;
  window.$message?.success("阅读状态已更新");
};

const addResourceRow = () => {
  if (!currentBook.value) {
    return;
  }
  currentBook.value.resource.push({ name: "", url: "", type: "other" });
  editingResourceIndex.value = currentBook.value.resource.length - 1;
};

const removeResourceRow = (index: number) => {
  currentBook.value?.resource.splice(index, 1);
  editingResourceIndex.value = null;
};

const saveResources = async () => {
  if (!currentBook.value) {
    return;
  }
  savingResources.value = true;
  try {
    const resources = currentBook.value.resource.filter((item) => item.name.trim() || item.url.trim());
    await updateResource(currentBook.value.id, resources);
    currentBook.value.resource = resources;
    editingResourceIndex.value = null;
    window.$message?.success("书源已保存");
  } finally {
    savingResources.value = false;
  }
};

const openLink = (url: string) => {
  if (!url) {
    window.$message?.warning("请先填写链接");
    return;
  }
  window.open(url, "_blank", "noopener,noreferrer");
};

const formatDate = (value?: string) => value ? value.slice(0, 10) : "未记录";

const resourceTypeLabels: Record<string, string> = {
  pdf: "PDF",
  note: "笔记",
  web: "网页",
  other: "其他",
};

const getResourceTypeLabel = (type?: string) => resourceTypeLabels[type || "other"] || "其他";

const getResourceHost = (url?: string) => {
  if (!url) {
    return "未填写链接";
  }
  try {
    return new URL(url).host.replace(/^www\./, "");
  } catch {
    return url.replace(/^https?:\/\//, "").split("/")[0] || url;
  }
};

const goBack = () => {
  router.push("/book");
};

watch(() => route.params.id, fetchBook);
onMounted(fetchBook);
</script>

<style scoped lang="scss">
@import "@/views/Book/css/book-detail.scss";
</style>
