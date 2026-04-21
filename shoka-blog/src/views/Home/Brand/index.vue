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
const fetchData = () => {
  fetch("https://international.v1.hitokoto.cn/?c=d")
      .then((res) => {
        return res.json();
      })
      .then(({hitokoto}) => {
        new EasyTyper(
            obj,
            hitokoto,
            () => {
            },
            () => {
            }
        );
      });
};
onMounted(() => {
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

  .artboard {
    font-family: "Fredericka the Great", Mulish, -apple-system, "PingFang SC", "Microsoft YaHei",
    sans-serif;
    font-size: clamp(2.5rem, 5.2vw, 3.7rem);
    line-height: 1.16;
    letter-spacing: 0.02em;
    text-shadow: 0 10px 22px rgba(4, 12, 28, 0.44);
    animation: titleScale 1s;
  }

  .title {
    margin-top: 0.45rem;
    letter-spacing: 0.08em;
    color: rgba(246, 250, 255, 0.9);
    text-shadow: 0 8px 20px rgba(3, 10, 23, 0.36);
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
  opacity: 0.88;
  filter: drop-shadow(0 8px 20px rgba(2, 10, 22, 0.45));
  -webkit-animation: arrow-shake 2.1s ease-out infinite;
  animation: arrow-shake 2.1s ease-out infinite;
  cursor: pointer;
  z-index: 8;
}

@media (max-width: 767px) {
  .brand-container {
    padding: 3rem 0.5rem 0;
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
