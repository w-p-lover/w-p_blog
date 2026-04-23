<template>
  <div class="page-header">
    <h1 class="page-title">书架</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg"
         alt="">
    <!-- 波浪 -->
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="book-container">
      <div class="book-showcase">
        <!-- 操作区 -->
        <div class="controls card">
          <div class="controls-left">
            <el-button type="primary" @click="showAddDialog = true" class="add-btn">
              <el-icon>
                <Edit/>
              </el-icon>
              添加书籍
            </el-button>
          </div>

          <div class="controls-right">
            <div class="select-group">
              <!-- 排序选择器 -->
              <el-select v-model="sortType" placeholder="排序方式" size="default" @change="changeSort(sortType)">
                <el-option label="添加时间（新→旧）" value="newest"/>
                <el-option label="添加时间（旧→新）" value="oldest"/>
                <el-option label="书名（A→Z）" value="nameAsc"/>
              </el-select>

              <!-- 布局选择器 -->
              <el-select v-model="gridLayout" placeholder="布局" size="default" @change="updateGridLayout">
                <el-option label="紧凑" value="compact"/>
                <el-option label="标准" value="standard"/>
                <el-option label="宽松" value="spacious"/>
              </el-select>
            </div>
          </div>
        </div>

        <!-- 3D书籍网格 -->
        <div class="books-grid">
          <!-- 3D书籍组件 -->
          <TransitionGroup name="book-move" tag="div" class="books-grid" style="display: contents">
            <div
                class="book-3d-container"
                v-for="book in sortedBooks"
                :key="book.id"
                @click="showBookDetail(book)"
                @mouseenter="book.hover = true"
                @mouseleave="book.hover = false"
            >
              <el-tooltip
                  raw-content
                  effect="light"
                  placement="top"
                  :hide-after="0"
                  popper-class="custom-tooltip"
                  :enterable="false"
                  :show-after="200"
              >
                <template #content>
                  <img :src="book.briefImg || book.cover || defaultCover" alt="封面" style="max-width:250px"/>
                  <div class="book-label">
                    <div class="label-header">
                      <div class="label-title" :title="book.title">{{ book.title }}</div>
                      <div class="label-author">{{ book.author || '未知作者' }}</div>
                    </div>
                    <div class="label-bottom">
                      <div class="label-tags">{{ book.tags || '未分类' }}</div>
                      <div class="label-status" :class="book.status">
                        {{ getStatusLabel(book.status) }}
                      </div>
                    </div>
                  </div>
                </template>

                <!-- 3D书籍结构 -->
                <div class="book-3d" :class="{ 'hovered': book.hover }">
                  <div class="book-spine" :style="{ backgroundColor: getSpineColor(book.tags) }">
                    <div class="spine-text">{{ book.title }}</div>
                  </div>
                  <div class="book-cover">
                    <img :src="book.cover || defaultCover" :alt="book.title" class="cover-img"/>
                    <div class="cover-reflection"></div>
                  </div>
                  <div class="book-edge"></div>
                </div>
              </el-tooltip>
            </div>
          </TransitionGroup>
        </div>


        <!-- 添加书籍弹窗（保持你原有内容绑定） -->
        <el-dialog title="添加书籍" v-model="showAddDialog" width="450px" :close-on-click-modal="false">
          <el-form :model="newBook" label-width="80px" class="add-form">
            <el-form-item label="书名" required>
              <el-input v-model="newBook.title" placeholder="请输入书名"/>
            </el-form-item>
            <el-form-item label="作者">
              <el-input v-model="newBook.author" placeholder="作者姓名"/>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="newBook.status" placeholder="选择状态">
                <el-option label="想读" value="wish"/>
                <el-option label="在读" value="reading"/>
                <el-option label="已读" value="read"/>
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-input-tag
                  v-model="tagInput"
                  :max="3"
                  draggable
                  placeholder="请输入标签（回车确认）"
              >
                <template #tag="{ value }">
                  <el-tag
                      :type="getTagType(value)"
                      effect="light"
                      @close="removeTag(value)"
                      style="display: flex; align-items: center;"
                  >
                    <el-icon class="mr-1">
                      <Edit/>
                    </el-icon>
                    <span>{{ value }}</span>
                  </el-tag>
                </template>
              </el-input-tag>
            </el-form-item>
            <el-form-item label="封面URL">
              <el-input v-model="newBook.cover" placeholder="图片链接（可选）"/>
            </el-form-item>
            <el-form-item label="简介URL">
              <el-input v-model="newBook.briefImg" placeholder="图片链接（可选）"/>
            </el-form-item>
            <el-form-item label="简介">
              <el-input
                  type="textarea"
                  v-model="newBook.brief"
                  placeholder="请输入书籍简介"
                  :rows="3"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="showAddDialog = false">取消</el-button>
            <el-button type="primary" @click="addBook">确认</el-button>
          </template>
        </el-dialog>

        <!-- 书籍详情弹窗（保持绑定，宽度与布局美化） -->
        <el-dialog
            title="书籍详情"
            v-model="showDetailDialog"
            width="980px"
            :modal-append-to-body="true"
            :close-on-click-modal="false"
        >
          <div v-if="currentBook" class="book-detail">
            <div class="detail-left">
              <div class="book-3d preview">
                <div class="book-spine" :style="{ backgroundColor: getSpineColor(currentBook.tags) }">
                  <div class="spine-text">{{ currentBook.title.substring(0, 1) }}</div>
                </div>
                <div class="book-cover">
                  <img :src="currentBook.cover || defaultCover" class="cover-img" :alt="currentBook.title"/>
                  <div class="cover-reflection"></div>
                </div>
                <div class="book-edge"></div>
              </div>
            </div>

            <div class="detail-right">
              <h2 class="book-title">{{ currentBook.title }}</h2>
              <p class="book-author">作者：{{ currentBook.author || '未知' }}</p>

              <div class="tags-status">
                <div class="tags">
                  <el-tag v-if="currentBook.tags" size="small" effect="dark">{{ currentBook.tags }}</el-tag>
                  <el-tag v-else size="small" effect="light">未分类</el-tag>
                </div>

                <div class="status-block">
                  <span class="status-tag" :class="currentBook.status">{{ getStatusLabel(currentBook.status) }}</span>
                </div>
              </div>

              <div class="status-buttons">
                <el-button @click="changeStatus(currentBook.id, 'wish')"
                           :type="currentBook.status === 'wish' ? 'primary' : 'default'" size="small">想读
                </el-button>
                <el-button @click="changeStatus(currentBook.id, 'reading')"
                           :type="currentBook.status === 'reading' ? 'primary' : 'default'" size="small">在读
                </el-button>
                <el-button @click="changeStatus(currentBook.id, 'read')"
                           :type="currentBook.status === 'read' ? 'primary' : 'default'" size="small">已读
                </el-button>
              </div>

              <div class="book-intro">
                <h3>简介</h3>
                <p>{{ currentBook.brief || '暂无简介，这是我喜欢的一本书～' }}</p>
              </div>

              <div class="book-sources">
                <h3>书源链接</h3>

                <el-table
                    :data="currentBook.resource"
                    border
                    style="width: 100%"
                >
                  <!-- 名称 -->
                  <el-table-column prop="name" label="名称">
                    <template #default="{ row }">
                      <el-input v-model="row.name" placeholder="输入书源名称"/>
                    </template>
                  </el-table-column>

                  <!-- 链接 -->
                  <el-table-column prop="url" label="链接">
                    <template #default="{ row }">
                      <el-input v-model="row.url" placeholder="输入书源URL"/>
                    </template>
                  </el-table-column>

                  <!-- 类型 -->
                  <el-table-column prop="type" label="类型">
                    <template #default="{ row }">
                      <el-select v-model="row.type" placeholder="类型" size="small">
                        <el-option label="PDF" value="pdf"/>
                        <el-option label="笔记" value="note"/>
                        <el-option label="其他" value="other"/>
                      </el-select>
                    </template>
                  </el-table-column>
                  <!-- 操作 -->
                  <el-table-column label="操作" width="100" align="center">
                    <template #default="{ row }">
                      <el-button
                          type="success"
                          size="small"
                          @click="openLink(row.url)"
                      >
                        访问
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div v-if="!currentBook.resource || currentBook.resource.length === 0" class="no-sources">
                  <el-empty description="暂无书源，可添加电子书/笔记等链接"/>
                </div>
              </div>
            </div>
          </div>
        </el-dialog>
      </div>
    </div>
  </div>

</template>

<script setup lang="ts">
import {onMounted, reactive, ref, toRefs, watch} from 'vue';

import {BookVO} from "@/api/book/type";
import {
  addBook as apiAddBook,
  getBookList,
  updateBookStatus as apiChangeBookStatus,
} from "@/api/book";
import {Edit} from '@element-plus/icons-vue'
import type {TagProps} from 'element-plus/dist/index.full.mjs'
import Waves from "@/components/Waves/index.vue";
const showAddDialog = ref(false);
const showDetailDialog = ref(false);
const currentBook = ref<BookVO | null>(null);
const sortType = ref('newest'); // 默认按最新添加排序
const colorCache = new Map<string, string>();
const gridLayout = ref('spacious'); // 默认布局
const defaultCover = ref('')
const tagTypes: TagProps['type'][] = ['primary', 'success', 'info', 'warning', 'danger']
const type = ref('primary')
const data = reactive({
  count: 0,
  queryParams: {
    sortType: 'newest',
  },
  bookList: [] as BookVO[],
});

const newBook = ref({
  title: '',
  author: '',
  status: 'wish' as 'wish' | 'reading' | 'read',
  cover: '',
  tags: '',
  brief: '',
  briefImg: '',
});

const tagInput = ref<string[]>([]);
const {count, queryParams, bookList} = toRefs(data);

// 修改排序
const changeSort = (type: string) => {
  queryParams.value.sortType = type;
  fetchBookList();
};

// 更新网格布局
const updateGridLayout = () => {
  const grid = document.querySelector('.books-grid') as HTMLElement;
  if (!grid) return;

  switch (gridLayout.value) {
    case 'compact':
      grid.style.gridTemplateColumns = 'repeat(auto-fill, minmax(120px, 1fr))';
      grid.style.gap = '16px';
      break;
    case 'standard':
      grid.style.gridTemplateColumns = 'repeat(auto-fill, minmax(160px, 1fr))';
      grid.style.gap = '34px';
      break;
    case 'spacious':
      grid.style.gridTemplateColumns = 'repeat(auto-fill, minmax(200px, 1fr))';
      grid.style.gap = '48px';
      break;
  }
};
const addBook = async () => {
  if (!newBook.value.title.trim()) return;
  const tags = computed(() => tagInput.value.join(","));
  newBook.value.tags = tags.value;
  const payload = {
    ...newBook.value,
    resource: JSON.stringify([])
  };
  await apiAddBook(payload);
  showAddDialog.value = false;
  resetNewBook();
  fetchBookList();
};

// 重置新书表单
const resetNewBook = () => {
  newBook.value = {title: '', author: '', cover: '', status: 'wish', tags: '', brief: '', briefImg: ''};
};

// 查看书籍详情
const showBookDetail = (book: BookVO) => {
  currentBook.value = {
    ...book,
    resource: book.resource.map(item => ({...item})) // 再次深拷贝
  };
  console.log(currentBook.value);
  showDetailDialog.value = true;
};


// TIP 对于针对组件的刷新，可以使用v-if来满足需求
const changeStatus = async (id: number, status: string) => {
  await apiChangeBookStatus(id, status);
  if (currentBook.value) {
    currentBook.value.status = status as 'wish' | 'reading' | 'read';
  }
  await fetchBookList();
};

// 获取状态标签
const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {wish: '想读', reading: '在读', read: '已读'};
  return map[status] || '未知';
};

// 获取书脊颜色
const getSpineColor = (tags?: string) => {
  if (!tags) return '#6b4226';
  const tagColors: Record<string, string> = {
    '科幻': '#2c3e50',
    '文学': '#8e44ad',
    '历史': '#c0392b',
    '技术': '#27ae60',
    '小说': '#f39c12'
  };
  const firstTag = tags.split('，')[0];
  if (tagColors[firstTag]) return tagColors[firstTag];

  // 如果缓存里有颜色，直接返回
  if (colorCache.has(firstTag)) return colorCache.get(firstTag)!;

  // 随机生成并缓存
  const randomColor = `hsl(${Math.random() * 360}, 50%, 40%)`;
  colorCache.set(firstTag, randomColor);
  return randomColor;
};

// 排序后的书籍列表
const sortedBooks = ref<BookVO[]>([]);

const fetchBookList = async () => {
  const {data} = await getBookList(queryParams.value);
  bookList.value = data.data.recordList.map((book: any) => {
    let res: any[] = [];
    try {
      const parsed = JSON.parse(book.resource);
      res = Array.isArray(parsed) ? parsed : [parsed];
    } catch (e) {
      res = [];
    }
    return {
      ...book,
      resource: res.map(item => ({...item})) //
    };
  });
  count.value = data.data.count;
  sortedBooks.value = [...bookList.value];
  console.log(data);
};

const openLink = (url: string) => {
  if (!url) return;
  window.open(url, "_blank"); // 新窗口打开
};

const getTagType = (value: string): TagProps['type'] => {
  const str = String(value) // 保证是字符串
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  type.value = tagTypes[Math.abs(hash) % tagTypes.length]
  return tagTypes[Math.abs(hash) % tagTypes.length]
}

const removeTag = (value: string) => {
  tagInput.value = tagInput.value.filter(tag => tag !== value)
}
// 监听排序变化
watch(sortType, () => changeSort(sortType.value));

onMounted(() => {
  fetchBookList();
  updateGridLayout();
});
</script>

<style scoped>
@import "@/views/Book/css/book-info.scss";
@import "@/views/Book/css/base.scss";
@import "@/views/Book/css/book-3d.scss";

:root {
  --bg-gradient-1: #0290fb;
  --bg-gradient-2: #f3f8ff;
  --card-bg: #ffffff;
  --muted: #6b7280;
  --primary: #3b82f6;
  --glass: rgba(255, 255, 255, 0.6);
  --soft-shadow: 0 8px 30px rgba(20, 20, 30, 0.06);
}
</style>
