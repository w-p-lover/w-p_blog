<template>
  <div class="catalog-header">
    <svg-icon icon-class="category"></svg-icon>
    目录
  </div>
  <div class="catalog-content">
    <div class="catalog-item"
         v-for="(anchor, index) of titleList"
         :key="`${anchor.titleHash}-${index}`"
         :class="currentIndex === index ? 'active' : ''"
         :style="{ paddingLeft: `${5 + anchor.indent * 15}px` }"
         @click="handleAnchorClick(anchor, index)">
      <a> {{ anchor.title }} </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue';
import { useScroll, watchThrottled } from '@vueuse/core';

const titleList = ref<any>([]);
const currentIndex = ref(0);
const props = defineProps({
  domRef: {
    type: Object,
    default: null,
  }
});

// 生成标题唯一标识（解决无id的问题）
const generateTitleHash = (title: string) => {
  return title.replace(/\s+/g, '_').replace(/[^\w\u4e00-\u9fa5]/g, '') + '_' + Date.now();
};

// 优化：兼容有id和无id的标题解析
const getTitles = () => {
  // 确保domRef存在且已挂载
  if (!props.domRef || !props.domRef.$el) {
    titleList.value = [];
    return;
  }

  // 关键修改1：去掉[id]限制，匹配所有h1/h2/h3
  const anchors = props.domRef.$el.querySelectorAll('h1, h2, h3');
  if (!anchors.length) {
    titleList.value = [];
    return;
  }

  const titles = Array.from(anchors).filter((t: any) => {
    const text = t.innerText?.trim();
    // 过滤空标题、纯图片/符号的标题
    return !!text && text.length < 100 && !/^[\s\u3000]*$/.test(text);
  });

  if (!titles.length) {
    titleList.value = [];
    return;
  }

  const hTags = Array.from(new Set(titles.map((t: any) => t.tagName))).sort();

  titleList.value = titles.map((el: any, idx: number) => {
    const titleText = el.innerText.trim();
    return {
      title: titleText,
      // 兼容逻辑：有id用id，无id生成唯一hash
      id: el.id || generateTitleHash(titleText),
      lineIndex: el.getAttribute('data-v-md-line'),
      indent: hTags.indexOf(el.tagName),
      offsetTop: el.offsetTop,
      // 记录元素索引，用于无id时的精准查找
      elementIndex: idx,
      // 原始元素引用（备用）
      rawElement: el
    };
  });
};

// 优化：兼容两种格式的点击定位逻辑
function handleAnchorClick(anchor: any, idx: number) {
  if (!props.domRef || !props.domRef.$el) return;

  let heading = null;
  const allHeadings = Array.from(props.domRef.$el.querySelectorAll('h1, h2, h3'));

  // 策略1：通过元素索引直接获取（最可靠，兼容无id场景）
  if (anchor.elementIndex !== undefined && allHeadings[anchor.elementIndex]) {
    heading = allHeadings[anchor.elementIndex];
  }
  // 策略2：通过id查找（兼容有id场景）
  else if (anchor.id) {
    heading = props.domRef.$el.querySelector(`#${CSS.escape(anchor.id)}`);
  }
  // 策略3：通过data-v-md-line查找
  else if (anchor.lineIndex) {
    heading = props.domRef.$el.querySelector(`[data-v-md-line="${anchor.lineIndex}"]`);
  }
  // 策略4：通过标题文本匹配（兜底）
  else if (anchor.title) {
    heading = allHeadings.find((h: any) => h.innerText.trim() === anchor.title);
  }

  if (heading) {
    // 计算滚动位置（适配固定头部）
    const scrollTop = heading.offsetTop - 60;
    window.scrollTo({
      behavior: 'smooth',
      top: scrollTop,
    });
    currentIndex.value = idx;
  }
}

// 优化：滚动监听逻辑（兼容两种格式）
const { y } = useScroll(window);
watchThrottled(y, () => {
  if (titleList.value.length === 0) return;

  let currentActiveIndex = 0;
  const scrollPosition = y.value + 70;
  const allHeadings = Array.from(props.domRef.$el.querySelectorAll('h1, h2, h3'));

  // 从后往前匹配，确保高亮当前可视区域最上方的标题
  for (let i = titleList.value.length - 1; i >= 0; i--) {
    const anchor = titleList.value[i];
    let heading = null;

    // 优先用元素索引查找
    if (anchor.elementIndex !== undefined && allHeadings[anchor.elementIndex]) {
      heading = allHeadings[anchor.elementIndex];
    } else if (anchor.id) {
      heading = props.domRef.$el.querySelector(`#${CSS.escape(anchor.id)}`);
    }

    if (heading && heading.offsetTop <= scrollPosition) {
      currentActiveIndex = i;
      break;
    }
  }

  currentIndex.value = currentActiveIndex;
}, { throttle: 100, leading: true, trailing: true });

// 监听domRef变化，自动重新解析目录
watch([() => props.domRef], () => {
  nextTick(() => {
    getTitles();
  });
}, { deep: true, immediate: true });

// 监听标题列表变化，确保offsetTop实时更新（解决动态渲染问题）
watch([titleList], () => {
  nextTick(() => {
    if (titleList.value.length && props.domRef?.$el) {
      const allHeadings = Array.from(props.domRef.$el.querySelectorAll('h1, h2, h3'));
      titleList.value.forEach((anchor: any, idx: number) => {
        if (allHeadings[anchor.elementIndex]) {
          anchor.offsetTop = allHeadings[anchor.elementIndex].offsetTop;
        }
      });
    }
  });
}, { deep: true });

onMounted(() => {
  // 双重nextTick确保MD内容完全渲染
  nextTick(() => {
    nextTick(() => {
      getTitles();
    });
  });
});
</script>

<style lang="scss" scoped>
.catalog-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.65rem;
  color: var(--home-accent-warm);
  font-weight: 700;
}

.catalog-content {
  max-height: calc(100vh - 100px);
  overflow: auto;
  margin-right: -16px;
  padding-right: 16px;
}

.catalog-item {
  margin: 0.2rem 0;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  font-size: 14px;
  padding: 0.24rem 0.4rem;
  border-radius: 0.35rem;
  color: var(--grey-6);
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    color: var(--primary-color);
  }
}

.active {
  background-color: var(--home-accent-soft);
  color: var(--home-accent);
  font-weight: 700;

  &:hover {
    background-color: var(--home-accent-soft);
    color: var(--home-accent);
  }
}
</style>
