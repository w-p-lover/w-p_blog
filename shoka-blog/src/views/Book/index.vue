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
                <Plus/>
              </el-icon>
              添加书籍
            </el-button>
          </div>

          <div class="controls-right">
            <el-select v-model="sortType" placeholder="排序方式" class="sort-select" size="default">
              <el-option label="添加时间（新→旧）" value="newest"/>
              <el-option label="添加时间（旧→新）" value="oldest"/>
              <el-option label="书名（A→Z）" value="nameAsc"/>
            </el-select>
          </div>
        </div>

        <!-- 3D书籍网格 -->
        <div class="books-grid">
          <!-- 3D书籍组件 -->
          <div
              class="book-3d-container"
              v-for="book in sortedBooks"
              :key="book.id"
              @click="showBookDetail(book)"
              @mouseenter="book.hover = true"
              @mouseleave="book.hover = false"
          >
            <!-- 3D书籍结构：封面+书脊+厚度 -->
            <div class="book-3d" :class="{ 'hovered': book.hover }">
              <!-- 书脊 -->
              <div class="book-spine" :style="{ backgroundColor: getSpineColor(book.tags) }">
                <div class="spine-text">{{ book.title }}</div>
              </div>
              <!-- 封面 -->
              <div class="book-cover">
                <img :src="book.cover || defaultCover" :alt="book.title" class="cover-img"/>
                <div class="cover-reflection"></div>
              </div>
              <!-- 书籍厚度（侧面） -->
              <div class="book-edge"></div>
            </div>

            <!-- 书籍信息标签（悬浮时显示） -->
            <div class="book-label" v-if="book.hover">
              <div class="label-header">
                <div class="label-title" title="{{ book.title }}">{{ book.title }}</div>
                <div class="label-author">{{ book.author || '未知作者' }}</div>
              </div>
              <div class="label-bottom">
                <div class="label-tags">{{ book.tags || '未分类' }}</div>
                <div class="label-status" :class="book.status">{{ getStatusLabel(book.status) }}</div>
              </div>
            </div>

          </div>
        </div>

        <!-- 添加书籍弹窗（保持你原有内容绑定） -->
        <el-dialog title="添加书籍" v-model="showAddDialog" width="420px" :close-on-click-modal="false">
          <el-form :model="newBook" label-width="80px" class="add-form">
            <el-form-item label="书名" required>
              <el-input v-model="newBook.title" placeholder="请输入书名"/>
            </el-form-item>
            <el-form-item label="作者">
              <el-input v-model="newBook.author" placeholder="作者姓名"/>
            </el-form-item>
            <el-form-item label="封面URL">
              <el-input v-model="newBook.cover" placeholder="图片链接（可选）"/>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="newBook.status" placeholder="选择状态">
                <el-option label="想读" value="wish"/>
                <el-option label="在读" value="reading"/>
                <el-option label="已读" value="read"/>
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="newBook.tags" placeholder="用逗号分隔"/>
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
              <el-button type="primary" size="medium" @click="addResource" style="align-content:center">
                添加书源
              </el-button>
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
                  <el-table-column label="操作" width="140">
                    <template #default="{ row, $index }">
                      <el-button
                          type="danger"
                          size="small"
                          @click="removeResource($index, row.id)"
                      >
                        删除
                      </el-button>
                      <el-button
                          type="success"
                          size="small"
                          @click="saveSource()"
                      >
                        保存
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
import {Plus} from '@element-plus/icons-vue';
import {BookVO} from "@/api/book/type";
import {addBook as apiAddBook, getBookList, updateBookStatus as apiChangeBookStatus,updateResource,deleteResource} from "@/api/book";
import { ElMessage } from 'element-plus';

const showAddDialog = ref(false);
const showDetailDialog = ref(false);
const currentBook = ref<BookVO | null>(null);
const sortType = ref('newest'); // 默认按最新添加排序
const colorCache = new Map<string, string>();

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
  brief: ''
});


const {count, queryParams, bookList} = toRefs(data);

// 修改排序
const changeSort = (type: string) => {
  queryParams.value.sortType = type;
  fetchBookList();
};

const addBook = async () => {
  if (!newBook.value.title.trim()) return;
  const payload = {
    ...newBook.value,
    resource: JSON.stringify([])
  };
  await apiAddBook(payload);
  showAddDialog.value = false;
  resetNewBook();
  fetchBookList();
};

// 新增书源
const addResource = () => {
  if (!currentBook.value) return;
  currentBook.value.resource.push({
    name: '',
    url: '',
    type: 'pdf',
  });
};

// 重置新书表单
const resetNewBook = () => {
  newBook.value = {title: '', author: '', cover: '', status: 'wish', tags: '',  brief: ''};
};

// 查看书籍详情
const showBookDetail = (book: BookVO) => {
  currentBook.value = {
    ...book,
    resource: book.resource.map(item => ({ ...item })) // 再次深拷贝
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
  const firstTag = tags.split(',')[0];
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
      resource: res.map(item => ({ ...item })) //
    };
  });
  count.value = data.data.count;
  sortedBooks.value = [...bookList.value];
  console.log(data);
};


// 删除书源
const removeResource = async (index: number, sourceId: number) => {
  try {
    if (!currentBook.value) return
    await deleteResource(currentBook.value.id,index); // 调用后端删除接口
    currentBook.value.resource.splice(index, 1); // 前端移除
    ElMessage.success('删除成功');
  } catch (err) {
    ElMessage.error('删除失败');
    console.error(err);
  }
};

// 保存书源
const saveSource = async () => {
  try {
    if (!currentBook.value) return
    await updateResource(currentBook.value.id, currentBook.value.resource);// 调用后端保存接口
    console.log('保存书源', currentBook.value.resource);
    ElMessage.success('保存成功');
  } catch (err) {
    ElMessage.error('保存失败');
    console.error(err);
  }
};

// 监听排序变化
watch(sortType, () => changeSort(sortType.value));

onMounted(() => {
  fetchBookList();
});
</script>

<style scoped>
:root {
  --bg-gradient-1: #f6fbff;
  --bg-gradient-2: #f3f8ff;
  --card-bg: #ffffff;
  --muted: #6b7280;
  --primary: #3b82f6; /* 主要色 */
  --glass: rgba(255, 255, 255, 0.6);
  --soft-shadow: 0 8px 30px rgba(20, 20, 30, 0.06);
}

/* 容器 */
.book-container {
  position: relative;
  width: calc(70% - 0.625rem);
  margin: 3.5rem auto;
  padding: 1.75rem 2.25rem;
  border-radius: 0.75rem;
  box-shadow: 0 0 1rem var(--box-bg-shadow);
  animation: slideUpIn 1s;
  background: linear-gradient(180deg, var(--bg-gradient-1), var(--bg-gradient-2));
  min-height: 60vh;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
  color: #222;
}

.title-group .title {
  margin: 0;
  font-size: 22px;
  letter-spacing: -0.3px;
  color: #172554;
}

.title-group .subtitle {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

/* 本书数量标签 */
.count-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.12), rgba(72, 187, 120, 0.06));
  padding: 6px 10px;
  border-radius: 20px;
  box-shadow: var(--soft-shadow);
}

.count-chip .count {
  font-weight: 700;
  color: var(--primary);
}

.count-chip .count-label {
  color: var(--muted);
  font-size: 13px;
}

/* 主卡片 */
.book-showcase {
  max-width: 1200px;
  margin: 10px auto 60px;
}

/* 操作区卡片 */
.controls.card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 12px;
  background: var(--card-bg);
  box-shadow: var(--soft-shadow);
  margin-bottom: 28px;
}

/* 按钮 */
.add-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.add-btn .el-icon {
  font-size: 16px;
}

/* 排序选择器 */
.sort-select {
  min-width: 180px;
}

/* 书籍网格 */
.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 34px;
  align-items: start;
}

/* 单个书籍容器 */
.book-3d-container {
  height: 300px;
  cursor: pointer;
  position: relative;
  transform-style: preserve-3d;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

/* 书籍3D主体 */
.book-3d {
  width: 160px;
  height: 230px;
  position: relative;
  transform-style: preserve-3d;
  transform: rotateY(22deg) rotateX(6deg) translateZ(0);
  transition: transform 380ms cubic-bezier(.2, .9, .3, 1), box-shadow 280ms ease, filter 220ms ease;
  margin-top: 6px;
  will-change: transform;
}

/* 悬停 */
.book-3d.hovered {
  transform: rotateY(0deg) rotateX(0deg) translateZ(28px) scale(1.06);
  filter: drop-shadow(0 22px 30px rgba(20, 24, 40, 0.14));
}

/* 书脊 */
.book-spine {
  position: absolute;
  width: 14px;
  height: 100%;
  left: -20px;
  bottom: 0;
  transform: rotateY(102deg) translateZ(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  box-shadow: inset 0 0 18px rgba(0, 0, 0, 0.28);
  border-radius: 2px;
}

.spine-text {
  writing-mode: vertical-rl;
  font-size: 12px;
  letter-spacing: 2px;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.18);
}

/* 封面 */
.book-cover {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 5px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(17, 24, 39, 0.06);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.02), rgba(0, 0, 0, 0.02));
  border: 1px solid rgba(10, 20, 40, 0.03);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 420ms ease;
  display: block;
}

.book-3d.hovered .cover-img {
  transform: scale(1.06);
}

/* 封面反光 */
.cover-reflection {
  position: absolute;
  left: -10%;
  top: -20%;
  width: 120%;
  height: 70%;
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.03) 40%, rgba(255, 255, 255, 0));
  transform: rotate(-12deg);
  pointer-events: none;
  mix-blend-mode: overlay;
  opacity: 0.65;
}

/* 书籍厚度（侧面） */
.book-edge {
  position: absolute;
  width: 95%;
  height: 48px;
  bottom: -25px;
  transform: rotateX(92deg) translateZ(0px);
  background-image: linear-gradient(90deg, #eee 0%, #fff 50%, #e9e9e9 100%);
  border-radius: 4px;
  box-shadow: 0 4px 10px rgba(10, 20, 30, 0.06);
}

/* 悬停信息卡 */
.book-label {
  position: absolute;
  bottom: -37px;
  left: 50%;
  transform: translateX(-50%);
  width: 220px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 10px;
  box-shadow: 0 12px 30px rgba(16, 24, 40, 0.12);
  z-index: 12;
  text-align: left;
  transition: transform 220ms ease, opacity 220ms ease;
}

/* 新增的容器样式 */
.label-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.label-title {
  font-weight: 700;
  font-size: 14px;
  color: #172554;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1; /* 让标题占据剩余空间 */
  margin-right: 8px; /* 与作者之间的间距 */
}

.label-author {
  font-size: 12px;
  color: var(--muted);
  /* 移除原有的margin-top */
  white-space: nowrap; /* 防止作者名换行 */
}

.label-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.label-tags {
  font-size: 12px;
  color: #445;
  background: #f3f6ff;
  padding: 4px 8px;
  border-radius: 8px;
}

.label-status {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  color: #fff;
}

.label-status.wish {
  background-color: #4299e1;
}

.label-status.reading {
  background-color: #48bb78;
}

.label-status.read {
  background-color: #ed8936;
}

/* 详情页 */
.book-detail {
  display: flex;
  gap: 28px;
  padding: 12px 6px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.detail-left {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 35px;
}

.book-3d.preview {
  width: 200px;
  height: 300px;
  transform: rotateY(0) rotateX(0);
  transition: transform 220ms ease;
}

.detail-right {
  flex: 1;
  min-width: 360px;
}

.book-title {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #0f172a;
}

.book-author {
  margin: 0 0 12px 0;
  color: var(--muted);
  font-size: 14px;
}

/* 标签与状态区 */
.tags-status {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-tag {
  padding: 6px 12px;
  border-radius: 14px;
  color: #fff;
  font-size: 13px;
}

.status-tag.wish {
  background-color: #4299e1;
}

.status-tag.reading {
  background-color: #48bb78;
}

.status-tag.read {
  background-color: #ed8936;
}

/* 按钮组 */
.status-buttons {
  display: flex;
  gap: 10px;
  margin: 14px 0 18px;
}

/* 简介与书源 */
.book-intro, .book-sources {
  margin-top: 10px;
  padding: 12px;
  background: linear-gradient(180deg, #fff, #fbfdff);
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(37, 51, 73, 0.04);
}

.book-intro h3, .book-sources h3 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #17325a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.book-intro p {
  margin: 0;
  color: #394050;
  line-height: 1.6;
  font-size: 14px;
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 6px rgba(16, 24, 40, 0.04);
}

.source-icon {
  font-size: 16px;
  color: var(--muted);
}

.source-name {
  flex: 1;
  color: #172554;
}

/* 无书源 */
.no-sources {
  padding: 18px 0;
}

/* 响应式 */
@media (max-width: 980px) {
  .books-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 24px;
  }

  .book-3d {
    width: 140px;
    height: 200px;
  }

  .book-3d.preview {
    width: 160px;
    height: 230px;
  }

  .book-detail {
    flex-direction: column;
    gap: 18px;
  }
}
</style>
