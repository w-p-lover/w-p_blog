<template>
  <div class="tags-view-container">
    <scroll-pane ref="scrollPaneRef" class="tags-view-wrapper" @scroll="handleScroll">
      <router-link v-for="item in visitedViews" :key="item.path" :data-path="item.path"
                   :class="isActive(item) ? 'active' : ''" :to="{ path: item.path, query: item.query }"
                   class="tags-view-item"
                   @click.middle="!isAffix(item) ? closeSelectedTag(item) : ''"
                   @contextmenu.prevent="openMenu(item, $event)">
        {{ item.meta?.title }}
        <span v-if="!isAffix(item)" class="icon-close" @click.prevent.stop="closeSelectedTag(item)">
          <svg-icon icon-class="close" size="0.9rem"/>
        </span>
      </router-link>
    </scroll-pane>
    <ul v-show="visible" :style="{ left: left + 'px', top: top + 'px' }" class="contextmenu">
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">
        <close style="width: 1em; height: 1em;vertical-align: -0.12em;"/>
        关闭当前
      </li>
      <li @click.stop="closeOtherTags">
        <circle-close style="width: 1em; height: 1em;vertical-align: -0.12em;"/>
        关闭其他
      </li>
      <li v-if="!isFirstView()" @click="closeLeftTags">
        <back style="width: 1em; height: 1em;vertical-align: -0.12em;"/>
        关闭左侧
      </li>
      <li v-if="!isLastView()" @click="closeRightTags">
        <right style="width: 1em; height: 1em;vertical-align: -0.12em;"/>
        关闭右侧
      </li>
      <li @click.stop="closeAllTags(selectedTag)">
        <circle-close style="width: 1em; height: 1em;vertical-align: -0.12em;"/>
        关闭所有
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import useStore from "@/store";
import {TagView} from "@/store/interface";
import {ComponentInternalInstance, computed, getCurrentInstance, nextTick, onMounted, ref, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import ScrollPane from "./ScrollPane.vue";

const router = useRouter();
const route = useRoute();
const {proxy} = getCurrentInstance() as ComponentInternalInstance;
const {tag, permission} = useStore();
const visible = ref(false);
const top = ref(0);
const left = ref(0);
const affixTags = ref<TagView[]>([]);
const selectedTag = ref({});
const scrollPaneRef = ref();
const visitedViews = computed(() => tag.visitedViews);
const routes = computed(() => permission.routes);

const openMenu = (tag: TagView, e: MouseEvent) => {
  const menuMinWidth = 105;
  const offsetLeft = proxy?.$el.getBoundingClientRect().left; // container margin left
  const offsetWidth = proxy?.$el.offsetWidth; // container width
  const maxLeft = offsetWidth - menuMinWidth; // left boundary
  const l = e.clientX - offsetLeft + 15; // 15: margin right
  if (l > maxLeft) {
    left.value = maxLeft;
  } else {
    left.value = l;
  }
  top.value = e.clientY;
  visible.value = true;
  selectedTag.value = tag;
};
const closeSelectedTag = (view: TagView) => {
  tag.delView(view).then((res: any) => {
    if (isActive(view)) {
      toLastView(res.visitedViews, view);
    }
  });
};
const closeOtherTags = () => {
  router.push(selectedTag.value);
  tag.delOtherViews(selectedTag.value).then(() => {
    moveToCurrentTag();
  });
  closeMenu();
};
const closeAllTags = (view: TagView) => {
  tag.delAllViews().then((res: any) => {
    toLastView(res.visitedViews, view);
  });
  closeMenu();
};
const toLastView = (visitedViews: TagView[], view?: any) => {
  const latestView = visitedViews.slice(-1)[0];
  if (latestView && latestView.fullPath) {
    router.push(latestView.fullPath);
  } else {
    // now the default is to redirect to the home page if there is no tags-view,
    // you can adjust it according to your needs.
    if (view.name === 'Dashboard') {
      // to reload home page
      router.replace({path: '/redirect' + view.fullPath});
    } else {
      router.push('/');
    }
  }
};
const isFirstView = () => {
  try {
    return (
        (selectedTag.value as TagView).fullPath ===
        tag.visitedViews[1].fullPath ||
        (selectedTag.value as TagView).fullPath === '/index'
    );
  } catch (err) {
    return false;
  }
};
const isLastView = () => {
  try {
    return (
        (selectedTag.value as TagView).fullPath ===
        tag.visitedViews[tag.visitedViews.length - 1].fullPath
    );
  } catch (err) {
    return false;
  }
};
const closeLeftTags = () => {
  tag.delLeftViews(selectedTag.value).then((res: any) => {
    if (!res.visitedViews.find((item: any) => item.fullPath === route.fullPath)) {
      toLastView(res.visitedViews);
    }
  });
}
const closeRightTags = () => {
  tag.delRightViews(selectedTag.value).then((res: any) => {
    if (!res.visitedViews.find((item: any) => item.fullPath === route.fullPath)) {
      toLastView(res.visitedViews);
    }
  });
};
const closeMenu = () => {
  visible.value = false;
};
const handleScroll = () => {
  visible.value = false;
};
const isActive = (tag: TagView) => {
  return tag.path === route.path;
};
const isAffix = (tag: TagView) => {
  return tag.meta && tag.meta.affix;
};
const addTags = () => {
  if (route.name) {
    tag.addView(route);
  }
};
const moveToCurrentTag = () => {
  nextTick(() => {
    for (const r of visitedViews.value) {
      if (r.path === route.path) {
        // when query is different then update
        if (r.fullPath !== route.fullPath) {
          tag.updateVisitedView(route);
        }
      }
    }
  });
};
const filterAffixTags = (routes: any[], basePath = '') => {
  let tags: TagView[] = [];
  routes.forEach(route => {
    if (route.meta && route.meta.affix) {
      const tagPath = getNormalPath(basePath + '/' + route.path)
      tags.push({
        fullPath: tagPath,
        path: tagPath,
        name: route.name,
        meta: {...route.meta}
      });
    }
    if (route.children) {
      const childTags = filterAffixTags(route.children, route.path);
      if (childTags.length >= 1) {
        tags = tags.concat(childTags);
      }
    }
  });
  return tags;
}
const getNormalPath = (p: string) => {
  if (p.length === 0 || !p || p == 'undefined') {
    return p
  }
  ;
  let res = p.replace('//', '/')
  if (res[res.length - 1] === '/') {
    return res.slice(0, res.length - 1)
  }
  return res;
}

const initTags = () => {
  const res = filterAffixTags(routes.value);
  affixTags.value = res;
  for (const item of res) {
    // Must have tag name
    if ((item as TagView).name) {
      tag.addVisitedView(item);
    }
  }
};
watch(
    route,
    () => {
      addTags();
      moveToCurrentTag();
    },
    {
      //初始化立即执行
      immediate: true
    }
);
watch(visible, value => {
  if (value) {
    document.body.addEventListener('click', closeMenu);
  } else {
    document.body.removeEventListener('click', closeMenu);
  }
});
onMounted(() => {
  initTags();
});
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: 38px;
  width: 100%;
  background-color: var(--el-bg-color);
  /* 顶部边框修饰 - 渐变线条 */
  border-top: 2px solid transparent;
  border-image: linear-gradient(90deg, #84a374 0%, #8f9de1 50%, #ab7de4 100%) 0.5;
  border-bottom: 2px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;

  .tags-view-wrapper {
    height: 100%;
    padding: 0 12px;

    .tags-view-item {
      display: inline-flex;
      align-items: center;
      position: relative;
      cursor: pointer;
      height: 27px;
      line-height: 27px;
      padding: 0 12px;
      margin: 4px 4px 0;
      font-size: 13px;
      border-radius: 4px;
      color: #193a68;
      border: 1px solid transparent;
      transition: all 0.2s ease;
      white-space: nowrap;
      background-color: rgba(109, 171, 232, 0.16);
      &:first-of-type {
        margin-left: 0;
      }

      &:hover {
        background-color: #f3f4f6;
        border-color: #ffffff;
      }

      &.active {
        background: linear-gradient(135deg, #2f437e 0%, #7c94cc 100%);
        color: #ffffff;
        border-color: #2563eb;
        box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
        font-weight: 500;

        &:hover {
          background: linear-gradient(135deg, #7c94cc 0%, #2f437e 100%);
          border-color: #1d4ed8;
        }
      }

      .icon-close {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 16px;
        height: 16px;
        margin-left: 6px;
        border-radius: 50%;
        opacity: 0.8;
        transition: all 0.2s ease;

        &:hover {
          background-color: rgba(255, 255, 255, 0.2);
          opacity: 1;
          transform: scale(1.1);
        }
      }
    }
  }

  .contextmenu {
    margin: 0;
    background: #ffffff;
    z-index: 3000;
    position: absolute;
    list-style-type: none;
    padding: 4px 0;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 400;
    color: #374151;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    border: 1px solid #e5e7eb;
    transition: opacity 0.2s ease, transform 0.2s ease;
    transform-origin: top left;
    transform: scale(1);
    opacity: 1;

    &:before {
      content: '';
      position: absolute;
      top: -5px;
      left: 12px;
      width: 10px;
      height: 10px;
      background: #ffffff;
      border-top: 1px solid #e5e7eb;
      border-left: 1px solid #e5e7eb;
      transform: rotate(45deg);
    }

    li {
      margin: 0;
      padding: 6px 16px;
      cursor: pointer;
      transition: background-color 0.2s ease;

      &:hover {
        background: #f3f4f6;
        color: #1e40af;
      }

      svg {
        margin-right: 6px;
      }
    }
  }
}

// 标签切换和关闭动画
@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.tags-view-item {
  animation: fadeIn 0.2s ease-out forwards;
}

.tags-view-item.active {
  z-index: 10;
}

// 右键菜单显示动画
.contextmenu {
  animation: menuFadeIn 0.15s ease-out forwards;
}

@keyframes menuFadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
</style>
