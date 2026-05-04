<template>
  <div class="bg home-bg">
    <div class="main-container home-main mt">
      <div class="left-container home-feed" :class="app.sideFlag ? 'test' : ''">
        <Images></Images>
        <Recommend></Recommend>
        <!-- 文章列表 -->
        <ArticleItem></ArticleItem>
      </div>
      <SideBar class="right-container home-sidebar" :class="app.sideFlag ? 'temp' : ''"></SideBar>
    </div>
  </div>
</template>

<script setup lang="ts">
import useStore from "@/store";
import ArticleItem from "@/views/Article/ArticleItem.vue";
import Images from "./Swiper/Images.vue";
import Recommend from "./Swiper/Recommend.vue";

const {app} = useStore();
</script>

<style lang="scss" scoped>
.mt {
  padding-top: 5.3rem;
  padding-bottom: 2.5rem;
}

.home-feed {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}
</style>
