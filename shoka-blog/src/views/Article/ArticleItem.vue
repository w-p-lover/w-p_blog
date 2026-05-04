<template>
  <div class="article-toolbar">
    <div class="sortList" aria-label="文章排序">
      <button
          type="button"
          class="sort-option"
          @click="changeSort('id')"
          :class="{ active: queryParams.sort === 'id' }"
      >
        <svg-icon icon-class="calendar" size="1rem"></svg-icon>
        <span>按时间</span>
      </button>
      <button
          type="button"
          class="sort-option"
          @click="changeSort('views')"
          :class="{ active: queryParams.sort === 'views' }"
      >
        <svg-icon icon-class="fun" size="1rem"></svg-icon>
        <span>按热度</span>
      </button>
    </div>
    <el-select
        clearable
        v-model="selectedTag"
        placeholder="选择标签"
        size="large"
        class="selectWidth"
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

  <div
      class="article-item"
      v-animate="['slideUpBigIn']"
      v-for="(article, index) of articleList"
      :key="article.id"
  >
    <div class="article-mark">
      <svg-icon icon-class="qizhi" size="1.25rem"></svg-icon>
    </div>
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
        <router-link class="meta-item ml" :to="`/tag/${tag.id}`" v-for="tag in article.tagVOList.slice(0, 2)" :key="tag.id">
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
        <div class="article-content" v-html="renderMarkdown(article.articleContent)"></div>
      <div class="article-footer">
        <!-- 文章分类 -->
        <div class="article-category">
          <svg-icon icon-class="qizhi" size="0.85rem" style="margin-right: 0.15rem"></svg-icon>
          <router-link :to="`/category/${article.category.id}`">{{
              article.category.categoryName
            }}
          </router-link>
        </div>
        <!-- 阅读按钮 -->
        <router-link class="article-btn" :to="`/article/${article.id}`">阅读全文</router-link>
      </div>
    </div>
  </div>
  <Pagination v-if="count > 5" v-model:current="queryParams.current" :total="Math.ceil(count / 5)"></Pagination>
</template>

<script setup lang="ts">
import {getArticleList} from "@/api/article";
import {Article} from "@/api/article/types";
import {PageQueryArticle} from "@/model";
import {formatDate} from "@/utils/date";
import EventBus from '@/eventBus';
import {formatDateTime} from "@/utils/date";
import {getTagList} from "@/api/tag";
import {Tag} from "@/api/tag/types";
import { marked } from 'marked'
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
    start: null,
    end: null,
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

function renderMarkdown(content: string) {
  return marked(content || '')
}

function filterByDate() {
  if (dateRange.value != null) {
    queryParams.value.start = formatDateTime(dateRange.value[0]);
    queryParams.value.end = formatDateTime(dateRange.value[1]);
  } else {
    queryParams.value.start = null;
    queryParams.value.end = null;
  }
  console.log(data.queryParams.start);
}

watch(
    () => [queryParams.value.current, queryParams.value.sort, queryParams.value.tagId],
    () => {
      getArticleList(queryParams.value).then(({data}) => {
        articleList.value = data.data.recordList;
        count.value = data.data.count;
      });
    }
);
watch(
    () => [dateRange.value, queryParams.value.tagId],
    () => {
      // 调用接口，使用新的日期范围
      filterByDate();
      queryParams.value.current = 1;
      console.log(dateRange.value);
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
  EventBus.on("refresh-articles", () => {
    queryParams.value.current = 1;
    getArticleList(queryParams.value).then(({data}) => {
      articleList.value = data.data.recordList;
      count.value = data.data.count;
    });
  });
});

</script>

<style lang="scss" scoped>
.article-toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0 0 0.25rem;
  padding: 0.72rem;
  border: 1px solid var(--home-border);
  border-radius: 0.65rem;
  background: var(--home-surface-strong);
  box-shadow: var(--home-shadow), var(--home-glow);
}

.sortList {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.sort-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  min-height: 2.5rem;
  padding: 0 0.9rem;
  border: 1px solid var(--home-border);
  border-radius: 999px;
  background: var(--home-surface-muted);
  color: var(--home-muted);
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

  &.active {
    color: var(--home-accent);
    border-color: var(--home-border-strong);
    background: var(--home-accent-soft);
  }

  &:hover {
    transform: translateY(-1px);
    color: var(--home-accent);
    border-color: var(--home-border-strong);
  }
}

.selectWidth {
  width: 13rem;
}

.date-picker-container {
  min-width: 18rem;
}

:deep(.el-select .el-input__wrapper),
:deep(.el-date-editor.el-input__wrapper) {
  min-height: 2.5rem;
  border-radius: 0.65rem;
  box-shadow: 0 0 0 1px var(--home-border) inset;
  background: var(--home-surface-muted);
  color: var(--home-muted);
}

.article-item {
  position: relative;
  display: flex;
  min-height: 10.2rem;
  margin: 0.25rem 0 0;
  padding: 1.25rem 1.05rem 1.05rem 1.45rem;
  border-radius: 0.65rem;
  border: 1px solid var(--home-border);
  border-left: 4px solid rgba(85, 162, 160, 0.6);
  background: var(--home-surface-strong);
  box-shadow: var(--home-shadow), var(--home-glow);
  animation-duration: 0.5s;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
  overflow: hidden;
  visibility: hidden;

  &:hover {
    transform: translateY(-2px);
    border-color: var(--home-border-strong);
    border-left-color: var(--home-accent);
    box-shadow: var(--home-shadow-hover), var(--home-glow);

    .cover {
      transform: scale(1.035);
    }
  }
}

.article-mark {
  flex: 0 0 2.2rem;
  padding-top: 1rem;
  color: var(--home-accent-warm);
}

.article-cover {
  order: 3;
  flex: 0 0 10.8rem;
  height: 6.7rem;
  align-self: center;
  margin-left: 1rem;
  border: 1px solid var(--home-border);
  border-radius: 0.45rem;
  overflow: hidden;
  background: var(--grey-2);

  .cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.28s ease;
  }
}

.article-info {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 0;

  .article-meta {
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 0.35rem 0.7rem;
    font-size: 0.78rem;
    line-height: 1.4;
    color: var(--home-muted);
  }

  .top {
    color: var(--home-accent-warm);
    font-weight: 700;
  }

  .meta-item {
    display: inline-flex;
    align-items: center;
  }

  .ml:not(:first-child) {
    margin-left: 0;
  }

  .article-title {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    margin: 0.5rem 0 0.4rem;
    color: var(--home-title);
    font-size: clamp(1.2rem, 2vw, 1.55rem);
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;

    a {
      color: inherit;
    }

    &:hover {
      color: var(--home-accent-cool);
    }
  }

  .article-content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    max-height: 3.5rem;
    font-family: Mulish, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
    font-size: 0.95rem;
    line-height: 1.75;
    color: var(--home-muted);
    overflow: hidden;

    :deep(p) {
      display: inline;
      margin: 0;
    }

    :deep(h1),
    :deep(h2),
    :deep(h3),
    :deep(h4),
    :deep(h5),
    :deep(h6),
    :deep(blockquote),
    :deep(ul),
    :deep(ol),
    :deep(pre) {
      display: inline;
      margin: 0;
      padding: 0;
      border: 0;
      background: transparent;
      color: inherit;
      font: inherit;
    }

    :deep(img) {
      display: none;
    }
  }
}

.article-category {
  display: flex;
  align-items: center;
  font-size: 0.8rem;
  color: var(--home-muted);
}

.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  margin-top: auto;
  padding-top: 0.9rem;
}

.article-btn {
  flex-shrink: 0;
  padding: 0.18rem 0.7rem;
  border: 1px solid var(--home-border-strong);
  border-radius: 99px;
  color: var(--home-accent);
  font-size: 0.82rem;
  line-height: 1.6;
  background: var(--home-accent-soft);
}

@media (max-width: 767px) {
  .article-toolbar {
    align-items: stretch;
    flex-direction: column;
    margin: 0 0 0.25rem;
  }

  .sortList {
    width: 100%;
  }

  .sort-option {
    flex: 1;
    min-width: 0;
  }

  .selectWidth,
  .date-picker-container {
    width: 100%;
    min-width: 0;
  }

  :deep(.el-date-editor--daterange.el-input__wrapper) {
    width: 100%;
  }

  .article-item {
    flex-direction: column;
    min-height: 0;
    padding: 1rem;

    .article-cover {
      order: 0;
      width: 100%;
      height: 9.3rem;
      flex: none;
      margin: 0 0 0.75rem;
      border-radius: 0.45rem;
    }

    .article-info {
      width: 100%;
      min-height: 0;
      padding: 0;

      .article-meta {
        justify-content: flex-start;
      }
    }

    .article-category {
      min-width: 0;
    }

    .article-footer {
      padding-top: 0.85rem;
    }
  }

  .article-mark {
    display: none;
  }
}
</style>
