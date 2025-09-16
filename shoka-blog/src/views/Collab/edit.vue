<template>
  <!-- 页面头部：保持结构一致性，适配Element风格 -->
  <div class="page-header">
    <h1 class="page-title">新建协作文档</h1>
    <img
        class="page-cover"
        src="https://ik.imagekit.io/nicexl/Wallpaper/ba41a32b219e4b40ad055bbb52935896_Y0819msuI.jpg"
        alt="新建协作文档背景"
    >
    <!-- 若Waves组件存在，保留；无则注释 -->
    <!-- <Waves></Waves> -->
  </div>

  <div class="bg">
    <div class="page-container">
      <el-card class="collab-create-card" shadow="hover">
        <h2 class="section-title blog-title">文档新增</h2> <!-- 区域标题区分 -->
        <!-- 表单：用Element Form组件，支持规则验证 -->
        <el-form
            ref="createFormRef"
            :model="docForm"
            :rules="formRules"
            label-width="120px"
            class="create-form"
        >
          <!-- 1. 基础信息区域 -->
          <el-form-item label="文档标题" prop="title">
            <el-input
                v-model="docForm.title"
                placeholder="输入协作文档标题（如：2024Q3产品需求文档）"
                maxlength="50"
                show-word-limit
            ></el-input>
          </el-form-item>

          <el-form-item label="文章分类" prop="categoryName">
            <el-tag
                type="success"
                v-show="docForm.categoryName"
                :disable-transitions="true"
                closable
                @close="removeCategory"
            >
              {{ docForm.categoryName }}
            </el-tag>

            <el-popover v-if="!docForm.categoryName" placement="bottom-start" width="460" trigger="click">
              <template #reference>
                <el-button type="success" plain>
                  <el-icon style="margin-right: 6px;">
                    <Edit/>
                  </el-icon>
                  添加分类
                </el-button>
              </template>

              <div class="popover-title">分类</div>

              <el-autocomplete
                  style="width: 100%"
                  v-model="categoryName"
                  :fetch-suggestions="searchCategory"
                  placeholder="请输入分类名搜索, Enter可添加自定义分类"
                  :trigger-on-focus="false"
                  @keyup.enter="saveCategory"
                  @select="handleSelectCategory"
              >
                <template #default="{ item }">
                  <div>{{ item.categoryName }}</div>
                </template>
              </el-autocomplete>

              <!-- 分类列表 -->
              <div class="popover-container">
                <div
                    v-for="item in categoryList"
                    :key="item.id"
                    class="category-item"
                    @click="addCategory(item.categoryName)"
                >
                  {{ item.categoryName }}
                </div>
              </div>
            </el-popover>
          </el-form-item>

          <el-form-item label="文章标签" prop="tagNameList">
            <el-tag
                v-for="(item, index) in docForm.tagNameList"
                :key="index"
                closable
                :disable-transitions="true"
                @close="removeTag(item)"
                style="margin-right: 1rem"
            >
              {{ item }}
            </el-tag>
            <el-popover placement="bottom-start" width="460" trigger="click" v-if="docForm.tagNameList.length < 3">
              <template #reference>
                <el-button type="success" plain>
                  <el-icon style="margin-right: 6px;">
                    <Edit/>
                  </el-icon>
                  添加标签
                </el-button>
              </template>
              <div class="popover-title">标签</div>
              <!-- 搜索框 -->
              <el-autocomplete
                  style="width: 100%"
                  v-model="tagName"
                  :fetch-suggestions="searchTag"
                  placeholder="请输入标签名搜索, Enter可添加自定义标签"
                  :trigger-on-focus="false"
                  @keyup.enter="saveTag"
                  @select="handleSelectTag"
              >
                <template #default="{ item }">
                  <div>{{ item.tagName }}</div>
                </template>
              </el-autocomplete>

              <!-- 标签候选 -->
              <div class="popover-container">
                <div style="margin-bottom: 1rem">可选标签</div>
                <el-tag
                    v-for="(item, index) in tagList"
                    :key="index"
                    :class="tagClass(item.tagName)"
                    @click="addTag(item.tagName)"
                    style="margin-right: 1rem"
                >
                  {{ item.tagName }}
                </el-tag>
              </div>
            </el-popover>
          </el-form-item>
          <el-form-item label="文档描述">
            <el-input
                v-model="docForm.desc"
                type="textarea"
                placeholder="简要说明文档用途（会显示在列表页摘要中）"
                rows="3"
                maxlength="200"
                show-word-limit
            ></el-input>
          </el-form-item>

          <!-- 2. 文档内容编辑：Element 多行输入框（可替换为富文本） -->
          <el-form-item label="文档内容" prop="content">
            <RichTextEditor
                v-model:value="docForm.content"
                style="width: 100%;"
            />
          </el-form-item>
          <!-- 3. 协作者设置：动态渲染协作者行 -->
          <el-form-item label="协作者">
            <div class="collab-group">
              <!-- 协作者行：循环渲染 -->
              <div class="collab-row" v-for="(item, idx) in docForm.collabs" :key="idx">
                <el-input
                    v-model="item.name"
                    placeholder="输入协作者姓名/账号"
                    style="width: 280px; margin-right: 6px;"
                ></el-input>
                <el-select
                    v-model="item.role"
                    placeholder="选择角色"
                    style="width: 180px; margin-right: 16px;"
                >
                  <el-option label="编辑者（可修改内容）" value="editor"></el-option>
                  <el-option label="查看者（仅阅读）" value="viewer"></el-option>
                </el-select>
                <el-button
                    type="text"
                    icon="el-icon-delete"
                    @click="removeCollab(idx)"
                    :disabled="docForm.collabs.length === 1"
                    class="del-collab-btn"
                ></el-button>
              </div>
              <!-- 添加协作者按钮 -->
              <el-button type="primary" @click="addCollab" style="margin-top:8px;">
                <el-icon style="margin-right: 6px;">
                  <Plus/>
                </el-icon>
                添加协作者
              </el-button>
            </div>
          </el-form-item>

          <!-- 4. 操作按钮区域：Element Button组件 -->
          <el-form-item class="form-btn-group">
            <el-button type="text" @click="handleCancel">取消</el-button>
            <el-button type="primary" @click="handleSaveDraft">保存为草稿</el-button>
            <el-button type="success" @click="handleSubmit">创建完成</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script>
import {
  ElCard, ElForm, ElFormItem, ElInput, ElSelect, ElOption,
  ElTag, ElButton, ElMessage
} from 'element-plus';
import {defineAsyncComponent} from 'vue'
import useStore from "@/store";
import {Edit, Plus} from '@element-plus/icons-vue'

const {user} = useStore();
export default {
  name: 'CollabCreatePage',
  components: {
    Edit,
    Plus,
    ElCard, ElForm, ElFormItem, ElInput, ElSelect, ElOption,
    ElTag, ElButton,
    RichTextEditor: defineAsyncComponent(() => import('@/components/Edit/index.vue'))
  },
  data() {
    return {
      // 表单数据
      docForm: {
        title: '', // 文档标题
        tagNameList: [],
        categoryName: '', // 已选标签
        desc: '', // 文档描述
        content: '', // 文档内容
        collabs: [ // 协作者列表（默认含当前用户）
          {name: this.getCurrentUserName(), role: 'editor'}
        ]
      },
      categoryName: '',
      tagName: '',
      categoryList: [
        {id: 1, categoryName: '前端开发'},
        {id: 2, categoryName: '后端架构'},
        {id: 3, categoryName: '算法与数据结构'},
        {id: 4, categoryName: '人工智能'},
        {id: 5, categoryName: '运维与部署'},
        {id: 6, categoryName: '产品设计'}
      ],

      // 🔹模拟的标签列表
      tagList: [
        {id: 101, tagName: 'Vue3'},
        {id: 102, tagName: 'React'},
        {id: 103, tagName: 'Node.js'},
        {id: 104, tagName: 'TypeScript'},
        {id: 105, tagName: 'SpringBoot'},
        {id: 106, tagName: 'Docker'},
        {id: 107, tagName: 'Redis'},
        {id: 108, tagName: 'AI/ML'},
        {id: 109, tagName: '大数据'}
      ],
      allTags: ['产品', '技术', '运营', '设计', '测试', '文档'], // 所有可选标签
      // 表单验证规则（Element Form规则）
      formRules: {
        title: [
          {required: true, message: '请输入文档标题', trigger: 'blur'},
          {max: 50, message: '标题长度不能超过50个字符', trigger: 'blur'}
        ],
        tags: [
          {required: true, message: '请至少选择1个标签', trigger: 'change'},
          {type: 'array', min: 1, message: '请至少选择1个标签', trigger: 'change'}
        ],
        categoryName: [
          {required: true, message: '请选择分类', trigger: 'change'}
        ],
        content: [
          { required: true, message: '请输入文档内容', trigger: 'blur' }
        ]
      },
      createFormRef: null // 表单引用
    };
  },
  methods: {
    getCurrentUserName() {
      // 示例：若用Vuex，返回this.getCurrentUser.name；若无，返回固定值
      return user.nickname;
    },
    tagClass(name) {
      return this.docForm.tagNameList.includes(name)
          ? 'tag-item-select'
          : 'tag-item'
    },
    searchCategory(query, cb) {
      // 接口搜索分类
      cb(this.categoryList.filter(i => i.categoryName.includes(query)));
    },
    handleSelectCategory(item) {
      this.docForm.categoryName = item.categoryName;
    },
    addCategory(name) {
      this.docForm.categoryName = name;
    },
    saveCategory() {
      if (this.categoryName) {
        this.docForm.categoryName = this.categoryName;
      }
    },
    removeCategory() {
      this.docForm.categoryName = '';
    },

    // 标签相关
    searchTag(query, cb) {
      cb(this.tagList.filter(i => i.tagName.includes(query)));
    },
    handleSelectTag(item) {
      if (!this.docForm.tagNameList.includes(item.tagName)) {
        this.docForm.tagNameList.push(item.tagName);
      }
    },
    addTag(name) {
      if (!this.docForm.tagNameList.includes(name) && this.docForm.tagNameList.length < 3) {
        this.docForm.tagNameList.push(name);
      }
    },
    saveTag() {
      if (
          this.tagName &&
          !this.docForm.tagNameList.includes(this.tagName) &&
          this.docForm.tagNameList.length < 3
      ) {
        this.docForm.tagNameList.push(this.tagName);
      }
      this.tagName = '';
    },
    removeTag(name) {
      this.docForm.tagNameList = this.docForm.tagNameList.filter(t => t !== name);
    },

  },

  // 表单验证：统一触发Element Form验证
  validateForm() {
    return new Promise((resolve) => {
      this.$refs.createFormRef.validate((isValid) => {
        resolve(isValid);
      });
    });
  },

  // 取消操作：未保存提示（Element MessageBox）
  async handleCancel() {
    // 判断是否有未保存内容
    const hasUnsaved = this.docForm.title || this.docForm.content || this.docForm.tagNameList.length;
    if (!hasUnsaved) {
      this.$router.push('/collab');
      return;
    }

    // Element 确认弹窗
    try {
      await this.$confirm(
          '文档内容尚未保存，确定要离开吗？',
          '提示',
          {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
      );
      this.$router.push('/collab');
    } catch (err) {
      // 取消离开，不做操作
    }
  },

  // 保存为草稿：状态设为“正在编辑”
  async handleSaveDraft() {
    const isValid = await this.validateForm();
    if (!isValid) return;

    // 构造草稿数据（根据接口需求调整）
    const draftData = {
      ...this.docForm,
      status: 'editing', // 草稿状态：正在编辑
      createTime: new Date().getTime(),
      version: '1.0',
      editCount: 0
    };

    try {
      ElMessage.success('草稿保存成功！');
      this.$router.push('/collab');
    } catch (err) {
      ElMessage.error('保存失败，请重试！');
      console.error('保存草稿错误：', err);
    }
  },

  // 创建完成：状态设为“已完成”
  async handleSubmit() {
    const isValid = await this.validateForm();
    if (!isValid) return;

    // 构造提交数据（根据接口需求调整）
    const submitData = {
      ...this.docForm,
      status: 'finished', // 完成状态：已完成
      createTime: new Date().getTime(),
      version: '1.0',
      editCount: 0
    };

    // 调用接口创建文档（示例：替换为你的接口请求）
    try {
      // await this.$api.collab.createDoc(submitData);
      ElMessage.success('协作文档创建成功！');
      this.$router.push('/collab');
    } catch (err) {
      ElMessage.error('创建失败，请重试！');
      console.error('创建文档错误：', err);
    }
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