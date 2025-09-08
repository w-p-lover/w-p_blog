<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form @submit.native.prevent :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="游戏名">
        <el-input v-model="queryParams.keyword" placeholder="请输入游戏名" clearable @keyup.enter="handleQuery"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb15">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openModel()">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain :disabled="gameIdList.length === 0" icon="Delete" @click="handleDeleteBatch">
          批量删除
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table border :data="gameList" style="width: 100%" @selection-change="handleSelectionChange" v-loading="loading">
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="coverUrl" label="封面" align="center" width="120">
        <template #default="scope">
          <img :src="scope.row.coverUrl" alt="封面" style="width: 60px; height: 80px; object-fit: cover;"/>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="游戏名称" min-width="100"></el-table-column>
      <el-table-column label="简介" min-width="140">
        <template #default="scope">
          <el-tooltip effect="dark" :content="scope.row.description" placement="top">
            <span>{{
                scope.row.description ? scope.row.description.slice(0, 20) + (scope.row.description.length > 20 ? '…' : '') : ''
              }}</span>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column prop="isInstalled" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isInstalled ? 'success' : 'info'">
            {{ scope.row.isInstalled ? '已安装' : '未安装' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="标签" min-width="110" align="center">
        <template #default="scope">
          <el-tag v-for="(tag, index) in scope.row.tags" :key="index" class="mr-2 mb-2">{{ tag }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="rating" label="评分" width="80" align="center"></el-table-column>
      <el-table-column prop="releaseDate" label="发行日期" width="120" align="center">
        <template #default="scope">{{ formatDate(scope.row.releaseDate) }}</template>
      </el-table-column>
      <el-table-column prop="developer" label="开发商" min-width="120"></el-table-column>

      <el-table-column label="操作" width="200" align="center">
        <template #default="scope">
          <el-button type="primary" icon="Edit" link @click="openModel(scope.row)">编辑</el-button>
          <el-button type="danger" icon="Delete" link @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-if="count > 0" :total="count" v-model:page="queryParams.current" v-model:limit="queryParams.size"
                @pagination="getList"/>

    <!-- 添加/编辑弹窗 -->
    <el-dialog :title="title" v-model="dialogVisible" width="600px" append-to-body>
      <el-form ref="currentGameRef" :model="currentGame" :rules="rules" label-width="100px">
        <el-form-item label="游戏名称" prop="name">
          <el-input v-model="currentGame.name" placeholder="请输入游戏名称"/>
        </el-form-item>
        <el-form-item label="封面URL" prop="coverUrl">
          <el-input v-model="currentGame.coverUrl" placeholder="请输入封面图片URL"/>
        </el-form-item>
        <el-form-item label="截图URL" prop="screenshotUrl">
          <el-input v-model="screenshotUrl" placeholder="输入截图url按回车" @keyup.enter="addScreenshotUrl"/>
          <div class="tag-container">
            <el-tag v-for="(url, index) in currentGame.screenshotUrl" :key="index" closable @close="removeScreenshotUrl(index)">
              {{ url }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input type="textarea" v-model="currentGame.description" placeholder="请输入游戏简介"/>
        </el-form-item>
        <el-form-item label="状态" prop="isInstalled">
          <el-switch v-model="currentGame.isInstalled" active-text="已安装" inactive-text="未安装"/>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="tagInput" placeholder="输入标签按回车" @keyup.enter="addTag"/>
          <div class="tag-container">
            <el-tag v-for="(tag, index) in currentGame.tags" :key="index" closable @close="removeTag(index)">
              {{ tag }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="评分" prop="rating">
          <el-input v-model="currentGame.rating" type="number" placeholder="请输入评分"/>
        </el-form-item>
        <el-form-item label="发行日期" prop="releaseDate">
          <el-date-picker v-model="currentGame.releaseDate" type="date" placeholder="选择日期"/>
        </el-form-item>
        <el-form-item label="开发商" prop="developer">
          <el-input v-model="currentGame.developer" placeholder="请输入开发商"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
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

onMounted(() => {
  getList();
});
</script>

<style scoped>
.mr-2 {
  margin-right: 4px;
}

.mb-2 {
  margin-bottom: 4px;
}

.tag-container {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}
</style>
