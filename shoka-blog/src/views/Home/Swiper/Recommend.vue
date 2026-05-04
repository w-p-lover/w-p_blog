<template>
  <swiper v-if="articleList.length > 0" class="swiper-container" :modules="modules" :loop="true" :slides-per-view="1"
          navigation mousewheel :autoplay="{ delay: 5000, disableOnInteraction: false, }"
          :pagination="{ clickable: true }">
    <swiper-slide v-for="article in articleList" :key="article.id">
      <div class="slide-content">
        <div class="recommend-badge">荐</div>
        <div class="slide-copy">
          <span class="slide-kicker">推荐阅读</span>
          <router-link :to="`/article/${article.id}`" class="slide-title">{{ article.articleTitle }}</router-link>
          <span class="slide-time">{{ formatDate(article.createTime) }}</span>
        </div>
        <router-link :to="`/article/${article.id}`" class="slide-thumb">
          <img :src="article.articleCover" alt="">
        </router-link>
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
onMounted(() => {
  getArticleRecommend().then(({data}) => {
    articleList.value = data.data;
  });
});
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.swiper-container {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: 6.4rem;
  border-radius: 0.65rem;
  border: 1px solid var(--home-border);
  border-left: 4px solid rgba(200, 85, 64, 0.58);
  background:
    linear-gradient(90deg, rgba(255, 254, 250, 0.98), rgba(250, 248, 242, 0.9));
  box-shadow: 0 7px 18px rgba(89, 78, 56, 0.055), var(--home-glow);
  overflow: hidden;
}

.slide-content {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  width: 100%;
  height: 100%;
  padding: 0.85rem 3.25rem 0.85rem 1rem;
}

.recommend-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 2.15rem;
  width: 2.15rem;
  height: 2.15rem;
  border: 1px solid rgba(200, 85, 64, 0.22);
  border-radius: 50%;
  background: rgba(200, 85, 64, 0.09);
  color: var(--home-accent-warm);
  font-weight: 800;
  line-height: 1;
}

.slide-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
}

.slide-kicker {
  display: inline-flex;
  align-items: center;
  margin-bottom: 0.15rem;
  color: var(--home-accent-warm);
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.4;
}

.slide-title {
  display: block;
  max-width: min(100%, 34rem);
  overflow: hidden;
  color: var(--home-title);
  font-size: clamp(1rem, 1.6vw, 1.16rem);
  font-weight: 800;
  line-height: 1.42;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slide-time {
  margin-top: 0.12rem;
  color: var(--home-muted);
  font-size: 0.82rem;
}

.slide-thumb {
  display: block;
  flex: 0 0 4.6rem;
  width: 4.6rem;
  height: 3.2rem;
  border: 1px solid var(--home-border);
  border-radius: 0.42rem;
  overflow: hidden;
  background: var(--grey-2);

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

:deep(.swiper-pagination) .swiper-pagination-bullet {
  width: 0.42rem;
  height: 0.42rem;
  margin: 0 0.22rem;
  border-radius: 6.1875rem;
  background: rgba(85, 162, 160, 0.38);
  opacity: 0.72;
  transition: all 0.3s;
}

:deep(.swiper-pagination) .swiper-pagination-bullet.swiper-pagination-bullet-active {
  opacity: 1;
  background-color: var(--home-accent-cool);
  width: 1.15rem;
}

:deep(.swiper-button-next),
:deep(.swiper-button-prev) {
  width: 1.85rem;
  height: 1.85rem;
  color: var(--home-accent);
  transition: all 0.3s;
}

:deep(.swiper-button-next):after,
:deep(.swiper-button-prev):after {
  font-size: 1rem !important;
}

:deep(.swiper-button-next):hover,
:deep(.swiper-button-prev):hover {
  background: var(--home-accent-soft);
  border-radius: 100%;
}

@media (max-width: 767px) {
  .swiper-container {
    height: 6.7rem;
  }

  .slide-content {
    gap: 0.65rem;
    padding: 0.75rem 2.45rem 0.8rem 0.8rem;
  }

  .slide-title {
    font-size: 0.95rem;
    white-space: normal;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }

  .recommend-badge {
    display: none;
  }

  .slide-thumb {
    flex-basis: 3.8rem;
    width: 3.8rem;
    height: 3rem;
  }
}
</style>
