<template>
  <div class="page-header">
    <h1 class="page-title">所有标签</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="标签页面背景">
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="page-container">
      <!-- 第一部分：博客标签 -->
      <div class="tag-section animated-section">
        <div class="section-header">
          <h2 class="section-title blog-title">博客标签</h2>
        </div>
        <div class="tag-cloud blog-tags">
          <router-link
              :to="`/tag/${tag.id}`"
              class="tag-item"
              v-for="tag in blogTagList"
              :key="'blog-' + tag.id"
              :style="{
                'font-size': getBlogTagSize(tag.articleCount),
                'color': getBlogTagColor(tag.articleCount)
              }">
            {{ tag.tagName }}
            <sup>{{ tag.articleCount }}</sup>
          </router-link>
        </div>
      </div>

      <!-- 第二部分：协作标签 -->
      <div class="tag-section animated-section">
        <div class="section-header">
          <h2 class="section-title collab-title">协作标签</h2>
        </div>
        <div class="tag-cloud collab-tags">
          <router-link
              :to="`/collab/tag/${tag.id}`"
              class="tag-item"
              v-for="tag in collabTagList"
              :key="'collab-' + tag.id"
              :style="{
                'font-size': getCollabTagSize(tag.docCount),
                'color': getCollabTagColor(tag.docCount)
              }">
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
import Waves from "@/components/Waves/index.vue";

// 数据变量
const blogTagList = ref<Tag[]>([]);
const collabTagList = ref<CollabTag[]>([]);

// 博客标签样式逻辑
const getBlogTagSize = (freq: number) => {
  const minSize = 1;
  const maxSize = 1.5;
  const maxCount = Math.max(...blogTagList.value.map(t => t.articleCount), 1);
  return (minSize + (freq / maxCount) * (maxSize - minSize)) + "rem";
};
const getBlogTagColor = (articleCount: number) => {
  const maxCount = Math.max(...blogTagList.value.map(t => t.articleCount), 1);
  const ratio = articleCount / maxCount;

  // 定义多个色系
  const colorSchemes = [
    {hue: 240, sat: 30, light: 50}, // 紫色
    {hue: 20, sat: 70, light: 45}, // 蓝色
    {hue: 180, sat: 65, light: 40}, // 青色
    {hue: 45, sat: 80, light: 55}, // 黄色
    {hue: 0, sat: 75, light: 50}  // 红色
  ];

  // 根据文章数量选择色系
  const schemeIndex = Math.floor(ratio * colorSchemes.length);
  const scheme = colorSchemes[Math.min(schemeIndex, colorSchemes.length - 1)];

  // 微调饱和度和亮度
  const saturation = scheme.sat + (ratio * 20);
  const lightness = scheme.light - (ratio * 15);

  return `hsl(${scheme.hue}, ${saturation}%, ${lightness}%)`;
};


// 协作标签样式逻辑
const getCollabTagSize = (freq: number) => {
  const minSize = 1.0;
  const maxSize = 1.5;
  const maxCount = Math.max(...collabTagList.value.map(t => t.docCount), 1);
  return (minSize + (freq / maxCount) * (maxSize - minSize)) + "rem";
};

const getCollabTagColor = (docCount: number) => {
  const maxCount = Math.max(...collabTagList.value.map(t => t.docCount), 1);
  const ratio = docCount / maxCount;

  // 定义多个色系用于协作标签
  const colorSchemes = [
    {hue: 210, sat: 70, light: 45}, // 蓝色
    {hue: 180, sat: 65, light: 40}, // 青色
    {hue: 240, sat: 60, light: 50}, // 靛蓝色
    {hue: 200, sat: 75, light: 40}, // 天蓝色
    {hue: 190, sat: 68, light: 48}  // 青蓝色
  ];

  // 根据文档数量选择色系
  const schemeIndex = Math.floor(ratio * colorSchemes.length);
  const scheme = colorSchemes[Math.min(schemeIndex, colorSchemes.length - 1)];

  // 微调饱和度和亮度
  const saturation = scheme.sat + (ratio * 20);
  const lightness = scheme.light - (ratio * 15);

  return `hsl(${scheme.hue}, ${saturation}%, ${lightness}%)`;
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

.section-subtitle {
  font-size: 1rem;
  color: #666;
  font-weight: 700;
}

.section-title {
  padding-bottom: 0.5rem;
  border-bottom: 2px solid;
  font-size: 1.4rem;
  font-weight: 600;
}

// 博客标签标题：紫灰色系
.blog-title {
  border-color: #8e8cd8;
  color: #6a6aab;
}

// 协作标签标题：蓝色系
.collab-title {
  border-color: #4299e1;
  color: #2b6cb0;
}

// 标签云通用样式
.tag-cloud {
  text-align: center;
  padding: 1.5rem 0;
}

.tag-item {
  display: inline-block;
  padding: 0.4rem 1.5rem;
  margin: 0.7rem;
  text-decoration: none;
  border-radius: 50px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  font-weight: 500;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
    z-index: 10;
  }

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(255, 255, 255, 0.2);
    opacity: 0;
    transition: opacity 0.3s;
    z-index: -1;
  }

  &:hover::before {
    opacity: 1;
  }
}

// 博客标签交互样式
.blog-tags .tag-item {
  background: linear-gradient(135deg, rgba(142, 140, 216, 0.1) 0%, rgba(142, 140, 216, 0.05) 100%);
  border: 1px solid rgba(142, 140, 216, 0.2);

  &:hover {
    background: linear-gradient(135deg, rgba(142, 140, 216, 0.2) 0%, rgba(142, 140, 216, 0.1) 100%);
    border-color: rgba(142, 140, 216, 0.4);
  }
}

// 协作标签交互样式
.collab-tags .tag-item {
  background: linear-gradient(135deg, rgba(66, 153, 225, 0.1) 0%, rgba(66, 153, 225, 0.05) 100%);
  border: 1px solid rgba(66, 153, 225, 0.2);

  &:hover {
    background: linear-gradient(135deg, rgba(66, 153, 225, 0.2) 0%, rgba(66, 153, 225, 0.1) 100%);
    border-color: rgba(66, 153, 225, 0.4);
  }
}

// 数量标识
sup {
  margin-left: 0.3rem;
  font-size: 0.7em;
  opacity: 0.8;
  background: rgba(0, 0, 0, 0.05);
  padding: 0.1rem 0.4rem;
  border-radius: 10px;
  vertical-align: super;
}

// 动画效果
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animated-section {
  animation: fadeInUp 0.6s ease-out forwards;
  opacity: 0;

  &:nth-child(1) {
    animation-delay: 0.1s;
  }

  &:nth-child(2) {
    animation-delay: 0.3s;
  }
}

// 响应式调整
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }

  .tag-section {
    padding: 1.5rem;
    margin-bottom: 1.5rem;
  }

  .section-title {
    font-size: 1.5rem;
  }

  .tag-item {
    padding: 0.4rem 0.8rem;
    margin: 0.3rem;
    font-size: 0.9rem;
  }

  .page-container {
    padding: 0 0.5rem;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 1.7rem;
  }

  .tag-section {
    padding: 1.2rem;
  }

  .section-title {
    font-size: 1.3rem;
  }

  .tag-item {
    padding: 0.3rem 0.6rem;
    margin: 0.2rem;
    font-size: 0.8rem;
  }
}
</style>
