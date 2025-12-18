<template>
  <div class="page-header">
    <h1 class="page-title">分类</h1>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="">
    <!-- 波浪 -->
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <!-- 博客分类区块 -->
      <div class="category-section category-card">
        <h2 class="section-title blog-title">
          <span class="title-decor"></span>
          博客分类
        </h2>
        <div class="chart-wrapper">
          <Echarts :options="categoryOption" class="category-chart"></Echarts>
        </div>
        <div class="category-list-wrap">
          <ul class="category-list blog-categories">
            <li class="category-item" v-for="category in categoryList" :key="'blog-' + category.id">
              <router-link :to="`/category/${category.id}`" class="category-link">
                {{ category.categoryName }}
              </router-link>
              <span class="category-count">({{ category.articleCount }})</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- 协作分类区块 -->
      <div class="category-section category-card">
        <h2 class="section-title collab-title">
          <span class="title-decor"></span>
          协作分类
        </h2>
        <div class="chart-wrapper">
          <Echarts :options="collabCategoryOption" class="category-chart"></Echarts>
        </div>
        <div class="category-list-wrap">
          <ul class="category-list collab-categories">
            <li class="category-item" v-for="category in collabCategoryList" :key="'collab-' + category.id">
              <router-link :to="`/collab/category/${category.id}`" class="category-link">
                {{ category.categoryName }}
              </router-link>
              <span class="category-count">({{ category.docCount }})</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getCategoryList } from "@/api/category";
import { Category } from "@/api/category/types";
import Echarts from "@/components/Echarts/index.vue";
import Waves from "@/components/Waves/index.vue";
import { ref, reactive, onMounted } from "vue";

interface CollabCategory {
  id: number;
  categoryName: string;
  docCount: number;
}

const mockCollabCategories: CollabCategory[] = [
  { id: 1, categoryName: "技术文档", docCount: 5 },
  { id: 2, categoryName: "项目方案", docCount: 8 },
  { id: 3, categoryName: "学习笔记", docCount: 2 },
  { id: 4, categoryName: "会议记录", docCount: 9 },
  { id: 5, categoryName: "问题解决方案", docCount: 1 },
];
const collabCategoryList = ref<CollabCategory[]>([]);

let categoryOption = reactive({
  tooltip: {
    trigger: "item",
    formatter: "{a} <br/>{b} : {c} ({d}%)",
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
      name: "分类统计",
      type: "pie",
      radius: [35, 130],
      center: ["50%", "47%"],
      roseType: "area",
      itemStyle: {
        borderRadius: 6,
      },
      data: [] as {
        value: number;
        name: string;
      }[],
    },
  ],
});

const collabCategoryOption = reactive({
  tooltip: {
    trigger: "item",
    formatter: "{a} <br/>{b} : {c} ({d}%)",
  },
  title: {
    text: "协作分类分布",
    x: "center",
  },
  legend: { top: "bottom" },
  series: [
    {
      name: "协作文档数",
      type: "pie",
      radius: [35, 130],
      center: ["50%", "47%"],
      roseType: "area",
      itemStyle: { borderRadius: 6 },
      data: [] as { value: number; name: string }[],
    },
  ],
});

const categoryList = ref<Category[]>([]);
onMounted(() => {
  getCategoryList().then(({ data }) => {
    categoryList.value = data.data;
    if (data.data != null) {
      data.data.forEach((item) => {
        categoryOption.series[0].data.push({
          value: item.articleCount,
          name: item.categoryName,
        });
      });
    }
  });
  collabCategoryList.value = mockCollabCategories;
  collabCategoryList.value.forEach((item) => {
    collabCategoryOption.series[0].data.push({
      value: item.docCount,
      name: item.categoryName,
    });
  });
});
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

// 分类区块卡片化样式
.category-card {
  margin-bottom: 2rem;
  padding: 1.5rem;
  border-radius: 8px;
  background-color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
  }
}

// 区域标题样式
.section-title {
  margin-bottom: 1.5rem;
  padding-bottom: 0;
  border-bottom: none;
  font-size: 1.2rem;
  font-weight: 600;
  display: flex;
  align-items: center;
}

// 标题装饰块
.title-decor {
  display: inline-block;
  width: 8px;
  height: 1.2rem;
  margin-right: 0.5rem;
  vertical-align: middle;
  border-radius: 2px;
}

// 博客分类标题样式
.blog-title {
  color: #8e8cd8;

  .title-decor {
    background-color: #8e8cd8;
  }
}

// 协作分类标题样式
.collab-title {
  color: #4299e1;

  .title-decor {
    background-color: #4299e1;
  }
}

// 图表容器样式
.chart-wrapper {
  width: 100%;
  padding: 1rem;
  margin: 0 auto 1.5rem;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  transition: transform 0.3s ease;

  &:hover {
    transform: scale(1.01);
  }
}

// 图表基础样式
.category-chart {
  width: 100%;
  height: 300px;
  margin: 0;
}

// 分类列表容器
.category-list-wrap {
  padding: 0.5rem 1rem;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

// 分类列表通用样式
.category-list {
  @include flex;
  flex-wrap: wrap;
  margin: 1.5rem 0;
  padding-left: 0.5rem;
  list-style: none;
}

// 列表项样式
.category-item {
  padding: 0.8rem 1.2rem;
  margin: 0.4rem;
  border-radius: 20px;
  transition: background-color 0.3s ease;
  position: relative;

  &:before {
    content: "";
    position: absolute;
    left: -0.5rem;
    top: 50%;
    transform: translateY(-50%);
    width: 8px;
    height: 8px;
    border-radius: 50%;
    transition-duration: 0.3s;
  }
}

// 博客分类列表项
.blog-categories .category-item {
  &:before {
    background-color: #8e8cd8;
  }

  &:hover {
    background-color: rgba(142, 140, 216, 0.05);
  }
}

// 协作分类列表项
.collab-categories .category-item {
  &:before {
    background-color: #4299e1;
  }

  &:hover {
    background-color: rgba(66, 153, 225, 0.05);
  }
}

// 分类链接样式
.category-link {
  position: relative;
  text-decoration: none;
  transition: color 0.3s ease;

  &:after {
    content: "";
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 0;
    height: 2px;
    transition: width 0.3s ease;
  }

  &:hover {
    text-decoration: none;
  }
}

// 博客分类链接
.blog-categories .category-link {
  color: #333;

  &:after {
    background-color: #8e8cd8;
  }

  &:hover {
    color: #8e8cd8;
  }

  &:hover:after {
    width: 100%;
  }
}

// 协作分类链接
.collab-categories .category-link {
  color: #333;

  &:after {
    background-color: #4299e1;
  }

  &:hover {
    color: #4299e1;
  }

  &:hover:after {
    width: 100%;
  }
}

// 数量统计样式
.category-count {
  margin-left: 0.5rem;
  font-size: 0.95rem;
  color: #999;
  transition: color 0.3s ease;
}

.category-item:hover .category-count {
  color: #666;
}

// 响应式调整
@media (max-width: 768px) {
  .category-card {
    padding: 1rem;
    margin-bottom: 1.5rem;
  }

  .chart-wrapper {
    padding: 0.5rem;
  }

  .category-chart {
    height: 220px;
  }

  .category-item {
    padding: 0.6rem 1rem;
    margin: 0.3rem;
  }

  .section-title {
    font-size: 1.1rem;
  }
}
</style>