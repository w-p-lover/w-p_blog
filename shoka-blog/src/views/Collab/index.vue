<template>
  <div class="page-header">
    <h1 class="page-title">所有标签</h1> <!-- 总标题 -->
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="标签页面背景">
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="page-container">
      <div class="collab-browsing-page">
        <!-- 顶部导航，聚焦协作内容 -->
        <header class="main-header">
          <div class="logo">知识空间</div>
          <nav class="main-nav">
            <router-link to="/" class="nav-item">我的博客</router-link>
            <router-link to="/collab" class="nav-item"
                         :class="{ active: currentActive === 'collab' }"
                         @click="handleRouterLinkActive('collab')">
              协作空间
            </router-link>

            <button @click="handleMyEdit" class="nav-item" :class="{ active: currentActive === 'my-edit' }">
              我的编辑
            </button>
            <button @click="handleFavorites" class="nav-item" :class="{ active: currentActive === 'favorites' }">
              收藏
            </button>
          </nav>
          <!-- 搜索：只搜索协作内容（不关联博客） -->
          <div class="search-bar">
            <input type="text" placeholder="搜索协作文档..." v-model="searchKeyword">
            <button @click="handleSearch"><i class="fa fa-search"></i></button>
          </div>
        </header>

        <!-- 筛选区域：只针对协作内容的筛选维度 -->
        <div class="filter-bar">
          <div class="filter-group">
            <span class="filter-label">排序方式:</span>
            <button class="filter-btn" :class="{ active: sortBy === 'latest' }" @click="sortBy = 'latest'">
              最新更新
            </button>
            <button class="filter-btn" :class="{ active: sortBy === 'popular' }" @click="sortBy = 'popular'">
              热门浏览
            </button>
            <button class="filter-btn" :class="{ active: sortBy === 'editCount' }" @click="sortBy = 'editCount'">
              编辑最多
            </button>
          </div>

          <div class="filter-group">
            <span class="filter-label">文档状态:</span>
            <div class="filter-btn-group">
              <button
                  class="filter-btn"
                  :class="{ active: docStatus === 'all' }"
                  @click="docStatus = 'all'"
              >
                全部状态
              </button>
              <button
                  class="filter-btn status-editing-btn"
                  :class="{ active: docStatus === 'editing' }"
                  @click="docStatus = 'editing'"
              >
                <span class="status-dot editing-dot"></span> 正在编辑
              </button>
              <button
                  class="filter-btn status-finished-btn"
                  :class="{ active: docStatus === 'finished' }"
                  @click="docStatus = 'finished'"
              >
                <span class="status-dot finished-dot"></span> 已完成
              </button>
            </div>
          </div>
          <div class="filter-group">
            <span class="filter-label">标签筛选:</span>
            <div class="tag-filter">
              <button class="tag-btn" :class="{ active: selectedTag === 'all' }" @click="selectedTag = 'all'">
                全部
              </button>
              <button class="tag-btn" :class="{ active: selectedTag === tag }" @click="selectedTag = tag"
                      v-for="tag in allTags" :key="tag">{{ tag }}
              </button>
            </div>
          </div>
        </div>
        <router-view></router-view>

        <!-- 主内容区 - 增强卡片质感 -->
        <main class="content-container">
          <div class="collab-header">
            <h1 class="collab-title">
              协作文档
              <span class="count-badge" :class="{ 'badge-pulse': filteredCollabs.length > 0 }">
                {{ filteredCollabs.length }} 篇
              </span>
            </h1>
            <router-link
                to="/collab/create"
                class="create-btn"
                @mouseenter="createBtnHover = true"
                @mouseleave="createBtnHover = false"
            >
              <i class="fa fa-plus" :class="{ 'plus-rotate': createBtnHover }"></i>
              新建文档
            </router-link>
          </div>

          <!-- 文档列表 - 优化卡片交互 -->
          <div class="content-grid">
            <article
                class="content-card collab-card"
                v-for="doc in filteredCollabs"
                :key="'collab-' + doc.id"
                @mouseenter="hoverDocId = doc.id"
                @mouseleave="hoverDocId = 0"
                :class="{ 'card-hover': hoverDocId === doc.id }"
            >
              <!-- 收藏按钮 - 增强动效 -->
              <button
                  class="favorite-btn"
                  @click="toggleFavorite(doc.id)"
                  :title="isFavorite(doc.id) ? '取消收藏' : '收藏'"
                  :data-doc-id="doc.id"
              >
                <i
                    class="fa"
                    :class="[
                    isFavorite(doc.id) ? 'fa-star favorite-active' : 'fa-star-o',
                    hoverDocId === doc.id ? 'star-hover' : ''
                  ]"
                >
                </i>
              </button>

              <!-- 文档状态 - 优化样式 -->
              <div
                  class="doc-status"
                  :class="doc.isEditing ? 'status-editing' : 'status-finished'"
              >
                {{ doc.isEditing ? '正在编辑' : '已完成' }}
              </div>

              <div class="card-header">
                <router-link
                    :to="`/collab/${doc.id}`"
                    class="card-title"
                    :class="{ 'title-hover': hoverDocId === doc.id }"
                >
                  {{ doc.title }}
                </router-link>
                <div class="card-meta">
                  <span class="lead-author">
                    <i class="fa fa-user-circle author-icon"></i> {{ doc.leadAuthor }}
                  </span>
                  <span class="date">
                    <i class="fa fa-clock-o date-icon"></i> {{ formatDate(doc.lastUpdateDate) }}
                  </span>
                </div>
              </div>

              <div class="card-content">
                <p class="excerpt" :class="{ 'excerpt-hover': hoverDocId === doc.id }">
                  {{ doc.excerpt }}
                </p>
                <div class="tag-list">
                  <span
                      class="tag"
                      v-for="tag in doc.tags"
                      :key="tag"
                      @mouseenter="showTagTooltip(tag)"
                      @mouseleave="hideTagTooltip()"
                  >
                    {{ tag }}
                  </span>
                </div>

                <!-- 协作信息 - 优化布局 -->
                <div class="collab-info">
                  <div class="collaborators">
                    <span class="collab-label">协作者:</span>
                    <div class="avatar-group">
                      <template v-for="(collaborator, index) in doc.collaborators" :key="`collab-${doc.id}-${index}`">
                        <img
                            :src="collaborator?.avatar || 'src/assets/img/head_portrait2.jpg'"
                            :alt="collaborator.name"
                            class="avatar"
                            v-if="index < 3"
                            :title="collaborator.name"
                        >
                      </template>
                      <span
                          class="more-avatars"
                          v-if="doc.collaborators.length > 3"
                          :title="`还有${doc.collaborators.length - 3}位协作者`"
                      >
                        +{{ doc.collaborators.length - 3 }}
                      </span>
                    </div>
                  </div>
                  <div class="version-info" :title="`当前版本：v${doc.version}，共编辑${doc.editCount}次`">
                    <i class="fa fa-history version-icon"></i>
                    v{{ doc.version }} ({{ doc.editCount }}次编辑)
                  </div>
                </div>
              </div>

              <div class="card-footer">
                <div class="stats">
                  <span class="stat-item" :title="`浏览次数：${doc.views}次`">
                    <i class="fa fa-eye view-icon"></i> {{ doc.views }} 浏览
                  </span>
                  <span class="stat-item" :title="`评论数：${doc.comments}条`">
                    <i class="fa fa-comment comment-icon"></i> {{ doc.comments }} 评论
                  </span>
                </div>
                <div class="action-btns">
                  <router-link
                      :to="`/collab/${doc.id}`"
                      class="read-btn"
                      :class="{ 'btn-hover': hoverDocId === doc.id }"
                  >
                    查看
                  </router-link>
                  <router-link
                      :to="`/collab/edit/${doc.id}`"
                      class="edit-btn"
                      v-if="isCollaborator(doc)"
                      :class="{ 'btn-hover': hoverDocId === doc.id }"
                  >
                    编辑
                  </router-link>
                </div>
              </div>
            </article>
          </div>

          <!-- 空状态 - 优化视觉体验 -->
          <div class="no-content" v-if="filteredCollabs.length === 0">
            <div class="empty-container">
              <div class="empty-icon">
                <i class="fa fa-file-text-o"></i>
              </div>
              <h3 class="empty-title">暂无符合条件的协作文档</h3>
              <p class="empty-desc">创建协作文档，与团队成员实时共享和编辑内容</p>
              <router-link to="/collab/create" class="empty-btn">
                <i class="fa fa-plus-circle"></i> 立即创建第一篇文档
              </router-link>
            </div>
          </div>
        </main>
        <!-- 分页 -->
        <Pagination v-if="count > 0"
                    v-model:current="queryParams.current"
                    :total="Math.ceil(count / queryParams.size)">
        </Pagination>
        <!-- 页脚 -->
        <footer class="main-footer">
          <div class="footer-content">
            <p>知识空间 &copy; 2024 - 个人博客与团队协作平台</p>
            <p class="link-group">
              <a href="/">返回我的博客</a> |
              <a href="/collab/help">协作指南</a>
            </p>
          </div>
        </footer>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, computed, reactive, toRefs, watch} from 'vue';
import 'font-awesome/css/font-awesome.min.css';
import {listDocs, getDocCount, getUserFavorites, cancelFavorite, addFavorite} from "@/api/collab";
import {Doc,DocCard} from "@/api/collab/type";
import {PageQuery} from "@/model";
import useStore from '@/store';
import Pagination from "@/components/Pagination/index.vue";
import {ElLoading, ElMessage} from 'element-plus'; // 引入提示组件

const {user} = useStore();
const data = reactive({
  count: 0,
  queryParams: {
    current: 1,
    size: 20,
    tag: '',
    status: '',
    keyword: '',
    sortType: ''
  } as PageQuery,
  allDocs: [] as DocCard[],
  collabDocuments: [] as DocCard[],

  favoriteDocIds: new Set<number>()
});
const {count, queryParams, collabDocuments, favoriteDocIds,allDocs} = toRefs(data);

// 其他原有状态
const searchKeyword = ref("");
const sortBy = ref("latest");
const docStatus = ref("all");
const selectedTag = ref("all");
const currentActive = ref("");
const route = useRoute();
const currentUser = computed(() => user.nickname || '默认用户');

// 4. 取后端数据并做字段映射（保留原逻辑）
const fetchDocs = async () => {
  try {
    favoriteDocIds.value.clear();
    const {data} = await listDocs(queryParams.value);
    if (data.code === 200) {
      allDocs.value = (data.data || []).map((d: Doc) => ({
        id: d.id,
        title: d.title,
        leadAuthor: d.leadAuthor,
        lastUpdateDate: d.lastUpdateDate,
        excerpt: d.description || "",               // 后端字段是 desc，这里转成 excerpt
        tags: d.tags || [],
        collaborators: d.collaborators || [],
        views: d.views || 0,
        editCount: d.editCount || 0,
        version: d.version || 1,
        isEditing: d.isEditing || false,
        comments: d.comments || 0,
      }));
      collabDocuments.value = allDocs.value;
      const response = await getDocCount();
      count.value = response.data.data;
      const favResponse = await getUserFavorites(user.id ?? 0);
      favResponse.data.data.forEach((id: number) => favoriteDocIds.value.add(id));
      console.log("获取文档数量成功", favoriteDocIds.value);
    }
  } catch (e) {
    console.error("获取协作文档失败", e);
  }
};

// 5. 收藏相关核心方法
const isFavorite = (docId: number) => {
  return favoriteDocIds.value.has(docId);
};

const toggleFavorite = async (docId: number) => {
  // 校验用户登录状态
  if (!user.id) {
    ElMessage.warning('请先登录再进行收藏操作');
    return;
  }

  // 校验文档ID有效性
  if (docId <= 0) {
    ElMessage.error('无效的文档ID');
    return;
  }

  // 防止重复点击（添加加载状态）
  const loadingKey = `fav_${docId}`;
  const loading = ElLoading.service({
    target: `.favorite-btn[data-doc-id="${docId}"]`, // 只在当前按钮上显示加载
    text: isFavorite(docId) ? '取消收藏中...' : '收藏中...',
    background: 'rgba(255, 255, 255, 0.7)'
  });

  try {
    if (isFavorite(docId)) {
      // 取消收藏：调用后端接口
      const { data } = await cancelFavorite(user.id, docId);
      if (data.code === 200) {
        favoriteDocIds.value.delete(docId);
        ElMessage.success('已取消收藏');
      } else {
        ElMessage.error('取消收藏失败：' + (data.msg || '操作异常'));
      }
    } else {
      const { data } = await addFavorite(user.id, docId);
      if (data.code === 200) {
        favoriteDocIds.value.add(docId);
        ElMessage.success('收藏成功');
      } else {
        ElMessage.error('收藏失败：' + (data.msg || '操作异常'));
      }
    }
  } catch (error) {
    console.error('收藏接口调用失败：', error);
    ElMessage.error('网络异常，请稍后重试');
  } finally {
    loading.close();
  }
};

const allTags = computed(() => {
  const tagList = collabDocuments.value.flatMap(doc => doc.tags);
  return [...new Set(tagList)];
});

const filteredCollabs = computed(() => {
  return collabDocuments.value
      .filter(doc => {
        if (searchKeyword.value) {
          const kw = searchKeyword.value.trim().toLowerCase();
          const matchTitle = doc.title.toLowerCase().includes(kw);
          const matchExcerpt = doc.excerpt.toLowerCase().includes(kw);
          const matchTag = doc.tags.some(tag => tag.toLowerCase().includes(kw));
          if (!matchTitle && !matchExcerpt && !matchTag) return false;
        }
        if (docStatus.value === "editing" && !doc.isEditing) return false;
        if (docStatus.value === "finished" && doc.isEditing) return false;
        return !(selectedTag.value !== "all" && !doc.tags.includes(selectedTag.value));
      })
      .sort((a, b) => {
        if (sortBy.value === "latest") {
          return new Date(b.lastUpdateDate).getTime() - new Date(a.lastUpdateDate).getTime();
        } else if (sortBy.value === "popular") {
          return b.views - a.views;
        } else {
          return b.editCount - a.editCount;
        }
      });
});

const isCollaborator = (doc: { collaborators: { name: string }[] }) => {
  return doc.collaborators.some(collab => collab.name === currentUser.value);
};

const handleSearch = () => {
  console.log("搜索协作关键词：", searchKeyword.value);
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString("zh-CN", {year: "numeric", month: "short", day: "numeric"});
};

const handleMyEdit = () => {
  currentActive.value = 'my-edit';
  collabDocuments.value = allDocs.value.filter(doc =>
      doc.collaborators.some(collab => collab.name === currentUser.value)
  );
};

const handleFavorites = () => {
  currentActive.value = 'favorites';
  // 筛选出已收藏的文档
  collabDocuments.value = allDocs.value.filter(doc => isFavorite(doc.id));
};

const handleRouterLinkActive = (key: string) => {
  currentActive.value = key;
  fetchDocs();
};

watch(() => queryParams.value.current, () => {fetchDocs()})
watch(() => user.id, () => {fetchDocs()})
onMounted(() => {
  if (route.path === '/blog') currentActive.value = 'blog';
  if (route.path.startsWith('/collab')) currentActive.value = 'collab';
  fetchDocs();
});
</script>

<style lang="scss" scoped>
$primary-color: #4299e1;
$primary-light: #63b3ed;
$primary-dark: #3182ce;
$success-color: #22c55e;
$warning-color: #f59e0b;
$danger-color: #ef4444;
$text-primary: #334155;
$text-secondary: #64748b;
$text-tertiary: #94a3b8;
$bg-light: #f8fafc;
$bg-white: #ffffff;
$border-light: #e2e8f0;
$shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.05);
$shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
$shadow-lg: 0 10px 25px rgba(0, 0, 0, 0.1);
$radius-sm: 4px;
$radius-md: 8px;
$radius-lg: 12px;
$radius-full: 999px;
$transition-base: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

.collab-browsing-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f9fafb;
  color: #333;
}

.main-header {
  display: flex;
  align-items: center;
  padding: 0 2rem;
  height: 60px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;

  .logo {
    font-size: 1.5rem;
    font-weight: 700;
    color: #4299e1;
    margin-right: 2rem;
  }

  .main-nav {
    display: flex;
    gap: 1.5rem;
    margin-right: 2rem;

    .nav-item {
      color: #666;
      text-decoration: none;
      padding: 0.5rem 0;
      position: relative;
      font-size: 14px;

      &.active {
        color: #4299e1;
        font-weight: 500;
        border-bottom: 2px solid #4299e1;

        &:after {
          content: "";
          position: absolute;
          bottom: 0;
          left: 0;
          width: 100%;
          height: 2px;
          background-color: #4299e1;
        }
      }

      &:hover {
        color: #4299e1;
      }
    }
  }

  .search-bar {
    display: flex;
    flex: 1;
    max-width: 500px;

    input {
      flex: 1;
      padding: 0.5rem 1rem;
      border: 1px solid #ddd;
      border-radius: 4px 0 0 4px;
      outline: none;
      transition: border-color 0.3s;

      &:focus {
        border-color: #4299e1;
      }
    }

    button {
      background-color: #4299e1;
      color: white;
      border: none;
      padding: 0 1rem;
      border-radius: 0 4px 4px 0;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background-color: #3182ce;
      }
    }
  }
}

// 筛选栏美化
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  padding: 1.2rem 2rem;
  background-color: $bg-white;
  border-bottom: 1px solid $border-light;
  align-items: center;

  .filter-group {
    display: flex;
    align-items: center;
    gap: 0.8rem;
    flex: auto;
    min-width: 280px;

    .filter-label {
      width: 85px;
      font-weight: 600;
      color: $text-secondary;
      font-size: 14px;
    }

    .filter-btn-group {
      display: flex;
      gap: 0.6rem;
      flex-wrap: wrap;
    }

    .filter-btn {
      padding: 0.4rem 1rem;
      border: 1px solid $border-light;
      border-radius: $radius-full;
      background-color: $bg-light;
      cursor: pointer;
      transition: $transition-base;
      font-size: 13px;
      color: $text-secondary;
      display: flex;
      align-items: center;
      gap: 0.4rem;

      &:hover:not(.active) {
        border-color: $primary-light;
        color: $primary-color;
        background-color: rgba(66, 153, 225, 0.05);
      }

      &.active {
        background-color: $primary-color;
        color: white;
        border-color: $primary-color;
        box-shadow: 0 2px 4px rgba(66, 153, 225, 0.2);
      }

      .status-dot {
        display: inline-block;
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }

      .editing-dot {
        background-color: $danger-color;
        animation: blink 2s infinite;
      }

      .finished-dot {
        background-color: $success-color;
      }
    }

    .tag-filter {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 20px;
      flex-wrap: wrap;

      .tag-btn {
        padding: 0.2rem 0.6rem;
        border-radius: 12px;
        font-size: 0.85rem;
        background-color: #f1f5f9;
        border: none;
        cursor: pointer;
        transition: all 0.2s;

        &.active {
          background-color: #4299e1;
          color: white;
        }
      }
    }
  }
}

.content-container {
  flex: 1;
  padding: 0.5rem 2rem;
  max-width: 1440px;
  margin: 0 auto;
  width: 100%;

  .collab-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.8rem;
    padding-bottom: 0.8rem;
    border-bottom: 1px solid $border-light;

    .collab-title {
      font-size: 1.9rem;
      font-weight: 600;
      color: $primary-dark;
      display: flex;
      align-items: center;

      .count-badge {
        font-size: 1rem;
        font-weight: normal;
        color: $text-secondary;
        margin-left: 0.8rem;
        background-color: $bg-light;
        padding: 0.2rem 0.8rem;
        border-radius: $radius-full;
        border: 1px solid $border-light;
        transition: $transition-base;
      }

      .badge-pulse {
        animation: pulse-light 2s infinite;
      }
    }

    .create-btn {
      background-color: $primary-color;
      color: white;
      padding: 0.65rem 1.5rem;
      border-radius: $radius-full;
      text-decoration: none;
      display: flex;
      align-items: center;
      gap: 0.6rem;
      transition: $transition-base;
      font-weight: 500;
      box-shadow: 0 2px 6px rgba(66, 153, 225, 0.2);

      &:hover {
        background-color: $primary-dark;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(66, 153, 225, 0.3);
      }

      .plus-rotate {
        animation: rotate 0.5s ease;
      }
    }
  }

  // 文档网格布局
  .content-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
    gap: 1.8rem;
    margin-bottom: 2.5rem;
  }

  // 文档卡片美化
  .content-card {
    background-color: $bg-white;
    border-radius: $radius-lg;
    overflow: hidden;
    box-shadow: $shadow-sm;
    transition: $transition-base;
    position: relative;
    padding-top: 1.6rem;
    border: 1px solid $border-light;

    &:hover {
      transform: translateY(-6px);
      box-shadow: $shadow-md;
      border-color: transparent;
    }

    &.card-hover {
      transform: translateY(-3px);
      box-shadow: $shadow-md;
    }

    .doc-status {
      position: absolute;
      top: 1.2rem;
      right: 3.5rem;
      padding: 0.25rem 0.7rem;
      border-radius: $radius-full;
      font-size: 0.78rem;
      font-weight: 500;
      z-index: 1;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

      &.status-editing {
        background-color: rgba(239, 68, 68, 0.08);
        color: $danger-color;
        border: 1px solid rgba(239, 68, 68, 0.15);
      }

      &.status-finished {
        background-color: rgba(34, 197, 94, 0.08);
        color: $success-color;
        border: 1px solid rgba(34, 197, 94, 0.15);
      }
    }

    .favorite-btn {
      position: absolute;
      top: 1.2rem;
      right: 1.2rem;
      background: transparent;
      border: none;
      cursor: pointer;
      font-size: 18px;
      color: $text-tertiary;
      transition: $transition-base;
      z-index: 1;
      width: 30px;
      height: 30px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        background-color: rgba(245, 158, 11, 0.1);
        color: $warning-color;
      }

      .fa-star {
        transition: $transition-base;
      }

      .favorite-active {
        color: $warning-color;
        animation: pulse 0.5s ease;
      }

      .star-hover {
        transform: scale(1.1);
      }
    }

    .card-header {
      padding: 0 1.5rem 1.2rem;
      border-bottom: 1px solid $border-light;

      .card-title {
        font-size: 1.15rem;
        font-weight: 600;
        margin-bottom: 0.7rem;
        color: $text-primary;
        text-decoration: none;
        transition: $transition-base;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        line-height: 1.4;

        &:hover {
          color: $primary-color;
        }

        &.title-hover {
          color: $primary-color;
        }
      }

      .card-meta {
        display: flex;
        justify-content: space-between;
        font-size: 0.88rem;
        color: $text-tertiary;

        .lead-author, .date {
          display: flex;
          align-items: center;
          gap: 0.3rem;
        }

        .author-icon, .date-icon {
          font-size: 0.8rem;
        }
      }
    }

    .card-content {
      padding: 1.5rem;

      .excerpt {
        color: $text-secondary;
        font-size: 0.92rem;
        margin-bottom: 1.2rem;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
        line-height: 1.5;
        transition: $transition-base;

        &.excerpt-hover {
          color: $text-primary;
        }
      }

      .tag-list {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        margin-bottom: 1.2rem;

        .tag {
          font-size: 0.78rem;
          padding: 0.25rem 0.7rem;
          border-radius: $radius-full;
          background-color: rgba(66, 153, 225, 0.1);
          color: $primary-color;
          transition: $transition-base;
          cursor: pointer;

          &:hover {
            background-color: $primary-color;
            color: white;
            transform: translateY(-2px);
          }
        }
      }

      .collab-info {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.88rem;
        color: $text-tertiary;
        padding-top: 1rem;
        border-top: 1px dashed $border-light;

        .collaborators {
          display: flex;
          align-items: center;
          gap: 0.6rem;

          .collab-label {
            margin-right: 0.3rem;
            font-weight: 500;
          }

          .avatar-group {
            display: flex;
            align-items: center;

            .avatar {
              width: 24px;
              height: 24px;
              border-radius: 50%;
              border: 2px solid $bg-white;
              margin-left: -6px;
              transition: $transition-base;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

              &:first-child {
                margin-left: 0;
              }

              &:hover {
                transform: scale(1.1);
                z-index: 2;
              }
            }

            .more-avatars {
              width: 24px;
              height: 24px;
              border-radius: 50%;
              background-color: $bg-light;
              color: $text-secondary;
              font-size: 0.7rem;
              display: flex;
              align-items: center;
              justify-content: center;
              margin-left: -6px;
              border: 2px solid $bg-white;
              box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
              transition: $transition-base;

              &:hover {
                background-color: $primary-light;
                color: white;
              }
            }
          }
        }

        .version-info {
          display: flex;
          align-items: center;
          gap: 0.4rem;
          transition: $transition-base;

          &:hover {
            color: $primary-color;
          }

          .version-icon {
            font-size: 0.8rem;
          }
        }
      }
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.5rem;
      border-top: 1px solid $border-light;
      font-size: 0.88rem;

      .stats {
        display: flex;
        gap: 1.2rem;
        color: $text-tertiary;

        .stat-item {
          display: flex;
          align-items: center;
          gap: 0.3rem;
          transition: $transition-base;

          &:hover {
            color: $primary-color;
          }

          .view-icon, .comment-icon {
            font-size: 0.8rem;
          }
        }
      }

      .action-btns {
        display: flex;
        gap: 0.8rem;

        .read-btn, .edit-btn {
          padding: 0.4rem 1rem;
          border-radius: $radius-full;
          font-size: 0.88rem;
          font-weight: 500;
          transition: $transition-base;
          text-decoration: none;
          display: inline-flex;
          align-items: center;
          justify-content: center;

          &.btn-hover {
            transform: translateY(-2px);
          }
        }

        .read-btn {
          color: $primary-color;
          border: 1px solid $primary-light;
          background-color: transparent;

          &:hover {
            background-color: $primary-color;
            color: white;
            box-shadow: 0 2px 4px rgba(66, 153, 225, 0.2);
          }
        }

        .edit-btn {
          color: white;
          background-color: $primary-color;
          border: 1px solid $primary-color;
          box-shadow: 0 2px 4px rgba(66, 153, 225, 0.15);

          &:hover {
            background-color: $primary-dark;
            box-shadow: 0 4px 8px rgba(66, 153, 225, 0.25);
          }
        }
      }
    }
  }

  // 空状态美化
  .no-content {
    text-align: center;
    padding: 4rem 2rem;
    background-color: $bg-white;
    border-radius: $radius-lg;
    box-shadow: $shadow-sm;
    margin-bottom: 2rem;

    .empty-container {
      max-width: 400px;
      margin: 0 auto;
    }

    .empty-icon {
      font-size: 4rem;
      color: $text-tertiary;
      margin-bottom: 1.5rem;
      transition: $transition-base;

      &:hover {
        color: $primary-light;
        transform: scale(1.05);
      }
    }

    .empty-title {
      font-size: 1.3rem;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: 0.8rem;
    }

    .empty-desc {
      color: $text-tertiary;
      font-size: 0.95rem;
      margin-bottom: 2rem;
      line-height: 1.6;
    }

    .empty-btn {
      background-color: $primary-color;
      color: white;
      padding: 0.7rem 1.8rem;
      border-radius: $radius-full;
      text-decoration: none;
      transition: $transition-base;
      font-weight: 500;
      display: inline-flex;
      align-items: center;
      gap: 0.6rem;
      box-shadow: 0 2px 6px rgba(66, 153, 225, 0.2);

      &:hover {
        background-color: $primary-dark;
        transform: translateY(-3px);
        box-shadow: 0 4px 12px rgba(66, 153, 225, 0.3);
      }
    }
  }
}

.main-footer {
  background-color: #fff;
  padding: 1.5rem 2rem;
  border-top: 1px solid #eee;
  margin-top: auto;

  .footer-content {
    max-width: 1400px;
    margin: 0 auto;
    text-align: center;
    color: #666;
    font-size: 0.9rem;

    .link-group {
      margin-top: 0.5rem;

      a {
        color: #4299e1;
        text-decoration: none;
        margin: 0 0.5rem;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}

// 响应式适配
@media (max-width: 768px) {
  .main-header {
    padding: 0 1rem;
    flex-wrap: wrap;
    height: auto;
    padding-top: 0.5rem;
    padding-bottom: 0.5rem;

    .logo {
      margin-right: 1rem;
      margin-bottom: 0.5rem;
    }

    .main-nav {
      margin-right: 0;
      margin-bottom: 0.5rem;
      gap: 1rem;
    }

    .search-bar {
      max-width: 100%;
      width: 100%;
    }
  }

  .filter-bar {
    padding: 1rem;
    gap: 0.5rem;

    .filter-group {
      .tag-filter {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 5px;
        flex-wrap: wrap;
      }
    }
  }

  .content-container {
    padding: 1rem;
  }

  .collab-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .content-card {
    .doc-status {
      right: 2.8rem;
      font-size: 0.7rem;
      padding: 0.15rem 0.4rem;
    }

    .favorite-btn {
      font-size: 14px;
    }
  }

}
</style>