<template>
  <div class="page-header">
    <h1 class="page-title">关于</h1>
    <img class="page-cover" src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-we56z6_5000x3000.png"
      alt="" />
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container" >
      <div class="avatar-box">
        <img class="author-avatar" :src="blog.blogInfo.siteConfig.authorAvatar" />
      </div>
      <v-md-preview class="md" :text="blog.blogInfo.siteConfig.aboutMe"></v-md-preview>
    </div>
  </div>
</template>

<script setup lang="ts">
import useStore from "@/store";
const { blog } = useStore();
</script>

<style scoped>
.author-avatar {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  transition: all 0.5s;
}

.author-avatar:hover {
  transform: rotate(360deg);
}
</style>
