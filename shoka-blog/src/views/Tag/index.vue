<template>
  <div class="page-header">
    <h1 class="page-title">所有标签</h1> <!-- 总标题 -->
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="标签页面背景">
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="page-container">
      <!-- 第一部分：博客标签 -->
      <div class="tag-section">
        <h2 class="section-title blog-title">博客标签</h2> <!-- 区域标题区分 -->
        <div class="tag-cloud blog-tags">
          <router-link :to="`/tag/${tag.id}`" class="tag-item"
                       v-for="tag in blogTagList" :key="'blog-' + tag.id"
                       :style="{ 'font-size': getBlogTagSize(tag.articleCount), 'color': getBlogTagColor() }">
            {{ tag.tagName }}
            <sup>{{ tag.articleCount }}</sup>
          </router-link>
        </div>
      </div>

      <!-- 第二部分：协作标签 -->
      <div class="tag-section">
        <h2 class="section-title collab-title">协作标签</h2> <!-- 区域标题区分 -->
        <div class="tag-cloud collab-tags">
          <router-link :to="`/collab/tag/${tag.id}`" class="tag-item"
                       v-for="tag in collabTagList" :key="'collab-' + tag.id"
                       :style="{ 'font-size': getCollabTagSize(tag.docCount), 'color': getCollabTagColor() }">
            {{ tag.tagName }}
            <sup>{{ tag.docCount }}</sup>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted} from 'vue';
import {getTagList} from "@/api/tag";
import {Tag} from "@/api/tag/types";
import {getDocTags} from "@/api/collab";
import {CollabTag} from "@/api/collab/type";



// 数据变量
const blogTagList = ref<Tag[]>([]);
const collabTagList = ref<CollabTag[]>([]);

// 博客标签样式逻辑
const getBlogTagSize = (freq: number) => {
  return ((1 + 6 * freq / 8) / 3) * 2 + "rem";
};
const getBlogTagColor = () => {
  // 博客标签用随机色（保留原有风格）
  return `rgb(${Math.floor(Math.random() * 150) + 50}, ${Math.floor(Math.random() * 150) + 50}, ${Math.floor(Math.random() * 200) + 55})`;
};

// 协作标签样式逻辑
const getCollabTagSize = (freq: number) => {
  return ((1 + 6 * freq / 20) / 3) * 2 + "rem"; // 基于最大数量20计算
};
const getCollabTagColor = () => {
  // 协作标签用蓝色系（统一且区分）
  const colors = [
    'rgb(66, 153, 225)', 'rgb(71, 183, 172)', 'rgb(95, 156, 228)',
    'rgb(102, 168, 227)', 'rgb(80, 174, 209)'
  ];
  return colors[Math.floor(Math.random() * colors.length)];
};

// 加载数据
onMounted(() => {
  getTagList().then(({data}) => {
    blogTagList.value = data.data;
  });
  getDocTags().then(({data}) => {
    collabTagList.value = data.data;
  });
});

</script>

<style lang="scss" scoped>
// 标签区域容器
.tag-section {
  padding: 1rem;
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.9);
}

// 区域标题样式（核心区分点）
.section-title {
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid;
  font-size: 1.2rem;
  font-weight: 600;
}

// 博客标签标题：紫灰色系
.blog-title {
  border-color: #8e8cd8;
  color: #8e8cd8;
}

// 协作标签标题：蓝色系
.collab-title {
  border-color: #4299e1;
  color: #4299e1;
}

// 标签云通用样式
.tag-cloud {
  text-align: center;
  padding: 1rem 0;
}

.tag-item {
  display: inline-block;
  padding: 0.2rem 0.2rem;
  margin: 0.2rem;
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.3s ease;
}

// 博客标签交互样式
.blog-tags .tag-item:hover {
  transform: scale(1.1);
  background-color: rgba(142, 140, 216, 0.1);
}

// 协作标签交互样式
.collab-tags .tag-item:hover {
  transform: scale(1.1);
  background-color: rgba(66, 153, 225, 0.1);
}

// 数量标识
sup {
  margin-left: 0.3rem;
  font-size: 0.8rem;
  opacity: 0.8;
}

// 响应式调整
@media (max-width: 768px) {
  .tag-section {
    padding: 1rem;
  }

  .tag-item {
    padding: 0.3rem 0.8rem;
    margin: 0.3rem;
  }
}
</style>
