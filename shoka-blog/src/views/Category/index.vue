<template>
  <div class="page-header">
    <h1 class="page-title">分类</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg"
         alt="">
    <!-- 波浪 -->
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <div class="category-section">
        <h2 class="section-title blog-title">博客分类</h2> <!-- 区域标题区分 -->
        <Echarts :options="categoryOption" class="category-chart"></Echarts>
        <ul class="category-list blog-categories">
          <li class="category-item" v-for="category in categoryList" :key="'blog-' + category.id">
            <router-link :to="`/category/${category.id}`">{{ category.categoryName }}</router-link>
            <span class="category-count">({{ category.articleCount }})</span>
          </li>
        </ul>
      </div>

      <!-- 第二部分：协作分类 -->
      <div class="category-section">
        <h2 class="section-title collab-title">协作分类</h2> <!-- 区域标题区分 -->
        <Echarts :options="collabCategoryOption" class="category-chart"></Echarts>
        <ul class="category-list collab-categories">
          <li class="category-item" v-for="category in collabCategoryList" :key="'collab-' + category.id">
            <router-link :to="`/collab/category/${category.id}`">{{ category.categoryName }}</router-link>
            <span class="category-count">({{ category.docCount }})</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {getCategoryList} from "@/api/category";
import {Category} from "@/api/category/types";
import Echarts from "@/components/Echarts/index.vue";

interface CollabCategory {
  id: number;
  categoryName: string;
  docCount: number
}

const mockCollabCategories: CollabCategory[] = [
  {id: 1, categoryName: "技术文档", docCount: 5},
  {id: 2, categoryName: "项目方案", docCount: 8},
  {id: 3, categoryName: "学习笔记", docCount: 2},
  {id: 4, categoryName: "会议记录", docCount: 9},
  {id: 5, categoryName: "问题解决方案", docCount: 1},
];
const collabCategoryList = ref<CollabCategory[]>([]);

let categoryOption = reactive({
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b} : {c} ({d}%)'
  },
  title: {
    text: "文章分类统计图🎉",
    x: "center",
  },
  legend: {
    top: "bottom",
  },
  series: [
    {
      name: '分类统计',
      type: 'pie',
      radius: [35, 130],
      center: ['50%', '47%'],
      roseType: 'area',
      itemStyle: {
        borderRadius: 6
      },
      data: [] as {
        value: number;
        name: string;
      }[],
    }
  ]
});

const collabCategoryOption = reactive({
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b} : {c} ({d}%)'
  },
  title: {
    text: "协作分类分布",
    x: "center"
  },
  legend: {top: "bottom"},
  series: [{
    name: '协作文档数',
    type: 'pie',
    radius: [35, 130],
    center: ['50%', '47%'],
    roseType: 'area',
    itemStyle: {borderRadius: 6},
    data: [] as { value: number; name: string }[]
  }]
});

const categoryList = ref<Category[]>([]);
onMounted(() => {
  getCategoryList().then(({data}) => {
    categoryList.value = data.data;
    if (data.data != null) {
      data.data.forEach((item) => {
        categoryOption.series[0].data.push({
          value: item.articleCount,
          name: item.categoryName,
        });
      });
    }
  })
  collabCategoryList.value = mockCollabCategories;
  collabCategoryList.value.forEach(item => {
    collabCategoryOption.series[0].data.push({
      value: item.docCount,
      name: item.categoryName
    });
  });
});
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.category-section {
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

// 博客分类标题：紫灰色系
.blog-title {
  border-color: #8e8cd8;
  color: #8e8cd8;
}

// 协作分类标题：蓝色系
.collab-title {
  border-color: #4299e1;
  color: #4299e1;
}

// 图表样式
.category-chart {
  width: 100%;
  height: 300px;
  margin: 0 auto 1.5rem;
}

// 列表前缀点（区分两类分类）
.category-item:before {
  display: inline-block;
  position: relative;
  left: -0.75rem;
  width: 12px;
  height: 12px;
  border: 0.2rem solid var(--color-blue);
  border-radius: 50%;
  background: var(--grey-0);
  content: "";
  transition-duration: 0.3s;
}

.category-list {
  @include flex;
  flex-wrap: wrap;
  margin: 3rem 0 1rem 0;
}

.category-item {
  padding: 0.12em 1.2em 0.12em 1.4em;
}

.category-item:before {
  display: inline-block;
  position: relative;
  left: -0.75rem;
  width: 12px;
  height: 12px;
  border: 0.2rem solid var(--color-blue);
  border-radius: 50%;
  background: var(--grey-0);
  content: "";
  transition-duration: 0.3s;
}

.category-item:hover:before {
  border: 0.2rem solid var(--color-orange);
}

.category-item a:hover {
  transition: all 0.3s;
  color: #8e8cd8;
}

.category-item a:not(:hover) {
  transition: all 0.3s;
}

.category-count {
  margin-left: 0.5rem;
  font-size: 0.95rem;
  color: var(--grey-5);
}

// 博客分类前缀点
.blog-categories .category-item:before {
  background-color: #8e8cd8;
}

// 协作分类前缀点
.collab-categories .category-item:before {
  background-color: #4299e1;
}


.blog-categories .category-item a:hover {
  color: #8e8cd8;
}

.collab-categories .category-item a:hover {
  color: #4299e1;
}

// 响应式调整
@media (max-width: 768px) {
  .category-section {
    padding: 1rem;
  }

  .category-chart {
    height: 220px;
  }

  .category-item {
    padding: 0.6rem 1rem;
  }
}

</style>
