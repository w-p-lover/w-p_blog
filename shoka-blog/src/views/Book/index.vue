<template>
  <div class="book-container">
  <div class="book-showcase">
    <!-- 操作区 -->
    <div class="controls">
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon> 添加书籍
      </el-button>
      <el-select
          v-model="sortType"
          placeholder="排序方式"
          style="margin-left: 15px; width: 180px;"
      >
        <el-option label="添加时间（新→旧）" value="newest" />
        <el-option label="添加时间（旧→新）" value="oldest" />
        <el-option label="书名（A→Z）" value="nameAsc" />
      </el-select>
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
            <img
                :src="book.cover || defaultCover"
                :alt="book.title"
                class="cover-img"
            />
          </div>
          <!-- 书籍厚度（侧面） -->
          <div class="book-edge"></div>
        </div>

        <!-- 书籍信息标签（悬浮时显示） -->
        <div class="book-label" v-if="book.hover">
          <div class="label-title">{{ book.title }}</div>
          <div class="label-author">{{ book.author || '未知作者' }}</div>
          <div class="label-status" :class="book.status">
            {{ getStatusLabel(book.status) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 添加书籍弹窗 -->
    <el-dialog title="添加书籍" v-model="showAddDialog" width="400px">
      <el-form :model="newBook" label-width="80px">
        <el-form-item label="书名" required>
          <el-input v-model="newBook.title" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="newBook.author" />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="newBook.cover" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="newBook.status">
            <el-option label="想读" value="wish" />
            <el-option label="在读" value="reading" />
            <el-option label="已读" value="read" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="newBook.tags" placeholder="用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addBook">确认</el-button>
      </template>
    </el-dialog>

    <!-- 书籍详情弹窗 -->
    <el-dialog title="书籍详情" v-model="showDetailDialog" width="500px">
      <div v-if="currentBook" class="book-detail">
        <div class="detail-3d-preview">
          <!-- 详情页3D预览 -->
          <div class="book-3d preview">
            <div class="book-spine" :style="{ backgroundColor: getSpineColor(currentBook.tags) }">
              <div class="spine-text">{{ currentBook.title.substring(0, 1) }}</div>
            </div>
            <div class="book-cover">
              <img :src="currentBook.cover || defaultCover" class="cover-img" />
            </div>
            <div class="book-edge"></div>
          </div>
        </div>
        <div class="detail-info">
          <h2>{{ currentBook.title }}</h2>
          <p>作者：{{ currentBook.author || '未知' }}</p>
          <p>状态：{{ getStatusLabel(currentBook.status) }}</p>
          <p>标签：{{ currentBook.tags ? currentBook.tags.split(',').join('、') : '无' }}</p>
          <div class="status-buttons">
            <el-button
                @click="changeStatus(currentBook.id, 'wish')"
                :type="currentBook.status === 'wish' ? 'primary' : 'default'"
            >
              标记为想读
            </el-button>
            <el-button
                @click="changeStatus(currentBook.id, 'reading')"
                :type="currentBook.status === 'reading' ? 'primary' : 'default'"
            >
              标记为在读
            </el-button>
            <el-button
                @click="changeStatus(currentBook.id, 'read')"
                :type="currentBook.status === 'read' ? 'primary' : 'default'"
            >
              标记为已读
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { Plus } from '@element-plus/icons-vue';

// 基础配置
const defaultCover = '/images/defaultbook.png';
const showAddDialog = ref(false);
const showDetailDialog = ref(false);
const currentBook = ref(null);
const sortType = ref('newest'); // 默认按最新添加排序

// 书籍数据（本地存储）
const books = ref(JSON.parse(localStorage.getItem('3dBooks') || '[]').map(book => ({
  ...book,
  hover: false,
  addTime: book.addTime || Date.now() // 兼容旧数据
})));

// 新书籍表单
const newBook = ref({
  title: '',
  author: '',
  cover: '',
  status: 'wish',
  tags: '',
  addTime: Date.now()
});

// 按选择的方式排序书籍
const sortedBooks = computed(() => {
  const sorted = [...books.value];
  switch (sortType.value) {
    case 'newest':
      return sorted.sort((a, b) => b.addTime - a.addTime);
    case 'oldest':
      return sorted.sort((a, b) => a.addTime - b.addTime);
    case 'nameAsc':
      return sorted.sort((a, b) => a.title.localeCompare(b.title));
    default:
      return sorted;
  }
});

// 添加书籍
const addBook = () => {
  if (!newBook.value.title.trim()) return;
  const book = { ...newBook.value, id: Date.now(), hover: false };
  books.value.push(book);
  saveToLocal();
  showAddDialog.value = false;
  newBook.value = { title: '', author: '', cover: '', status: 'wish', tags: '', addTime: Date.now() };
};

// 保存到本地存储
const saveToLocal = () => {
  const data = books.value.map(({ hover, ...rest }) => rest);
  localStorage.setItem('3dBooks', JSON.stringify(data));
};

// 查看详情
const showBookDetail = (book) => {
  currentBook.value = { ...book };
  showDetailDialog.value = true;
};

// 切换状态
const changeStatus = (id, status) => {
  const book = books.value.find(b => b.id === id);
  if (book) book.status = status;
  saveToLocal();
};

// 获取状态文本
const getStatusLabel = (status) => {
  const map = { wish: '想读', reading: '在读', read: '已读' };
  return map[status] || '未知';
};

// 书脊颜色（根据标签动态生成）
const getSpineColor = (tags) => {
  if (!tags) return '#6b4226';
  const tagColors = {
    '科幻': '#2c3e50',
    '文学': '#8e44ad',
    '历史': '#c0392b',
    '技术': '#27ae60',
    '小说': '#f39c12'
  };
  const firstTag = tags.split(',')[0];
  return tagColors[firstTag] || `hsl(${Math.random() * 360}, 50%, 40%)`;
};
</script>

<style scoped>
.book-showcase {
  padding: 30px;
  max-width: 1200px;
  margin: 60px auto;
}

/* 操作区样式 */
.controls {
  margin-bottom: 60px;
  display: flex;
  align-items: center;
}

/* 3D书籍网格布局 */
.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 40px;
}

/* 3D书籍容器 */
.book-3d-container {
  height: 260px;
  cursor: pointer;
  position: relative;
  transform-style: preserve-3d;
}

/* 3D书籍核心样式 */
.book-3d {
  width: 140px;
  height: 200px;
  position: relative;
  transform-style: preserve-3d;
  transform: rotateY(20deg) rotateX(10deg); /* 初始3D角度 */
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  margin: 0 auto;
}

/* 悬停动画 */
.book-3d.hovered {
  transform: rotateY(0deg) rotateX(0deg) translateZ(20px) scale(1.05);
  filter: drop-shadow(0 15px 15px rgba(0, 0, 0, 0.2));
}

/* 书脊（侧面） */
.book-spine {
  position: absolute;
  width: 13px;
  height: 100%;
  left: -18px;
  bottom: 0px;
  transform: rotateY(100deg) translateZ(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  box-shadow: inset 0 0 15px rgba(0, 0, 0, 0.3);
}

.spine-text {
  writing-mode: vertical-rl; /* 文字竖排 */
  font-size: 12px;
  letter-spacing: 2px;
}

/* 封面 */
.book-cover {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden; /* 隐藏背面 */
  border-radius: 2px;
  overflow: hidden;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.book-3d.hovered .cover-img {
  transform: scale(1.05); /* 封面微放大，增强立体感 */
}

/* 书籍厚度（边缘） */
.book-edge {
  position: absolute;
  width: 100%;
  height: 20px;
  bottom: -10px;
  transform: rotateX(90deg) translateZ(0px); /* 旋转至底部 */
  background-color: #e0e0e0;
  background-image: linear-gradient(90deg, #ddd 0%, #fff 50%, #ddd 100%);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

/* 悬停信息标签 */
.book-label {
  position: absolute;
  bottom: -60px;
  left: 50%;
  transform: translateX(-50%);
  width: 160px;
  padding: 8px 12px;
  background-color: white;
  border-radius: 6px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  z-index: 10;
  text-align: center;
}

.label-title {
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.label-author {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.label-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

.label-status.wish {
  background-color: #e8f4fd;
  color: #3498db;
}

.label-status.reading {
  background-color: #eafaf1;
  color: #27ae60;
}

.label-status.read {
  background-color: #fef5e7;
  color: #f39c12;
}

/* 详情页样式 */
.book-detail {
  display: flex;
  gap: 30px;
  padding: 10px 0;
}

.detail-3d-preview {
  flex: 0 0 160px;
}

.book-3d.preview {
  width: 160px;
  height: 240px;
  transform: rotateY(0deg) rotateX(0deg);
}

.detail-info {
  flex: 1;
}

.detail-info h2 {
  margin: 0 0 20px 0;
  color: #333;
}

.detail-info p {
  margin: 10px 0;
  color: #666;
  line-height: 1.6;
}

.status-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>