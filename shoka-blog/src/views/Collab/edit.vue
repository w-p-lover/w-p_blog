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
          <el-form-item label="文章标签" prop="tagNameList">
            <el-tag
                v-for="(item, index) in docForm.tagNameList"
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
                v-for="(item, index) in docForm.tagNameList"
                :key="'view-' + index"
                v-if="mode === 'view'"
                style="margin-right: 1rem"
            >
              {{ item }}
            </el-tag>
            <el-popover placement="bottom-start" width="460" trigger="click" v-if="docForm.tagNameList.length < 3 && mode !== 'view'">
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
                    v-for="(item, index) in tagList"
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
            <el-input type="textarea" rows="3" v-model="docForm.description" :readonly="mode === '查看'"/>
          </el-form-item>

          <!-- 内容 -->
          <el-form-item label="文档内容" prop="content">
            <RichTextEditor v-model:value="docForm.content" :readonly="mode === '查看'"/>
          </el-form-item>

          <!-- 协作者 -->
          <el-form-item label="协作者">
            <div class="collab-group">
              <div class="collab-row" v-for="(item, idx) in docForm.collabs" :key="idx">
                <el-input v-model="item.name" placeholder="输入协作者姓名" :readonly="mode === '查看'"
                          style="width:280px;margin-right:6px"/>
                <el-select v-model="item.role" placeholder="选择角色" :disabled="mode === '查看'"
                           style="width:180px;margin-right:16px">
                  <el-option label="编辑者" value="editor"/>
                  <el-option label="查看者" value="viewer"/>
                </el-select>
                <el-button icon="el-icon-delete" type="text" @click="removeCollab(idx)"
                           :disabled="docForm.collabs.length===1 || mode==='查看'"/>
              </div>
              <el-button type="primary" @click="addCollab" v-if="mode!=='查看'" style="margin-top:8px">添加协作者
              </el-button>
            </div>
          </el-form-item>
          <!-- 按钮 -->
          <el-form-item class="form-btn-group">
            <el-button type="text" @click="handleCancel">取消</el-button>
            <el-button type="primary" v-if="mode !== '查看'" @click="saveDraft()">
              {{ '保存草稿' }}
            </el-button>
            <el-button type="success" v-if="mode !== '查看'" @click="editDoc()">
              {{ mode === '新建' ? '创建完成' : '保存修改' }}
            </el-button>
          </el-form-item>

        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref, onMounted} from 'vue';
import {ElMessage} from 'element-plus';
import {getDoc, listDocs} from '@/api/collab/index';
import RichTextEditor from '@/components/Edit/index.vue';
import useStore from '@/store';

const {user} = useStore();

// reactive 状态
const docForm = reactive({
  title: '',
  tagNameList: [] as string[],
  categoryName: '',
  desc: '',
  content: '',
  collabs: [{name: user.nickname || '默认用户', role: 'editor'}]
});

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
  tagNameList: [{type: 'array', min: 1, message: '请选择至少一个标签', trigger: 'change'}],
  categoryName: [{required: true, message: '请选择分类', trigger: 'change'}],
  content: [{required: true, message: '请输入内容', trigger: 'blur'}]
});

// 页面初始化
onMounted(() => {
  const path = location.pathname;
  if (path.includes('/collab/edit/')) mode.value = '编辑';
  else if (path.match(/^\/collab\/\d+$/)) mode.value = '查看';
  if (mode.value !== '新建') {
    const id = Number(location.pathname.split('/').pop());
    fetchDoc(id);
  }
});

// 获取文档
async function fetchDoc(id: number) {
  try {
    const {data} = await getDoc(id);
    Object.assign(docForm, data.data);
  } catch (error) {
    ElMessage.error('加载文档失败');
  }
}

// 分类操作
function handleSelectCategory(item: any) {
  docForm.categoryName = item.categoryName;
}

function addCategory(name: string) {
  docForm.categoryName = name;
}

function saveCategory() {
  if (categoryName.value) docForm.categoryName = categoryName.value;
}

function removeCategory() {
  docForm.categoryName = '';
}

function searchCategory(query: string, cb: any) {
  cb(categoryList.filter(i => i.categoryName.includes(query)));
}

// 标签操作
function tagClass(name: string) {
  return docForm.tagNameList.includes(name) ? 'tag-item-select' : 'tag-item';
}

function handleSelectTag(item: any) {
  if (!docForm.tagNameList.includes(item.tagName))
    docForm.tagNameList.push(item.tagName);
}

function addTag(name: string) {
  if (!docForm.tagNameList.includes(name) && docForm.tagNameList.length < 3)
    docForm.tagNameList.push(name);
}

function saveTag() {
  if (tagName.value && !docForm.tagNameList.includes(tagName.value) && docForm.tagNameList.length < 3)
    docForm.tagNameList.push(tagName.value);
  tagName.value = '';
}

function removeTag(name: string) {
  docForm.tagNameList = docForm.tagNameList.filter(t => t !== name);
}

function searchTag(query: string, cb: any) {
  cb(tagList.filter(i => i.tagName.includes(query)));
}

// 协作者操作
function addCollab() {
  docForm.collabs.push({name: '', role: 'viewer'});
}

function removeCollab(idx: number) {
  docForm.collabs.splice(idx, 1);
}

// 表单操作
async function validateForm() {
  return new Promise(resolve => formRef.value.validate((valid: any) => resolve(valid)));
}

async function handleCancel() {
  history.back();
}

async function saveDraft() {
  if (!await validateForm()) return;
  ElMessage.success('草稿保存成功');
}

async function submitDoc() {
  if (!await validateForm()) return;
  ElMessage.success('文档创建成功');
}

async function editDoc() {
  if (!await validateForm()) return;
  ElMessage.success('文档修改成功');
}
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
  border-color: #8e8cd8;
  color: #8e8cd8;
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

</style>