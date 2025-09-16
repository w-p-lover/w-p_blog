<template>
  <div class="quill-card">
    <!-- Quill 编辑器 -->
    <QuillEditor
        ref="myQuillEditor"
        theme="snow"
        v-model:content="content"
        :options="data.editorOption"
        contentType="html"
        @update:content="setValue"
    />
    <!-- 自定义图片上传按钮 -->
    <input type="file" hidden accept=".jpg,.png" ref="fileBtn" @change="handleUpload"/>
  </div>
</template>

<script setup>
import {QuillEditor} from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import {reactive, ref, onMounted, toRaw, watch} from 'vue'

const props = defineProps(['value'])
const emit = defineEmits(['updateValue'])

const content = ref('')
const myQuillEditor = ref()
const fileBtn = ref()

// 编辑器配置
const data = reactive({
  editorOption: {
    modules: {
      toolbar: [
        ['bold', 'italic', 'underline', 'strike'],
        [{'size': ['small', false]}],
        [{'font': []}],
        [{'align': []}],
        [{'list': 'ordered'}, {'list': 'bullet'}],
        [{'indent': '-1'}, {'indent': '+1'}],
        [{'header': 1}, {'header': 2}],
        ['image'],
        [{'direction': 'rtl'}],
        [{'color': []}, {'background': []}]
      ]
    },
    placeholder: '请输入内容...'
  }
})

// 回显内容
watch(() => props.value, (val) => {
  if (val && myQuillEditor.value) {
    toRaw(myQuillEditor.value).setHTML(val)
  }
}, {deep: true})

// 设置内容并抛出到父组件
const setValue = () => {
  if (!myQuillEditor.value) return
  const text = toRaw(myQuillEditor.value).getHTML()
  emit('updateValue', text)
}

// 自定义图片上传
const imgHandler = (state) => {
  if (state) {
    fileBtn.value.click()
  }
}

const handleUpload = (e) => {
  const files = Array.from(e.target.files)
  if (!files.length) return
  const formdata = new FormData()
  formdata.append('file', files[0])

  backsite.uploadFile(formdata) // 服务端接口
      .then(res => {
        if (res.data.url) {
          const quill = toRaw(myQuillEditor.value).getQuill()
          const length = quill.getSelection()?.index || 0
          quill.insertEmbed(length, 'image', res.data.url)
          quill.setSelection(length + 1)
        }
      })
}

// 初始化编辑器
onMounted(() => {
  const quill = toRaw(myQuillEditor.value).getQuill()
  quill.getModule('toolbar').addHandler('image', imgHandler)
})
</script>

<style scoped lang="scss">
/* 外层卡片 */
.quill-card {
  border-radius: 8px;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .06);
}

:deep(.ql-snow.ql-toolbar button, .ql-snow .ql-toolbar button) {
  background: none;
  border: none;
  cursor: pointer;
  display: inline-block;
  height: 26px;
  padding: 4px 5px;
  width: 32px;
  margin-left: 0;
  margin-right: 0;
}

/* 工具栏样式 */
:deep(.ql-toolbar) {
  border-bottom: 1px solid rgba(105, 119, 165, 0.73);
  padding: 0 0 6px 10px;
  background: #f9fafb;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
  font-family: var(--el-font-family), serif;
}

:deep(.ql-toolbar.ql-snow .ql-formats) {
  margin-right: 6px;
}

/* 编辑器区域 */
:deep(.ql-editor) {
  min-height: 240px;
  font-size: 14px;
  line-height: 1.8;
  padding: 16px;
  border: none;
  font-family: var(--el-font-family), serif;
}

/* 编辑器容器圆角边框 */
:deep(.ql-container) {
  border: 1px solid #dcdfe6;
  border-radius: 0 0 8px 8px;
}

/* 工具栏按钮颜色 */
:deep(.ql-toolbar button svg) {
  stroke: #409eff; /* Element 主色 */
}

:deep(.ql-snow .ql-picker.ql-size) {
  height: 40px;
  line-height: 40px;
  width: 79px;
}

:deep(.ql-snow .ql-picker.ql-font) {
  height: 40px;
  line-height: 40px;
  width: 100px;
}

:deep(.ql-snow .ql-picker-options .ql-picker-item) {
  cursor: pointer;
  display: block;
  padding: 0 0;
}
</style>
