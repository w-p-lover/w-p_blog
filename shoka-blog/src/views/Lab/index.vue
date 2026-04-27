<template>
  <div class="page-header">
    <h1 class="page-title">实验室</h1>
    <p class="page-subtitle">一些兴趣、尝试和临时灵感，统一放在这里。</p>
    <img class="page-cover" src="../../assets/images/bg.jpg" alt="背景图">
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container lab-container">
      <div class="lab-header">
        <span class="lab-kicker">LAB INDEX</span>
        <p>重要的尝试，生活痕迹，零散工具。</p>
      </div>

      <section
          v-for="section in labSections"
          :key="section.title"
          class="lab-section"
      >
        <div class="section-heading">
          <span>{{ section.kicker }}</span>
          <h3>{{ section.title }}</h3>
        </div>

        <div class="lab-grid">
          <router-link
              v-for="item in section.items"
              :key="item.path"
              :to="item.path"
              class="lab-card"
          >
            <span class="card-icon">
              <svg-icon :icon-class="item.icon"></svg-icon>
            </span>
            <span class="card-body">
              <strong>{{ item.name }}</strong>
              <small>{{ item.description }}</small>
            </span>
            <svg-icon class="card-arrow" icon-class="angle-right"></svg-icon>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import {labMenuItems} from "@/router/menu";
import Waves from "@/components/Waves/index.vue";

const descriptions: Record<string, string> = {
  AI问答: "结构化提问与回答",
  共享: "协作文档与知识空间",
  天气: "城市天气与生活参考",
  项目: "趋势、项目和技术记录",
  游戏: "Steam 与游戏兴趣",
  路线: "生活轨迹和阶段记录",
  书目: "阅读与收藏",
  聊天: "实时聊天实验",
};

const labCardMap = labMenuItems.reduce((result, item) => {
  if (item.path !== "/lab") {
    result[item.name] = {
      ...item,
      description: descriptions[item.name] ?? "个人实验入口",
    };
  }
  return result;
}, {} as Record<string, typeof labMenuItems[number] & { description: string }>);

const pickCards = (names: string[]) => names.map((name) => labCardMap[name]).filter(Boolean);

const labSections = [
  {
    kicker: "FOCUS",
    title: "重点实验",
    items: pickCards(["AI问答", "共享", "项目"]),
  },
  {
    kicker: "RECORD",
    title: "长期记录",
    items: pickCards(["路线", "书目", "游戏"]),
  },
  {
    kicker: "UTILITY",
    title: "小工具",
    items: pickCards(["天气", "聊天"]),
  },
];
</script>

<style lang="scss" scoped>
.lab-header {
  margin-bottom: 0.65rem;

  p {
    margin: -0.45rem 0 0;
    color: var(--grey-5);
  }
}

.lab-kicker {
  display: inline-block;
  color: var(--color-pink);
}

.lab-section {
  &:not(:first-of-type) {
    margin-top: 1.25rem;
  }
}

.lab-container {
  padding-top: 1.15rem;
}

.section-heading {
  display: flex;
  align-items: baseline;
  gap: 0.55rem;
  margin-bottom: 0.45rem;

  span {
    color: var(--color-pink);
  }

  h3 {
    margin: 0;
    color: var(--grey-7);
  }
}

.lab-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.lab-card {
  display: grid;
  grid-template-columns: 2.45rem 1fr auto;
  align-items: center;
  min-height: 5.5rem;
  padding: 1rem;
  border: 1px solid var(--surface-border-soft);
  border-radius: 0.5rem;
  background: var(--surface-soft);
  box-shadow: 0 0.35rem 1rem rgba(13, 31, 62, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-0.18rem);
    border-color: var(--color-pink-a3);
    box-shadow: 0 0.75rem 1.35rem rgba(13, 31, 62, 0.12);
  }
}

.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  color: var(--color-pink);
  background: var(--note-bg);
}

.card-body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.5;

  strong {
    color: var(--grey-7);
  }

  small {
    overflow: hidden;
    color: var(--grey-5);
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.card-arrow {
  color: var(--grey-4);
}

@media (max-width: 991px) {
  .lab-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 575px) {
  .lab-grid {
    grid-template-columns: 1fr;
  }

  .lab-card {
    min-height: 4.8rem;
  }
}
</style>
