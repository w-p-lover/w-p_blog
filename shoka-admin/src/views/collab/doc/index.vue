<template>
  <div class="doc-management-container">
    <!-- 顶部操作栏：增加渐变主按钮、间距优化 -->
    <div class="top-action-bar">
      <el-button type="primary" @click="handleAddDoc" class="btn-primary-gradient">
        <el-icon class="icon-mr">
          <Plus/>
        </el-icon>
        新增文档
      </el-button>
      <el-button type="info" @click="refreshDocList" class="btn-secondary">
        <el-icon class="icon-mr">
          <Refresh/>
        </el-icon>
        刷新列表
      </el-button>
      <el-button
          type="danger"
          @click="handleBatchDelete"
          :disabled="!selectedDocIds.length || currentUser.role !== 'admin'"
          class="btn-secondary"
      >
        <el-icon class="icon-mr">
          <Delete/>
        </el-icon>
        批量删除
      </el-button>
      <!-- 回收站入口：改为红色强调，增加徽章样式 -->
      <el-button
          type="danger"
          @click="showRecycleBin = !showRecycleBin"
          class="recycle-btn"
      >
        <el-icon class="icon-mr">
          <Delete/>
        </el-icon>
        回收站
        <el-badge :value="recycleBin.length" class="recycle-badge"/>
      </el-button>
    </div>

    <div class="main-content">
      <!-- 左侧：文档列表/回收站 - 增加卡片阴影、圆角 -->
      <div class="left-panel card-shadow">
        <div class="list-header">
          <h3 class="panel-title">{{ showRecycleBin ? '文档回收站' : '文档管理列表' }}</h3>
          <!-- 筛选+搜索：优化输入框样式 -->
          <div class="filter-bar">
            <el-select
                v-model="filterStatus"
                placeholder="筛选状态"
                style="width: 160px; margin-right: 12px;"
                @change="filterDocs"
                v-if="!showRecycleBin"
                class="filter-select"
            >
              <el-option label="全部状态" value=""></el-option>
              <el-option label="编辑中" value="editing"></el-option>
              <el-option label="已完成" value="completed"></el-option>
              <el-option label="已驳回" value="rejected"></el-option>
              <el-option label="已发版" value="published"></el-option>
            </el-select>
            <el-input
                v-model="searchKeyword"
                placeholder="搜索文档标题"
                style="width: 220px;"
                @clear="filterDocs"
                @keyup.enter="filterDocs"
                class="search-input"
            >
              <template #append>
                <el-button icon="Search" @click="filterDocs" class="search-btn"></el-button>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 文档列表表格：优化表头、行hover效果 -->
        <el-table
            :data="filteredDocs"
            row-key="id"
            :selection-change="handleSelectionChange"
            @row-click="handleDocSelect"
            border
            style="width: 100%;"
            class="doc-table"
            :cell-style="{ padding: '12px 0' }"
            :header-cell-style="{
            background: '#f8fafc',
            fontWeight: 500,
            color: '#334155',
            borderBottom: '1px solid #e2e8f0'
          }"
        >
          <el-table-column type="selection" width="55" v-if="!showRecycleBin"></el-table-column>
          <el-table-column prop="title" label="文档标题" align="center" min-width="220">
            <template #default="scope">
              <span
                  :class="{'doc-title-completed': scope.row.status === 'completed'}"
                  class="doc-title"
              >
                {{ scope.row.title }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="100">
            <template #default="scope">
              <el-tag
                  :type="statusTagType[scope.row.status]"
                  :disable-transitions="false"
                  class="status-tag"
              >
                {{ statusMap[scope.row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" align="center" label="当前版本" width="95"></el-table-column>
          <el-table-column prop="leadAuthor" align="center" label="创建人" width="90"></el-table-column>
          <el-table-column label="操作" align="center" width="160">
            <template #default="scope">
              <el-button
                  size="small"
                  @click="handleDocEdit(scope.row)"
                  :disabled="showRecycleBin || (scope.row.status === 'completed' && currentUser.role !== 'admin' && scope.row.leadAuthor !== currentUser.name)"
                  class="table-btn edit-btn"
              >
                编辑
              </el-button>
              <el-button
                  size="small"
                  @click="handleDocDelete(scope.row)"
                  :disabled="showRecycleBin && currentUser.role !== 'admin'"
                  :class="['table-btn', showRecycleBin ? 'restore-btn' : 'delete-btn']"
              >
                {{ showRecycleBin ? '恢复' : '删除' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 右侧：详情操作面板 - 增强阴影、圆角 -->
      <div class="right-panel card-shadow">
        <div class="special-panel-header">
          <h3 class="special-panel-title">
            {{ selectedDoc ? (selectedDoc.title + ' - 详情') : '请选择文档' }}
          </h3>
          <el-button
              size="large"
              @click="selectedDoc = null"
              :disabled="!selectedDoc"
              class="close-btn"
          >
            <el-icon>
              <Close/>
            </el-icon>
            关闭
          </el-button>
        </div>


        <div class="panel-content" v-if="selectedDoc">
          <!-- 1. 基础信息：卡片美化、间距调整 -->
          <el-card shadow="never" class="info-card card-inner">
            <template #header>
              <h4 class="card-title">基础信息</h4>
            </template>
            <el-form :model="selectedDoc" label-width="120px" class="info-form">
              <el-form-item label="文档标题" class="form-item">
                <el-input
                    v-model="selectedDoc.title"
                    :disabled="selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.leadAuthor !== currentUser.name"
                    class="form-input"
                />
              </el-form-item>
              <el-form-item label="文章分类" class="form-item">
                <el-select
                    v-model="selectedDoc.categoryName"
                    placeholder="选择分类"
                    :disabled="selectedDoc.status === 'completed' && currentUser.role !== 'admin' && selectedDoc.leadAuthor !== currentUser.name"
                    class="form-select"
                >
                  <el-option label="前端开发" value="frontend"></el-option>
                  <el-option label="后端架构" value="backend"></el-option>
                  <el-option label="算法与数据结构" value="algorithm"></el-option>
                  <el-option label="产品需求" value="product"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="文章标签" class="form-item" prop="tags">
                <el-tag
                    v-for="(item, index) in selectedDoc.tags"
                    :key="index"
                    closable
                    @close="removeTag(item)"
                    style="padding: 14px ; margin-right: 1rem; font-size:12px"
                    :disable-transitions="true"
                >
                  {{ item }}
                </el-tag>
                <el-popover placement="bottom-start" width="460" trigger="click"
                            v-if="selectedDoc.tags.length < 3 ">
                  <template #reference>
                    <el-button type="success" size="small" plain
                               style="padding: 14px ; margin-right: 1rem; font-size:12px">
                      添加标签
                    </el-button>
                  </template>
                  <el-autocomplete
                      style="width: 100%"
                      v-model="tagName"
                      :fetch-suggestions="searchTag"
                      placeholder="请输入标签名搜索"
                      :trigger-on-focus="false"
                      @keyup.enter="saveTag"
                      @select="handleSelectTag"
                  />
                  <div class="popover-container">
                    <div style="margin-bottom: 1rem">可选标签</div>
                    <el-tag
                        v-for="(item) in tagList"
                        :key="item.id"
                        :class="tagClass(item.tagName)"
                        @click="addTag(item.tagName)"
                    >
                      {{ item.tagName }}
                    </el-tag>
                  </div>
                </el-popover>
              </el-form-item>
              <el-form-item label="创建人" class="form-item">
                <el-input v-model="selectedDoc.leadAuthor" disabled class="form-input"/>
              </el-form-item>
              <el-form-item label="当前状态" class="form-item">
                <el-tag :type="statusTagType[selectedDoc.status]" class="status-tag">
                  {{ statusMap[selectedDoc.status] }}
                </el-tag>
              </el-form-item>
              <el-form-item label="当前版本" class="form-item">
                <el-input :value="selectedDoc.version" disabled class="form-input"/>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 2. 状态操作：按钮样式统一、间距优化 -->
          <el-card shadow="never" class="status-card card-inner">
            <template #header>
              <h4 class="card-title">状态操作</h4>
            </template>
            <div class="status-actions">
              <!-- 普通用户操作 -->
              <template v-if="currentUser.role !== 'user'">
                <el-button
                    type="primary"
                    @click="handleContinueEdit"
                    v-if="selectedDoc.status === 'editing'"
                    class="action-btn btn-primary"
                >
                  <el-icon class="icon-mr-sm">
                    <Edit/>
                  </el-icon>
                  继续编辑
                </el-button>
                <el-button
                    type="success"
                    @click="handleApplyEdit"
                    v-if="selectedDoc.status === 'rejected' ||
                          selectedDoc.status === 'published'&&
                          selectedDoc.leadAuthor === currentUser.name"
                    class="action-btn btn-success"
                >
                  <el-icon class="icon-mr-sm">
                    <Edit/>
                  </el-icon>
                  申请重新编辑
                </el-button>
                <el-button
                    type="primary"
                    @click="handleSaveDraft"
                    v-if="selectedDoc.status === 'editing'"
                    class="action-btn btn-primary"
                >
                  <el-icon class="icon-mr-sm">
                    <Finished/>
                  </el-icon>
                  保存草稿
                </el-button>
              </template>

              <!-- 管理者操作 -->
              <template v-if="currentUser.role === 'admin'">
                <el-button
                    type="success"
                    @click="handlePublishDoc"
                    v-if="selectedDoc.status === 'completed'"
                    class="action-btn btn-success"
                >
                  <el-icon class="icon-mr-sm">
                    <Check/>
                  </el-icon>
                  审核发版
                </el-button>
                <el-button
                    type="danger"
                    @click="handleRejectDoc"
                    v-if="selectedDoc.status === 'completed'"
                    class="action-btn btn-danger"
                >
                  <el-icon class="icon-mr-sm">
                    <Close/>
                  </el-icon>
                  驳回
                </el-button>
                <el-button
                    type="warning"
                    @click="handleForceEdit"
                    v-if="selectedDoc.status === 'completed'"
                    class="action-btn btn-warning"
                >
                  <el-icon class="icon-mr-sm">
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
                  class="action-btn btn-primary"
              >
                <el-icon class="icon-mr-sm">
                  <Edit/>
                </el-icon>
                查看驳回原因并编辑
              </el-button>
            </div>

            <!-- 驳回原因显示：美化背景、边框 -->
            <div class="reject-reason" v-if="selectedDoc.rejectReason">
              <h5 class="reason-title">驳回原因：</h5>
              <p class="reason-content">{{ selectedDoc.rejectReason }}</p>
            </div>
          </el-card>

          <!-- 3. 版本记录：表格样式统一 -->
          <el-card shadow="never" class="version-card card-inner">
            <template #header>
              <h4 class="card-title">版本记录</h4>
            </template>
            <el-table
                :data="docVersions"
                row-key="id"
                border
                style="width: 100%;"
                class="version-table"
                :cell-style="{ padding: '10px 0' }"
                :header-cell-style="{
                background: '#f8fafc',
                fontWeight: 500,
                color: '#334155',
                borderBottom: '1px solid #e2e8f0'
              }"
            >
              <el-table-column prop="version" label="版本号" width="100"></el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="statusVersionTagType[scope.row.status]" class="version-tag">
                    {{ statusVersionMap[scope.row.status] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="author" label="更新人" width="120"></el-table-column>
              <el-table-column prop="createdAt" label="更新时间" width="180"></el-table-column>
              <el-table-column prop="content" label="更新说明" min-width="200">
                <template #default="scope">
                  <div class="version-desc" :title="scope.row.description">
                    {{ scope.row.description || scope.row.content }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160">
                <template #default="scope">
                  <el-button
                      size="small"
                      @click="handleVersionCompare(scope.row)"
                      :disabled="docVersions.length < 2"
                      class="table-btn compare-btn"
                  >
                    对比
                  </el-button>
                  <el-button
                      size="small"
                      @click="handleVersionRollback(scope.row)"
                      :disabled="scope.row.version === selectedDoc.version || currentUser.role !== 'admin'"
                      class="table-btn rollback-btn"
                  >
                    回滚
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 4. 内容编辑/预览：编辑器样式美化 -->
          <el-card shadow="never" class="content-card card-inner">
            <template #header>
              <h4 class="card-title">
                {{
                  (selectedDoc.status === 'editing' && currentUser.role !== 'admin' && selectedDoc.leadAuthor !== currentUser.name)
                      ? '文档预览'
                      : '文档编辑'
                }}
              </h4>
            </template>
            <!-- 模拟富文本编辑器：增加边框、圆角 -->
            <div class="editor-container">
              <RichTextEditor
                  v-model:value="selectedDoc.content"
                  :readonly="selectedDoc.status === 'completed'&& currentUser.role !== 'admin' && selectedDoc.leadAuthor !== currentUser.name"
              />
            </div>
          </el-card>
        </div>

        <!-- 未选择文档时的占位：美化空状态 -->
        <div class="empty-placeholder" v-else>
          <div class="empty-wrap">
            <el-empty
                description="请在左侧列表选择一个文档进行管理"
                class="empty-custom"
            >
              <template #image>
                <img
                    src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/4cff071f5d6d3978adea84ce7714039d.png"
                    alt="空状态"
                    class="empty-img"
                >
              </template>
            </el-empty>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗：统一美化弹窗样式、增加过渡 -->
    <el-dialog
        title="审核发版"
        v-model="publishDialogVisible"
        width="500px"
        class="custom-dialog"
        :before-close="handleDialogClose"
    >
      <el-form :model="publishForm" label-width="120px" class="dialog-form">
        <el-form-item label="版本更新说明" required class="form-item">
          <el-input
              type="textarea"
              v-model="publishForm.description"
              placeholder="请输入本次发版的更新说明（如：修复内容错误、新增章节等）"
              rows="3"
              class="form-input"
          />
        </el-form-item>
        <el-form-item label="版本类型" class="form-item">
          <el-radio-group v-model="publishForm.versionType" class="radio-group">
            <el-radio label="minor" class="radio-item">次版本更新（1.0 → 1.1）</el-radio>
            <el-radio label="major" class="radio-item">主版本更新（1.1 → 2.0）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false" class="dialog-btn btn-cancel">取消</el-button>
        <el-button type="primary" @click="handlePublishConfirm" class="dialog-btn btn-confirm">确认发版</el-button>
      </template>
    </el-dialog>

    <el-dialog
        title="驳回文档"
        v-model="rejectDialogVisible"
        width="500px"
        class="custom-dialog"
        :before-close="handleDialogClose"
    >
      <el-form :model="rejectForm" label-width="120px" class="dialog-form">
        <el-form-item label="驳回原因" required class="form-item">
          <el-input
              type="textarea"
              v-model="rejectForm.reason"
              placeholder="请输入驳回原因，方便用户修改"
              rows="3"
              class="form-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false" class="dialog-btn btn-cancel">取消</el-button>
        <el-button type="primary" @click="handleRejectConfirm" class="dialog-btn btn-confirm">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog
        title="申请重新编辑"
        v-model="applyEditDialogVisible"
        width="500px"
        class="custom-dialog"
        :before-close="handleDialogClose"
    >
      <el-form :model="applyEditForm" label-width="120px" class="dialog-form">
        <el-form-item label="申请理由" required class="form-item">
          <el-input
              type="textarea"
              v-model="applyEditForm.reason"
              placeholder="请说明重新编辑的原因"
              rows="3"
              class="form-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyEditDialogVisible = false" class="dialog-btn btn-cancel">取消</el-button>
        <el-button type="primary" @click="handleApplyEditConfirm" class="dialog-btn btn-confirm">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog
        title="版本对比"
        v-model="versionCompareDialogVisible"
        width="800px"
        class="custom-dialog"
        :before-close="handleDialogClose"
    >
      <div class="version-compare-container">
        <h5 class="version-title">
          版本 {{ compareVersions.old.version }} ({{ statusVersionMap[compareVersions.old.status] }})
          vs
          版本 {{ compareVersions.new.version }} ({{ statusVersionMap[compareVersions.new.status] }})
        </h5>
        <CodeDiff
            :old-string="compareVersions.old.content"
            :new-string="compareVersions.new.content"
            language="html"
            output-format="line-by-line"
            diff-style="char"
            filename="out.txt"
            theme="light"
        />
      </div>

      <template #footer>
        <el-button
            @click="versionCompareDialogVisible = false"
            class="dialog-btn btn-confirm"
        >
          关闭
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
        title="添加标签"
        v-model="addTagDialogVisible"
        width="300px"
        class="custom-dialog"
        :before-close="handleDialogClose"
    >
      <el-form :model="addTagForm" label-width="80px" class="dialog-form">
        <el-form-item label="标签名称" required class="form-item">
          <el-input
              v-model="addTagForm.tag"
              placeholder="请输入标签（最多5个字符）"
              maxlength="5"
              class="form-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTagDialogVisible = false" class="dialog-btn btn-cancel">取消</el-button>
        <el-button type="primary" @click="handleAddTagConfirm" class="dialog-btn btn-confirm">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, computed, onMounted} from 'vue';
import {ElMessage, ElEmpty} from 'element-plus';
import RichTextEditor from '@/components/Edit/index.vue';
import {
  Plus, Refresh, Delete, Close, Edit, Check, Finished
} from '@element-plus/icons-vue';
import {getDocTags, listDocs, updateDoc} from "@/api/collab";
import {DocVersion} from "@/api/version/types";
import {Doc} from "@/api/collab/type";
import {listHistory, submitForPublish} from "@/api/version";
import {CodeDiff} from 'v-code-diff'
import useUserStore from "@/store/modules/user";

interface User {
  name: string;
  role: 'admin' | 'user';
  avatar: string;
}

// 2. 模拟数据
const currentUser = ref<User>({
  name: useUserStore.name ||  '管理员', // 切换角色：'管理员'（admin） / '张三'（user）
  role: 'admin', // 切换角色：'admin' / 'user'
  avatar: 'https://picsum.photos/id/1/40/40'
});

// 初始文档列表
const documents = ref<Doc[]>([]);
const docVersions = ref<DocVersion[]>([]);

// 回收站
const recycleBin = ref<Doc[]>([]);

// 3. 响应式状态
const selectedDoc = ref<Doc | null>(null);
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

const tagName = ref<string>('');
const tagList = reactive([
  {id: 101, tagName: 'Vue3'},
  {id: 102, tagName: 'React'},
  {id: 103, tagName: 'Node.js'}
]);
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
  old: {} as DocVersion,
  new: {} as DocVersion
});

// 4. 计算属性
// 筛选后的文档列表
const filteredDocs = computed(() => {
  const list = showRecycleBin.value ? recycleBin.value : documents.value;
  return list.filter(doc => {
    // 状态筛选
    if (filterStatus.value && doc.status !== filterStatus.value) return false;
    // 关键词筛选（标题）
    return !(searchKeyword.value && !doc.title.toLowerCase().includes(searchKeyword.value.toLowerCase()));
  });
});

// 状态映射
const statusMap = ref({
  editing: '编辑中',
  completed: '已完成',
  published: '已发布',
  rejected: '驳回'
});

// 状态标签类型
const statusTagType = ref({
  editing: 'info',
  completed: 'success',
  rejected: 'danger',
  published: 'warning'
});

const statusVersionMap = ref({
  DRAFT: '草稿',
  PENDING: '待审',
  ROLLBACK: '回退',
  PUBLISHED: '发布',
});

// 状态标签类型
const statusVersionTagType = ref({
  DRAFT: 'info',
  PENDING: 'success',
  ROLLBACK: 'warning',
  PUBLISHED: 'danger'
});

function tagClass(name: string) {
  if (!selectedDoc.value) return 'tag-item';
  return selectedDoc.value.tags.includes(name) ? 'tag-item-select' : 'tag-item';
}

function handleSelectTag(item: any) {
  if (!selectedDoc.value) return;
  if (!selectedDoc.value.tags.includes(item.tagName))
    selectedDoc.value.tags.push(item.tagName);
}

function addTag(name: string) {
  if (!selectedDoc.value) return;
  if (!selectedDoc.value.tags.includes(name) && selectedDoc.value.tags.length < 3)
    selectedDoc.value.tags.push(name);
}

function saveTag() {
  if (!selectedDoc.value) return;
  if (tagName.value && !selectedDoc.value.tags.includes(tagName.value) && selectedDoc.value.tags.length < 3)
    selectedDoc.value.tags.push(tagName.value);
  tagName.value = '';
}

function removeTag(name: string) {
  if (!selectedDoc.value) return;
  selectedDoc.value.tags = selectedDoc.value.tags.filter(t => t !== name);
}


function searchTag(query: string, cb: any) {
  const q = query.toLowerCase();
  const result = tagList
      .filter(i => i.tagName.toLowerCase().includes(q))
      .map(i => ({value: i.tagName, ...i}));
  cb(result);
}

// 5. 核心方法
const handleDocSelect = (doc: Doc) => {
  docVersions.value = [];
  selectedDoc.value = null;
  listHistory(doc.id)
      .then(({data}) => {
        if (!data?.data || !Array.isArray(data.data)) {
          ElMessage.warning('版本数据格式错误');
          return;
        }
        const formattedVersions = (data.data as DocVersion[]).map(version => ({
          ...version,
          createdAt: version.createdAt ? version.createdAt.replace('T', ' ') : '无时间信息'
        }));
        docVersions.value = formattedVersions;
        selectedDoc.value = JSON.parse(JSON.stringify(doc));
        console.log(`文档[${doc.id}]的版本数据加载完成，共${formattedVersions.length}条`);
      })
      .catch(error => {
        console.error('加载版本历史失败：', error);
        ElMessage.error('获取版本历史失败，请稍后重试');
      });
};

// 表格选择事件
const handleSelectionChange = (vals: Doc[]) => {
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
  const newDoc: Doc = {
    comments: 0,
    description: "",
    id: documents.value.length + 1,
    title: '新建文档',
    status: 'editing',
    version: "1.0",
    leadAuthor: currentUser.value.name,
    categoryName: 'frontend',
    tags: [],
    content: '<h2>请输入文档内容</h2><p>可使用富文本编辑器编辑格式</p>'
  };
  documents.value.unshift(newDoc);
  handleDocSelect(newDoc);
  ElMessage.success('新建文档成功');
};

// 编辑文档（跳转到详情面板）
const handleDocEdit = (doc: Doc) => {
  handleDocSelect(doc);
};

// 删除/恢复文档
const handleDocDelete = (doc: Doc) => {
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

// 保存草稿
const handleSaveDraft = () => {
  if (!selectedDoc.value) return;
  // 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
    updateDoc(documents.value[idx])
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
  let newVersion: string;
  if (publishForm.versionType === 'major') {
    newVersion = `${major + minor}.0`;
  } else {
    newVersion = `${major}.${minor + 1}`;
  }
  console.log('新版本号：', newVersion)
  // 2. 添加新版本记录
  const newVersionItem: DocVersion = {
    docId: selectedDoc.value.id,
    version: newVersion,
    status: 'PUBLISHED',
    author: currentUser.value.name,
    createdAt: new Date().toLocaleString('zh-CN', {
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

  // 3. 更新文档状态和版本
  selectedDoc.value.status = 'published';
  docVersions.value.push(newVersionItem)
  submitForPublish(selectedDoc.value.id, newVersionItem)
  // 4. 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
    updateDoc(documents.value[idx])
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
  // 2. 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
    updateDoc(documents.value[idx])
  }

  rejectDialogVisible.value = false;
  rejectForm.reason = '';
  ElMessage.success('文档已驳回，已通知用户');
};

// 版本对比
const handleVersionCompare = (version: DocVersion) => {
  if (!selectedDoc.value) return;
  // 取最新版本和当前选中版本对比
  const latestVersion = docVersions.value[0];
  compareVersions.value = {
    old: version,
    new: latestVersion
  };
  console.log('版本对比：', version, latestVersion);
  versionCompareDialogVisible.value = true;
};

/*// 格式化对比内容（模拟差异标记）
const formatCompareContent = (content: string) => {
  // 实际项目可集成diff库（如diff-match-patch），这里简化模拟
  return content
      .replace(/<p>/g, '<p class="compare-paragraph">')
      .replace(/新增/g, '<span class="add-diff">新增</span>')
      .replace(/优化/g, '<span class="update-diff">优化</span>')
      .replace(/删除/g, '<span class="delete-diff">删除</span>');
};*/

const handleDialogClose = () => {
  versionCompareDialogVisible.value = false;

};
// 版本回滚
const handleVersionRollback = (version: DocVersion) => {
  if (!selectedDoc.value || !confirm(`确定要回滚到版本 ${version.version} 吗？回滚后当前内容将被覆盖`)) {
    return;
  }

  // 1. 恢复内容
  if (version.content != null) {
    selectedDoc.value.content = version.content;
  }

  // 2. 添加回滚版本记录
  const [major, minor] = selectedDoc.value.version.split('.').map(Number);
  const newVersion = `${major}.${minor + 1}`;
  const newVersionItem: DocVersion = {
    docId: selectedDoc.value.id,
    version: newVersion,
    status: 'ROLLBACK',
    author: currentUser.value.name,
    createdAt: new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).replace(/\//g, '-'),
    description: `回滚到版本 ${version.version}`,
    content: version.content
  };
  selectedDoc.value.status = 'editing';
  docVersions.value.push(newVersionItem)
  submitForPublish(selectedDoc.value.id, newVersionItem)
  // 3. 同步到原文档列表
  const idx = documents.value.findIndex(doc => doc.id === selectedDoc.value?.id);
  if (idx > -1) {
    documents.value[idx] = JSON.parse(JSON.stringify(selectedDoc.value));
  }

  ElMessage.success(`已回滚到版本 ${version.version}，新版本号：${newVersion}`);
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


async function fetchDocTag() {
  try {
    const {data} = await getDocTags();
    Object.assign(tagList, data.data);
  } catch (error) {
    ElMessage.error('加载文档标签失败');
  }
}

const loadDocList = () => {
  listDocs().then(({data}) => {
    documents.value = data.data;
    console.log(documents.value);
  })
};

onMounted(() => {
  loadDocList();
  fetchDocTag();
});
</script>

<style scoped>
@import '@/views/collab/doc/base.scss';
</style>