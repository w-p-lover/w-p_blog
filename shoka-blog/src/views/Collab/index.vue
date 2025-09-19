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
            <button class="filter-btn" :class="{ active: docStatus === 'all' }" @click="docStatus = 'all'">
              全部状态
            </button>
            <button class="filter-btn" :class="{ active: docStatus === 'editing' }" @click="docStatus = 'editing'">
              正在编辑
            </button>
            <button class="filter-btn" :class="{ active: docStatus === 'finished' }" @click="docStatus = 'finished'">
              已完成
            </button>
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
        <main class="content-container">
          <!-- 协作内容统计与创建入口 -->
          <div class="collab-header">
            <h1 class="collab-title">
              协作文档
              <span class="count-badge">{{ filteredCollabs.length }} 篇</span>
            </h1>
            <router-link to="/collab/create" class="create-btn">
              <i class="fa fa-plus"></i>
              新建文档
            </router-link>
          </div>

          <!-- 协作文档列表 -->
          <div class="content-grid">
            <article class="content-card collab-card" v-for="doc in filteredCollabs" :key="'collab-' + doc.id">
              <button
                  class="favorite-btn"
                  @click="toggleFavorite(doc.id)"
                  :title="isFavorite(doc.id) ? '取消收藏' : '收藏'">
                <i class="fa"
                   :class="isFavorite(doc.id) ? 'fa-star favorite-active' : 'fa-star-o'">
                </i>
              </button>

              <div class="doc-status" :class="doc.isEditing ? 'status-editing' : 'status-finished'">
                {{ doc.isEditing ? '正在编辑' : '已完成' }}
              </div>

              <div class="card-header">
                <router-link :to="`/collab/${doc.id}`" class="card-title">{{ doc.title }}</router-link>
                <div class="card-meta">
                  <span class="lead-author">主导者: {{ doc.leadAuthor }}</span>
                  <span class="date">{{ formatDate(doc.lastUpdateDate) }}</span>
                </div>
              </div>

              <div class="card-content">
                <p class="excerpt">{{ doc.excerpt }}</p>
                <div class="tag-list">
                  <span class="tag" v-for="tag in doc.tags" :key="tag">{{ tag }}</span>
                </div>

                <!-- 协作者头像与版本信息（强化协作特性） -->
                <div class="collab-info">
                  <div class="collaborators">
                    <span class="collab-label">协作者:</span>
                    <div class="avatar-group">
                      <template v-for="(collaborator, index) in doc.collaborators" :key="`collab-${doc.id}-${index}`">
                        <img
                            :src="collaborator?.avatar || 'src/assets/img/head_portrait2.jpg'"
                            :alt="collaborator"
                            class="avatar"
                            v-if="index < 3">
                      </template>
                      <span class="more-avatars" v-if="doc.collaborators.length > 3">+
                          {{ doc.collaborators.length - 3 }}
                        </span>
                    </div>
                  </div>
                  <div class="version-info">
                    <i class="fa fa-history"></i> v{{ doc.version }} ({{ doc.editCount }}次编辑)
                  </div>
                </div>
              </div>

              <div class="card-footer">
                <div class="stats">
                  <span class="stat-item"><i class="fa fa-eye"></i> {{ doc.views }} 浏览</span>
                  <span class="stat-item"><i class="fa fa-comment"></i> {{ doc.comments }} 评论</span>
                </div>
                <div class="action-btns">
                  <router-link :to="`/collab/${doc.id}`" class="read-btn">查看</router-link>
                  <router-link :to="`/collab/edit/${doc.id}`" class="edit-btn" v-if="isCollaborator(doc)">
                    编辑
                  </router-link>
                </div>
              </div>
            </article>
          </div>

          <!-- 空状态提示 -->
          <div class="no-content" v-if="filteredCollabs.length === 0">
            <el-empty description="暂无符合条件的协作文档"/>
            <router-link to="/collab/create" class="empty-btn">立即创建第一篇文档</router-link>
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

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  padding: 1rem 2rem;
  background-color: #fff;
  border-bottom: 1px solid #eee;

  .filter-group {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex: auto;

    .filter-label {
      width: 80px;
      font-weight: 700;
      color: #666;
    }

    .filter-btn {
      padding: 0.3rem 0.8rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      background-color: transparent;
      cursor: pointer;
      transition: all 0.2s;

      &.active {
        background-color: #4299e1;
        color: white;
        border-color: #4299e1;
      }

      &:hover:not(.active) {
        border-color: #4299e1;
        color: #4299e1;
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
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.collab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;

  .collab-title {
    font-size: 1.8rem;
    font-weight: 600;
    color: #4299e1;

    .count-badge {
      font-size: 1rem;
      font-weight: normal;
      color: #666;
      margin-left: 0.5rem;
    }
  }

  .create-btn {
    background-color: #4299e1;
    color: white;
    padding: 0.5rem 1.2rem;
    border-radius: 4px;
    text-decoration: none;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    transition: background-color 0.3s;

    &:hover {
      background-color: #3182ce;
    }
  }
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.content-card {
  background-color: #fff;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s, box-shadow 0.3s;
  position: relative;
  padding-top: 1.5rem;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  }

  .doc-status {
    position: absolute;
    top: 1rem;
    right: 3rem; // 给收藏按钮留出空间
    padding: 0.2rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: 500;
    z-index: 1;
  }

  .status-editing {
    background-color: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }

  .status-finished {
    background-color: rgba(34, 197, 94, 0.1);
    color: #22c55e;
  }

  .favorite-btn {
    position: absolute;
    top: 22px;
    right: 18px;
    background: transparent;
    border: none;
    cursor: pointer;
    font-size: 16px;
    color: #9ca3af;
    transition: color 0.2s;
    z-index: 1;
  }

  &:hover {
    color: #f59e0b;
  }

  .favorite-active {
    color: #f59e0b;
    animation: pulse 0.5s ease;
  }
}

.card-header {
  padding: 0 1rem 1rem;
  border-bottom: 1px solid #f1f5f9;

  .card-title {
    font-size: 1.1rem;
    font-weight: 600;
    margin-bottom: 0.5rem;
    color: #333;
    text-decoration: none;
    transition: color 0.3s;

    &:hover {
      color: #4299e1;
    }
  }

  .card-meta {
    display: flex;
    justify-content: space-between;
    font-size: 0.85rem;
    color: #666;
  }
}

.card-content {
  padding: 1rem;

  .excerpt {
    color: #666;
    font-size: 0.9rem;
    margin-bottom: 1rem;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1rem;

    .tag {
      font-size: 0.75rem;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      background-color: rgba(66, 153, 225, 0.1);
      color: #4299e1;
    }
  }

  .collab-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.85rem;
    color: #666;

    .collaborators {
      display: flex;
      align-items: center;
      gap: 0.5rem;

      .collab-label {
        margin-right: 0.3rem;
      }

      .avatar-group {
        display: flex;
        align-items: center;

        .avatar {
          width: 22px;
          height: 22px;
          border-radius: 50%;
          border: 2px solid white;
          margin-left: -5px;

          &:first-child {
            margin-left: 0;
          }
        }

        .more-avatars {
          width: 22px;
          height: 22px;
          border-radius: 50%;
          background-color: #e2e8f0;
          color: #666;
          font-size: 0.65rem;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-left: -5px;
        }
      }
    }

    .version-info {
      display: flex;
      align-items: center;
      gap: 0.3rem;
    }
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.8rem 1rem;
  border-top: 1px solid #f1f5f9;
  font-size: 0.85rem;

  .stats {
    display: flex;
    gap: 1rem;
    color: #666;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 0.2rem;
    }
  }

  .action-btns {
    display: flex;
    gap: 0.5rem;

    .read-btn {
      color: #4299e1;
      text-decoration: none;
      padding: 0.3rem 0.8rem;
      border-radius: 4px;
      border: 1px solid #4299e1;
      transition: all 0.2s;

      &:hover {
        background-color: #4299e1;
        color: white;
      }
    }

    .edit-btn {
      color: white;
      background-color: #4299e1;
      text-decoration: none;
      padding: 0.3rem 0.8rem;
      border-radius: 4px;
      transition: background-color 0.2s;

      &:hover {
        background-color: #3182ce;
      }
    }
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.no-content {
  text-align: center;
  padding: 3rem;
  background-color: #fff;
  border-radius: 8px;

  .empty-img {
    width: 120px;
    height: 120px;
    margin-bottom: 1rem;
    opacity: 0.5;
  }

  .empty-btn {
    background-color: #4299e1;
    color: white;
    padding: 0.6rem 1.5rem;
    border-radius: 4px;
    text-decoration: none;
    transition: background-color 0.3s;

    &:hover {
      background-color: #3182ce;
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