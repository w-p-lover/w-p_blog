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
  height: 13.25rem;
  margin: 0 0.5rem;
  border-radius: 0.85rem;
  border: 1px solid var(--home-border);
  box-shadow: var(--home-shadow), var(--home-glow);
  overflow: hidden;

  &::before {
    content: '推荐';
    position: absolute;
    z-index: 2;
    top: 0.65rem;
    left: 0.75rem;
    display: flex;
    justify-content: center;
    min-width: 3.7rem;
    padding: 0.1rem 0.6rem;
    border-radius: 99px;
    color: var(--grey-0);
    background: rgba(233, 113, 146, 0.86);
    font-size: 0.82rem;
    line-height: 1.5;
    box-shadow: 0 8px 16px rgba(210, 113, 146, 0.22);
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
    font-size: clamp(1.35rem, 2.6vw, 1.8rem);
    font-weight: 700;
    text-shadow: 0 8px 20px rgba(5, 10, 20, 0.34);
  }

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(145deg, rgba(6, 12, 26, 0.46), rgba(8, 14, 28, 0.2) 48%, rgba(5, 10, 19, 0.46));
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
  width: 0.55rem;
  height: 0.55rem;
  margin: 0 0.22rem;
  border-radius: 6.1875rem;
  background: rgba(255, 255, 255, 0.88);
  opacity: 0.72;
  transition: all 0.3s;
}

:deep(.swiper-pagination) .swiper-pagination-bullet.swiper-pagination-bullet-active {
  opacity: 1;
  background-color: var(--home-accent-cool);
  width: 1.55rem;
}

:deep(.swiper-button-next),
:deep(.swiper-button-prev) {
  width: 2.5rem;
  height: 2.5rem;
  color: rgba(255, 255, 255, 0.86);
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
