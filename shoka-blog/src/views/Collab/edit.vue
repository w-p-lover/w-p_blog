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
                           :disabled="mode === '查看' || (mode !== '新建' && item.name !== user.nickname && item.role == 'editor' && item.addMode !== true)"
                           style=" width:180px ; margin-right:16px"
                           @change="i => onCollabChange(item, i)">
                  <el-option
                      v-for="user in collabList"
                      :key="user.name"
                      :label="user.name"
                      :value="user.name"
                  />
                </el-select>
                <el-select v-model="item.role" placeholder="选择角色"
                           :disabled="mode === '查看' || (mode !== '新建' && item.name !== user.nickname && item.role == 'editor' && item.addMode !== true)"
                           style="width:180px;margin-right:16px">
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
                  <el-icon><Delete /></el-icon>
                </el-button>

              </div>
              <el-button type="primary" @click="addCollab" v-if="mode!=='查看'" style="margin-top:8px">
                添加协作者
              </el-button>
            </div>
          </el-form-item>
          <!-- 按钮 -->
          <el-form-item class="form-btn-group">
            <el-button type="text" @click="handleCancel">取消</el-button>
            <el-button type="primary" v-if="mode !== '查看'" @click="saveDraft()">
              {{ '保存草稿' }}
            </el-button>
            <el-button
                type="success"
                v-if="mode !== '查看'"
                @click="mode === '新建' ? submitDoc() : editDoc()"
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
import {Delete} from '@element-plus/icons-vue';
import {getCategoryList, getCollabCategoryList} from "@/api/category";
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
  categoryName: [{required: true, message: '请选择分类', trigger: 'change'}],
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
}

function onCollabChange(item: any, selectedName: string) {
  const selectedUser = collabList.value.find(u => u.name === selectedName)
  if (selectedUser) {
    item.avatar = selectedUser.avatar
    // 可以根据需要同步更新 role
    item.role = item.role || 'editor'
  }
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
  const q = query.toLowerCase();
  const result = categoryList
      .filter(i =>i.categoryName.toLowerCase().includes(q))
      .map(i => ({ value: i.categoryName, ...i })); // 关键：必须有 value
  cb(result);
}


// 标签操作
function tagClass(name: string) {
  return docForm.tags.includes(name) ? 'tag-item-select' : 'tag-item';
}

function handleSelectTag(item: any) {
  if (!docForm.tags.includes(item.tagName))
    docForm.tags.push(item.tagName);
}

function addTag(name: string) {
  if (!docForm.tags.includes(name) && docForm.tags.length < 3)
    docForm.tags.push(name);
}

function saveTag() {
  if (tagName.value && !docForm.tags.includes(tagName.value) && docForm.tags.length < 3)
    docForm.tags.push(tagName.value);
  tagName.value = '';
}

function removeTag(name: string) {
  docForm.tags = docForm.tags.filter(t => t !== name);
}

function searchTag(query: string, cb: any) {
  const q = query.toLowerCase();
  const result = tagList
      .filter(i => i.tagName.toLowerCase().includes(q))
      .map(i => ({ value: i.tagName, ...i }));
  cb(result);
}

// 协作者操作
function addCollab() {
  docForm.collaborators.push({name: '', role: 'viewer', avatar: '', addMode: true});
  console.log(docForm.collaborators)
}

function removeCollab(idx: number) {
  docForm.collaborators.splice(idx, 1);
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
    const {data} = await createDoc(payload);
    if (data.flag) {
      ElMessage.success('文档创建成功');
    } else {
      ElMessage.error(data.msg || '文档创建失败');
    }
  } catch (error) {
    ElMessage.error('网络错误，创建文档失败');
  }
};

const editDoc = async () => {
  if (!await validateForm()) return;
  try {
    const payload = {...docForm};
    const {data} = await updateDoc(payload);
    if (data.flag) {
      ElMessage.success('文档修改成功');
    } else {
      ElMessage.error(data.msg || '修改失败');
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