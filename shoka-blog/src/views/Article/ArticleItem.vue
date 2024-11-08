<template>
  <!-- 悬浮排序选项卡 -->
  <div class="sort-options">
    <span
        class="sort-option"
        @click="changeSort('id')"
        :class="{ active: queryParams.sort === 'id' }"
    >
      <svg-icon icon-class="calendar" size="1rem"></svg-icon> 按时间
    </span>
    <span
        class="sort-option"
        @click="changeSort('views')"
        :class="{ active: queryParams.sort === 'views' }"
    >
      <svg-icon icon-class="fun" size="1rem"></svg-icon> 按热度
    </span>
    <el-select
        clearable
        v-model="selectedTag"
        placeholder="选择标签"
        size="large"
        style="width: 238px"
        @change="filterByTag"
    >
      <el-option
          v-for="tag in tagList"
          :key="tag.id"
          :label="tag.tagName"
          :value="tag.id"
      />
    </el-select>
    <div class="date-picker-container">
      <el-date-picker
          is-range
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="filterByDate"
      ></el-date-picker>
    </div>

  </div>

  <div class="article-item" v-animate="['slideUpBigIn']" v-for="article of articleList" :key="article.id">
    <!-- 文章缩略图 -->
    <div class="article-cover">
      <router-link :to="`/article/${article.id}`" href="">
        <img class="cover" v-lazy="article.articleCover"/>
      </router-link>
    </div>
    <!-- 文章信息 -->
    <div class="article-info">
      <div class="article-meta">
        <!-- 置顶 -->
        <span class="top" v-if="article.isTop == 1">
          <svg-icon icon-class="top" size="0.85rem" style="margin-right: 0.15rem"></svg-icon>置顶</span>
        <!-- 发表时间 -->
        <span class="meta-item ml">
          <svg-icon icon-class="calendar" size="0.9rem" style="margin-right: 0.15rem"></svg-icon>{{
            formatDate(article.createTime)
          }}
        </span>
        <!-- 文章标签 -->
        <router-link class="meta-item ml" :to="`/tag/${tag.id}`" v-for="tag in article.tagVOList" :key="tag.id">
          <svg-icon icon-class="tag" size="0.9rem" style="margin-right: 0.15rem"></svg-icon>
          {{ tag.tagName }}
        </router-link>
      </div>
      <!-- 文章标题 -->
      <h3 class="article-title">
        <router-link :to="`/article/${article.id}`">
          {{ article.articleTitle }}
        </router-link>
      </h3>
      <!-- 文章内容 -->
      <div class="article-content">{{ article.articleContent }}</div>
      <!-- 文章分类 -->
      <div class="article-category">
        <svg-icon icon-class="qizhi" size="0.85rem" style="margin-right: 0.15rem"></svg-icon>
        <router-link :to="`/category/${article.category.id}`">{{
            article.category.categoryName
          }}
        </router-link>
      </div>
      <!-- 阅读按钮 -->
      <router-link class="article-btn" :to="`/article/${article.id}`">more...</router-link>
    </div>
  </div>
  <Pagination v-if="count > 5" v-model:current="queryParams.current" :total="Math.ceil(count / 5)"></Pagination>
</template>

<script setup lang="ts">
import {getArticleList} from "@/api/article";
import {Article} from "@/api/article/types";
import {PageQuery, PageQueryArticle} from "@/model";
import {formatDate} from "@/utils/date";
import {formatDateTime} from "@/utils/date";
import {getTagList} from "@/api/tag";
import {Tag} from "@/api/tag/types";

const selectedTag = ref(null);
const tagList = ref<Tag[]>([]);
const dateRange = ref<[Date, Date]>([
  new Date(2023, 1, 10, 8, 40),
  new Date(2025, 11, 10, 9, 40),
])
const data = reactive({
  count: 0,
  queryParams: {
    current: 1,
    size: 5,
    sort: 'id',
    start : null,
    end : null,
    tagId: null
  } as PageQueryArticle,
  articleList: [] as Article[],
});

const {count, queryParams, articleList} = toRefs(data);

function changeSort(sortType: string) {
  queryParams.value.sort = sortType;
  console.log("Current sort:", queryParams.value.sort);
}

function filterByTag() {
  data.queryParams.tagId = selectedTag.value;
}

function filterByDate() {
  if(dateRange.value != null){
    queryParams.value.start = formatDateTime(dateRange.value[0]);
    queryParams.value.end = formatDateTime(dateRange.value[1]);
  }
  console.log(data.queryParams.start);
}

watch(
    () => [queryParams.value.current, queryParams.value.sort,dateRange.value,queryParams.value.tagId],
    () => {
      // 调用接口，使用新的日期范围
      filterByDate();
      getArticleList(queryParams.value).then(({data}) => {
        articleList.value = data.data.recordList;
        count.value = data.data.count;
      });
    }
);
onMounted(() => {
  getArticleList(queryParams.value).then(({data}) => {
    articleList.value = data.data.recordList;
    count.value = data.data.count;
  });
  getTagList().then(({data}) => {
    tagList.value = data.data;
  });
});

</script>

<style lang="scss" scoped>
/* 修改日期选择器容器的背景色和圆角 */
::v-deep .el-date-editor.el-input__wrapper {
  box-shadow: 0 0 0 1px var(--grey-9-a6) inset;
  padding: 18.3px 10px;
  color: var(--grey-9-a6);
}

.sort-options {
  display: flex;
  gap: 0.75rem; /* 选项之间的间隔 */
  margin-left: 10px;

  .sort-option {
    align-items: center;
    padding: 0.1rem 1.25rem;
    background-color: var(--color-light-grey);
    color: var(--grey-9-a6);
    border: 1px solid var(--grey-9-a6); // 设置选中状态的边框颜色
    border-radius: 0.5rem;
    cursor: pointer;
    transition: background 0.3s;

    &.active {
      background-color: var(--background-color);
      color: var(--primary-color);
      border: 1px solid var(--primary-color);
    }

    &:hover {
      background-color: var(--color-pink-light-a5);
    }
  }
}

.article-item {
  display: flex;
  height: 14rem;
  margin: 1.25rem 0.5rem 0;
  border-radius: 0.5rem;
  box-shadow: 0 0.625rem 1.875rem -0.9375rem var(--box-bg-shadow);
  animation-duration: 0.5s;
  transition: all 0.2s ease-in-out 0s;
  overflow: hidden;
  visibility: hidden;

  &:hover {
    box-shadow: 0 0 1.5rem var(--box-bg-shadow);

    .cover {
      transform: scale(1.05) rotate(1deg);
    }
  }

  &:nth-child(even) {
    flex-direction: row-reverse;

    .article-cover {
      margin-right: auto;
      margin-left: 1.5rem;
      -webkit-clip-path: polygon(0 0, 100% 0, 100% 100%, 8% 100%);
      clip-path: polygon(0 0, 100% 0, 100% 100%, 8% 100%);
      border-radius: 0 0.625rem 0.625rem 0;
    }

    .article-info {
      padding: 1rem 0 3rem 1.5rem;

      .article-meta {
        justify-content: flex-start;
      }
    }

    .article-btn {
      left: 0;
      right: auto;
      border-radius: 0 1rem;
      background-image: linear-gradient(to right, var(--color-orange) 0, var(--color-pink) 100%);
    }

    .article-category {
      right: 0.5rem;
      justify-content: flex-start;
    }
  }
}

.article-cover {
  width: 50%;
  margin-right: 1.5rem;
  clip-path: polygon(0 0, 92% 0, 100% 100%, 0 100%);
  border-radius: 0.625rem 0 0 0.625rem;

  .cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: all 0.2s ease-in-out 0s;
  }
}

.article-info {
  position: relative;
  width: 50%;
  padding: 1rem 1.5rem 3rem 0;

  .article-meta {
    display: flex;
    justify-content: flex-end;
    font-size: 0.8125rem;
    color: var(--grey-5);
  }

  .top {
    color: var(--color-orange);
  }

  .meta-item {
    display: flex;
    align-items: center;
  }

  .ml:not(:first-child) {
    margin-left: 0.625rem;
  }

  .article-title {
    text-overflow: ellipsis;
    white-space: nowrap;
    margin: 0.625rem 0;
    color: var(--primary-color);
    overflow: hidden;
  }

  .article-content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    max-height: 5rem;
    font-size: 0.875em;
    overflow: hidden;
  }
}

.sort-options {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.article-category {
  position: absolute;
  display: flex;
  align-items: center;
  bottom: 0.5rem;
  font-size: 0.8125em;
  color: var(--grey-5);
}

.article-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 0.3rem 1rem;
  border-radius: 1rem 0;
  color: var(--grey-0);
  background-image: linear-gradient(to right, var(--color-pink) 0, var(--color-orange) 100%);
}

@media (max-width: 767px) {
  .article-item {
    flex-direction: column;
    height: fit-content;

    .article-cover {
      width: 100%;
      height: 14rem;
      margin: auto;
      clip-path: polygon(0 0, 100% 0, 100% 92%, 0 100%);
      border-radius: 0.625rem 0.625rem 0 0;
    }

    .article-info {
      width: 100%;
      height: 14rem;
      padding: 0 1rem 3rem;
    }

    &:nth-child(even) {
      flex-direction: column;

      .article-cover {
        width: 100%;
        margin: auto;
        clip-path: polygon(0 0, 100% 0, 100% 100%, 0 92%);
        border-radius: 0.625rem 0.625rem 0 0;
      }

      .article-info {
        padding: 0 1rem 3rem;
      }
    }
  }
}
</style>
