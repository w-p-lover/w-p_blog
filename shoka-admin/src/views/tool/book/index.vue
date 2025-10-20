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
      <el-form-item label="书名" class="form-item">
        <el-input
            v-model="queryParams.keyword"
            placeholder="请输入书名"
            clearable
            @keyup.enter="handleQuery"
            class="search-input"
        />
      </el-form-item>
      <el-form-item class="form-item">
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
    <el-row :gutter="10" class="mb15">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openModel()">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Plus"
                   @click="handleRunSpider">书源脚本
        </el-button>
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

      <el-table-column prop="title" label="书名" min-width="90"></el-table-column>
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
          <div class="source-count">
            <el-badge :value="scope.row.resource.length" class="source-badge"/>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="addTime" label="添加时间" width="160" align="center">
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
            <el-button type="primary" size="default" @click="addResource" style="align-content :center">
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
    <div v-if="spider" class="spider-status-container">
      <!-- 状态图标：根据状态显示不同图标，增强直观性 -->
      <div class="spider-status-icon" :class="statusIconClass">
        <i v-if="spider === 'RUNNING'" class="el-icon-loading"></i>
        <i v-else-if="spider === 'COMPLETED'" class="el-icon-circle-check"></i>
        <i v-else-if="spider === 'FAILED'" class="el-icon-circle-exclamation"></i>
        <i v-else class="el-icon-spider"></i> <!-- 未开始用爬虫图标 -->
      </div>

      <!-- 文字区域：拆分主状态和详情，优化排版层次 -->
      <div class="spider-status-text">
        <h4 class="status-title" :class="statusTextClass">{{ spiderMessage }}</h4>
      </div>

      <!-- 进度条：优化尺寸、颜色渐变，增加过渡动画 -->
      <div class="spider-progress-wrapper">
        <el-progress
            type="line"
            :percentage="spiderPercentage"
            :status="statusProgressClass"
            :color="progressColor"
            :stroke-width="12"
            :gap-degree="30"
        ></el-progress>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, toRefs, onMounted, computed} from "vue";
import {getBookList, addBook, updateBook, updateResource, deleteBookBatch, searchBook} from "@/api/book";
import {BookVO} from "@/api/book/types";
import {notifySuccess, messageConfirm} from "@/utils/modal";
import {ElMessage, ElNotification, FormInstance, FormRules} from "element-plus";
import {formatDate} from "@/utils/date";
import axios from "axios";

const spiderMessage = ref('');
const status = ref('');
const spiderPercentage = ref(0.0);
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
  spider: false,
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
const {
  count,
  showSearch,
  loading,
  title,
  addFlag,
  UpdateFlag,
  queryParams,
  bookList,
  bookIdList,
  spider
} = toRefs(data);

const statusProgressClass = computed(() => {
  if (status.value === 'COMPLETED') return 'success';
  if (status.value === 'FAILED') return 'exception';
  return '';
});

const progressColor = computed(() => {
  // 进度条渐变色：运行中用蓝紫渐变，成功用绿蓝渐变，失败用红橙渐变
  if (status.value === 'RUNNING') return 'linear-gradient(to right, #4096ff, #6772e5)';
  if (status.value === 'COMPLETED') return 'linear-gradient(to right, #67c23a, #52c41a)';
  if (status.value === 'FAILED') return 'linear-gradient(to right, #f56c6c, #fa8c16)';
  return '#4096ff';
});

const statusIconClass = computed(() => {
  const base = 'spider-icon';
  if (status.value === 'RUNNING') return `${base} icon-loading`;
  if (status.value === 'COMPLETED') return `${base} icon-success`;
  if (status.value === 'FAILED') return `${base} icon-failed`;
  return `${base} icon-default`;
});

const statusTextClass = computed(() => {
  if (status.value === 'COMPLETED') return 'text-success';
  if (status.value === 'FAILED') return 'text-failed';
  return 'text-default';
});

const handleRunSpider = async () => {
  if (spider.value) return;
  spider.value = true;

  try {
    // 1️⃣ 启动爬虫任务
    await axios.post('http://localhost:8080/book/run');

    // 启动通知
    showSpiderNotification(
        'info',
        `<div style="text-align: left; line-height: 1.6; margin-left: 40px">
          <strong>🚀 爬虫任务已启动</strong><br>
          📝 书源网站: <span style="color:#409EFF;">ZXCS</span><br>
      </div>`
    );

    const startTime = Date.now();

    // 2️⃣ 定义轮询逻辑
    const pollStatus = async () => {
      try {
        const {data} = await axios.get('http://localhost:8080/book/status');
        status.value = data.data.status?.trim()?.toUpperCase();
        spiderPercentage.value = data.data.spiderPercentage;
        spiderMessage.value = data.data.message || '';
        console.log('爬虫状态:', status.value);

        // 3️⃣ 判断状态
        const elapsed = Date.now() - startTime;
        if (['COMPLETED', 'FAILED'].includes(status.value) || elapsed > 480000) {
          spider.value = false;

          if (status.value === 'COMPLETED') {
            showSpiderNotification('success', '✅ 爬虫任务完成，开始下载结果...');
            setTimeout(() => {
              window.location.href = 'http://localhost:8080/book/download';
            }, 1000);

          } else if (status.value === 'FAILED') {
            showSpiderNotification('error', '❌ 爬虫任务失败！');
          } else {
            showSpiderNotification('warning', '⚠️ 爬虫任务超时！');
          }

        } else {
          // 每次轮询间隔 10 秒
          setTimeout(pollStatus, 10000);
        }

      } catch (err) {
        console.error('轮询失败:', err);
        spider.value = false;
        showSpiderNotification('error', '轮询失败，请重试！');
      }
    };

    // 5️⃣ 启动第一次轮询
    await pollStatus();

  } catch (err) {
    console.error('启动爬虫失败:', err);
    spider.value = false;
    showSpiderNotification('error', '启动爬虫失败！');
  }
};


const showSpiderNotification = (type: 'success' | 'error' | 'warning' | 'info', message: string) => {
  ElNotification({
    title: '爬虫状态',
    message,
    type,
    duration: 5000, // 显示 5 秒后自动关闭
    offset: 60,     // 距离顶部 60px
    dangerouslyUseHTMLString: true
  })
}

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


// 获取书籍列表
const searchList = () => {
  loading.value = true;
  searchBook(queryParams.value.keyword).then(({data}) => {
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
  searchList();
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

.resource-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

/* 进度条容器：控制尺寸，增加内边距 */
.spider-progress-wrapper {
  width: 100%;
  max-width: 280px; /* 缩小仪表盘最大宽度，更精致 */
}

.book-sources {
  margin-top: 25px;
  padding-left: 25px;
  background: linear-gradient(180deg, #fff, #fbfdff);
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(37, 51, 73, 0.04);
}

/* 文字区域：优化字体层级、间距、颜色 */
.spider-status-text {
  text-align: center;
  margin-bottom: 20px;
}

.text-default {
  color: #678ad1;
}

.text-success {
  color: #88c169;
}

.text-failed {
  color: #c15858;
}

.sources-add {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20px;
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

.spider-status-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 1px solid #f0f2f5;
  border-radius: 16px;
  padding: 28px 32px;
  margin-top: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.spider-status-container:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.search-form {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
  gap: 30px;
}

.form-item {
  margin: 0 !important;
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

.source-count {
  display: flex;
  justify-content: center;
}

.source-badge {
  --el-badge-font-size: 14px;
  --el-badge-background-color: #4096ff;
}
</style>
