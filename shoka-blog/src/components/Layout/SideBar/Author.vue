<template>
  <div>
    <!-- 作者信息 -->
    <div class="author-container">
      <!-- 作者头像 -->
      <img class="author-avatar" :src="blog.blogInfo.siteConfig.authorAvatar" :alt="blog.blogInfo.siteConfig.siteAuthor"/>
      <!-- 作者名称 -->
      <p class="author-name">{{ blog.blogInfo.siteConfig.siteAuthor }}</p>
      <!-- 网站简介 -->
      <div v-if="blog.blogInfo.siteConfig.siteIntro" class="site-desc">{{ blog.blogInfo.siteConfig.siteIntro }}</div>
    </div>
    <!-- 博客信息 -->
    <blog-info></blog-info>
    <!-- 社交 -->
    <social-list></social-list>
  </div>
</template>

<script setup lang="ts">
import useStore from "@/store";

const {blog} = useStore();

</script>

<style lang="scss" scoped>
.author-avatar {
  width: 5.5rem;
  height: 5.5rem;
  object-fit: cover;
  box-shadow: none;
}

.author-name {
  margin: 0.65rem 0 0;
  font-size: 1.05rem;
  font-weight: 600;
  line-height: 1.5;
}

.site-desc {
  margin-top: 0.35rem;
  font-size: 0.8125rem;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

:deep(.blog-container) {
  margin-top: 0.9rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--home-border);
}

:deep(.blog-item) {
  flex: 1;
  padding: 0 0.35rem;
}

:deep(.count) { font-size: 1.1rem; }
:deep(.name) { font-size: 0.75rem; }
:deep(.social-container) { margin-top: 0.7rem; }

:deep(a:focus-visible) {
  outline: 2px solid var(--text-color);
  outline-offset: 3px;
}
</style>
