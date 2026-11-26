<template>
  <div v-if="showSocialList.length" class="social-container">
      <a v-for="item in showSocialList" class="social-item" :class="{ 'social-github': item.type === 'github' }" :key="item.type" target="_blank" rel="noopener noreferrer" :href="item.href" :aria-label="item.type">
        <svg-icon :icon-class="item.type" size="1.4rem" :color="item.color"></svg-icon>
      </a>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import useStore from "@/store";

const { blog } = useStore();

// 安全判断：socialList 存在时才调用 includes
const isShowSocial = (social: string) => {
  return blog.blogInfo?.siteConfig?.socialList?.includes(social) ?? false;
};

// 用 computed 生成 showSocialList，避免 blog 数据未初始化时报错
const showSocialList = computed(() => {
  const siteConfig = blog.blogInfo?.siteConfig || {};
  return [
    {
      type: "github",
      href: siteConfig.github || "",
    },
    {
      type: "gitee",
      href: siteConfig.gitee || "",
    },
    {
      type: "bilibili",
      href: siteConfig.bilibili || "",
    },
    {
      type: "qq",
      href: siteConfig.qq ? `https://user.qzone.qq.com/${siteConfig.qq}/main` : "",
      color: "#00a1d6",
    },
  ].filter(item => item.href && isShowSocial(item.type));
});
</script>

<style scoped>
[theme="dark"] .social-github {
  filter: brightness(0) invert(0.85);
}
</style>
