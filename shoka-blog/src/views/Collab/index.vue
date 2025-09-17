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
                         @click="handleRouterLinkActive('collab')">协作空间
            </router-link>

            <button @click="handleMyEdit" class="nav-item" :class="{ active: currentActive === 'my-edit' }">我的编辑
            </button>
            <button @click="handleFavorites" class="nav-item" :class="{ active: currentActive === 'favorites' }">收藏
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
            <button class="filter-btn" :class="{ active: sortBy === 'latest' }" @click="sortBy = 'latest'">最新更新
            </button>
            <button class="filter-btn" :class="{ active: sortBy === 'popular' }" @click="sortBy = 'popular'">热门浏览
            </button>
            <button class="filter-btn" :class="{ active: sortBy === 'editCount' }" @click="sortBy = 'editCount'">
              编辑最多
            </button>
          </div>

          <div class="filter-group">
            <span class="filter-label">文档状态:</span>
            <button class="filter-btn" :class="{ active: docStatus === 'all' }" @click="docStatus = 'all'">全部状态
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
              <button class="tag-btn" :class="{ active: selectedTag === 'all' }" @click="selectedTag = 'all'">全部
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
              <i class="fa fa-plus"></i> 新建文档
            </router-link>
          </div>

          <!-- 协作文档列表 -->
          <div class="content-grid">
            <article class="content-card collab-card" v-for="doc in filteredCollabs" :key="'collab-' + doc.id">
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
                            :src="`src/assets/img/head_portrait2.jpg`"
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
                  <router-link :to="`/collab/edit/${doc.id}`" class="edit-btn" v-if="isCollaborator(doc)">编辑</router-link>
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
import {ref, computed, reactive, toRefs} from 'vue';
import 'font-awesome/css/font-awesome.min.css';
import { listDocs } from "@/api/collab";
import { Doc } from "@/api/collab/type";
import {PageQuery} from "@/model";
import useStore from '@/store';

const {user} = useStore();
const data = reactive({
  count: 0,
  queryParams: {
    current: 1,
    size: 5,
    tag : '',
    status: '',
    keyword : '',
    sortType: ''
  } as PageQuery,
  collabDocuments: [] as Array<{
    id: number;
    title: string;
    leadAuthor: string; // 主导者（创建者）
    lastUpdateDate: string; // 最后更新时间
    excerpt: string; // 摘要
    tags: string[]; // 标签
    collaborators: string[]; // 所有协作者
    views: number; // 浏览量
    editCount: number; // 编辑次数
    version: number; // 版本号
    isEditing: boolean; // 是否正在编辑
    comments: number; // 评论数
  }>
});
const {count, queryParams, collabDocuments} = toRefs(data);
// 3. 其他状态
const searchKeyword = ref("");
const sortBy = ref("latest");
const docStatus = ref("all");
const selectedTag = ref("all");
const currentActive = ref("");
const route = useRoute();
const currentUser = computed(() => user.nickname || '默认用户');

// 4. 取后端数据并做字段映射
const fetchDocs = async () => {
  try {
    const { data } = await listDocs(queryParams.value);
    if (data.code === 200) {
      console.log("获取协作文档成功", data);
      collabDocuments.value = (data.data || []).map((d: Doc) => ({
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
    }
  } catch (e) {
    console.error("获取协作文档失败", e);
  }
};

// 5. 计算属性
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

// 6. 权限控制：判断当前用户是否为文档协作者（决定是否显示编辑按钮）
const isCollaborator = (doc) => {
console.log("当前用户：", currentUser.value);
return doc.collaborators.some(collab => collab.name === currentUser.value);
};


// 7. 搜索处理
const handleSearch = () => {
  console.log("搜索协作关键词：", searchKeyword.value);
};

// 8. 日期格式化
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString("zh-CN", {year: "numeric", month: "short", day: "numeric"});
};

onMounted(() => {
  if (route.path === '/blog') currentActive.value = 'blog';
  if (route.path.startsWith('/collab')) currentActive.value = 'collab';
  fetchDocs();
});

const handleMyEdit = () => {
  currentActive.value = 'my-edit';
  collabDocuments.value = collabDocuments.value.filter(doc =>
      doc.collaborators.includes(currentUser.value)
  );
};

const handleFavorites = () => {
  currentActive.value = 'favorites';
  collabDocuments.value = [];
};

const handleRouterLinkActive = (key: string) => {
  currentActive.value = key;
  fetchDocs();
};

</script>

<style lang="scss" scoped>
// 基础容器
.collab-browsing-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f9fafb;
  color: #333;
}

// 顶部导航（与现有博客风格保持一致）
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

// 筛选区域（协作专属维度）
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
      grid-template-columns: repeat(8, 1fr);
      gap: 0.8rem;
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

// 内容容器
.content-container {
  flex: 1;
  padding: 0.5rem 2rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

// 协作内容头部（统计+创建入口）
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

// 内容网格（协作卡片布局）
.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

// 协作卡片样式（强化协作特性）
.content-card {
  background-color: #fff;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s, box-shadow 0.3s;
  position: relative; // 用于状态标签定位

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  }

  // 文档状态标签（绝对定位在右上角）
  .doc-status {
    position: absolute;
    top: 1rem;
    right: 1rem;
    padding: 0.2rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: 500;
  }

  .status-editing {
    background-color: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }

  .status-finished {
    background-color: rgba(34, 197, 94, 0.1);
    color: #22c55e;
  }

  .card-header {
    padding: 1rem;
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

    // 协作者与版本信息（新增：强化协作属性）
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
}

// 空状态样式（更友好的引导）
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

// 页脚（增加返回博客入口）
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
}
</style>