<template>
  <!-- 页面头部 -->
  <div class="page-header">
    <h1 class="page-title">
      {{ mode }}协作文档
    </h1>
    <img
        class="page-cover"
        src="https://ik.imagekit.io/nicexl/Wallpaper/ba41a32b219e4b40ad055bbb52935896_Y0819msuI.jpg"
        alt="文档背景"
    >
  </div>

  <div class="bg">
    <div class="page-container">
      <el-card class="collab-create-card" shadow="hover">
        <h2 class="section-title blog-title">{{ mode }}文档</h2>

        <el-form ref="formRef" :model="docForm" :rules="formRules" label-width="120px">
          <!-- 标题 -->
          <el-form-item label="文档标题" prop="title">
            <el-input v-model="docForm.title" placeholder="输入协作文档标题" :readonly="mode === '查看'"/>
          </el-form-item>

          <!-- 分类 -->
          <el-form-item label="文章分类" prop="categoryName">
            <el-tag v-if="docForm.categoryName" closable @close="removeCategory">
              {{ docForm.categoryName }}
            </el-tag>
            <el-popover v-if="!docForm.categoryName && mode !== '查看'" placement="bottom-start" width="460"
                        trigger="click">
              <template #reference>
                <el-button type="success" plain>添加分类</el-button>
              </template>
              <el-autocomplete
                  style="width:100%"
                  v-model="categoryName"
                  :fetch-suggestions="searchCategory"
                  placeholder="请输入分类名搜索"
                  :trigger-on-focus="false"
                  @keyup.enter="saveCategory"
                  @select="handleSelectCategory"
              />
              <div class="popover-container">
                <div v-for="item in categoryList" :key="item.id" class="category-item"
                     @click="addCategory(item.categoryName)">
                  {{ item.categoryName }}
                </div>
              </div>
            </el-popover>
          </el-form-item>

          <!-- 标签 -->
          <el-form-item label="文章标签" prop="tags">
            <el-tag
                v-for="(item, index) in docForm.tags"
                :key="index"
                closable
                @close="removeTag(item)"
                style="margin-right: 1rem"
                :disable-transitions="true"
                v-if="mode !== 'view'"
            >
              {{ item }}
            </el-tag>
            <el-tag
                v-for="(item, index) in docForm.tags"
                :key="'view-' + index"
                v-if="mode === 'view'"
                style="margin-right: 1rem"
            >
              {{ item }}
            </el-tag>
            <el-popover placement="bottom-start" width="460" trigger="click"
                        v-if="docForm.tags.length < 3 && mode !== 'view'">
              <template #reference>
                <el-button type="success" plain>添加标签</el-button>
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

          <!-- 描述 -->
          <el-form-item label="文档描述">
            <el-input type="textarea" :rows="3" v-model="docForm.description" :readonly="mode === '查看'"/>
          </el-form-item>

          <!-- 内容 -->
          <el-form-item label="文档内容" prop="content">
            <RichTextEditor v-model:value="docForm.content" :readonly="mode === '查看'"/>
          </el-form-item>

          <!-- 协作者 -->
          <el-form-item label="协作者">
            <div class="collab-group">
              <div class="collab-row" v-for="(item, idx) in docForm.collaborators" :key="idx">
                <el-select v-model="item.name" placeholder="选择协作者"
                           :disabled="mode === '查看' || (mode !== '新建' && item.name !== user.nickname
                           && item.role == 'editor' && item.addMode !== true)"
                           style=" width: 250px; margin-right: 16px"
                           @change="i => onCollabChange(item, i)">
                  <el-option
                      v-for="user in collabList"
                      :key="user.name"
                      :label="user.name"
                      :value="user.name"
                  />
                </el-select>
                <el-select v-model="item.role" placeholder="选择角色"
                           :disabled="mode === '查看' || (mode !== '新建' && item.name !== user.nickname
                           && item.role == 'editor' && item.addMode !== true)"
                           style=" width: 250px; margin-right: 16px">
                  <el-option label="编辑者" value="editor"/>
                  <el-option label="查看者" value="viewer"/>
                </el-select>
                <el-input v-model="item.avatar" placeholder="请输入头像URL" :disabled="mode === '查看'">
                </el-input>
                <el-button
                    type="primary"
                    @click="removeCollab(idx)"
                    :disabled="docForm.collaborators.length === 1 || mode === '查看'"
                    style="margin-left: 15px"
                >
                  <el-icon>
                    <Delete/>
                  </el-icon>
                </el-button>
              </div>
              <el-button
                  type="primary"
                  @click="addCollab"
                  v-if="mode!=='查看'"
                  class="add-collab-btn"
              >
                <el-icon style="margin-right: 4px;">
                  <Plus/>
                </el-icon>
                添加协作者
              </el-button>
            </div>
          </el-form-item>

          <!-- 按钮组：视觉层级优化 -->
          <el-form-item class="form-btn-group">
            <el-button type="text" @click="handleCancel" class="cancel-btn">取消</el-button>
            <el-button
                type="default"
                v-if="mode !== '查看'"
                @click="saveDraft()"
                class="draft-btn"
            >
              保存草稿
            </el-button>
            <el-button
                type="primary"
                v-if="mode !== '查看'"
                @click="mode === '新建' ? submitDoc() : editDoc()"
                class="submit-btn"
            >
              {{ mode === '新建' ? '创建完成' : '保存修改' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue';
import {ElMessage} from 'element-plus';
import {createDoc, getDoc, getDocTags, updateDoc} from '@/api/collab';
import RichTextEditor from '@/components/Edit/index.vue';
import useStore from '@/store';
import {getUserList} from "@/api/user";
import {Delete, Plus} from '@element-plus/icons-vue';
import {getCollabCategoryList} from "@/api/category";
import {submitForPublish} from "@/api/version";
import {DocVersion} from "@/api/version/types";

interface CollabUser {
  name: string;
  avatar: string;
  role: string;
}

const {user} = useStore();

// reactive 状态
const docForm = reactive({
  id: 0,
  title: '',
  tags: [] as string[],
  categoryName: '',
  description: '',
  content: '',
  collaborators: [{name: user.nickname || '默认用户', role: 'editor', avatar: user.avatar, addMode: false,}],
  status: "docForm",
  leadAuthor: user.nickname || '默认用户',
  version: '',
});

const collabList = ref<CollabUser[]>([]);
const categoryName = ref('');
const categoryList = reactive([
  {id: 1, categoryName: '前端开发'},
  {id: 2, categoryName: '后端架构'},
  {id: 3, categoryName: '算法与数据结构'}
]);

const tagName = ref('');
const tagList = reactive([
  {id: 101, tagName: 'Vue3'},
  {id: 102, tagName: 'React'},
  {id: 103, tagName: 'Node.js'}
]);

const formRef = ref();
const mode = ref<'新建' | '编辑' | '查看'>('新建');

// 表单规则
const formRules = reactive({
  title: [{required: true, message: '请输入标题', trigger: 'blur'}],
  tags: [{type: 'array', min: 1, message: '请选择至少一个标签', trigger: 'change'}],
  categoryName: [{required: true, message: '请选择分类', trigger: 'blur'}],
  content: [{required: true, message: '请输入内容', trigger: 'blur'}]
});

// 页面初始化
onMounted(async () => {
  const path = location.pathname;

  // 判断模式
  if (path.includes('/collab/edit/')) {
    mode.value = '编辑';
  } else if (path.match(/^\/collab\/\d+$/)) {
    mode.value = '查看';
  }

  const id = Number(path.split('/').pop());
  const draftKey = id ? `collabDocDraft-${id}` : 'collabDocDraft-new';
  const draft = localStorage.getItem(draftKey);
  if (draft) {
    Object.assign(docForm, JSON.parse(draft));
    ElMessage.info('已加载本地草稿');
  }

  if (mode.value !== '新建' && !draft) {
    await fetchDoc(id);
  }

  // 加载协作者列表（新建或编辑模式）
  if (mode.value !== '查看') {
    const {data} = await getUserList();
    collabList.value = data.data.recordList;
    await fetchDocTag();
    await fetchDoCategory()
  }
});


// 获取文档
async function fetchDoc(id: number) {
  try {
    const {data} = await getDoc(id);
    docForm.id = id;
    Object.assign(docForm, data.data);
  } catch (error) {
    ElMessage.error('加载文档失败');
  }
}

// 获取文档
async function fetchDocTag() {
  try {
    const {data} = await getDocTags();
    Object.assign(tagList, data.data);
  } catch (error) {
    ElMessage.error('加载文档标签失败');
  }
}

// 获取文档
async function fetchDoCategory() {
  try {
    const {data} = await getCollabCategoryList();
    Object.assign(categoryList, data.data);
  } catch (error) {
    ElMessage.error('加载文档标签失败');
  }
}

// 分类操作
function handleSelectCategory(item: any) {
  docForm.categoryName = item.categoryName;
  docForm.description = docForm.description + "分类添加\n"
}

function addCategory(name: string) {
  docForm.categoryName = name;
  docForm.description = docForm.description + "分类添加\n"
}

function saveCategory() {
  if (categoryName.value){
    docForm.categoryName = categoryName.value;
    docForm.description = docForm.description + "分类添加\n"
  }
}

function removeCategory() {
  docForm.categoryName = '';
  docForm.description = docForm.description + "分类删除\n"
}

function searchCategory(query: string, cb: any) {
  const q = query.toLowerCase();
  const result = categoryList
      .filter(i => i.categoryName.toLowerCase().includes(q))
      .map(i => ({value: i.categoryName, ...i})); // 关键：必须有 value
  cb(result);
}


// 标签操作
function tagClass(name: string) {
  return docForm.tags.includes(name) ? 'tag-item-select' : 'tag-item';
}

function handleSelectTag(item: any) {
  if (!docForm.tags.includes(item.tagName)){
    docForm.tags.push(item.tagName);
    docForm.description = docForm.description + "标签添加\n"
  }
}

function addTag(name: string) {
  if (!docForm.tags.includes(name) && docForm.tags.length < 3){
    docForm.tags.push(name);
    docForm.description = docForm.description + "标签添加\n"
  }
}

function saveTag() {
  if (tagName.value && !docForm.tags.includes(tagName.value) && docForm.tags.length < 3){
    docForm.tags.push(tagName.value);
    docForm.description = docForm.description + "标签添加\n"
  }
  tagName.value = '';
}

function removeTag(name: string) {
  docForm.tags = docForm.tags.filter(t => t !== name);
  docForm.description = docForm.description + "标签删除\n"
}

function searchTag(query: string, cb: any) {
  const q = query.toLowerCase();
  const result = tagList
      .filter(i => i.tagName.toLowerCase().includes(q))
      .map(i => ({value: i.tagName, ...i}));
  cb(result);
}

// 协作者操作
function addCollab() {
  docForm.collaborators.push({name: '', role: 'viewer', avatar: '', addMode: true});
  docForm.description = docForm.description + "添加协作者\n"
  console.log(docForm.collaborators)
}

function removeCollab(idx: number) {
  docForm.collaborators.splice(idx, 1);
  docForm.description = docForm.description + "删除协作者\n"
}

function onCollabChange(item: any, selectedName: string) {
  const selectedUser = collabList.value.find(u => u.name === selectedName)
  if (selectedUser) {
    item.avatar = selectedUser.avatar
    // 可以根据需要同步更新 role
    item.role = item.role || 'editor'
  }
  docForm.description = docForm.description + "修改协作者\n"
}


// 表单操作
async function validateForm() {
  return new Promise(resolve => formRef.value.validate((valid: any) => resolve(valid)));
}

async function handleCancel() {
  history.back();
}

function saveDraft() {
  // 1. 表单验证
  validateForm().then((valid) => {
    if (!valid) return;

    try {
      const key = 'collabDocDraft-' + (docForm.id || 'new'); // 可以加 id 区分不同文档
      localStorage.setItem(key, JSON.stringify(docForm));
      ElMessage.success('草稿已保存到本地');
    } catch (err) {
      console.error(err);
      ElMessage.error('保存草稿失败');
    }
  });
}


const submitDoc = async () => {
  if (!await validateForm()) return;
  try {
    const payload = {...docForm};
    const docVersion: DocVersion = {
      docId: docForm.id,
      version: '1.0',
      status: 'DRAFT',
      author: docForm.leadAuthor,
      createdAt: new Date().toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      }).replace(/\//g, '-'),
      description: "新文档创建: " + docForm.title,
      content: docForm.content
    };
    const { data: updateDocData } = await createDoc(payload);
    const { data: submitData } = await submitForPublish(docForm.id, docVersion);

    if (updateDocData.flag && submitData.flag) {
      ElMessage.success('文档创建成功');
    } else if(!updateDocData.flag){
      ElMessage.error(updateDocData.msg || '创建失败');
    } else if (!submitData.flag){
      ElMessage.error(submitData.msg || '版本提交失败');
    }
  } catch (error) {
    ElMessage.error('网络错误，创建文档失败');
  }
};

const editDoc = async () => {
  if (!await validateForm()) return;
  try {
    const payload = {...docForm};
    const docVersion: DocVersion = {
      docId: docForm.id,
      version: docForm.version,
      status: 'PENDING',
      author: docForm.leadAuthor,
      createdAt: new Date().toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      }).replace(/\//g, '-'),
      description: "文档更新: " + docForm.title,
      content: docForm.content
    };
    const { data: createDocData } = await createDoc(payload);
    const { data: submitData } = await submitForPublish(docForm.id, docVersion);

    if (createDocData.flag && submitData.flag) {
      ElMessage.success('文档修改成功');
    } else if(!createDocData.flag){
      ElMessage.error(createDocData.msg || '修改失败');
    } else if (!submitData.flag){
      ElMessage.error(submitData.msg || '版本提交失败');
    }
  } catch (error) {
    ElMessage.error('网络错误，修改文档失败');
  }
};
</script>

<style scoped>

/* 表单卡片 */
.collab-create-card {
  border-radius: 8px;
  padding: 24px;
}

.collab-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.del-collab-btn {
  color: #f56c6c;
}

/* 按钮组样式 */
.form-btn-group {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

/* 内容编辑器样式 */
.content-editor {
  resize: vertical;
}

.popover-title {
  margin-bottom: 1rem;
  text-align: center;
}

.popover-container {
  margin-top: 1rem;
  height: 260px;
  overflow-y: auto;
}

.category-item {
  cursor: pointer;
  padding: 0.6rem 0.5rem;
}

.category-item:hover {
  background-color: #f0f9eb;
  color: #67c23a;
}

.section-title {
  margin-bottom: 1.8rem;
  padding-bottom: 0.5rem;
  margin-left: 40px;
  border-bottom: 2px solid;
  font-size: 1.3rem;
  font-weight: 600;
}

.blog-title {
  border-color: #293898;
  color: #293898;
}

.tag-item {
  margin-right: 1rem;
  margin-bottom: 1rem;
  cursor: pointer;
}

.tag-item-select {
  margin-right: 1rem;
  margin-bottom: 1rem;
  background-color: #999b9c;
  color: #fff !important;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: not-allowed;
}

/* 添加协作者按钮 */
.add-collab-btn {
  margin-top: 8px;
  background-color: #F0F7FF;
  color: #165DFF;
  border-color: #C9E2FF;
}

.add-collab-btn:hover {
  background-color: #E8F3FF;
  border-color: #91C9FF;
}

/* 添加按钮（分类/标签） */
.add-btn {
  background-color: #F0F7FF;
  color: #165DFF;
  border-color: #C9E2FF;
}

.add-btn:hover {
  background-color: #E8F3FF;
  border-color: #91C9FF;
}

/* 按钮组：右对齐+间距 */
.form-btn-group {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  margin-bottom: 8px;
}

/* 取消按钮 */
.cancel-btn {
  color: #175abc;
}

.cancel-btn:hover {
  color: #1D2129;
  background-color: #F2F3F5;
}

/* 草稿按钮 */
.draft-btn {
  background-color: #F7F8FA;
  color: #1D2129;
  border-color: #E5E6EB;
}

.draft-btn:hover {
  background-color: #F2F3F5;
  border-color: #C9CDD4;
}

/* 提交按钮：主色调 */
.submit-btn {
  background-color: #165DFF;
  border-color: #165DFF;
}

.submit-btn:hover {
  background-color: #0E42D2;
  border-color: #0E42D2;
}
</style>