<template>
  <div class="brand-container">
    <div class="brand">
      <!-- 标题 -->
      <p class="artboard">{{ blog.blogInfo.siteConfig.siteName }}</p>
      <!-- 打字机 -->
      <div class="title">
        {{ obj.output }}
        <span class="easy-typed-cursor">|</span>
      </div>
    </div>
    <label class="hitokoto-type" for="hitokoto-type">
      <span>一言类型</span>
      <select id="hitokoto-type" v-model="sentenceType" :disabled="isFetchingSentence" @change="fetchData">
        <option v-for="item in HITOKOTO_TYPES" :key="item.value" :value="item.value">
          {{ item.label }}
        </option>
      </select>
    </label>
    <!-- 波浪 -->
    <Waves></Waves>
    <!-- 向下按钮 -->
    <svg-icon class="arrow-down" icon-class="down" size="32px" @click="scrollDown"></svg-icon>
  </div>
</template>

<script setup lang="ts">
import useStore from "@/store";
import EasyTyper from "easy-typer-js";
import Waves from "@/components/Waves/index.vue";
import {
  buildHitokotoUrl,
  DEFAULT_HITOKOTO_TYPE,
  HITOKOTO_TYPE_CHANGE_EVENT,
  HITOKOTO_TYPE_STORAGE_KEY,
  HITOKOTO_TYPES,
  normalizeHitokotoType,
  type HitokotoType,
} from "@/views/Home/Swiper/hitokotoModel";

const fallbackSentence = "愿你在此寻得片刻安宁。";
const sentenceType = ref<HitokotoType>(DEFAULT_HITOKOTO_TYPE);
const isFetchingSentence = ref(false);
let typer: EasyTyper | null = null;
let sentenceRequestId = 0;

const obj = reactive({
  output: "",
  isEnd: false,
  speed: 100,
  singleBack: false,
  sleep: 0,
  type: "normal",
  backSpeed: 100,
  sentencePause: false,
});
const {blog} = useStore();
const scrollDown = () => {
  window.scrollTo({
    behavior: "smooth",
    top: document.documentElement.clientHeight,
  });
};
const startTyping = (sentence: string) => {
  typer?.close?.();
  obj.output = "";
  obj.isEnd = false;
  typer = new EasyTyper(
      obj,
      sentence || fallbackSentence,
      () => {
      },
      () => {
      }
  );
};
const fetchData = () => {
  sentenceType.value = normalizeHitokotoType(sentenceType.value);
  localStorage.setItem(HITOKOTO_TYPE_STORAGE_KEY, sentenceType.value);
  window.dispatchEvent(new CustomEvent(HITOKOTO_TYPE_CHANGE_EVENT, {detail: sentenceType.value}));
  const requestId = ++sentenceRequestId;
  isFetchingSentence.value = true;
  fetch(buildHitokotoUrl(sentenceType.value))
      .then((res) => {
        return res.json();
      })
      .then(({hitokoto}) => {
        if (requestId === sentenceRequestId) {
          startTyping(hitokoto);
        }
      })
      .catch(() => {
        if (requestId === sentenceRequestId) {
          startTyping(fallbackSentence);
        }
      })
      .finally(() => {
        if (requestId === sentenceRequestId) {
          isFetchingSentence.value = false;
        }
      });
};
onMounted(() => {
  sentenceType.value = normalizeHitokotoType(localStorage.getItem(HITOKOTO_TYPE_STORAGE_KEY));
  fetchData();
});
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.brand-container {
  @include flex;
  flex-direction: column;
  position: relative;
  width: 100%;
  height: 100vh;
  min-height: 10rem;
  color: var(--header-text-color);
}

.brand {
  @include flex;
  flex-direction: column;
  position: fixed;
  z-index: -1;
  width: min(92vw, 62rem);
  text-align: center;

  .artboard {
    font-family: "Fredericka the Great", Mulish, -apple-system, "PingFang SC", "Microsoft YaHei",
    sans-serif;
    font-size: clamp(2.7rem, 5vw, 3.8rem);
    line-height: 1.14;
    text-shadow: 0 12px 28px rgba(4, 12, 28, 0.38);
    animation: titleScale 1s;
  }

  .title {
    max-width: min(86vw, 48rem);
    margin-top: 0.55rem;
    color: rgba(249, 251, 255, 0.9);
    line-height: 1.7;
    text-shadow: 0 8px 22px rgba(3, 10, 23, 0.3);
  }
}

.hitokoto-type {
  @include flex;
  gap: 0.42rem;
  position: absolute;
  bottom: 8.25rem;
  left: 50%;
  z-index: 9;
  max-width: calc(100vw - 2rem);
  padding: 0.34rem 0.45rem 0.34rem 0.72rem;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  background: rgba(16, 24, 38, 0.2);
  box-shadow: 0 10px 24px rgba(2, 10, 22, 0.16);
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.78rem;
  line-height: 1;
  white-space: nowrap;
  backdrop-filter: blur(8px);
  transform: translateX(-50%);
  transition: border-color 0.2s ease, background 0.2s ease, opacity 0.2s ease;

  &:hover,
  &:focus-within {
    border-color: rgba(255, 255, 255, 0.48);
    background: rgba(16, 24, 38, 0.3);
  }

  span {
    opacity: 0.78;
  }

  select {
    max-width: 6.6rem;
    border: 0;
    outline: 0;
    background: transparent;
    color: rgba(255, 255, 255, 0.92);
    font: inherit;
    cursor: pointer;
  }

  select:disabled {
    cursor: progress;
    opacity: 0.72;
  }

  option {
    color: #263241;
  }
}

.easy-typed-cursor {
  margin-left: 0.625rem;
  opacity: 1;
  color: rgba(255, 241, 245, 0.92);
  -webkit-animation: blink 0.95s infinite;
  -moz-animation: blink 0.95s infinite;
  animation: blink 0.95s infinite;
}

.arrow-down {
  position: absolute;
  bottom: 70px;
  opacity: 0.78;
  filter: drop-shadow(0 8px 18px rgba(2, 10, 22, 0.36));
  -webkit-animation: arrow-shake 2.2s ease-out infinite;
  animation: arrow-shake 2.2s ease-out infinite;
  cursor: pointer;
  z-index: 8;
}

@media (max-width: 767px) {
  .brand-container {
    padding: 3rem 0.5rem 0;
  }

  .hitokoto-type {
    bottom: 7.25rem;
    padding: 0.32rem 0.38rem 0.32rem 0.58rem;
    font-size: 0.72rem;
  }
}

@media (min-width: 760px) {
  .title {
    font-size: 1.5rem;
  }
}

@keyframes arrow-shake {
  0% {
    opacity: 0.9;
    transform: translateY(0);
  }

  35% {
    opacity: 0.52;
    transform: translateY(18px);
  }

  100% {
    opacity: 0.9;
    transform: translateY(0);
  }
}

@keyframes blink {
  0% {
    opacity: 0;
  }

  100% {
    opacity: 1;
  }
}
</style>
