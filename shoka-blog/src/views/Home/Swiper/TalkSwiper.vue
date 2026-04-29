<template>
  <router-link to="/talk" class="talk-swiper" v-if="talkList.length > 0">
    <svg-icon icon-class="laba" size="1.25rem"></svg-icon>
    <swiper class="swiper-container" :direction="'vertical'" :speed="2000" :modules="modules" :loop="true"
            :slides-per-view="1" :autoplay="{ delay: 3000, disableOnInteraction: false, }">
      <swiper-slide v-for="(talk, index) in talkList" :key="index">
        <div class="slide-content" v-html="talk"></div>
      </swiper-slide>
    </swiper>
    <svg-icon icon-class="right-arrow" class="arrow"></svg-icon>
  </router-link>
</template>

<script setup lang="ts">
import {getTalkHomeList} from '@/api/talk';
import {Autoplay} from 'swiper';
import {Swiper, SwiperSlide} from 'swiper/vue';
// 自动播放
const modules = [Autoplay];
const talkList = ref<string[]>([]);
onMounted(() => {
  getTalkHomeList().then(({data}) => {
    talkList.value = data.data;
  })
});
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.talk-swiper {
  @include flex;
  min-height: 2.9rem;
  margin: 0 0.5rem;
  padding: 0.58rem 0.95rem;
  gap: 0.65rem;
  font-size: 0.92rem;
  border-radius: 0.75rem;
  border: 1px solid var(--home-border);
  background: linear-gradient(180deg, var(--home-surface-strong), var(--home-surface));
  box-shadow: 0 10px 24px rgba(22, 35, 66, 0.08), var(--home-glow);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;

  &:hover {
    transform: translateY(-1px);
    border-color: var(--home-border-strong);
    box-shadow: 0 14px 28px rgba(22, 35, 66, 0.11), var(--home-glow);
  }
}

.swiper-container {
  width: 100%;
  height: 1.5625rem;
  line-height: 1.5625rem;
  border-radius: 0.75rem;
}

.slide-content {
  width: 100%;
  height: 100%;
  text-align: left;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--home-muted);
}

.arrow {
  color: var(--home-accent);
  opacity: 0.72;
  animation: 1.5s passing infinite;
}

@keyframes passing {
  0% {
    transform: translateX(-50%);
    opacity: 0;
  }

  50% {
    transform: translateX(0);
    opacity: 1;
  }

  100% {
    transform: translateX(50%);
    opacity: 0;
  }
}
</style>
