<template>
  <div class="imgs">
    <ul>
      <li class="item" v-for="(image, index) in imageList" :key="index" :style="{
        'background-image': 'url(' + image + ')'
      }">
      </li>
    </ul>
    <div class="quote-card">
      <div class="quote-toolbar">
        <span class="quote-label">一言</span>
        <label class="quote-type">
          <span>类型</span>
          <select
            v-model="selectedType"
            class="quote-type-select"
            aria-label="选择一言类型"
            title="选择一言类型"
            @change="handleTypeChange"
          >
            <option v-for="type in HITOKOTO_TYPES" :key="type.value" :value="type.value">
              {{ type.label }}
            </option>
          </select>
        </label>
      </div>
      <p class="quote-text">
        {{ typer.output }}
        <span class="easy-typed-cursor">|</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import EasyTyper from "easy-typer-js";
import {
  buildHitokotoUrl,
  DEFAULT_HITOKOTO_TYPE,
  HITOKOTO_TYPE_CHANGE_EVENT,
  HITOKOTO_TYPE_STORAGE_KEY,
  HITOKOTO_TYPES,
  normalizeHitokotoType,
  type HitokotoType,
} from "./hitokotoModel";

const imageList = [
  "\n" +
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img/202410311034088.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240508_190550.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240508_174939.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240508_173624.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240420_190212.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img202410311029127.jpg",
  "\n"
];
const fallbackSentence = "愿你在此寻得片刻安宁。";
const selectedType = ref<HitokotoType>(DEFAULT_HITOKOTO_TYPE);
const typer = reactive({
  output: "",
  isEnd: false,
  speed: 80,
  singleBack: false,
  sleep: 0,
  type: "normal",
  backSpeed: 80,
  sentencePause: false,
});
const startTyping = (sentence: string) => {
  typer.output = "";
  new EasyTyper(
      typer,
      sentence || fallbackSentence,
      () => {
      },
      () => {
      }
  );
};
const fetchSentence = (type: HitokotoType = selectedType.value) => {
  fetch(buildHitokotoUrl(type))
      .then((res) => res.json())
      .then(({hitokoto}) => {
        startTyping(hitokoto);
      })
      .catch(() => {
        startTyping(fallbackSentence);
      });
};
const handleStoredTypeChange = (event: Event) => {
  const nextType = normalizeHitokotoType((event as CustomEvent).detail);
  selectedType.value = nextType;
  fetchSentence(nextType);
};
const handleTypeChange = () => {
  selectedType.value = normalizeHitokotoType(selectedType.value);
  localStorage.setItem(HITOKOTO_TYPE_STORAGE_KEY, selectedType.value);
  window.dispatchEvent(new CustomEvent(HITOKOTO_TYPE_CHANGE_EVENT, {detail: selectedType.value}));
};
onMounted(() => {
  selectedType.value = normalizeHitokotoType(localStorage.getItem(HITOKOTO_TYPE_STORAGE_KEY));
  window.addEventListener(HITOKOTO_TYPE_CHANGE_EVENT, handleStoredTypeChange);
  fetchSentence();
});
onBeforeUnmount(() => {
  window.removeEventListener(HITOKOTO_TYPE_CHANGE_EVENT, handleStoredTypeChange);
});
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.imgs {
  position: relative;
  width: 100%;
  height: clamp(11rem, 24vw, 17rem);
  border: 1px solid var(--home-border);
  border-radius: 0.65rem;
  background: var(--home-surface-strong);
  box-shadow: var(--home-shadow), var(--home-glow);
  overflow: hidden;

  .item {
    @include absolute;
    width: 100%;
    height: 100%;
    background: no-repeat 50% 50% / cover;
    opacity: 0;
    animation: imageAnimation 38s linear infinite 0s;
    backface-visibility: hidden;
    transform-style: preserve-3d;

    &:nth-child(2) {
      animation-delay: 6s;
    }

    &:nth-child(3) {
      animation-delay: 12s;
    }

    &:nth-child(4) {
      animation-delay: 18s;
    }

    &:nth-child(5) {
      animation-delay: 24s;
    }

    &:nth-child(6) {
      animation-delay: 30s;
    }
  }

  &::before {
    content: '';
    display: block;
    position: absolute;
    inset: 0;
    z-index: 1;
    background:
      linear-gradient(90deg, rgba(253, 251, 247, 0.3), rgba(85, 162, 160, 0.08) 46%, rgba(36, 36, 36, 0.18)),
      linear-gradient(180deg, rgba(255, 254, 250, 0.05), rgba(253, 251, 247, 0.28));
    transition: all .2s ease-in-out 0s;
  }

  &::after {
    content: "";
    position: absolute;
    inset: 0.8rem;
    z-index: 2;
    border: 1px solid rgba(255, 254, 250, 0.46);
    border-radius: 0.5rem;
    pointer-events: none;
  }
}

.quote-card {
  position: absolute;
  left: 1.35rem;
  bottom: 1.15rem;
  z-index: 3;
  width: min(70%, 32rem);
  padding: 0.72rem 0.9rem 0.76rem 1rem;
  border: 1px solid rgba(255, 254, 250, 0.54);
  border-left: 4px solid rgba(85, 162, 160, 0.76);
  border-radius: 0.45rem;
  background: rgba(255, 254, 250, 0.72);
  box-shadow: 0 10px 24px rgba(42, 35, 24, 0.12);
  backdrop-filter: blur(8px);
}

.quote-label {
  display: inline-flex;
  align-items: center;
  color: var(--home-accent);
  font-size: 0.76rem;
  font-weight: 800;
  line-height: 1.2;
}

.quote-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  margin-bottom: 0.18rem;
}

.quote-type {
  display: inline-flex;
  align-items: center;
  gap: 0.28rem;
  flex: 0 0 auto;
  max-width: 8.2rem;
  padding: 0.12rem 0.18rem 0.12rem 0.45rem;
  border: 1px solid rgba(85, 162, 160, 0.32);
  border-radius: 999px;
  background-color: rgba(255, 254, 250, 0.64);
  color: rgba(51, 51, 51, 0.58);
  font-size: 0.7rem;
  line-height: 1;

  span {
    flex: 0 0 auto;
  }
}

.quote-type-select {
  max-width: 5.6rem;
  height: 1.3rem;
  padding: 0 0.32rem;
  border: 0;
  outline: none;
  background-color: transparent;
  color: rgba(51, 51, 51, 0.78);
  font-size: 0.72rem;
  line-height: 1;
  cursor: pointer;
  transition: color .18s ease;

  &:hover,
  &:focus {
    color: rgba(51, 51, 51, 0.92);
  }
}

.quote-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  margin: 0;
  overflow: hidden;
  color: rgba(51, 51, 51, 0.88);
  font-size: clamp(0.92rem, 1.45vw, 1.08rem);
  font-weight: 700;
  line-height: 1.55;
}

.easy-typed-cursor {
  margin-left: 0.18rem;
  color: var(--home-accent-warm);
  animation: blink 0.95s infinite;
}

@keyframes imageAnimation {
  0% {
    opacity: 0;
    transform: scale(1);
    animation-timing-function: ease-in;
  }

  2% {
    opacity: 1;
  }

  10% {
    opacity: 1;
    transform: scale(1.03);
    animation-timing-function: ease-out;
  }

  19% {
    opacity: 1;
    transform: scale(1.075);
  }

  26% {
    opacity: 0;
    transform: scale(1.085);
  }

  100% {
    opacity: 0;
    transform: scale(1.085);
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

@media (max-width: 768px) {
  .imgs {
    height: 10.5rem;
    border-radius: 0.6rem;
  }

  .quote-card {
    left: 0.85rem;
    right: 0.85rem;
    bottom: 0.75rem;
    width: auto;
    padding: 0.55rem 0.7rem 0.58rem 0.78rem;
  }

  .quote-type {
    max-width: 7.4rem;
    padding-left: 0.36rem;
    font-size: 0.66rem;
  }

  .quote-type-select {
    max-width: 4.85rem;
    height: 1.25rem;
    font-size: 0.68rem;
  }

  .quote-text {
    -webkit-line-clamp: 1;
    font-size: 0.86rem;
  }
}
</style>
