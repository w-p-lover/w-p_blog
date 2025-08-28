<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form @submit.native.prevent :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="书名">
        <el-input v-model="queryParams.keyword" placeholder="请输入书名" clearable @keyup.enter="handleQuery"/>
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
        <el-button type="danger" plain :disabled="bookIdList.length === 0" icon="Delete" @click="handleDeleteBatch">
          批量删除
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table border :data="bookList" style="width: 100%" @selection-change="handleSelectionChange" v-loading="loading">
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="cover" label="封面" align="center" width="180">
        <template #default="scope">
          <img :src="scope.row.cover" alt="封面" style="width: 60px; height: 90px; object-fit: cover;"/>
        </template>
      </el-table-column>
      <el-table-column prop="briefImg" label="浮窗" align="center" width="180">
        <template #default="scope">
          <img :src="scope.row.briefImg" alt="浮窗" style="width: 60px; height: 90px; object-fit: cover;"/>
        </template>
      </el-table-column>

      <el-table-column prop="title" label="书名" min-width="90" ></el-table-column>
      <el-table-column prop="author" label="作者" min-width="60"></el-table-column>
      <el-table-column label="简介" min-width="80">
        <template #default="scope">
          <el-tooltip class="item" effect="dark" :content="scope.row.brief" placement="top">
            <span style="font-weight: bold">
              {{ scope.row.brief ? scope.row.brief.slice(0, 9) + (scope.row.brief.length > 9 ? '…' : '') : '' }}
            </span>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag
              :type="getStatusType(scope.row.status)"
              size="large"
          >
            {{ statusMap[scope.row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="130" align="center">
        <template #default="scope">
          <el-tag
              v-for="(tag, index) in (scope.row.tags ? scope.row.tags.split('，') : [])"
              :key="index"
              class="mr-2 mb-2"
              size="default"
          >
            {{ tag }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="书源数量" width="100" align="center">
        <template #default="scope">
          {{ scope.row.resource.length }}
        </template>
      </el-table-column>
      <el-table-column prop="addTime" label="添加时间" width="160">
        <template #default="scope">{{ formatDate(scope.row.addTime) }}</template>
      </el-table-column>
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
      <el-form ref="currentBookRef" :model="currentBook" :rules="rules" label-width="100px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="currentBook.title" placeholder="请输入书名"/>
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="currentBook.author" placeholder="请输入作者"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="currentBook.status" placeholder="请选择状态">
            <el-option label="想读" value="wish"></el-option>
            <el-option label="在读" value="reading"></el-option>
            <el-option label="已读" value="read"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="封面" prop="cover">
          <el-input v-model="currentBook.cover" placeholder="请输入封面图片URL"/>
        </el-form-item>
        <el-form-item label="浮窗图片" prop="cover">
          <el-input v-model="currentBook.briefImg" placeholder="请输入浮窗图片URL"/>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="currentBook.tags" placeholder="请输入标签(逗号分隔)"/>
        </el-form-item>
        <el-form-item label="简介" prop="brief">
          <el-input type="textarea" v-model="currentBook.brief" placeholder="请输入简介"/>
        </el-form-item>

        <div class="book-sources" v-if="isEdit">
          <div class="sources-add">
            <h3>书源链接</h3>
            <el-button type="primary" size="default" @click="addResource" style="align-content:center">
              添加书源
            </el-button>
          </div>
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
      </el-form>
      <template #footer>
        <el-button @click=" dialogVisible= false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, toRefs, onMounted} from "vue";
import {getBookList, addBook, updateBook, updateResource, deleteBookBatch} from "@/api/book";
import {BookVO} from "@/api/book/types";
import {notifySuccess, messageConfirm} from "@/utils/modal";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {formatDate} from "@/utils/date";

// 状态映射
const statusMap = {
  wish: "想读",
  reading: "在读",
  read: "已读"
};

const dialogVisible = ref(false);
const isEdit = ref(false);
const getStatusType = (status: string) => {
  return status === "wish" ? "danger" : status === "reading" ? "warning" : "success";
};

const currentBookRef = ref<FormInstance>();
const rules = reactive<FormRules>({
  title: [{required: true, message: "请输入书名", trigger: "blur"}],
  author: [{required: true, message: "请输入作者", trigger: "blur"}],
  cover: [{required: true, message: "请输入封面URL", trigger: "blur"}]
});

const data = reactive({
  count: 0,
  showSearch: true,
  loading: false,
  title: "",
  addFlag: false,
  UpdateFlag: false,
  queryParams: {current: 1, size: 10, keyword: ""},
  bookList: [] as BookVO[],
  bookIdList: [] as number[]
});
const currentBook = ref<BookVO | null>(null);
const {count, showSearch, loading, title, addFlag, UpdateFlag, queryParams, bookList, bookIdList} = toRefs(data);

const handleSelectionChange = (selection: BookVO[]) => {
  bookIdList.value = selection.map((item) => item.id);
};

const openModel = (book?: BookVO) => {
  currentBookRef.value?.clearValidate();
  if (book) {
    currentBook.value = JSON.parse(JSON.stringify(book));
    if (!Array.isArray(currentBook.value!.resource)) {
      currentBook.value!.resource = [];
    }
    title.value = "编辑书籍";
    isEdit.value = true;
  } else {
    title.value = "新增书籍";
    currentBook.value = {
      addTime: formatDate(new Date()),
      id: 0,
      title: "",
      author: "",
      status: "wish",
      cover: "",
      tags: "",
      brief: "",
      briefImg: "",
      resource: []
    };
    isEdit.value = false;
  }
  dialogVisible.value = true;
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
// 删除书源
const removeResource = (index: number) => {
  if (!currentBook.value) return
  currentBook.value.resource.splice(index, 1);
};

const openLink = (url: string) => {
  console.log(url);
  if (!url) return;
  window.open(url, "_blank"); // 新窗口打开
};

const submitForm = () => {
  currentBookRef.value?.validate((valid) => {
    if (!currentBook.value) return;

    if (valid) {
      const payload = {
        ...currentBook.value,
        resource: JSON.stringify(currentBook.value.resource || [])
      };

      if (currentBook.value.id) {
        updateBook(payload).then(({data}) => {
          if (data.flag) {
            notifySuccess(data.msg);
            getList();
          }
          UpdateFlag.value = false;
        });
      } else {
        addBook(payload).then(({data}) => {
          if (data.flag) {
            notifySuccess(data.msg);
            getList();
          }
          addFlag.value = false;
        });
      }
    }
  });
};

// 单个删除书籍（适配批量接口）
const handleDelete = (id: number) => {
  messageConfirm("确认删除该书籍吗？").then(async () => {
    try {
      const {data} = await deleteBookBatch([id]); // 调用批量接口，传单元素数组
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
        const index = bookIdList.value.indexOf(id);
        if (index !== -1) bookIdList.value.splice(index, 1);
      } else {
        ElMessage.error(data.msg || "删除失败");
      }
    } catch (err) {
      console.error(err);
      ElMessage.error("删除请求失败");
    }
  });
};


// 批量删除书籍
const handleDeleteBatch = () => {
  if (bookIdList.value.length === 0) return;

  messageConfirm("确认删除选中的书籍吗？").then(async () => {
    try {
      const {data} = await deleteBookBatch(bookIdList.value); // 假设后端接口名为 deleteBookBatch
      if (data.flag) {
        notifySuccess(data.msg);
        getList();
        bookIdList.value = []; // 清空选中列表
      } else {
        ElMessage.error(data.msg || "删除失败");
      }
    } catch (err) {
      console.error(err);
      ElMessage.error("删除请求失败");
    }
  });
};


// 获取书籍列表
const getList = () => {
  loading.value = true;
  getBookList(queryParams.value).then(({data}) => {
    bookList.value = data.data.recordList.map((item: BookVO) => {
      return {
        ...item,
        resource: parseResource(item.resource)
      };
    });
    count.value = data.data.count;
    loading.value = false;
  });
};

// 封装解析函数
const parseResource = (val: any): any[] => {
  if (!val) return [];
  if (Array.isArray(val)) return val;
  try {
    const parsed = JSON.parse(val);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
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
.resource-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.book-sources {
  margin-top: 25px;
  padding-left: 25px;
  background: linear-gradient(180deg, #fff, #fbfdff);
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(37, 51, 73, 0.04);
}

.sources-add {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 40px;
  margin-bottom: 10px;
  margin-top: -20px;
}

:deep(.el-empty) {
  --el-empty-padding: 0px 0;
}

:deep(.el-empty__image svg) {
  width: 70%;
}

:deep(.el-overlay-dialog) {
  position: fixed;
  top: -101px;
  right: 0;
  bottom: 0;
  left: 0;
  overflow: auto;;
}
.mr-2 {
  margin-right: 4px;
}
.mb-2 {
  margin-bottom: 4px;
}
</style>
