<template>
  <div class="doc-management-container">
    <!-- 顶部操作栏 -->
    <div class="top-action-bar">
      <el-button type="primary" @click="handleAddDoc">
        <el-icon>
          <Plus/>
        </el-icon>
        新增文档
      </el-button>
      <el-button type="default" @click="refreshDocList">
        <el-icon>
          <Refresh/>
        </el-icon>
        刷新列表
      </el-button>
      <el-button
          type="danger"
          @click="handleBatchDelete"
          :disabled="!selectedDocIds.length || currentUser.role !== 'admin'"
      >
        <el-icon>
          <Delete/>
        </el-icon>
        批量删除
      </el-button>
      <!-- 回收站入口 -->
      <el-button type="text" @click="showRecycleBin = !showRecycleBin" class="recycle-btn">
        <el-icon>
          <Edit/>
        </el-icon>
        回收站（{{ recycleBin.length }}）
      </el-button>
    </div>

    <div class="main-content">
      <!-- 左侧：文档列表/回收站 -->
      <div class="left-panel">
        <div class="list-header">
          <h3>{{ showRecycleBin ? '文档回收站' : '文档管理列表' }}</h3>
          <!-- 筛选+搜索 -->
          <div class="filter-bar">
            <el-select
                v-model="filterStatus"
                placeholder="筛选状态"
                style="width: 160px; margin-right: 12px;"
                @change="filterDocs"
                v-if="!showRecycleBin"
            >
              <el-option label="全部状态" value=""></el-option>
              <el-option label="编辑中" value="editing"></el-option>
              <el-option label="已完成" value="completed"></el-option>
              <el-option label="已驳回" value="rejected"></el-option>
            </el-select>
            <el-input
                v-model="searchKeyword"
                placeholder="搜索文档标题"
                style="width: 220px;"
                @clear="filterDocs"
                @keyup.enter="filterDocs"
            >
              <template #append>
                <el-button icon="Search" @click="filterDocs"></el-button>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 文档列表表格 -->
        <el-table
            :data="filteredDocs"
            row-key="id"
            :selection-change="handleSelectionChange"
            @row-click="handleDocSelect"
            border
            style="width: 100%;"
        >
          <el-table-column type="selection" width="55" v-if="!showRecycleBin"></el-table-column>
          <el-table-column prop="title" label="文档标题" align="center" min-width="200">
            <template #default="scope">
              <span :class="{'doc-title-completed': scope.row.status === 'completed'}">
                {{ scope.row.title }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" align="center" width="90">
            <template #default="scope">
              <el-tag
                  :type="statusTagType[scope.row.status]"
                  :disable-transitions="false"
              >
                {{ statusMap[scope.row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" align="center" label="当前版本" width="90"></el-table-column>
          <el-table-column prop="creator" align="center" label="创建人" width="80"></el-table-column>
          <el-table-column label="操作" align="center" width="200">
            <template #default="scope">
              <el-button
                  type="primary"
                  size="small"
                  @click="handleDocEdit(scope.row)"
                  :disabled="showRecycleBin || (scope.row.status === 'completed' && currentUser.role !== 'admin' && scope.row.creator !== currentUser.name)"
              >
                编辑
              </el-button>
              <el-button
                  type="primary"
                  size="small"
                  @click="handleDocDelete(scope.row)"
                  :disabled="showRecycleBin && currentUser.role !== 'admin'"
                  :class="showRecycleBin ? 'text-success' : 'text-danger'"
              >
                {{ showRecycleBin ? '恢复' : '删除' }}
              </el-button>
              <el-button
                  type="primary"
                  size="small"
                  @click="handleVersionManage(scope.row)"
                  :disabled="showRecycleBin"
              >
                版本
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 右侧：详情操作面板 -->
      <div class="right-panel">
        <div class="panel-header">
          <h3>{{ selectedDoc ? (selectedDoc.title + ' - 详情') : '请选择文档' }}</h3>
          <el-button
              type="text"
              size="small"
              @click="selectedDoc = null"
              :disabled="!selectedDoc"
          >
            <el-icon>
              <Close/>
            </el-icon>
            关闭
          </el-button>
        </div>

        <div class="panel-content" v-if="selectedDoc">
          <!-- 1. 基础信息 -->
          <el-card shadow="hover" class="info-card">
            <template #header>
              <h4 class="card-title">基础信息</h4>
            </template>
            <el-form :model="selectedDoc" label-width="120px" class="info-form">
              <el-form-item label="文档标题">
                <el-input
                    v-model="selectedDoc.title"
                    :disabled="selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.creator !== currentUser.name"
                />
              </el-form-item>
              <el-form-item label="文章分类">
                <el-select
                    v-model="selectedDoc.category"
                    placeholder="选择分类"
                    :disabled="selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.creator !== currentUser.name"
                >
                  <el-option label="前端开发" value="frontend"></el-option>
                  <el-option label="后端架构" value="backend"></el-option>
                  <el-option label="算法与数据结构" value="algorithm"></el-option>
                  <el-option label="产品需求" value="product"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="文章标签">
                <el-tag
                    v-for="(tag, idx) in selectedDoc.tags"
                    :key="idx"
                    closable
                    @close="handleTagRemove(idx)"
                    :disabled="selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.creator !== currentUser.name"
                    style="margin-right: 8px;"
                >
                  {{ tag }}
                </el-tag>
                <el-button
                    type="text"
                    size="small"
                    @click="handleTagAdd"
                    :disabled="selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.creator !== currentUser.name"
                >
                  <el-icon>
                    <Plus/>
                  </el-icon>
                  添加
                </el-button>
              </el-form-item>
              <el-form-item label="创建人">
                <el-input v-model="selectedDoc.creator" disabled/>
              </el-form-item>
              <el-form-item label="当前状态">
                <el-tag :type="statusTagType[selectedDoc.status]">
                  {{ statusMap[selectedDoc.status] }}
                </el-tag>
              </el-form-item>
              <el-form-item label="当前版本">
                <el-input v-model="selectedDoc.version" disabled/>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 2. 状态操作 -->
          <el-card shadow="hover" class="status-card" style="margin-top: 16px;">
            <template #header>
              <h4 class="card-title">状态操作</h4>
            </template>
            <div class="status-actions">
              <!-- 普通用户操作 -->
              <template v-if="currentUser.role === 'user'">
                <el-button
                    type="primary"
                    @click="handleContinueEdit"
                    v-if="selectedDoc.status === 'editing'"
                >
                  <el-icon>
                    <Edit/>
                  </el-icon>
                  继续编辑
                </el-button>
                <el-button
                    type="success"
                    @click="handleApplyEdit"
                    v-if="selectedDoc.status === 'completed' && selectedDoc.creator === currentUser.name"
                >
                  <el-icon>
                    <Edit/>
                  </el-icon>
                  申请重新编辑
                </el-button>
                <el-button
                    type="default"
                    @click="handleSaveDraft"
                    v-if="selectedDoc.status === 'editing'"
                >
                  <el-icon>
                    <Save/>
                  </el-icon>
                  保存草稿
                </el-button>
              </template>

              <!-- 管理者操作 -->
              <template v-if="currentUser.role === 'admin'">
                <el-button
                    type="success"
                    @click="handlePublishDoc"
                    v-if="selectedDoc.status === 'editing'"
                >
                  <el-icon>
                    <Check/>
                  </el-icon>
                  审核发版
                </el-button>
                <el-button
                    type="danger"
                    @click="handleRejectDoc"
                    v-if="selectedDoc.status === 'editing'"
                >
                  <el-icon>
                    <Close/>
                  </el-icon>
                  驳回
                </el-button>
                <el-button
                    type="warning"
                    @click="handleForceEdit"
                    v-if="selectedDoc.status === 'completed'"
                >
                  <el-icon>
                    <Edit/>
                  </el-icon>
                  强制编辑
                </el-button>
              </template>

              <!-- 驳回状态操作 -->
              <el-button
                  type="primary"
                  @click="handleContinueEdit"
                  v-if="selectedDoc.status === 'rejected'"
              >
                <el-icon>
                  <Edit/>
                </el-icon>
                查看驳回原因并编辑
              </el-button>
            </div>

            <!-- 驳回原因显示 -->
            <div class="reject-reason" v-if="selectedDoc.rejectReason">
              <h5 class="reason-title">驳回原因：</h5>
              <p class="reason-content">{{ selectedDoc.rejectReason }}</p>
            </div>
          </el-card>

          <!-- 3. 版本记录 -->
          <el-card shadow="hover" class="version-card" style="margin-top: 16px;">
            <template #header>
              <h4 class="card-title">版本记录</h4>
            </template>
            <el-table
                :data="selectedDoc.versions"
                row-key="version"
                border
                style="width: 100%;"
            >
              <el-table-column prop="version" label="版本号" width="100"></el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="statusTagType[scope.row.status]">
                    {{ statusMap[scope.row.status] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="updater" label="更新人" width="120"></el-table-column>
              <el-table-column prop="updateTime" label="更新时间" width="180"></el-table-column>
              <el-table-column prop="description" label="更新说明" min-width="200"></el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="scope">
                  <el-button
                      type="text"
                      size="small"
                      @click="handleVersionCompare(scope.row)"
                      :disabled="selectedDoc.versions.length < 2"
                  >
                    对比
                  </el-button>
                  <el-button
                      type="text"
                      size="small"
                      @click="handleVersionRollback(scope.row)"
                      :disabled="scope.row.version === selectedDoc.version || currentUser.role !== 'admin'"
                  >
                    回滚
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 4. 内容编辑/预览 -->
          <el-card shadow="hover" class="content-card" style="margin-top: 16px;">
            <template #header>
              <h4 class="card-title">
                {{
                  (selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.creator !== currentUser.name)
                      ? '文档预览'
                      : '文档编辑'
                }}
              </h4>
            </template>
            <!-- 模拟富文本编辑器 -->
            <RichTextEditor
                v-model="selectedDoc.content"
                :readonly="selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.creator !== currentUser.name"
            />
          </el-card>
        </div>

        <!-- 未选择文档时的占位 -->
        <div class="empty-placeholder" v-else>
          <el-empty description="请在左侧列表选择一个文档进行管理"></el-empty>
        </div>
      </div>
    </div>

    <!-- 弹窗：审核发版 -->
    <el-dialog title="审核发版" v-model="publishDialogVisible" width="500px">
      <el-form :model="publishForm" label-width="120px">
        <el-form-item label="版本更新说明" required>
          <el-input
              type="textarea"
              v-model="publishForm.description"
              placeholder="请输入本次发版的更新说明（如：修复内容错误、新增章节等）"
              rows="3"
          />
        </el-form-item>
        <el-form-item label="版本类型">
          <el-radio-group v-model="publishForm.versionType">
            <el-radio label="minor">次版本更新（1.0 → 1.1）</el-radio>
            <el-radio label="major">主版本更新（1.1 → 2.0）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublishConfirm">确认发版</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：驳回文档 -->
    <el-dialog title="驳回文档" v-model="rejectDialogVisible" width="500px">
      <el-form :model="rejectForm" label-width="120px">
        <el-form-item label="驳回原因" required>
          <el-input
              type="textarea"
              v-model="rejectForm.reason"
              placeholder="请输入驳回原因，方便用户修改"
              rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRejectConfirm">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：申请重新编辑 -->
    <el-dialog title="申请重新编辑" v-model="applyEditDialogVisible" width="500px">
      <el-form :model="applyEditForm" label-width="120px">
        <el-form-item label="申请理由" required>
          <el-input
              type="textarea"
              v-model="applyEditForm.reason"
              placeholder="请说明重新编辑的原因"
              rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApplyEditConfirm">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：版本对比 -->
    <el-dialog title="版本对比" v-model="versionCompareDialogVisible" width="800px">
      <div class="version-compare-container">
        <div class="version-item">
          <h5>版本 {{ compareVersions.old.version }}（{{ statusMap[compareVersions.old.status] }}）</h5>
          <div class="version-content" v-html="formatCompareContent(compareVersions.old.content)"></div>
        </div>
        <div class="version-divider">
          <el-divider direction="vertical">vs</el-divider>
        </div>
        <div class="version-item">
          <h5>版本 {{ compareVersions.new.version }}（{{ statusMap[compareVersions.new.status] }}）</h5>
          <div class="version-content" v-html="formatCompareContent(compareVersions.new.content)"></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="versionCompareDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：添加标签 -->
    <el-dialog title="添加标签" v-model="addTagDialogVisible" width="300px">
      <el-form :model="addTagForm" label-width="80px">
        <el-form-item label="标签名称" required>
          <el-input
              v-model="addTagForm.tag"
              placeholder="请输入标签（最多5个字符）"
              maxlength="5"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddTagConfirm">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, computed} from 'vue';
import {ElMessage, ElEmpty, ElDivider} from 'element-plus';
import {
  Plus, Refresh, Delete, Close, Edit, Check
} from '@element-plus/icons-vue';

// 1. 类型定义
interface Version {
  version: string;
  status: 'editing' | 'completed' | 'rejected';
  updater: string;
  updateTime: string;
  description: string;
  content: string;
}

interface Document {
  id: number;
  title: string;
  status: 'editing' | 'completed' | 'rejected';
  version: string;
  creator: string;
  createTime: string;
  updateTime: string;
  category: 'frontend' | 'backend' | 'algorithm' | 'product';
  tags: string[];
  content: string;
  rejectReason?: string;
  versions: Version[];
}

interface User {
  name: string;
  role: 'admin' | 'user';
  avatar: string;
}

// 2. 模拟数据
const currentUser = ref<User>({
  name: '管理员', // 切换角色：'管理员'（admin） / '张三'（user）
  role: 'admin', // 切换角色：'admin' / 'user'
  avatar: 'https://picsum.photos/id/1/40/40'
});

// 初始文档列表
const documents = ref<Document[]>([
  {
    id: 1,
    title: 'Vue3 组合式API实战指南',
    status: 'completed',
    version: '1.2',
    creator: '张三',
    createTime: '2024-09-01 10:30:00',
    updateTime: '2024-09-15 16:45:00',
    category: 'frontend',
    tags: ['Vue3', '组合式API', '实战'],
    content: `<h2>Vue3 组合式API实战指南</h2>
              <p>1. 组合式API的核心优势：逻辑复用、类型安全</p>
              <p>2. setup函数的使用场景与注意事项</p>
              <p>3. 响应式API：ref、reactive、toRefs详解</p>
              <p>4. 实战案例：基于组合式API的表单组件封装</p>`,
    versions: [
      {
        version: '1.0',
        status: 'completed',
        updater: '张三',
        updateTime: '2024-09-01 11:30:00',
        description: '初始版本发布，包含组合式API基础内容',
        content: `<h2>Vue3 组合式API实战指南</h2>
                  <p>1. 组合式API的核心优势：逻辑复用、类型安全</p>
                  <p>2. setup函数的使用场景与注意事项</p>`
      },
      {
        version: '1.1',
        status: 'completed',
        updater: '张三',
        updateTime: '2024-09-08 14:20:00',
        description: '新增响应式API详解章节',
        content: `<h2>Vue3 组合式API实战指南</h2>
                  <p>1. 组合式API的核心优势：逻辑复用、类型安全</p>
                  <p>2. setup函数的使用场景与注意事项</p>
                  <p>3. 响应式API：ref、reactive、toRefs详解</p>`
      },
      {
        version: '1.2',
        status: 'completed',
        updater: '管理员',
        updateTime: '2024-09-15 16:45:00',
        description: '新增实战案例章节，优化排版',
        content: `<h2>Vue3 组合式API实战指南</h2>
                  <p>1. 组合式API的核心优势：逻辑复用、类型安全</p>
                  <p>2. setup函数的使用场景与注意事项</p>
                  <p>3. 响应式API：ref、reactive、toRefs详解</p>
                  <p>4. 实战案例：基于组合式API的表单组件封装</p>`
      }
    ]
  },
  {
    id: 2,
    title: 'SpringBoot 分布式事务解决方案',
    status: 'editing',
    version: '1.0',
    creator: '李四',
    createTime: '2024-09-10 09:15:00',
    updateTime: '2024-09-22 11:20:00',
    category: 'backend',
    tags: ['SpringBoot', '分布式事务', 'Seata'],
    content: `<h2>SpringBoot 分布式事务解决方案</h2>
              <p>1. 分布式事务的产生原因：跨服务数据一致性问题</p>
              <p>2. 常见解决方案对比：2PC、TCC、SAGA、本地消息表</p>
              <p>3. Seata 框架集成步骤（AT模式）：</p>
              <ul>
                <li>3.1 配置Seata Server</li>
                <li>3.2 微服务集成Seata依赖</li>
                <li>3.3 配置全局事务注解</li>
              </ul>`,
    rejectReason: '',
    versions: [
      {
        version: '1.0',
        status: 'editing',
        updater: '李四',
        updateTime: '2024-09-22 11:20:00',
        description: '初稿完成，待审核',
        content: `<h2>SpringBoot 分布式事务解决方案</h2>
                  <p>1. 分布式事务的产生原因：跨服务数据一致性问题</p>
                  <p>2. 常见解决方案对比：2PC、TCC、SAGA、本地消息表</p>
                  <p>3. Seata 框架集成步骤（AT模式）：</p>
                  <ul>
                    <li>3.1 配置Seata Server</li>
                    <li>3.2 微服务集成Seata依赖</li>
                    <li>3.3 配置全局事务注解</li>
                  </ul>`
      }
    ]
  },
  {
    id: 3,
    title: 'Redis 缓存穿透与雪崩解决方案',
    status: 'rejected',
    version: '1.0',
    creator: '王五',
    createTime: '2024-09-18 14:00:00',
    updateTime: '2024-09-20 09:30:00',
    category: 'backend',
    tags: ['Redis', '缓存', '性能优化'],
    content: `<h2>Redis 缓存穿透与雪崩解决方案</h2>
              <p>1. 缓存穿透定义：查询不存在的数据，导致请求直达数据库</p>
              <p>2. 解决方案：布隆过滤器、空值缓存</p>
              <p>3. 缓存雪崩定义：大量缓存同时过期，导致数据库压力骤增</p>
              <p>4. 解决方案：过期时间随机化、集群部署</p>`,
    rejectReason: '1. 布隆过滤器的实现原理未说明；2. 缺少缓存击穿的解决方案；3. 建议补充实战配置示例',
    versions: [
      {
        version: '1.0',
        status: 'rejected',
        updater: '王五',
        updateTime: '2024-09-20 09:30:00',
        description: '初稿完成，待审核',
        content: `<h2>Redis 缓存穿透与雪崩解决方案</h2>
                  <p>1. 缓存穿透定义：查询不存在的数据，导致请求直达数据库</p>
                  <p>2. 解决方案：布隆过滤器、空值缓存</p>
                  <p>3. 缓存雪崩定义：大量缓存同时过期，导致数据库压力骤增</p>
                  <p>4. 解决方案：过期时间随机化、集群部署</p>`
      }
    ]
  }
]);

// 回收站
const recycleBin = ref<Document[]>([]);

// 3. 响应式状态
const selectedDoc = ref<Document | null>(null);
const selectedDocIds = ref<number[]>([]);
const showRecycleBin = ref<boolean>(false);
const filterStatus = ref<string>('');
const searchKeyword = ref<string>('');

// 弹窗状态
const publishDialogVisible = ref<boolean>(false);
const rejectDialogVisible = ref<boolean>(false);
const applyEditDialogVisible = ref<boolean>(false);
const versionCompareDialogVisible = ref<boolean>(false);
const addTagDialogVisible = ref<boolean>(false);

// 弹窗表单
const publishForm = reactive({
  description: '',
  versionType: 'minor' // minor: 次版本, major: 主版本
});

const rejectForm = reactive({
  reason: ''
});

const applyEditForm = reactive({
  reason: ''
});

const addTagForm = reactive({
  tag: ''
});

// 版本对比临时数据
const compareVersions = ref({
  old: {} as Version,
  new: {} as Version
});

// 4. 计算属性
// 筛选后的文档列表
const filteredDocs = computed(() => {
  const list = showRecycleBin.value ? recycleBin.value : documents.value;
  return list.filter(doc => {
    // 状态筛选
    if (filterStatus.value && doc.status !== filterStatus.value) return false;
    // 关键词筛选（标题）
    if (searchKeyword.value && !doc.title.toLowerCase().includes(searchKeyword.value.toLowerCase())) return false;
    return true;
  });
});

// 状态映射
const statusMap = ref({
  editing: '编辑中',
  completed: '已完成',
  rejected: '已驳回'
});

// 状态标签类型
const statusTagType = ref({
  editing: 'info',
  completed: 'success',
  rejected: 'danger'
});

// 5. 核心方法
// 选择文档
const handleDocSelect = (doc: Document) => {
  selectedDoc.value = JSON.parse(JSON.stringify(doc)); // 深拷贝，避免直接修改原数据
};

// 表格选择事件
const handleSelectionChange = (vals: Document[]) => {
  selectedDocIds.value = vals.map(doc => doc.id);
};

// 刷新列表
const refreshDocList = () => {
  filterStatus.value = '';
  searchKeyword.value = '';
  selectedDoc.value = null;
  selectedDocIds.value = [];
  ElMessage.success('列表已刷新');
};

// 筛选文档
const filterDocs = () => {
  // 依赖computed自动更新
};

// 新增文档
const handleAddDoc = () => {
  const newDoc: Document = {
    id: documents.value.length + 1,
    title: '新建文档',
    status: 'editing',
    version: '1.0',
    creator: currentUser.value.name,
    createTime: new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).replace(/\//g, '-'),
    updateTime: new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).replace(/\//g, '-'),
    category: 'frontend',
    tags: [],
    content: '<h2>请输入文档内容</h2><p>可使用富文本编辑器编辑格式</p>',
    versions: [
      {
        version: '1.0',
        status: 'editing',
        updater: currentUser.value.name,
        updateTime: new Date().toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        }).replace(/\//g, '-'),
        description: '初始草稿',
        content: '<h2>请输入文档内容</h2><p>可使用富文本编辑器编辑格式</p>'
      }
    ]
  };
  documents.value.unshift(newDoc);
  handleDocSelect(newDoc);
  ElMessage.success('新建文档成功');
};

// 编辑文档（跳转到详情面板）
const handleDocEdit = (doc: Document) => {
  handleDocSelect(doc);
};

// 删除/恢复文档
const handleDocDelete = (doc: Document) => {
  if (showRecycleBin.value) {
    // 从回收站恢复
    const idx = recycleBin.value.findIndex(item => item.id === doc.id);
    if (idx > -1) {
      recycleBin.value.splice(idx, 1);
      documents.value.push(doc);
      ElMessage.success(`文档《${doc.title}》已恢复`);
    }
  } else {
    // 删除到回收站
    const idx = documents.value.findIndex(item => item.id === doc.id);
    if (idx > -1) {
      documents.value.splice(idx, 1);
      recycleBin.value.push(doc);
      if (selectedDoc.value?.id === doc.id) {
        selectedDoc.value = null;
      }
      ElMessage.success(`文档《${doc.title}》已移至回收站`);
    }
  }
};

// 批量删除
const handleBatchDelete = () => {
  if (selectedDocIds.value.length === 0) return;
  const docsToDelete = documents.value.filter(doc => selectedDocIds.value.includes(doc.id));
  documents.value = documents.value.filter(doc => !selectedDocIds.value.includes(doc.id));
  recycleBin.value.push(...docsToDelete);
  selectedDoc.value = null;
  selectedDocIds.value = [];
  ElMessage.success(`已批量删除 ${docsToDelete.length} 个文档`);
};

// 版本管理（跳转到详情面板的版本记录）
const handleVersionManage = (doc: Document) => {
  handleDocSelect(doc);
  // 滚动到版本记录区域（实际项目可加锚点）
  ElMessage.info('已跳转到版本管理');
};

// 保存草稿
const handleSaveDraft = () => {
  if (!selectedDoc.value) return;
  // 更新文档内容和时间
  selectedDoc.value.content = selectedDoc.value.content;
  selectedDoc.value.updateTime = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '-');
  // 更新最新版本的内容
  const latestVersion = selectedDoc.value.versions[selectedDoc.value.versions.length - 1];
  latestVersion.content = selectedDoc.value.content;
  latestVersion.updateTime = selectedDoc.value.updateTime;
  latestVersion.description = '草稿保存';
  // 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
  }
  ElMessage.success('草稿已保存');
};

// 继续编辑（仅更新状态提示）
const handleContinueEdit = () => {
  ElMessage.info('可直接在下方编辑器修改内容，修改后请保存草稿');
};

// 申请重新编辑
const handleApplyEdit = () => {
  applyEditDialogVisible.value = true;
};

// 提交重新编辑申请
const handleApplyEditConfirm = () => {
  if (!applyEditForm.reason.trim()) {
    ElMessage.warning('请输入申请理由');
    return;
  }
  if (!selectedDoc.value) return;
  // 状态改为编辑中
  selectedDoc.value.status = 'editing';
  // 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx].status = 'editing';
  }
  applyEditDialogVisible.value = false;
  applyEditForm.reason = '';
  ElMessage.success('重新编辑申请已提交，当前文档状态已改为“编辑中”');
};

// 强制编辑（管理者）
const handleForceEdit = () => {
  if (!selectedDoc.value) return;
  selectedDoc.value.status = 'editing';
  // 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx].status = 'editing';
  }
  ElMessage.success('已强制将文档状态改为“编辑中”，可直接编辑');
};

// 审核发版
const handlePublishDoc = () => {
  if (!selectedDoc.value) return;
  publishForm.description = '';
  publishForm.versionType = 'minor';
  publishDialogVisible.value = true;
};

// 确认发版
const handlePublishConfirm = () => {
  if (!publishForm.description.trim()) {
    ElMessage.warning('请输入版本更新说明');
    return;
  }
  if (!selectedDoc.value) return;

  // 1. 更新版本号
  const [major, minor] = selectedDoc.value.version.split('.').map(Number);
  let newVersion = '';
  if (publishForm.versionType === 'major') {
    newVersion = `${major + 1}.0`;
  } else {
    newVersion = `${major}.${minor + 1}`;
  }

  // 2. 添加新版本记录
  const newVersionItem: Version = {
    version: newVersion,
    status: 'completed',
    updater: currentUser.value.name,
    updateTime: new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).replace(/\//g, '-'),
    description: publishForm.description,
    content: selectedDoc.value.content
  };
  selectedDoc.value.versions.push(newVersionItem);

  // 3. 更新文档状态和版本
  selectedDoc.value.status = 'completed';
  selectedDoc.value.version = newVersion;
  selectedDoc.value.updateTime = newVersionItem.updateTime;
  selectedDoc.value.rejectReason = '';

  // 4. 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
  }

  publishDialogVisible.value = false;
  ElMessage.success(`文档发版成功，新版本号：${newVersion}`);
};

// 驳回文档
const handleRejectDoc = () => {
  if (!selectedDoc.value) return;
  rejectForm.reason = '';
  rejectDialogVisible.value = true;
};

// 确认驳回
const handleRejectConfirm = () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入驳回原因');
    return;
  }
  if (!selectedDoc.value) return;

  // 1. 更新文档状态和驳回原因
  selectedDoc.value.status = 'rejected';
  selectedDoc.value.rejectReason = rejectForm.reason;
  selectedDoc.value.updateTime = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '-');

  // 2. 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
  }

  rejectDialogVisible.value = false;
  rejectForm.reason = '';
  ElMessage.success('文档已驳回，已通知用户');
};

// 版本对比
const handleVersionCompare = (version: Version) => {
  if (!selectedDoc.value) return;
  // 取最新版本和当前选中版本对比
  const latestVersion = selectedDoc.value.versions[selectedDoc.value.versions.length - 1];
  compareVersions.value = {
    old: version,
    new: latestVersion
  };
  versionCompareDialogVisible.value = true;
};

// 格式化对比内容（模拟差异标记）
const formatCompareContent = (content: string) => {
  // 实际项目可集成diff库（如diff-match-patch），这里简化模拟
  return content
      .replace(/<p>/g, '<p class="compare-paragraph">')
      .replace(/新增/g, '<span class="add-diff">新增</span>')
      .replace(/优化/g, '<span class="update-diff">优化</span>')
      .replace(/删除/g, '<span class="delete-diff">删除</span>');
};

// 版本回滚
const handleVersionRollback = (version: Version) => {
  if (!selectedDoc.value || !confirm(`确定要回滚到版本 ${version.version} 吗？回滚后当前内容将被覆盖`)) {
    return;
  }

  // 1. 恢复内容
  selectedDoc.value.content = version.content;
  selectedDoc.value.updateTime = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '-');

  // 2. 添加回滚版本记录
  const [major, minor] = selectedDoc.value.version.split('.').map(Number);
  const newVersion = `${major}.${minor + 1}`;
  const newVersionItem: Version = {
    version: newVersion,
    status: 'editing',
    updater: currentUser.value.name,
    updateTime: selectedDoc.value.updateTime,
    description: `回滚到版本 ${version.version}`,
    content: version.content
  };
  selectedDoc.value.versions.push(newVersionItem);
  selectedDoc.value.version = newVersion;
  selectedDoc.value.status = 'editing';

  // 3. 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
  }

  ElMessage.success(`已回滚到版本 ${version.version}，新版本号：${newVersion}`);
};

// 添加标签
const handleTagAdd = () => {
  addTagForm.tag = '';
  addTagDialogVisible.value = true;
};

// 确认添加标签
const handleAddTagConfirm = () => {
  if (!addTagForm.tag.trim()) {
    ElMessage.warning('请输入标签名称');
    return;
  }
  if (!selectedDoc.value) return;
  if (selectedDoc.value.tags.includes(addTagForm.tag.trim())) {
    ElMessage.warning('该标签已存在');
    return;
  }
  selectedDoc.value.tags.push(addTagForm.tag.trim());
  // 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx].tags = [...selectedDoc.value.tags];
  }
  addTagDialogVisible.value = false;
  addTagForm.tag = '';
  ElMessage.success('标签添加成功');
};

// 删除标签
const handleTagRemove = (idx: number) => {
  if (!selectedDoc.value) return;
  selectedDoc.value.tags.splice(idx, 1);
  // 同步到原文档列表
  const docIdx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (docIdx > -1) {
    documents.value[docIdx].tags = [...selectedDoc.value.tags];
  }
  ElMessage.success('标签已删除');
};

// 6. 模拟富文本编辑器组件（实际项目可替换为真实组件如Tinymce/Vditor）
const RichTextEditor = (props: { modelValue: string; readonly: boolean }) => {
};
</script>

<style scoped>
/* 整体容器 */
.doc-management-container {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 16px;
}

/* 顶部操作栏 */
.top-action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 8px 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.recycle-btn {
  margin-left: auto;
  color: #86909c;
}

/* 主内容区 */
.main-content {
  display: flex;
  gap: 16px;
  height: calc(100vh - 100px);
}

/* 左侧面板 */
.left-panel {
  width: 40%;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.list-header {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.list-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1d2129;
}

.filter-bar {
  display: flex;
  align-items: center;
}

.doc-title-completed {
  color: #86909c;
  text-decoration: line-through;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.panel-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.panel-content {
  padding: 16px;
  overflow-y: auto;
  flex: 1;
}

.empty-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

/* 卡片样式 */
.info-card, .status-card, .version-card, .content-card {
  margin-bottom: 16px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.info-form {
  margin-top: 16px;
}

/* 状态操作区 */
.status-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.reject-reason {
  margin-top: 16px;
  padding: 12px;
  background-color: #fff8f0;
  border-radius: 6px;
  border-left: 4px solid #faad14;
}

.reason-title {
  font-size: 14px;
  font-weight: 600;
  color: #faad14;
  margin-bottom: 8px;
}

.reason-content {
  font-size: 13px;
  color: #737373;
}

/* 富文本编辑器 */
.rich-text-editor {
  min-height: 300px;
  padding: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  outline: none;
  line-height: 1.8;
}

.editor-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.01);
  pointer-events: none;
}

/* 版本对比 */
.version-compare-container {
  display: flex;
  gap: 16px;
  height: 400px;
  overflow-y: auto;
}

.version-item {
  flex: 1;
  padding: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
}

.version-divider {
  display: flex;
  align-items: center;
  justify-content: center;
}

.version-content {
  margin-top: 12px;
  line-height: 1.8;
  color: #1d2129;
}

.compare-paragraph {
  margin-bottom: 12px;
}

.add-diff {
  color: #52c41a;
  background-color: #f6ffed;
  padding: 2px 4px;
  border-radius: 4px;
}

.update-diff {
  color: #1890ff;
  background-color: #e6f7ff;
  padding: 2px 4px;
  border-radius: 4px;
}

.delete-diff {
  color: #f5222d;
  background-color: #fff2f0;
  padding: 2px 4px;
  border-radius: 4px;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
    height: auto;
  }

  .left-panel {
    width: 100%;
    height: 400px;
  }

  .right-panel {
    height: 600px;
  }
}
</style>