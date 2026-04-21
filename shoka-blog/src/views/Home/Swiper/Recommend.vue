<template>
  <swiper v-if="articleList.length > 0" class="swiper-container" :modules="modules" :loop="true" :slides-per-view="1"
          navigation mousewheel :autoplay="{ delay: 5000, disableOnInteraction: false, }"
          :pagination="{ clickable: true }">
    <swiper-slide v-for="article in articleList" :key="article.id">
      <div class="slide-content" :style="articleCover(article.articleCover)">
        <router-link :to="`/article/${article.id}`" class="slide-title">{{ article.articleTitle }}</router-link>
        <span class="slide-time">发布时间：{{ formatDate(article.createTime) }}</span>
      </div>
    </swiper-slide>
  </swiper>
</template>

<script setup lang="ts">
import {getArticleRecommend} from "@/api/article";
import {ArticleRecommend} from "@/api/article/types";
import {formatDate} from "@/utils/date";
import {Autoplay, Mousewheel, Navigation, Pagination} from 'swiper';
import {Swiper, SwiperSlide} from 'swiper/vue';
// 自定义模块
const modules = [Pagination, Navigation, Mousewheel, Autoplay];
const articleList = ref<ArticleRecommend[]>([]);
const articleCover = computed(() => (cover: string) => 'background:url(' + cover + ')');
onMounted(() => {
  getArticleRecommend().then(({data}) => {
    articleList.value = data.data;
  });
});
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.swiper-container {
  height: 13.875rem;
  margin: 1rem 0.5rem;
  border-radius: 0.9rem;
  border: 1px solid var(--surface-border-soft);
  box-shadow: var(--shadow-soft);

  &::before {
    content: '推荐';
    position: absolute;
    z-index: 2;
    color: var(--grey-0);
    background: linear-gradient(90deg, rgba(231, 172, 60, 0.94), rgba(232, 131, 96, 0.96));
    top: 0;
    letter-spacing: 0.1875rem;
    left: 0.625rem;
    font-size: 0.9375rem;
    width: 4.0625rem;
    display: flex;
    justify-content: center;
    border-radius: 0 0 0.75rem 0.75rem;
    box-shadow: 0 8px 16px rgba(210, 132, 62, 0.28);
  }
}

.slide-content {
  @include flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 0 3.125rem 1.25rem;
  background-position: center !important;
  background-size: cover !important;

  .slide-title {
    font-size: 1.9rem;
    text-shadow: 0 8px 20px rgba(5, 10, 20, 0.34);
  }

  &::after {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    background:
      linear-gradient(145deg, rgba(6, 12, 26, 0.34), rgba(8, 14, 28, 0.16) 46%, rgba(5, 10, 19, 0.4));
    left: 0;
    top: 0;
  }
}

.slide-title,
.slide-time {
  text-align: center;
  line-height: 1.5;
  margin: 0.125rem 0;
  color: var(--grey-0);
  z-index: 1;
}

:deep(.swiper-pagination) .swiper-pagination-bullet {
  display: inline-block;
  width: 0.6875rem;
  height: 0.6875rem;
  margin: 0 0.25rem;
  border-radius: 6.1875rem;
  background: rgba(255, 255, 255, 0.92);
  opacity: 0.78;
  transition: all 0.3s;
}

:deep(.swiper-pagination) .swiper-pagination-bullet.swiper-pagination-bullet-active {
  opacity: 1;
  background-color: var(--color-blue);
  width: 1.875rem;
}

:deep(.swiper-button-next),
:deep(.swiper-button-prev) {
  width: 2.75rem;
  transition: all 0.3s;
}

:deep(.swiper-button-next):after,
:deep(.swiper-button-prev):after {
  font-size: 1.5rem !important;
}

:deep(.swiper-button-next):hover,
:deep(.swiper-button-prev):hover {
  background: rgba(255, 255, 255, .3);
  border-radius: 100%;
}
</style>
