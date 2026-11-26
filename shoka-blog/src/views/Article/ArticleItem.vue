<template>
  <div class="article-toolbar">
    <div class="sortList" aria-label="文章排序">
      <button
          type="button"
          class="sort-option"
          @click="changeSort('id')"
          :class="{ active: queryParams.sort === 'id' }"
          :aria-pressed="queryParams.sort === 'id'"
      >
        <svg-icon icon-class="calendar" size="1rem"></svg-icon>
        <span>按时间</span>
      </button>
      <button
          type="button"
          class="sort-option"
          @click="changeSort('views')"
          :class="{ active: queryParams.sort === 'views' }"
          :aria-pressed="queryParams.sort === 'views'"
      >
        <svg-icon icon-class="fun" size="1rem"></svg-icon>
        <span>按热度</span>
      </button>
    </div>
    <el-select
        clearable
        v-model="selectedTag"
        placeholder="全部标签"
        aria-label="按标签筛选文章"
        :disabled="tagsFailed"
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
          aria-label="按发布日期筛选文章"
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="filterByDate"
      ></el-date-picker>
    </div>

  </div>

  <div class="feed-summary" role="status" aria-live="polite">
    <span v-if="loading">正在加载文章…</span>
    <span v-else-if="failed">文章暂时无法加载</span>
    <span v-else>共 {{ count }} 篇文章<span v-if="count"> · 第 {{ queryParams.current }} / {{ Math.ceil(count / queryParams.size) }} 页</span></span>
    <button v-if="hasFilters" type="button" class="feed-action" @click="clearFilters">清除筛选</button>
  </div>
  <p v-if="tagsFailed" class="tag-error">
    标签暂时无法加载，仍可按时间和热度浏览。
    <button type="button" class="feed-action" @click="loadTags">重试标签</button>
  </p>
  <div class="article-results" :aria-busy="loading">
    <div v-if="loading" class="feed-state">正在整理文章，请稍候…</div>
    <div v-else-if="failed" class="feed-state">
      <h3>文章加载失败</h3>
      <p>请稍后重试，当前筛选条件已保留。</p>
      <button type="button" class="feed-action" @click="loadArticles">重新加载</button>
    </div>
    <div v-else-if="!articleList.length" class="feed-state">
      <h3>{{ hasFilters ? "没有符合条件的文章" : "还没有可展示的文章" }}</h3>
      <p>{{ hasFilters ? "试试其他标签或日期，也可以清除筛选查看全部文章。" : "新的内容发布后会显示在这里。" }}</p>
    </div>
    <template v-else>
      <article
          class="article-item"
          v-for="article of displayArticles"
          :key="article.id"
      >
        <div class="article-mark">
          <svg-icon icon-class="qizhi" size="1.25rem"></svg-icon>
        </div>
        <!-- 文章缩略图 -->
        <div v-if="article.articleCover" class="article-cover">
          <router-link :to="`/article/${article.id}`" :aria-label="`阅读：${article.articleTitle}`">
            <img class="cover" :src="article.articleCover" alt="" loading="lazy" decoding="async"/>
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
            <router-link class="meta-item ml" :to="`/tag/${tag.id}`" v-for="tag in (article.tagVOList || []).slice(0, 2)" :key="tag.id">
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
            <p class="article-content">{{ article.preview }}</p>
          <div class="article-footer">
            <!-- 文章分类 -->
            <div v-if="article.category" class="article-category">
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
      </article>
    </template>
  </div>
  <Pagination v-if="!loading && !failed && count > queryParams.size" v-model:current="queryParams.current" :total="Math.ceil(count / queryParams.size)"></Pagination>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { marked } from "marked";
import { formatDate } from "@/utils/date";
import { useArticleFeed } from "./useArticleFeed";

const {
  queryParams, selectedTag, dateRange, articleList, count, loading, failed,
  tagList, tagsFailed, hasFilters, changeSort, filterByTag, filterByDate,
  clearFilters, loadArticles, loadTags,
} = useArticleFeed();

const displayArticles = computed(() => articleList.value.map(article => {
  // 在惰性模板中提取文本，不把摘要 HTML 插入页面。
  const template = document.createElement("template");
  template.innerHTML = marked.parse(article.articleContent || "", { async: false }) as string;
  template.content.querySelectorAll("script, style, img, iframe, object").forEach(node => node.remove());
  const preview = (template.content.textContent || "").replace(/\s+/g, " ").trim();
  return { ...article, preview: preview || "这篇文章暂未提供摘要，阅读全文了解内容。" };
}));
</script>

<style lang="scss" scoped>
.article-results {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.feed-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
  color: var(--home-muted);
  font-size: 0.875rem;
}

.feed-state {
  padding: 2.5rem 1.25rem;
  border: 1px solid var(--home-border);
  border-radius: 0.65rem;
  background: var(--home-surface-strong);
  color: var(--home-muted);
  text-align: center;

  h3 { margin: 0 0 0.5rem; color: var(--text-color); }
  p { margin: 0 0 0.75rem; }
}

.feed-action {
  padding: 0.3rem 0.6rem;
  border: 1px solid var(--home-border-strong);
  border-radius: 0.35rem;
  background: var(--home-accent-soft);
  color: var(--text-color);
  font: inherit;
  cursor: pointer;
}

.tag-error { color: var(--home-muted); font-size: 0.875rem; }

button:focus-visible,
a:focus-visible {
  outline: 2px solid var(--text-color);
  outline-offset: 3px;
}

@media (prefers-reduced-motion: reduce) {
  .article-item, .sort-option, .cover { transition: none !important; }
}

.article-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin: 0 0 0.25rem;
  padding: 0.72rem;
  border: 1px solid var(--home-border);
  border-radius: 0.65rem;
  background: var(--home-surface-strong);
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
    color: var(--text-color);
    border-color: var(--home-border-strong);
    background: var(--home-accent-soft);
  }

  &:hover {
    transform: translateY(-1px);
    color: var(--text-color);
    border-color: var(--home-border-strong);
  }
}

.selectWidth {
  width: 13rem;
}

.date-picker-container {
  flex: 1 1 18rem;
  min-width: 0;

  :deep(.el-date-editor--daterange) {
    width: 100%;
  }
}

:deep(.el-select__wrapper),
:deep(.el-select .el-input__wrapper),
:deep(.el-date-editor.el-input__wrapper) {
  min-height: 2.5rem;
  border-radius: 0.65rem;
  box-shadow: 0 0 0 1px var(--home-border) inset;
  background: var(--home-surface-muted);
  color: var(--home-muted);
}

:deep(.el-date-editor .el-range-input),
:deep(.el-date-editor .el-range-separator) {
  color: var(--home-muted);
}

:deep(.el-select__placeholder),
:deep(.el-date-editor input::placeholder) {
  color: var(--home-muted);
  opacity: 1;
}

.article-item {
  position: relative;
  display: flex;
  min-height: 10.2rem;
  margin: 0.25rem 0 0;
  padding: 1.25rem 1.05rem 1.05rem 1.45rem;
  border-radius: 0.65rem;
  border: 1px solid var(--home-border);
  background: var(--home-surface-strong);
  animation-duration: 0.5s;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    border-color: var(--home-border-strong);

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
    color: var(--text-color);
    font-size: clamp(1.2rem, 2vw, 1.55rem);
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;

    a {
      color: inherit;
    }

    &:hover {
      color: var(--text-color);
      text-decoration: underline;
    }
  }

  .article-content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    max-height: 3.5rem;
    margin: 0;
    overflow-wrap: anywhere;
    font-family: Mulish, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
    font-size: 0.95rem;
    line-height: 1.75;
    color: var(--home-muted);
    overflow: hidden;
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
  color: var(--text-color);
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
    flex: none;
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
