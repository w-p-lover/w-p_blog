<template>
  <div class="app-container">
    <!-- 文章状态 -->
    <el-row :gutter="24" class="status-filter mb15">
      <el-col :span="1.5" class="status-label">
        状态
      </el-col>
      <el-col :span="1.5"
              :class="isActive('all')"
              @click="changeStatus('all')"
              class="status-item">
        全部
      </el-col>
      <el-col :span="1.5"
              :class="isActive('public')"
              @click="changeStatus('public')"
              class="status-item">
        公开
      </el-col>
      <el-col :span="1.5"
              :class="isActive('secret')"
              @click="changeStatus('secret')"
              class="status-item">
        私密
      </el-col>
      <el-col :span="1.5"
              :class="isActive('draft')"
              @click="changeStatus('draft')"
              class="status-item">
        草稿
      </el-col>
      <el-col :span="1.5"
              :class="isActive('delete')"
              @click="changeStatus('delete')"
              class="status-item">
        回收站
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="hover" class="search-card mb15" v-show="showSearch">
      <el-form :model="queryParams" :inline="true" class="search-form">
        <el-form-item label="名称">
          <el-input @keyup.enter="handleQuery"
                    v-model="queryParams.keyword"
                    style="width: 180px"
                    placeholder="请输入文章名称"
                    clearable/>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.articleType"
                     placeholder="请选择类型"
                     clearable
                     style="width: 140px;">
            <el-option v-for="item in typeList"
                       :key="item.value"
                       :label="item.label"
                       :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="queryParams.tagId"
                     placeholder="请选择标签"
                     clearable
                     filterable
                     @visible-change="getTag"
                     style="width: 140px">
            <el-option v-for="item in tagList"
                       :key="item.id"
                       :label="item.tagName"
                       :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.categoryId"
                     placeholder="请选择分类"
                     clearable
                     filterable
                     @visible-change="getCategory"
                     style="width: 140px">
            <el-option v-for="item in categoryList"
                       :key="item.id"
                       :label="item.categoryName"
                       :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary"
                     icon="Search"
                     @click="handleQuery"
                     class="search-btn">
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="operation-bar mb15">
      <el-col :span="1.5">
        <el-button type="danger"
                   plain
                   icon="Delete"
                   :disabled="articleIdList.length === 0"
                   @click="handleDelete(undefined)"
                   class="operation-btn">
          批量删除
        </el-button>
      </el-col>
      <el-col :span="1.5" v-if="queryParams.isDelete == 0">
        <el-button type="danger"
                   plain
                   icon="Delete"
                   :disabled="articleIdList.length === 0"
                   @click="handleRecycle(undefined)"
                   class="operation-btn">
          批量回收
        </el-button>
      </el-col>
      <el-col :span="1.5" v-if="queryParams.isDelete == 1">
        <el-button type="success"
                   plain
                   icon="Finished"
                   :disabled="articleIdList.length === 0"
                   @click="handleRecycle(undefined)"
                   class="operation-btn">
          批量恢复
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格展示 -->
    <el-card shadow="hover" class="table-card">
      <el-table border
                :data="articleList"
                @selection-change="handleSelectionChange"
                v-loading="loading"
                class="article-table"
                :row-class-name="tableRowClassName">
        <!-- 表格列 -->
        <el-table-column type="selection" width="55" align="center"></el-table-column>

        <!-- 缩略图 -->
        <el-table-column prop="articleCover" label="缩略图" width="180" align="center">
          <template #default="scope">
            <div class="cover-container">
              <el-image class="article-cover"
                        :src="scope.row.articleCover || defaultCover"
                        fallback="https://picsum.photos/200/120?grayscale">
              </el-image>
              <el-icon v-if="scope.row.status == 1" class="article-status-icon public">
                <View/>
              </el-icon>
              <el-icon v-if="scope.row.status == 2" class="article-status-icon secret">
                <Hide/>
              </el-icon>
              <el-icon v-if="scope.row.status == 3" class="article-status-icon draft">
                <EditPen/>
              </el-icon>
            </div>
          </template>
        </el-table-column>

        <!-- 文章标题 -->
        <el-table-column prop="articleTitle" label="标题" align="center">
          <template #default="scope">
            <div class="article-title" :title="scope.row.articleTitle">
              {{ scope.row.articleTitle }}
              <span v-if="scope.row.isTop" class="top-badge">置顶</span>
            </div>
          </template>
        </el-table-column>

        <!-- 文章分类 -->
        <el-table-column prop="categoryName" label="分类" width="150" align="center"></el-table-column>

        <!-- 文章标签 -->
        <el-table-column prop="tagVOList" label="标签" width="280" align="center">
          <template #default="scope">
            <div class="tag-group">
              <el-tag v-for="item of scope.row.tagVOList"
                      :key="item.tagId"
                      size="small"
                      effect="light"
                      class="article-tag">
                {{ item.tagName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <!-- 文章浏览量 -->
        <el-table-column prop="viewCount" label="浏览量" width="90" align="center">
          <template #default="scope">
            <div class="stat-item">
              <el-icon size="16"><Pointer/></el-icon>
              <span>{{ scope.row.viewCount }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 文章点赞量 -->
        <el-table-column prop="likeCount" label="点赞量" width="90" align="center">
          <template #default="scope">
            <div class="stat-item">
              <span>{{ scope.row.likeCount }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 文章类型 -->
        <el-table-column prop="articleType" label="类型" width="90" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.articleType == 1" type="success" size="small">原创</el-tag>
            <el-tag v-if="scope.row.articleType == 2" type="danger" size="small">转载</el-tag>
            <el-tag v-if="scope.row.articleType == 3" type="info" size="small">翻译</el-tag>
          </template>
        </el-table-column>

        <!-- 文章置顶 -->
        <el-table-column prop="isTop" label="置顶" width="90" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isTop"
                       style="--el-switch-on-color: #13ce66;"
                       :disabled="scope.row.isDelete == 1"
                       :active-value="1"
                       :inactive-value="0"
                       @change="handleTop(scope.row)"></el-switch>
          </template>
        </el-table-column>

        <!-- 文章推荐 -->
        <el-table-column prop="isRecommend" label="推荐" width="90" align="center">
          <template #default="scope">
            <el-switch v-model="scope.row.isRecommend"
                       style="--el-switch-on-color: #409eff;"
                       :disabled="scope.row.isDelete == 1"
                       :active-value="1"
                       :inactive-value="0"
                       @change="handleRecommend(scope.row)"></el-switch>
          </template>
        </el-table-column>

        <!-- 创建时间 -->
        <el-table-column prop="createTime" width="160" label="创建时间" align="center">
          <template #default="scope">
            <div class="create-time">
              <el-icon size="16">
                <Clock/>
              </el-icon>
              <span style="margin-left: 5px">{{ formatDate(scope.row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column width="220" label="操作" align="center">
          <template #default="scope">
            <div class="operation-group">
              <el-button type="primary"
                         icon="Edit"
                         link
                         @click="handleEdit(scope.row.id)"
                         v-if="scope.row.isDelete == 0"
                         class="operation-link">
                编辑
              </el-button>
              <el-button type="danger"
                         icon="Delete"
                         link
                         @click="handleDelete(scope.row.id)"
                         class="operation-link">
                删除
              </el-button>
              <el-button type="danger"
                         icon="Delete"
                         link
                         @click="handleRecycle(scope.row.id)"
                         v-if="queryParams.isDelete == 0"
                         class="operation-link">
                回收
              </el-button>
              <el-button type="success"
                         icon="Finished"
                         link
                         @click="handleRecycle(scope.row.id)"
                         v-if="queryParams.isDelete == 1"
                         class="operation-link">
                恢复
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分页 -->
    <div class="pagination-container" v-if="count > 0">
      <pagination :total="count"
                  v-model:page="queryParams.current"
                  v-model:limit="queryParams.size"
                  @pagination="getList"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  deleteArticle,
  getArticleList,
  getCategoryOption,
  getTagOption,
  recycleArticle,
  updateArticleRecommend,
  updateArticleTop
} from '@/api/article';
import {Article, ArticleQuery, CategoryVO, DeleteDTO, TagVO} from '@/api/article/types';
import router from "@/router";
import {formatDate} from "@/utils/date";
import {messageConfirm, notifySuccess} from '@/utils/modal';
import {computed, onMounted, reactive, toRefs} from 'vue';
import {View, Hide, EditPen, Pointer, Clock} from '@element-plus/icons-vue';

// 默认封面图
const defaultCover = 'https://picsum.photos/200/120?blur=2';

const isActive = computed(() => {
  return function (value: string | undefined) {
    return activeStatus.value == value ? "active-status" : "status";
  }
});

const data = reactive({
  count: 0,
  showSearch: true,
  loading: false,
  title: "",
  queryParams: {
    current: 1,
    size: 10,
    isDelete: 0,
  } as ArticleQuery,
  typeList: [
    {
      value: 1,
      label: "原创",
    },
    {
      value: 2,
      label: "转载",
    },
    {
      value: 3,
      label: "翻译",
    },
  ],
  activeStatus: "all",
  categoryList: [] as CategoryVO[],
  tagList: [] as TagVO[],
  articleIdList: [] as number[],
  articleList: [] as Article[],
});

const {
  count,
  showSearch,
  loading,
  queryParams,
  typeList,
  activeStatus,
  categoryList,
  tagList,
  articleIdList,
  articleList,
} = toRefs(data);

// 表格行样式
const tableRowClassName = ({row}: { row: Article }) => {
  return row.isDelete === 1 ? 'deleted-row' : '';
};

const handleSelectionChange = (selection: Article[]) => {
  articleIdList.value = selection.map((item) => item.id);
};

const changeStatus = (status: string) => {
  switch (status) {
    case "all":
      queryParams.value.isDelete = 0;
      queryParams.value.status = undefined;
      break;
    case "public":
      queryParams.value.isDelete = 0;
      queryParams.value.status = 1;
      break;
    case "secret":
      queryParams.value.isDelete = 0;
      queryParams.value.status = 2;
      break;
    case "draft":
      queryParams.value.isDelete = 0;
      queryParams.value.status = 3;
      break;
    case "delete":
      queryParams.value.isDelete = 1;
      queryParams.value.status = undefined;
      break;
  }
  activeStatus.value = status;
  handleQuery();
};

const handleRecycle = (id?: number) => {
  let params: DeleteDTO = {
    idList: [],
    isDelete: 0,
  };
  if (id != undefined) {
    params.idList = [id];
  } else {
    params.idList = articleIdList.value;
  }
  params.isDelete = queryParams.value.isDelete === 0 ? 1 : 0;
  let text = queryParams.value.isDelete === 0 ? "回收" : "恢复";
  messageConfirm("确认" + text + "已选中的数据项?").then(() => {
    recycleArticle(params).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
      }
    });
  }).catch(() => {
  });
};

const handleEdit = (id: number) => {
  router.push({path: `/article/write/${id}`});
};

const handleDelete = (id?: number) => {
  let ids: number[] = [];
  if (id == undefined) {
    ids = articleIdList.value;
  } else {
    ids = [id];
  }
  messageConfirm("确认删除已选中的数据项?").then(() => {
    deleteArticle(ids).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
      }
    });
  }).catch(() => {
  });
};

const handleTop = (article: Article) => {
  let text = article.isTop === 0 ? "取消置顶" : "置顶";
  messageConfirm("确定要" + text + "该文章吗?").then(() => {
    updateArticleTop({id: article.id, isTop: article.isTop}).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
      } else {
        article.isTop = article.isTop === 0 ? 1 : 0;
      }
    });
  }).catch(() => {
    article.isTop = article.isTop === 0 ? 1 : 0;
  });
};

const handleRecommend = (article: Article) => {
  let text = article.isRecommend === 0 ? "取消推荐" : "推荐";
  messageConfirm("确定要" + text + "该文章吗?").then(() => {
    updateArticleRecommend({id: article.id, isRecommend: article.isRecommend}).then(({data}) => {
      if (data.flag) {
        notifySuccess(data.msg);
      } else {
        article.isRecommend = article.isRecommend === 0 ? 1 : 0;
      }
    });
  }).catch(() => {
    article.isRecommend = article.isRecommend === 0 ? 1 : 0;
  });
};

const getCategory = (val: boolean) => {
  if (val) {
    getCategoryOption().then(({data}) => {
      categoryList.value = data.data;
    });
  }
};

const getTag = (val: boolean) => {
  if (val) {
    getTagOption().then(({data}) => {
      tagList.value = data.data;
    });
  }
};

const getList = () => {
  loading.value = true;
  getArticleList(queryParams.value).then(({data}) => {
    articleList.value = data.data.recordList;
    count.value = data.data.count;
    loading.value = false;
  })
};

const handleQuery = () => {
  queryParams.value.current = 1;
  getList();
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
/* 基础样式 */
.mb15 {
  margin-bottom: 15px;
}

/* 页面标题 */
.page-header {
  margin: 0 0 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #303133;
  font-weight: 500;
}

.page-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

/* 状态筛选 */
.status-filter {
  padding: 10px 0;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.status-label {
  color: #606266;
  font-weight: 500;
  line-height: 24px;
}

.status-item {
  padding: 2px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 24px;
}

.status {
  color: #606266;
}

.status:hover {
  background-color: #f0f0f0;
  color: #303133;
}

.active-status {
  color: #409eff;
  background-color: #ecf5ff;
  font-weight: 500;
}

/* 搜索栏 */
.search-card {
  box-shadow: 2px 2px 8px rgb(0 0 0 / 10%)
}

.search-form {
  display: flex;
  align-items: center;
  height: 20px;
  padding: 20px 0 0;
}

.search-btn {
  transition: all 0.2s ease;
}

.search-btn:hover {
  transform: translateY(-2px);
}

/* 操作栏 */
.operation-bar {
  display: flex;
  align-items: center;
}

.operation-btn {
  transition: all 0.2s ease;
}

.operation-btn:hover {
  transform: translateY(-2px);
}

/* 表格样式 */
.table-card {
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.article-table {
  border-radius: 4px;
}

.article-table th {
  background-color: #f5f7fa;
  font-weight: 500;
}

.article-table tr:hover > td {
  background-color: #fafafa !important;
}

/* 已删除行样式 */
:deep( .deleted-row) {
  background-color: #fef0f0 !important;
}

:deep( .deleted-row td) {
  color: #909399 !important;
}

/* 缩略图容器 */
.cover-container {
  position: relative;
  width: 100%;
  height: 90px;
  border-radius: 4px;
  overflow: hidden;
}

.article-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.cover-container:hover .article-cover {
  transform: scale(1.05);
}

/* 状态图标 */
.article-status-icon {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
}

.public {
  background-color: rgba(64, 158, 255, 0.8);
}

.secret {
  background-color: rgba(153, 153, 153, 0.8);
}

.draft {
  background-color: rgba(250, 173, 20, 0.8);
}

/* 文章标题 */
.article-title {
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 2px 0;
  transition: color 0.2s ease;
}

.article-title:hover {
  color: #409eff;
}

.top-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 0 4px;
  font-size: 12px;
  color: #f56c6c;
  background-color: #fef0f0;
  border-radius: 2px;
}

/* 标签样式 */
.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.article-tag {
  margin: 0 !important;
}

/* 统计项 */
.stat-item {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
}

.stat-item span {
  margin-left: 4px;
}

/* 创建时间 */
.create-time {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  font-size: 13px;
}

/* 操作按钮组 */
.operation-group {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.operation-link {
  padding: 0 6px !important;
  transition: all 0.2s ease;
}

.operation-link:hover {
  transform: translateY(-2px);
}

/* 分页容器 */
.pagination-container {
  margin-top: 15px;
  text-align: right;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .article-table .el-table__column--width-180 {
    width: 140px !important;
  }
}

@media (max-width: 992px) {
  .status-filter {
    flex-wrap: wrap;
  }

  .search-form {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>