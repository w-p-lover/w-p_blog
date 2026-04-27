<template>
  <div class="page-header">
    <div class="page-title">
      <h1>关于</h1>
      <p class="page-subtitle">{{ siteConfig.siteIntro || "写代码，也写下生活里值得保存的部分。" }}</p>
    </div>
    <img class="page-cover" src="@/assets/images/bg.jpg" alt="">
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="page-container about-container">
      <section class="about-identity">
        <div class="identity-mark">W&P</div>
        <img class="identity-avatar" :src="authorAvatar" alt="">
        <div class="identity-copy">
          <span>ABOUT</span>
          <h2>{{ siteConfig.siteAuthor || siteConfig.siteName || "W&P" }}</h2>
          <p>{{ siteConfig.siteIntro || "在长期记录里，把技术、热爱和生活慢慢串起来。" }}</p>
        </div>
        <div class="mood-gallery" aria-label="喜欢的片段">
          <div
              v-for="(item, index) in favoriteFrames"
              :key="item.quote"
              class="mood-frame"
              :style="{
                '--mood-image': `url(${item.image})`,
                animationDelay: `${index * 4}s`
              }"
          >
            <div class="mood-image-stage">
              <img :src="item.image" alt="">
            </div>
            <div class="mood-copy">
              <span>{{ item.label }}</span>
              <p>{{ item.quote }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="about-body">
        <article class="about-main">
          <div class="section-title">
            <span>INTRODUCTION</span>
            <h3>一些关于我的事</h3>
          </div>
          <v-md-preview
              v-if="siteConfig.aboutMe"
              class="md about-md"
              :text="siteConfig.aboutMe"
          ></v-md-preview>
          <p v-else class="empty-text">这里还没有填写关于我的内容。</p>
        </article>

        <aside class="about-aside">
          <div class="aside-block">
            <span class="aside-label">SITE</span>
            <div class="site-stats">
              <div v-for="item in stats" :key="item.label">
                <strong>{{ item.value }}</strong>
                <span>{{ item.label }}</span>
              </div>
            </div>
          </div>

          <div class="aside-block" v-if="contactLinks.length">
            <span class="aside-label">ELSEWHERE</span>
            <div class="link-list">
              <a
                  v-for="item in contactLinks"
                  :key="item.label"
                  :href="item.href"
                  target="_blank"
                  rel="noopener noreferrer"
              >
                <svg-icon :icon-class="item.icon" size="1rem"></svg-icon>
                {{ item.label }}
              </a>
            </div>
          </div>
        </aside>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import useStore from "@/store";
import Waves from "@/components/Waves/index.vue";
import fallbackAvatar from "@/assets/img/head_portrait.jpg";
import portraitOne from "@/assets/img/head_portrait1.jpg";
import portraitTwo from "@/assets/img/head_portrait2.jpg";
import portraitThree from "@/assets/img/head_portrait3.jpg";

const { blog } = useStore();

const siteConfig = computed(() => blog.blogInfo.siteConfig);

const authorAvatar = computed(() =>
    siteConfig.value.authorAvatar ||
    siteConfig.value.userAvatar ||
    siteConfig.value.touristAvatar ||
    fallbackAvatar
);

const stats = computed(() => [
  {
    label: "文章",
    value: blog.blogInfo.articleCount ?? 0,
  },
  {
    label: "分类",
    value: blog.blogInfo.categoryCount ?? 0,
  },
  {
    label: "标签",
    value: blog.blogInfo.tagCount ?? 0,
  },
  {
    label: "访问",
    value: blog.blogInfo.viewCount ?? 0,
  },
]);

const favoriteFrames = [
  {
    image: portraitOne,
    label: "IMAGE",
    quote: "把喜欢的东西，慢慢变成自己的秩序。",
  },
  {
    image: portraitTwo,
    label: "QUOTE",
    quote: "保持热爱，也保持一点清醒的距离。",
  },
  {
    image: portraitThree,
    label: "MOMENT",
    quote: "生活不是素材库，但值得被认真收藏。",
  },
];

const contactLinks = computed(() => {
  const links = [
    {
      label: "GitHub",
      icon: "github",
      href: siteConfig.value.github,
    },
    {
      label: "Gitee",
      icon: "gitee",
      href: siteConfig.value.gitee,
    },
    {
      label: "Bilibili",
      icon: "bilibili",
      href: siteConfig.value.bilibili,
    },
    {
      label: "QQ",
      icon: "qq",
      href: siteConfig.value.qq ? `https://user.qzone.qq.com/${siteConfig.value.qq}/main` : "",
    },
  ];

  return links.filter((item) => item.href);
});
</script>

<style lang="scss" scoped>
.page-subtitle {
  margin: 0.35rem 0 0;
  max-width: 38rem;
  text-align: center;
  font-size: 1rem;
  letter-spacing: 0;
}

.about-container {
  width: min(62rem, calc(100% - 0.625rem));
  overflow: hidden;
}

.about-identity {
  position: relative;
  display: grid;
  grid-template-columns: 7.5rem minmax(0, 1fr) 16rem;
  gap: 1.35rem;
  align-items: center;
  min-height: 14.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--surface-border-soft);
}

.identity-mark {
  position: absolute;
  right: -0.25rem;
  top: -1.85rem;
  color: var(--color-pink-a1);
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(5.75rem, 12vw, 8rem);
  line-height: 1;
  pointer-events: none;
}

.identity-avatar {
  position: relative;
  z-index: 1;
  width: 7.5rem;
  height: 7.5rem;
  border-radius: 50%;
  object-fit: cover;
  padding: 0.18rem;
  border: 1px solid var(--surface-border-soft);
  box-shadow: 0 0.55rem 1.35rem rgba(13, 31, 62, 0.12);
}

.identity-copy {
  position: relative;
  z-index: 1;
  padding-left: 0.95rem;
  border-left: 0.18rem solid var(--color-pink-a3);

  span {
    color: var(--color-pink);
  }

  h2 {
    margin: 0.2rem 0 0.45rem;
    color: var(--grey-7);
  }

  p {
    max-width: 34rem;
    margin: 0;
    color: var(--grey-5);
  }
}

.mood-gallery {
  position: relative;
  z-index: 1;
  height: 12.75rem;
  overflow: hidden;
  border-radius: 0.5rem;
  border: 1px solid var(--surface-border-soft);
  background: var(--surface-soft);
  box-shadow: 0 0.7rem 1.5rem rgba(13, 31, 62, 0.1);

  &::before {
    content: "";
    position: absolute;
    right: 0.75rem;
    top: 0.75rem;
    z-index: 3;
    width: 2.4rem;
    height: 0.18rem;
    border-radius: 999px;
    background: var(--color-pink);
    box-shadow: 0 0.45rem 0 var(--color-orange);
  }
}

.mood-frame {
  position: absolute;
  inset: 0;
  display: grid;
  grid-template-rows: 7.4rem 1fr;
  opacity: 0;
  transform: translateX(0.35rem);
  animation: moodFrameFade 12s infinite ease-in-out;
}

.mood-image-stage {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 20%, rgba(233, 84, 107, 0.14), transparent 34%),
    radial-gradient(circle at 82% 12%, rgba(83, 178, 210, 0.12), transparent 36%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.74), rgba(246, 248, 255, 0.52)),
    var(--mood-image) center / cover;
  border-top: 1px solid var(--surface-border-soft);

  &::before {
    content: "";
    position: absolute;
    inset: -1.4rem;
    background: var(--mood-image) center / cover;
    filter: blur(1.8rem) saturate(0.56) contrast(0.9);
    opacity: 0.24;
    transform: scale(1.12);
  }

  &::after {
    content: "";
    position: absolute;
    inset: 0;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.66)),
      linear-gradient(135deg, rgba(233, 84, 107, 0.08), rgba(83, 178, 210, 0.08));
    pointer-events: none;
  }

  img {
    position: relative;
    z-index: 2;
    width: calc(100% - 1.2rem);
    height: calc(100% - 1rem);
    object-fit: contain;
    filter: saturate(0.98) contrast(1.02);
    transform: scale(1.02);
    animation: moodImage 12s infinite;
  }
}

.mood-copy {
  position: relative;
  min-height: 5.35rem;
  padding: 0 0.85rem 0 1.25rem;
  color: var(--grey-7);
  background:
    linear-gradient(90deg, rgba(233, 84, 107, 0.08), transparent 54%),
    rgba(255, 255, 255, 0.78);
  border-top: 1px solid var(--surface-border-soft);

  span {
    color: var(--color-pink);
    font-size: 0.7rem;
    letter-spacing: 0.12rem;
  }

  p {
    color: var(--grey-7);
    font-size: 0.86rem;
    word-break: break-all;
    line-height: 1.45;
    display: -webkit-box;
    overflow: hidden;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
  }
}

.about-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 14rem;
  gap: 1.8rem;
  padding-top: 1.35rem;
}

.section-title,
.aside-label {
  color: var(--color-pink);
}

.section-title {
  margin-bottom: 0.8rem;

  h3 {
    margin: 0.15rem 0 0;
    color: var(--grey-7);
  }
}

.about-md {
  overflow: hidden;
}

.about-md :deep(.v-md-editor-preview) {
  padding: 0 !important;
  background: transparent !important;
}

.about-aside {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  border-left: 1px solid var(--surface-border-soft);
  padding-left: 1.25rem;
}

.aside-block {
  display: grid;
  gap: 0.75rem;
}

.site-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.7rem;

  div {
    display: flex;
    flex-direction: column;
    color: var(--grey-5);
    line-height: 1.35;
  }

  strong {
    color: var(--grey-7);
    font-size: 1.25rem;
  }
}

.link-list {
  display: grid;
  gap: 0.45rem;

  a {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    color: var(--grey-6);

    &:hover {
      color: var(--color-pink);
      transform: translateX(0.15rem);
    }
  }
}

.empty-text {
  margin: 0;
  color: var(--grey-5);
}

@media (max-width: 820px) {
  .about-identity {
    grid-template-columns: 7.5rem minmax(0, 1fr);
  }

  .mood-gallery {
    grid-column: 1 / -1;
    height: 17rem;
  }

  .mood-frame {
    grid-template-rows: 11.6rem 1fr;
  }

  .about-body {
    grid-template-columns: 1fr;
  }

  .about-aside {
    border-left: 0;
    border-top: 1px solid var(--surface-border-soft);
    padding: 1.25rem 0 0;
  }
}

@media (max-width: 575px) {
  .about-identity {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .identity-avatar {
    margin: 0 auto;
  }

  .identity-copy p {
    margin: 0 auto;
  }

  .identity-copy {
    padding-left: 0;
    border-left: 0;
  }

  .mood-gallery {
    height: 14.5rem;
  }

  .mood-frame {
    grid-template-rows: 8.6rem 1fr;
  }
}

@keyframes moodFrameFade {
  0% {
    opacity: 0;
    transform: translateX(0.35rem);
  }

  7%,
  31% {
    opacity: 1;
    transform: translateX(0);
  }

  38%,
  100% {
    opacity: 0;
    transform: translateX(-0.35rem);
  }
}

@keyframes moodImage {
  0% {
    transform: scale(1.02) translateX(0);
  }

  32% {
    transform: scale(1.06) translateX(-0.2rem);
  }

  100% {
    transform: scale(1.02) translateX(0);
  }
}
</style>
