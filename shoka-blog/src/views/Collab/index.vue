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
                class="content-card "
                v-for="doc in filteredCollabs"
                :key="'collab-' + doc.id"
                @mouseenter="hoverDocId = doc.id"
                @mouseleave="hoverDocId = 0"
                :class="{ 'card-hover': hoverDocId === doc.id }"
            >
              <!-- 收藏按钮 - 增强动效 -->
              <button class="favorite-btn"
                  @click="toggleFavorite(doc.id)"
                  :title="isFavorite(doc.id) ? '取消收藏' : '收藏'"
                  :data-doc-id="doc.id">
                <i class="fa"
                    :class="[
                    isFavorite(doc.id) ? 'fa-star favorite-active' : 'fa-star-o',
                    hoverDocId === doc.id ? 'star-hover' : '']">
                </i>
              </button>
              <div
                  class="doc-status"
                  :class="doc.status === 'editing' ? 'status-editing' : 'status-finished'">
                {{ doc.status === 'editing' ? '正在编辑' : '已完成' }}
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
                      @click="selectedTag = tag"
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
import {Doc, DocCard} from "@/api/collab/type";
import {PageQuery} from "@/model";
import useStore from '@/store';
import Pagination from "@/components/Pagination/index.vue";
import {ElLoading, ElMessage} from 'element-plus/dist/index.full.mjs'; // 引入提示组件
import Waves from "@/components/Waves/index.vue";
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
const {count, queryParams, collabDocuments, favoriteDocIds, allDocs} = toRefs(data);

// 其他原有状态
const searchKeyword = ref("");
const sortBy = ref("latest");
const docStatus = ref("all");
const selectedTag = ref("all");
const currentActive = ref("");
const hoverDocId = ref(null);
const route = useRoute();
const currentUser = computed(() => user.nickname || '默认用户');
const createBtnHover = ref(false);


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
        status: d.status || false,
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
      const {data} = await cancelFavorite(user.id, docId);
      if (data.code === 200) {
        favoriteDocIds.value.delete(docId);
        ElMessage.success('已取消收藏');
      } else {
        ElMessage.error('取消收藏失败：' + (data.msg || '操作异常'));
      }
    } else {
      const {data} = await addFavorite(user.id, docId);
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
        if (docStatus.value === "editing" && doc.status !== 'editing') return false;
        if (docStatus.value === "finished" && doc.status === 'editing') return false;
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
  collabDocuments.value = allDocs.value.filter(doc => isFavorite(doc.id));
};

const handleRouterLinkActive = (key: string) => {
  currentActive.value = key;
  fetchDocs();
};

watch([() => queryParams.value.current, () => user.id], () => {
  fetchDocs()
})

onMounted(() => {
  if (route.path === '/blog') currentActive.value = 'blog';
  if (route.path.startsWith('/collab')) currentActive.value = 'collab';
  fetchDocs();
});
</script>

<style lang="scss" scoped>
@import "@/views/Collab/css/base.scss";
</style>