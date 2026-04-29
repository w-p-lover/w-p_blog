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
      :class="{ 'article-item--reverse': index % 2 === 1 }"
      v-animate="['slideUpBigIn']"
      v-for="(article, index) of articleList"
      :key="article.id"
  >
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
        <div class="article-content" v-html="renderMarkdown(article.articleContent)"></div>
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
  margin: 0 0.5rem 0.25rem;
  padding: 0.72rem;
  border: 1px solid var(--home-border);
  border-radius: 0.85rem;
  background: linear-gradient(180deg, var(--home-surface-strong), var(--home-surface));
  box-shadow: 0 10px 24px rgba(22, 35, 66, 0.08), var(--home-glow);
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
  border: 1px solid transparent;
  border-radius: 0.65rem;
  background: var(--home-surface-muted);
  color: var(--home-muted);
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

  &.active {
    color: var(--home-accent);
    border-color: rgba(233, 113, 146, 0.34);
    background: var(--home-accent-soft);
  }

  &:hover {
    transform: translateY(-1px);
    color: var(--home-accent);
    border-color: rgba(233, 113, 146, 0.28);
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
  display: flex;
  min-height: 15rem;
  margin: 0.25rem 0.5rem 0;
  border-radius: 0.95rem;
  border: 1px solid var(--home-border);
  background: linear-gradient(180deg, var(--home-surface-strong), var(--home-surface));
  box-shadow: var(--home-shadow), var(--home-glow);
  animation-duration: 0.5s;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
  overflow: hidden;
  visibility: hidden;

  &:hover {
    transform: translateY(-3px);
    border-color: var(--home-border-strong);
    box-shadow: var(--home-shadow-hover), var(--home-glow);

    .cover {
      transform: scale(1.045);
    }
  }

  &.article-item--reverse {
    flex-direction: row-reverse;

    .article-cover {
      margin-right: auto;
      margin-left: 1.15rem;
      -webkit-clip-path: polygon(4% 0, 100% 0, 100% 100%, 0 100%);
      clip-path: polygon(4% 0, 100% 0, 100% 100%, 0 100%);
      border-radius: 0 0.95rem 0.95rem 0;
    }

    .article-info {
      padding: 1.15rem 0 3.15rem 1.35rem;

      .article-meta {
        justify-content: flex-start;
      }
    }

    .article-btn {
      left: 1.35rem;
      right: auto;
    }

    .article-category {
      right: 1.35rem;
      justify-content: flex-start;
    }
  }
}

.article-cover {
  width: 48%;
  margin-right: 1.15rem;
  -webkit-clip-path: polygon(0 0, 96% 0, 100% 100%, 0 100%);
  clip-path: polygon(0 0, 96% 0, 100% 100%, 0 100%);
  border-radius: 0.95rem 0 0 0.95rem;
  overflow: hidden;

  .cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.28s ease;
  }
}

.article-info {
  position: relative;
  width: 52%;
  padding: 1.15rem 1.35rem 3.15rem 0;

  .article-meta {
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.35rem 0.55rem;
    font-size: 0.78rem;
    line-height: 1.4;
    color: var(--home-muted);
  }

  .top {
    color: var(--home-accent-warm);
  }

  .meta-item {
    display: inline-flex;
    align-items: center;
  }

  .ml:not(:first-child) {
    margin-left: 0;
  }

  .article-title {
    margin: 0.7rem 0 0.45rem;
    color: var(--home-title);
    font-size: clamp(1.15rem, 2vw, 1.45rem);
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .article-content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    max-height: 5.3rem;
    font-family: Mulish, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
    font-size: 0.9rem;
    line-height: 1.7;
    color: var(--home-muted);
    overflow: hidden;

    :deep(p) {
      margin: 0;
    }
  }
}

.article-category {
  position: absolute;
  display: flex;
  align-items: center;
  bottom: 1rem;
  font-size: 0.8rem;
  color: var(--home-muted);
}

.article-btn {
  position: absolute;
  right: 1.35rem;
  bottom: 0.85rem;
  padding: 0.24rem 0.8rem;
  border: 1px solid rgba(233, 113, 146, 0.32);
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
    margin: 0 0.5rem 0.25rem;
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

    .article-cover {
      width: 100%;
      height: 13.5rem;
      margin: 0;
      -webkit-clip-path: polygon(0 0, 100% 0, 100% 96%, 0 100%);
      clip-path: polygon(0 0, 100% 0, 100% 96%, 0 100%);
      border-radius: 0.95rem 0.95rem 0 0;
    }

    .article-info {
      width: 100%;
      min-height: 13rem;
      padding: 0.95rem 1rem 3.4rem;

      .article-meta {
        justify-content: flex-start;
      }
    }

    .article-category {
      left: 1rem;
      right: auto;
    }

    .article-btn {
      right: 1rem;
      left: auto;
    }

    &.article-item--reverse {
      flex-direction: column;

      .article-cover {
        width: 100%;
        margin: 0;
        -webkit-clip-path: polygon(0 0, 100% 0, 100% 100%, 0 96%);
        clip-path: polygon(0 0, 100% 0, 100% 100%, 0 96%);
        border-radius: 0.95rem 0.95rem 0 0;
      }

      .article-info {
        padding: 0.95rem 1rem 3.4rem;
      }

      .article-category {
        left: 1rem;
        right: auto;
      }

      .article-btn {
        right: 1rem;
        left: auto;
      }
    }
  }
}
</style>
