<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form
        @submit.native.prevent
        :model="queryParams"
        :inline="true"
        v-show="showSearch"
        class="search-form"
    >
      <el-form-item label="游戏名">
        <el-input
            v-model="queryParams.keyword"
            placeholder="请输入游戏名"
            clearable
            @keyup.enter="handleQuery"
            class="search-input"
        />
      </el-form-item>
      <el-form-item>
        <el-button
            type="primary"
            icon="Search"
            @click="handleQuery"
            class="search-btn"
        >
          搜索
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="operation-row">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="openModel()"
            class="operation-btn add-btn"
        >
          新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            :disabled="gameIdList.length === 0"
            icon="Delete"
            @click="handleDeleteBatch"
            class="operation-btn delete-btn"
        >
          批量删除
        </el-button>
      </el-col>
      <right-toolbar
          v-model:showSearch="showSearch"
          @queryTable="getList"
          class="right-toolbar"
      />
    </el-row>

    <!-- 表格 -->
    <el-table
        border
        :data="gameList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        v-loading="loading"
        class="game-table"
        :row-class-name="tableRowClassName"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="coverUrl" label="封面" align="center" width="120">
        <template #default="scope">
          <div class="cover-container">
            <img
                :src="scope.row.coverUrl"
                alt="封面"
                class="game-cover"
                loading="lazy"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="游戏名称" min-width="100" class="table-column-highlight"></el-table-column>

      <el-table-column label="简介" min-width="140">
        <template #default="scope">
          <el-tooltip effect="dark" :content="scope.row.description" placement="top">
            <div class="description-wrapper">
              <span>{{
                  scope.row.description ? scope.row.description.slice(0, 20) + (scope.row.description.length > 20 ? '…' : '') : ''
                }}</span>
            </div>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column prop="isInstalled" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag
              :type="scope.row.isInstalled ? 'success' : 'info'"
              class="status-tag"
          >
            {{ scope.row.isInstalled ? '已安装' : '未安装' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="标签" min-width="110" align="center">
        <template #default="scope">
          <div class="tags-wrapper">
            <el-tag
                v-for="(tag, index) in scope.row.tags"
                :key="index"
                class="game-tag"
            >
              {{ tag }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="rating" label="评分" width="80" align="center">
        <template #default="scope">
          <div class="rating-star">
            <span class="star-icon">★</span>
            <span>{{ scope.row.rating }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="releaseDate" label="发行日期" width="120" height= "100" align="center">
        <template #default="scope">{{ formatDate(scope.row.releaseDate) }}</template>
      </el-table-column>

      <el-table-column prop="developer" label="开发商" min-width="120"></el-table-column>

      <el-table-column label="操作" width="200" align="center">
        <template #default="scope">
          <el-button
              type="primary"
              icon="Edit"
              link
              @click="openModel(scope.row)"
              class="operation-link edit-link"
          >
            编辑
          </el-button>
          <el-button
              type="danger"
              icon="Delete"
              link
              @click="handleDelete(scope.row.id)"
              class="operation-link delete-link"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
        v-if="count > 0"
        :total="count"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
        class="pagination-container"
    />

    <!-- 添加/编辑弹窗 -->
    <el-dialog
        :title="title"
        v-model="dialogVisible"
        width="600px"
        append-to-body
        class="game-dialog"
    >
      <el-form
          ref="currentGameRef"
          :model="currentGame"
          :rules="rules"
          label-width="100px"
          class="game-form"
      >
        <el-form-item label="游戏名称" prop="name" class="form-item">
          <el-input
              v-model="currentGame.name"
              placeholder="请输入游戏名称"
              class="form-input"
          />
        </el-form-item>

        <el-form-item label="封面URL" prop="coverUrl" class="form-item">
          <el-input
              v-model="currentGame.coverUrl"
              placeholder="请输入封面图片URL"
              class="form-input"
          />
        </el-form-item>

        <el-form-item label="截图URL" prop="screenshotUrl" class="form-item">
          <el-input
              v-model="screenshotUrl"
              placeholder="输入截图url按回车"
              @keyup.enter="addScreenshotUrl"
              class="form-input"
          />
          <div class="tag-container screenshot-container">
            <el-tag
                v-for="(url, index) in currentGame.screenshotUrl"
                :key="index"
                closable
                @close="removeScreenshotUrl(index)"
                class="screenshot-tag"
            >
              {{ url }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="简介" prop="description" class="form-item">
          <el-input
              type="textarea"
              v-model="currentGame.description"
              placeholder="请输入游戏简介"
              rows="4"
              class="form-textarea"
          />
        </el-form-item>

        <el-form-item label="状态" prop="isInstalled" class="form-item">
          <el-switch
              v-model="currentGame.isInstalled"
              active-text="已安装"
              inactive-text="未安装"
              class="status-switch"
          />
        </el-form-item>

        <el-form-item label="标签" prop="tags" class="form-item">
          <el-input
              v-model="tagInput"
              placeholder="输入标签按回车"
              @keyup.enter="addTag"
              class="form-input"
          />
          <div class="tag-container">
            <el-tag
                v-for="(tag, index) in currentGame.tags"
                :key="index"
                closable
                @close="removeTag(index)"
                class="form-tag"
            >
              {{ tag }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="评分" prop="rating" class="form-item">
          <el-input
              v-model="currentGame.rating"
              type="number"
              placeholder="请输入评分"
              class="form-input"
              min="0"
              max="10"
              step="0.1"
          />
        </el-form-item>

        <el-form-item label="发行日期" prop="releaseDate" class="form-item">
          <el-date-picker
              v-model="currentGame.releaseDate"
              type="date"
              placeholder="选择日期"
              class="form-datepicker"
          />
        </el-form-item>

        <el-form-item label="开发商" prop="developer" class="form-item">
          <el-input
              v-model="currentGame.developer"
              placeholder="请输入开发商"
              class="form-input"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button
              @click="dialogVisible=false"
              class="dialog-btn cancel-btn"
          >
            取消
          </el-button>
          <el-button
              type="primary"
              @click="submitForm"
              class="dialog-btn confirm-btn"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 原有脚本逻辑保持不变
import {ref, reactive, toRefs, onMounted} from "vue";
import {getGameList, addGame, updateGame, deleteGameBatch} from "@/api/game";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {formatDate} from "@/utils/date";
import {messageConfirm, notifySuccess} from "@/utils/modal";
import {PageQuery} from "@/model";

// 弹窗控制
const dialogVisible = ref(false);
const isEdit = ref(false);
const currentGameRef = ref<FormInstance>();

// 表单校验规则
const rules = reactive<FormRules>({
  name: [{required: true, message: "请输入游戏名称", trigger: "blur"}],
  coverUrl: [{required: true, message: "请输入封面URL", trigger: "blur"}]
});

// 标签处理
const tagInput = ref("");
const screenshotUrl = ref("")
const addTag = () => {
  if (tagInput.value.trim() && !currentGame.value.tags.includes(tagInput.value)) {
    currentGame.value.tags.push(tagInput.value.trim());
  }
  tagInput.value = "";
};
const removeTag = (index: number) => {
  currentGame.value.tags.splice(index, 1);
};

const addScreenshotUrl = () => {
  if (screenshotUrl.value.trim() && !currentGame.value.screenshotUrl.includes(screenshotUrl.value)) {
    currentGame.value.screenshotUrl.push(screenshotUrl.value.trim());
  }
  screenshotUrl.value = "";
};
const removeScreenshotUrl = (index: number) => {
  currentGame.value.screenshotUrl.splice(index, 1);
};
// 表格数据
const data = reactive({
  count: 0,
  showSearch: true,
  loading: false,
  title: "",
  queryParams: {
    current: 1,
    size: 10,
    keyword: "",
  },
  gameList: [] as any[],
  gameIdList: [] as number[],
});

const currentGame = ref<any>({
  id: 0,
  name: "",
  coverUrl: "",
  description: "",
  isInstalled: false,
  tags: [],
  rating: "",
  releaseDate: "",
  developer: "",
  screenshotUrl: [],
});
const {count, showSearch, loading, title, queryParams, gameList, gameIdList} = toRefs(data);

// 打开弹窗
const openModel = (game?: any) => {
  currentGameRef.value?.clearValidate();
  if (game) {
    currentGame.value = JSON.parse(JSON.stringify(game));
    title.value = "编辑游戏";
    isEdit.value = true;
  } else {
    title.value = "新增游戏";
    currentGame.value = {
      id: 0,
      name: "",
      coverUrl: "",
      description: "",
      isInstalled: false,
      tags: [],
      rating: "",
      releaseDate: "",
      developer: "",
      screenshotUrl: [],
    };
    isEdit.value = false;
  }
  dialogVisible.value = true;
};

// 表单提交
const submitForm = () => {
  currentGameRef.value?.validate(async (valid) => {
    if (!valid) return;
    const payload = {...currentGame.value, tags: currentGame.value.tags};
    if (isEdit.value) {
      const {data} = await updateGame(payload);
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
        dialogVisible.value = false;
      }
    } else {
      const {data} = await addGame(payload);
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
        dialogVisible.value = false;
      }
    }
  });
};

// 删除（单个）
const handleDelete = (id: number) => {
  messageConfirm("确认删除该游戏吗？").then(async () => {
    const {data} = await deleteGameBatch([id]);
    if (data.flag) {
      notifySuccess(data.msg);
      getList();
    }
  });
};

// 批量删除
const handleDeleteBatch = () => {
  if (gameIdList.value.length === 0) return;
  messageConfirm("确认删除选中的游戏吗？").then(async () => {
    const {data} = await deleteGameBatch(gameIdList.value);
    if (data.flag) {
      notifySuccess(data.msg);
      getList();
      gameIdList.value = [];
    }
  });
};

// 获取列表
const getList = async () => {
  loading.value = true;
  try {
    const res = await getGameList(queryParams.value);
    const result = res.data;
    gameList.value = result.data.recordList;
    count.value = result.data.count;
  } catch (error: any) {
    console.error("获取游戏列表失败:", error);
    ElMessage.error(error?.message || "获取游戏列表失败");
  } finally {
    loading.value = false;
  }
};


const handleSelectionChange = (selection: any[]) => {
  gameIdList.value = selection.map(item => item.id);
};

const handleQuery = () => {
  queryParams.value.current = 1;
  getList();
};

// 表格行样式
const tableRowClassName = ({rowIndex}: {rowIndex: number}) => {
  return rowIndex % 2 === 0 ? 'table-row-even' : 'table-row-odd';
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 搜索栏样式 */
.search-form {
  padding: 16px 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

.search-input {
  width: 280px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.search-input:focus {
  box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.2);
}

.search-btn {
  transition: all 0.2s ease;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 150, 255, 0.3);
}

/* 操作按钮区域 */
.operation-row {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.operation-btn {
  width: 100%;
  transition: all 0.2s ease;
}

.operation-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.add-btn {
  border-color: #4096ff;
  color: #4096ff;
}

.add-btn:hover {
  background-color: #f0f7ff;
}

.delete-btn {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.delete-btn:hover {
  background-color: #fff2f0;
}

.right-toolbar {
  margin-left: auto;
}

/* 表格样式 */
.game-table {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.game-table th {
  background-color: #fafafa;
  font-weight: 500;
  color: #4e5969;
}

.table-row-even {
  background-color: #fff;
}

.table-row-odd {
  background-color: #f9fafb;
}

.game-table tr:hover > td {
  background-color: #f0f7ff !important;
  transition: background-color 0.2s ease;
}

.table-column-highlight {
  color: #1890ff;
  font-weight: 500;
}

/* 封面样式 */
.cover-container {
  padding: 4px;
}

.game-cover {
  width: 60px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.game-cover:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

/* 描述样式 */
.description-wrapper {
  line-height: 1.5;
  color: #6b7280;
}

/* 状态标签 */
.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/* 标签样式 */
.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.game-tag {
  background-color: rgba(164, 199, 251, 0.56);
  color: rgba(16, 79, 170, 0.88);
  border: none;
  padding: 2px 8px;
  font-size: 12px;
}

/* 评分样式 */
.rating-star {
  color: #faad14;
  font-weight: 500;
}

.star-icon {
  margin-right: 4px;
}

/* 操作链接 */
.operation-link {
  transition: all 0.2s ease;
  padding: 0 6px;
}

.edit-link {
  color: #4096ff;
}

.edit-link:hover {
  color: #1890ff;
  background-color: rgba(64, 150, 255, 0.1);
}

.delete-link {
  color: #ff4d4f;
}

.delete-link:hover {
  color: #d93f30;
  background-color: rgba(255, 77, 79, 0.1);
}

/* 分页样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 弹窗样式 */
.game-dialog {
  --el-dialog-border-radius: 10px;
  --el-dialog-box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.game-dialog .el-dialog__header {
  padding: 18px 20px;
  border-bottom: 1px solid #f0f2f5;
}

.game-dialog .el-dialog__title {
  font-size: 18px;
  font-weight: 500;
  color: #1d2129;
}

.game-dialog .el-dialog__body {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

/* 表单样式 */
.game-form {
  margin-top: 10px;
}

.form-item {
  margin-bottom: 18px;
}

.form-item .el-form-item__label {
  font-weight: 500;
  color: #4e5969;
}

.form-input, .form-textarea, .form-datepicker {
  width: 100%;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.form-input:focus, .form-textarea:focus {
  box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.2);
}

.form-textarea {
  resize: vertical;
}

.status-switch {
  --el-switch-on-color: #52c41a;
  --el-switch-off-color: #d9d9d9;
}

/* 标签容器 */
.tag-container {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
  padding: 6px;
  background-color: rgba(249, 250, 251, 0);
  border-radius: 4px;
  min-height: 36px;
}


.screenshot-container::-webkit-scrollbar {
  width: 6px;
}

.screenshot-container::-webkit-scrollbar-thumb {
  background-color: rgba(221, 221, 221, 0);
  border-radius: 3px;
}

.form-tag, .screenshot-tag {
  background-color: rgba(230, 247, 255, 0);
  color: #1890ff;
  border-color: #91d5ff;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 弹窗底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid #f0f2f5;
}

.dialog-btn {
  padding: 6px 16px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.confirm-btn {
  background-color: #4096ff;
  border-color: #4096ff;
}

.confirm-btn:hover {
  background-color: #1890ff;
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(64, 150, 255, 0.3);
}

.cancel-btn:hover {
  background-color: #f5f5f5;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .app-container {
    padding: 12px;
  }

  .search-input {
    width: 100%;
    margin-bottom: 10px;
  }

  .operation-row {
    flex-wrap: wrap;
  }

  .operation-row .el-col {
    margin-bottom: 10px;
  }

  .game-table {
    font-size: 12px;
  }

  .game-cover {
    width: 50px;
    height: 70px;
  }

  .game-dialog {
    width: 95% !important;
  }
}
.el-form--inline .el-form-item {
  display: inline-flex;
  vertical-align: middle;
  margin-right: 32px;
  margin-bottom: 0;
}
</style>