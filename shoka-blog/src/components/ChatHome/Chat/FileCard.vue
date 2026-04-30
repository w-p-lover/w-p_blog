<template>
  <a :href="file" target="_blank" class="file-card">
    <img src="../../../assets/img/fileImg/unknowfile.png" alt="" v-if="fileType === 0"/>
    <img src="../../../assets/img/fileImg/word.png" alt="" v-else-if="fileType === 1"/>
    <img src="../../../assets/img/fileImg/excel.png" alt="" v-else-if="fileType === 2"/>
    <img src="../../../assets/img/fileImg/ppt.png" alt="" v-else-if="fileType === 3"/>
    <img src="../../../assets/img/fileImg/pdf.png" alt="" v-else-if="fileType === 4"/>
    <img src="../../../assets/img/fileImg/zpi.png" alt="" v-else-if="fileType === 5"/>
    <img src="../../../assets/img/fileImg/txt.png" alt="" v-else/>
    <div class="word">
      <span>{{ fileName || '未知' }}</span>
      <span>{{ fileSize || '文件大小未知' }}</span>
    </div>
  </a>
</template>

<script>
import {defineComponent, watch, onMounted} from 'vue';

export default defineComponent({
  name: 'FileCard',
  props: {
    fileType: {
      type: Number,
      required: true,
    },
    file: {
      type: String, // File 对象类型
      required: true,
    },
    fileName: {
      type: String,
      required: true,
    },
    fileSize: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    // 监听 file 对象变化
    watch(
        () => props.file,
        (newFile) => {
          console.log('文件更新:', newFile);
        }
    );

    // 生命周期：组件挂载
    onMounted(() => {
      console.log('文件:', props.file);
      console.log('文件类型:', props.fileType);
    });

    return {};
  },
});
</script>

<style lang="scss" scoped>
.file-card {
  width: 250px;
  min-height: 92px;
  background: rgba(255, 255, 255, 0.075);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px;
  box-sizing: border-box;
  cursor: pointer;
  transition: 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.105);
    transform: translateY(-1px);
  }

  img {
    width: 60px;
    height: 60px;
  }

  .word {
    width: 60%;
    margin-left: 10px;
    overflow: hidden;

    span {
      width: 90%;
      display: inline-block;
      color: #f2f0ea;
    }

    span:first-child {
      font-size: 14px;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    span:last-child {
      font-size: 12px;
      color: rgba(220, 224, 218, 0.58);
    }
  }
}
</style>
