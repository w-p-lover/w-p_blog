<template>
  <div>
    <Author class="side-card"></Author>
    <Notice v-if="blog.blogInfo.siteConfig.siteNotice" class="side-card"></Notice>
    <RecentComment class="side-card"></RecentComment>
  </div>
</template>

<script setup lang="ts">
import useStore from "@/store";
const {blog} = useStore();
</script>

<style lang="scss" scoped></style>
