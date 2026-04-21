<template>
  <div class="page-header">
    <h1 class="page-title">关于</h1>
    <img
      class="page-cover"
      src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png"
      alt=""
    />
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <section class="about-shell">
        <div class="hero-card aurora-border">
          <div class="avatar-box">
            <span class="avatar-glow"></span>
            <img class="author-avatar" :src="authorAvatar" alt="author avatar" />
          </div>

          <div class="hero-copy">
            <p class="hero-kicker">HELLO, I'M</p>
            <h2 class="hero-name">{{ siteConfig.siteName || "Shoka Blog" }}</h2>
            <p class="hero-subtitle">把热爱写成故事，把灵感变成作品。</p>

            <div class="tag-list">
              <span v-for="tag in profileTags" :key="tag" class="tag-item">{{ tag }}</span>
            </div>
          </div>

          <div class="hero-meta">
            <div class="meta-card">
              <span class="meta-label">定位</span>
              <p>创作者 / 开发者 / 记录者</p>
            </div>
            <div class="meta-card">
              <span class="meta-label">更新节奏</span>
              <p>持续更新 · 长期主义</p>
            </div>
            <div class="meta-card" v-if="contactLinks.length">
              <span class="meta-label">社交</span>
              <div class="contact-links">
                <a
                  v-for="item in contactLinks"
                  :key="item.label"
                  class="contact-link"
                  :href="item.href"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {{ item.label }}
                </a>
              </div>
            </div>
          </div>
        </div>

        <div class="about-grid">
          <article class="glass-card intro-card">
            <div class="card-head">
              <h3>关于我</h3>
              <span>About</span>
            </div>
            <v-md-preview class="md" :text="siteConfig.aboutMe || ''"></v-md-preview>
          </article>

          <article class="glass-card timeline-card">
            <div class="card-head">
              <h3>成长轨迹</h3>
              <span>Timeline</span>
            </div>
            <ul class="timeline-list">
              <li v-for="item in milestones" :key="item.title" class="timeline-item">
                <span class="timeline-dot"></span>
                <div class="timeline-content">
                  <p class="timeline-title">{{ item.title }}</p>
                  <p class="timeline-desc">{{ item.desc }}</p>
                </div>
              </li>
            </ul>
          </article>

          <article class="glass-card values-card">
            <div class="card-head">
              <h3>创作理念</h3>
              <span>Principles</span>
            </div>
            <div class="value-list">
              <div v-for="value in values" :key="value.title" class="value-item">
                <p class="value-title">{{ value.title }}</p>
                <p class="value-desc">{{ value.desc }}</p>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import useStore from "@/store";
import Waves from "@/components/Waves/index.vue";

const { blog } = useStore();

const siteConfig = computed(() => blog.blogInfo?.siteConfig ?? ({} as Record<string, string>));

const authorAvatar = computed(() => siteConfig.value.authorAvatar || siteConfig.value.touristAvatar || "");

const profileTags = ["全栈开发", "技术写作", "设计审美", "终身学习"];

const milestones = [
  {
    title: "从好奇到热爱",
    desc: "在持续实践中打磨技术，逐渐建立自己的表达方式与创作风格。"
  },
  {
    title: "从输入到输出",
    desc: "把日常学习沉淀为文章和项目，让知识真正可复用、可传播。"
  },
  {
    title: "从个人到共创",
    desc: "乐于交流与协作，用技术连接更多有趣的人和想法。"
  }
];

const values = [
  {
    title: "长期主义",
    desc: "相信微小但持续的积累，会在未来形成复利。"
  },
  {
    title: "真实表达",
    desc: "记录真实问题、真实思考与真实成长。"
  },
  {
    title: "美感与工程并重",
    desc: "既关注代码质量，也重视页面体验与视觉细节。"
  }
];

const contactLinks = computed(() => {
  const links = [
    { label: "GitHub", href: siteConfig.value.github },
    { label: "Gitee", href: siteConfig.value.gitee },
    {
      label: "QQ空间",
      href: siteConfig.value.qq ? `https://user.qzone.qq.com/${siteConfig.value.qq}/main` : ""
    }
  ];
  return links.filter((item) => item.href);
});
</script>

<style scoped>
.about-shell {
  position: relative;
  z-index: 1;
  padding: 44px 0 72px;
  color: #eef3ff;
}

.about-shell::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 10% 20%, rgba(140, 175, 255, 0.2), transparent 38%),
    radial-gradient(circle at 90% 4%, rgba(111, 233, 211, 0.16), transparent 35%);
  pointer-events: none;
}

.hero-card,
.glass-card {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(145deg, rgba(19, 29, 52, 0.74), rgba(14, 22, 40, 0.5));
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 18px 45px rgba(3, 8, 20, 0.3);
}

.hero-card {
  display: grid;
  grid-template-columns: 180px 1fr 320px;
  gap: 28px;
  padding: 34px;
  border-radius: 28px;
  overflow: hidden;
}

.aurora-border::after {
  content: "";
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(120deg, rgba(157, 193, 255, 0.45), rgba(104, 245, 200, 0.25), rgba(157, 193, 255, 0.4));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.avatar-box {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-glow {
  position: absolute;
  width: 148px;
  height: 148px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(147, 176, 255, 0.5), rgba(111, 233, 211, 0.16) 55%, transparent 72%);
  filter: blur(2px);
}

.author-avatar {
  position: relative;
  z-index: 1;
  width: 124px;
  height: 124px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.66);
  object-fit: cover;
  box-shadow: 0 12px 32px rgba(9, 14, 28, 0.45);
  transition: transform 0.5s ease;
}

.author-avatar:hover {
  transform: translateY(-6px) rotate(4deg);
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  margin: 0;
  color: rgba(223, 232, 255, 0.76);
  font-size: 12px;
  letter-spacing: 0.34em;
}

.hero-name {
  margin: 14px 0 12px;
  font-family: "Noto Serif SC", "STZhongsong", serif;
  font-size: 42px;
  font-weight: 700;
  line-height: 1.12;
  color: #f7fbff;
  text-shadow: 0 8px 22px rgba(13, 22, 44, 0.35);
}

.hero-subtitle {
  margin: 0;
  font-size: 16px;
  line-height: 1.8;
  color: rgba(236, 244, 255, 0.86);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.tag-item {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(191, 213, 255, 0.5);
  background: rgba(103, 140, 227, 0.16);
  font-size: 12px;
  letter-spacing: 0.04em;
  color: #e8f2ff;
}

.hero-meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-card {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(187, 206, 245, 0.26);
  background: rgba(20, 33, 59, 0.5);
}

.meta-label {
  display: block;
  margin-bottom: 6px;
  color: rgba(201, 217, 247, 0.8);
  font-size: 12px;
  letter-spacing: 0.2em;
}

.meta-card p {
  margin: 0;
  color: #f4f7ff;
  line-height: 1.7;
}

.contact-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.contact-link {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(194, 220, 255, 0.42);
  color: #f2f8ff;
  font-size: 12px;
  text-decoration: none;
  transition: all 0.25s ease;
}

.contact-link:hover {
  color: #0f1d39;
  background: rgba(231, 244, 255, 0.95);
  box-shadow: 0 6px 20px rgba(176, 214, 255, 0.34);
}

.about-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 20px;
  margin-top: 22px;
}

.glass-card {
  border-radius: 24px;
  padding: 24px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}

.card-head h3 {
  margin: 0;
  font-family: "Noto Serif SC", "STZhongsong", serif;
  font-size: 24px;
  color: #f8fcff;
}

.card-head span {
  color: rgba(212, 226, 249, 0.72);
  font-size: 12px;
  letter-spacing: 0.16em;
}

.intro-card {
  grid-row: span 2;
}

.intro-card :deep(.md) {
  margin-top: 4px;
  border-radius: 14px;
  background: rgba(12, 20, 36, 0.24) !important;
}

.intro-card :deep(.v-md-editor-preview) {
  padding: 0 !important;
  background: transparent !important;
}

.intro-card :deep(.v-md-editor-preview pre),
.intro-card :deep(.v-md-editor-preview code) {
  border-radius: 10px;
}

.timeline-list {
  list-style: none;
  padding: 0;
  margin: 4px 0 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 9px;
  border-radius: 50%;
  background: linear-gradient(140deg, #7fc3ff, #78f0ca);
  box-shadow: 0 0 0 4px rgba(121, 221, 203, 0.18);
  flex-shrink: 0;
}

.timeline-title {
  margin: 0;
  font-size: 16px;
  color: #f2f6ff;
}

.timeline-desc {
  margin: 6px 0 0;
  color: rgba(219, 232, 255, 0.82);
  line-height: 1.75;
}

.value-list {
  display: grid;
  gap: 12px;
}

.value-item {
  padding: 13px 14px;
  border-radius: 14px;
  border: 1px solid rgba(186, 208, 245, 0.24);
  background: rgba(17, 28, 50, 0.46);
}

.value-title {
  margin: 0;
  color: #f3f8ff;
  font-weight: 600;
}

.value-desc {
  margin: 8px 0 0;
  color: rgba(215, 229, 252, 0.84);
  line-height: 1.74;
}

@media (max-width: 1180px) {
  .hero-card {
    grid-template-columns: 140px 1fr;
  }

  .hero-meta {
    grid-column: 1 / -1;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .meta-card {
    flex: 1 1 220px;
  }
}

@media (max-width: 900px) {
  .about-shell {
    padding-top: 28px;
  }

  .hero-card {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 24px;
  }

  .avatar-box {
    justify-content: flex-start;
  }

  .hero-name {
    font-size: 34px;
  }

  .about-grid {
    grid-template-columns: 1fr;
  }

  .intro-card {
    grid-row: auto;
  }
}

@media (max-width: 640px) {
  .hero-name {
    font-size: 30px;
  }

  .glass-card,
  .hero-card {
    border-radius: 18px;
  }

  .glass-card {
    padding: 18px;
  }
}
</style>
