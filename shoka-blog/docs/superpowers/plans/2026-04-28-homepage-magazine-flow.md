# Homepage Magazine Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the home page into an ornate but orderly article-first magazine flow while keeping the alternating article card layout.

**Architecture:** This is a focused Vue/Sass visual refinement. The plan changes shared theme tokens first, then updates home-page support modules, then makes the article list the dominant visual system. It avoids data-flow, API, and routing changes.

**Tech Stack:** Vue 3 single-file components, TypeScript setup scripts, SCSS, Swiper, Element Plus, Vite.

---

## File Structure

- Modify: `src/assets/styles/theme-shoka.scss`
  - Owns reusable visual tokens for refined surfaces, borders, accent colors, text contrast, and card shadows in light and dark themes.
- Modify: `src/assets/styles/common.scss`
  - Owns global page background, two-column home/page layout spacing, shared side-card styling, and responsive container behavior.
- Modify: `src/views/Home/index.vue`
  - Adds semantic wrappers/classes for the home content rhythm without changing component order or data behavior.
- Modify: `src/views/Home/Brand/index.vue`
  - Refines the first-screen title, typed sentence, scroll cue, and hero spacing.
- Modify: `src/views/Home/Swiper/Images.vue`
  - Unifies the hero image overlay so image changes keep consistent contrast.
- Modify: `src/views/Home/Swiper/TalkSwiper.vue`
  - Reduces the talk strip into a light personal signal.
- Modify: `src/views/Home/Swiper/Recommend.vue`
  - Keeps the recommendation carousel while reducing its badge, shadow, overlay, and control intensity.
- Modify: `src/views/Article/ArticleItem.vue`
  - Converts filters into a toolbar, preserves alternating cards, softens image clipping, improves typography, and refines the read action.

## Task 1: Add Refined Home Visual Tokens

**Files:**
- Modify: `src/assets/styles/theme-shoka.scss`

- [ ] **Step 1: Add light-theme tokens**

In `:root`, add these tokens near the existing `--surface-*` and `--shadow-*` variables:

```scss
  --home-accent: #e97192;
  --home-accent-soft: rgba(233, 113, 146, 0.13);
  --home-accent-warm: #ee9a76;
  --home-accent-cool: #5aaedc;
  --home-surface: rgba(255, 255, 255, 0.82);
  --home-surface-strong: rgba(255, 255, 255, 0.94);
  --home-surface-muted: rgba(248, 251, 255, 0.7);
  --home-border: rgba(128, 148, 188, 0.22);
  --home-border-strong: rgba(132, 151, 190, 0.34);
  --home-title: #2f3442;
  --home-muted: rgba(66, 74, 92, 0.68);
  --home-shadow: 0 14px 34px rgba(22, 35, 66, 0.11);
  --home-shadow-hover: 0 20px 42px rgba(22, 35, 66, 0.16);
  --home-glow: 0 0 0 1px rgba(255, 255, 255, 0.62) inset;
```

- [ ] **Step 2: Add dark-theme tokens**

In `[theme="dark"]:root`, add corresponding dark tokens near the existing `--surface-*` and `--shadow-*` variables:

```scss
  --home-accent: #ef8faf;
  --home-accent-soft: rgba(239, 143, 175, 0.16);
  --home-accent-warm: #f0a387;
  --home-accent-cool: #75bad8;
  --home-surface: rgba(33, 40, 52, 0.78);
  --home-surface-strong: rgba(37, 45, 59, 0.94);
  --home-surface-muted: rgba(29, 35, 46, 0.72);
  --home-border: rgba(144, 173, 232, 0.24);
  --home-border-strong: rgba(166, 189, 235, 0.36);
  --home-title: rgba(246, 248, 255, 0.92);
  --home-muted: rgba(218, 224, 238, 0.68);
  --home-shadow: 0 16px 36px rgba(0, 0, 0, 0.34);
  --home-shadow-hover: 0 22px 44px rgba(0, 0, 0, 0.44);
  --home-glow: 0 0 0 1px rgba(255, 255, 255, 0.09) inset;
```

- [ ] **Step 3: Run a build check**

Run:

```bash
npm run build
```

Expected: build completes with no Sass variable errors.

- [ ] **Step 4: Commit**

```bash
git add src/assets/styles/theme-shoka.scss
git commit -m "style: add refined homepage theme tokens"
```

## Task 2: Compose The Home Content Area

**Files:**
- Modify: `src/views/Home/index.vue`
- Modify: `src/assets/styles/common.scss`

- [ ] **Step 1: Add semantic home content classes**

In `src/views/Home/index.vue`, replace the content block with:

```vue
  <div class="bg home-bg">
    <div class="main-container home-main mt">
      <div class="left-container home-feed" :class="app.sideFlag ? 'test' : ''">
        <!-- 说说 -->
        <TalkSwiper></TalkSwiper>
        <!-- 推荐文章 -->
        <Recommend></Recommend>
        <!-- 文章列表 -->
        <ArticleItem></ArticleItem>
      </div>
      <SideBar class="right-container home-sidebar" :class="app.sideFlag ? 'temp' : ''"></SideBar>
    </div>
  </div>
```

- [ ] **Step 2: Refine scoped home spacing**

In the scoped style of `src/views/Home/index.vue`, replace `.mt` with:

```scss
.mt {
  margin-top: 1.15rem;
  padding-bottom: 2.5rem;
}

.home-feed {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
```

- [ ] **Step 3: Refine global page background and layout**

In `src/assets/styles/common.scss`, update `.bg`, `.main-container`, `.right-container`, and `.side-card` to use the new home tokens:

```scss
.bg {
  position: relative;
  padding-bottom: 9.5rem;
  background:
    radial-gradient(circle at 12% -8%, rgba(233, 113, 146, 0.13), transparent 32%),
    radial-gradient(circle at 88% -4%, rgba(90, 174, 220, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(246, 249, 255, 0.82), var(--grey-0) 34%);
}

.main-container {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: calc(100% - 0.625rem);
  margin: 0 auto;
  padding-bottom: 2rem;
  gap: 0.95rem;
  animation: slideUpIn 1s;
}

.right-container {
  position: sticky;
  top: 1rem;
  width: 18rem;
  margin-left: 0;
}

.side-card {
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid var(--home-border);
  background: linear-gradient(180deg, var(--home-surface-strong), var(--home-surface));
  box-shadow: var(--home-shadow), var(--home-glow);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;

  &:not(:first-child) {
    margin-top: 1rem;
  }

  &:hover {
    transform: translateY(-1px);
    border-color: var(--home-border-strong);
    box-shadow: var(--home-shadow-hover), var(--home-glow);
  }
}
```

- [ ] **Step 4: Verify sidebar behavior**

Run:

```bash
npm run build
```

Expected: build passes and no selector errors are reported.

- [ ] **Step 5: Commit**

```bash
git add src/views/Home/index.vue src/assets/styles/common.scss
git commit -m "style: compose homepage magazine layout"
```

## Task 3: Refine The Hero Atmosphere

**Files:**
- Modify: `src/views/Home/Brand/index.vue`
- Modify: `src/views/Home/Swiper/Images.vue`

- [ ] **Step 1: Refine brand title styles**

In `src/views/Home/Brand/index.vue`, update `.brand-container`, `.brand`, `.artboard`, `.title`, and `.arrow-down`:

```scss
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

.arrow-down {
  position: absolute;
  bottom: 70px;
  opacity: 0.78;
  filter: drop-shadow(0 8px 18px rgba(2, 10, 22, 0.36));
  animation: arrow-shake 2.2s ease-out infinite;
  cursor: pointer;
  z-index: 8;
}
```

- [ ] **Step 2: Refine hero image overlay**

In `src/views/Home/Swiper/Images.vue`, replace the `.imgs::before` background with:

```scss
  &::before {
    content: '';
    display: block;
    position: absolute;
    inset: 0;
    background:
      linear-gradient(155deg, rgba(5, 12, 28, 0.42), rgba(8, 18, 38, 0.2) 42%, rgba(5, 12, 24, 0.44)),
      radial-gradient(circle at 50% 38%, rgba(255, 255, 255, 0.08), transparent 34%);
    transition: all .2s ease-in-out 0s;
  }
```

- [ ] **Step 3: Verify production build**

Run:

```bash
npm run build
```

Expected: build passes.

- [ ] **Step 4: Commit**

```bash
git add src/views/Home/Brand/index.vue src/views/Home/Swiper/Images.vue
git commit -m "style: refine homepage hero atmosphere"
```

## Task 4: Calm Supporting Home Modules

**Files:**
- Modify: `src/views/Home/Swiper/TalkSwiper.vue`
- Modify: `src/views/Home/Swiper/Recommend.vue`

- [ ] **Step 1: Refine talk strip**

In `src/views/Home/Swiper/TalkSwiper.vue`, update `.talk-swiper`, `.swiper-container`, `.slide-content`, and `.arrow`:

```scss
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
  animation: 1.7s passing infinite;
}
```

- [ ] **Step 2: Refine recommendation carousel shell**

In `src/views/Home/Swiper/Recommend.vue`, update `.swiper-container` and its `&::before`:

```scss
.swiper-container {
  height: 13.25rem;
  margin: 0 0.5rem;
  border-radius: 0.85rem;
  border: 1px solid var(--home-border);
  box-shadow: var(--home-shadow), var(--home-glow);
  overflow: hidden;

  &::before {
    content: '推荐';
    position: absolute;
    z-index: 2;
    top: 0.65rem;
    left: 0.75rem;
    display: flex;
    justify-content: center;
    min-width: 3.7rem;
    padding: 0.1rem 0.6rem;
    border-radius: 99px;
    color: var(--grey-0);
    background: rgba(233, 113, 146, 0.86);
    font-size: 0.82rem;
    line-height: 1.5;
    box-shadow: 0 8px 16px rgba(210, 113, 146, 0.22);
  }
}
```

- [ ] **Step 3: Refine recommendation content overlay and controls**

In the same file, update `.slide-content`, `.slide-title`, pagination, and navigation rules:

```scss
.slide-content {
  @include flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 0 3.125rem 1.25rem;
  background-position: center !important;
  background-size: cover !important;

  .slide-title {
    font-size: clamp(1.35rem, 2.6vw, 1.8rem);
    font-weight: 700;
    text-shadow: 0 8px 20px rgba(5, 10, 20, 0.34);
  }

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(145deg, rgba(6, 12, 26, 0.46), rgba(8, 14, 28, 0.2) 48%, rgba(5, 10, 19, 0.46));
  }
}

:deep(.swiper-pagination) .swiper-pagination-bullet {
  display: inline-block;
  width: 0.55rem;
  height: 0.55rem;
  margin: 0 0.22rem;
  border-radius: 6.1875rem;
  background: rgba(255, 255, 255, 0.88);
  opacity: 0.72;
  transition: all 0.3s;
}

:deep(.swiper-pagination) .swiper-pagination-bullet.swiper-pagination-bullet-active {
  opacity: 1;
  background-color: var(--home-accent-cool);
  width: 1.55rem;
}

:deep(.swiper-button-next),
:deep(.swiper-button-prev) {
  width: 2.5rem;
  height: 2.5rem;
  color: rgba(255, 255, 255, 0.86);
  transition: all 0.3s;
}
```

- [ ] **Step 4: Build check**

Run:

```bash
npm run build
```

Expected: build passes.

- [ ] **Step 5: Commit**

```bash
git add src/views/Home/Swiper/TalkSwiper.vue src/views/Home/Swiper/Recommend.vue
git commit -m "style: calm homepage supporting modules"
```

## Task 5: Make Article Filters A Toolbar

**Files:**
- Modify: `src/views/Article/ArticleItem.vue`

- [ ] **Step 1: Update filter template classes**

In `src/views/Article/ArticleItem.vue`, replace the filter block at the top of the template with:

```vue
  <div class="article-toolbar">
    <div class="sortList" aria-label="文章排序">
      <button
          type="button"
          class="sort-option"
          @click="changeSort('id')"
          :class="{ active: queryParams.sort === 'id' }"
      >
        <svg-icon icon-class="calendar" size="1rem"></svg-icon>
        <span>按时间</span>
      </button>
      <button
          type="button"
          class="sort-option"
          @click="changeSort('views')"
          :class="{ active: queryParams.sort === 'views' }"
      >
        <svg-icon icon-class="fun" size="1rem"></svg-icon>
        <span>按热度</span>
      </button>
    </div>
    <el-select
        clearable
        v-model="selectedTag"
        placeholder="选择标签"
        size="large"
        class="selectWidth"
        @change="filterByTag"
    >
      <el-option
          v-for="tag in tagList"
          :key="tag.id"
          :label="tag.tagName"
          :value="tag.id"
      />
    </el-select>
    <div class="date-picker-container">
      <el-date-picker
          is-range
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="filterByDate"
      ></el-date-picker>
    </div>
  </div>
```

- [ ] **Step 2: Replace filter styles**

Remove the duplicated `.sort-options` style blocks and the existing `:deep( .el-date-editor.el-input__wrapper )` rule. Add:

```scss
.article-toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0 0.5rem 0.25rem;
  padding: 0.72rem;
  border: 1px solid var(--home-border);
  border-radius: 0.85rem;
  background: linear-gradient(180deg, var(--home-surface-strong), var(--home-surface));
  box-shadow: 0 10px 24px rgba(22, 35, 66, 0.08), var(--home-glow);
}

.sortList {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.sort-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  min-height: 2.5rem;
  padding: 0 0.9rem;
  border: 1px solid transparent;
  border-radius: 0.65rem;
  background: var(--home-surface-muted);
  color: var(--home-muted);
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;

  &.active {
    color: var(--home-accent);
    border-color: rgba(233, 113, 146, 0.34);
    background: var(--home-accent-soft);
  }

  &:hover {
    transform: translateY(-1px);
    color: var(--home-accent);
    border-color: rgba(233, 113, 146, 0.28);
  }
}

.selectWidth {
  width: 13rem;
}

.date-picker-container {
  min-width: 18rem;
}

:deep(.el-select .el-input__wrapper),
:deep(.el-date-editor.el-input__wrapper) {
  min-height: 2.5rem;
  border-radius: 0.65rem;
  box-shadow: 0 0 0 1px var(--home-border) inset;
  background: var(--home-surface-muted);
  color: var(--home-muted);
}
```

- [ ] **Step 3: Add responsive toolbar behavior**

Inside the existing `@media (max-width: 767px)` block, replace the `.sort-options` rules with:

```scss
  .article-toolbar {
    align-items: stretch;
    flex-direction: column;
    margin: 0 0.5rem 0.25rem;
  }

  .sortList {
    width: 100%;
  }

  .sort-option {
    flex: 1;
    min-width: 0;
  }

  .selectWidth,
  .date-picker-container {
    width: 100%;
    min-width: 0;
  }

  :deep(.el-date-editor--daterange.el-input__wrapper) {
    width: 100%;
  }
```

- [ ] **Step 4: Build check**

Run:

```bash
npm run build
```

Expected: build passes.

- [ ] **Step 5: Commit**

```bash
git add src/views/Article/ArticleItem.vue
git commit -m "style: refine homepage article filters"
```

## Task 6: Refine Alternating Article Cards

**Files:**
- Modify: `src/views/Article/ArticleItem.vue`

- [ ] **Step 1: Update read action text**

In the article card template, replace:

```vue
      <router-link class="article-btn" :to="`/article/${article.id}`">more...</router-link>
```

with:

```vue
      <router-link class="article-btn" :to="`/article/${article.id}`">阅读全文</router-link>
```

- [ ] **Step 2: Replace base card styles**

Replace `.article-item`, `.article-cover`, `.article-info`, `.article-category`, and `.article-btn` styles with:

```scss
.article-item {
  display: flex;
  min-height: 15rem;
  margin: 0.25rem 0.5rem 0;
  border-radius: 0.95rem;
  border: 1px solid var(--home-border);
  background: linear-gradient(180deg, var(--home-surface-strong), var(--home-surface));
  box-shadow: var(--home-shadow), var(--home-glow);
  animation-duration: 0.5s;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
  overflow: hidden;
  visibility: hidden;

  &:hover {
    transform: translateY(-3px);
    border-color: var(--home-border-strong);
    box-shadow: var(--home-shadow-hover), var(--home-glow);

    .cover {
      transform: scale(1.045);
    }
  }

  &:nth-child(even) {
    flex-direction: row-reverse;

    .article-cover {
      margin-right: auto;
      margin-left: 1.15rem;
      clip-path: polygon(4% 0, 100% 0, 100% 100%, 0 100%);
      border-radius: 0 0.95rem 0.95rem 0;
    }

    .article-info {
      padding: 1.15rem 0 3.15rem 1.35rem;

      .article-meta {
        justify-content: flex-start;
      }
    }

    .article-btn {
      left: 1.35rem;
      right: auto;
    }

    .article-category {
      right: 1.35rem;
      justify-content: flex-start;
    }
  }
}

.article-cover {
  width: 48%;
  margin-right: 1.15rem;
  clip-path: polygon(0 0, 96% 0, 100% 100%, 0 100%);
  border-radius: 0.95rem 0 0 0.95rem;
  overflow: hidden;

  .cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.28s ease;
  }
}

.article-info {
  position: relative;
  width: 52%;
  padding: 1.15rem 1.35rem 3.15rem 0;

  .article-meta {
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.35rem 0.55rem;
    font-size: 0.78rem;
    line-height: 1.4;
    color: var(--home-muted);
  }

  .top {
    color: var(--home-accent-warm);
  }

  .meta-item {
    display: inline-flex;
    align-items: center;
  }

  .ml:not(:first-child) {
    margin-left: 0;
  }

  .article-title {
    margin: 0.7rem 0 0.45rem;
    color: var(--home-title);
    font-size: clamp(1.15rem, 2vw, 1.45rem);
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .article-content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    max-height: 5.3rem;
    font-family: Mulish, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
    font-size: 0.9rem;
    line-height: 1.7;
    color: var(--home-muted);
    overflow: hidden;
  }
}

.article-category {
  position: absolute;
  display: flex;
  align-items: center;
  bottom: 1rem;
  font-size: 0.8rem;
  color: var(--home-muted);
}

.article-btn {
  position: absolute;
  right: 1.35rem;
  bottom: 0.85rem;
  padding: 0.24rem 0.8rem;
  border: 1px solid rgba(233, 113, 146, 0.32);
  border-radius: 99px;
  color: var(--home-accent);
  font-size: 0.82rem;
  line-height: 1.6;
  background: var(--home-accent-soft);
}
```

- [ ] **Step 3: Replace mobile article card styles**

Inside `@media (max-width: 767px)`, replace the existing `.article-item` mobile block with:

```scss
  .article-item {
    flex-direction: column;
    min-height: 0;

    .article-cover {
      width: 100%;
      height: 13.5rem;
      margin: 0;
      clip-path: polygon(0 0, 100% 0, 100% 96%, 0 100%);
      border-radius: 0.95rem 0.95rem 0 0;
    }

    .article-info {
      width: 100%;
      min-height: 13rem;
      padding: 0.95rem 1rem 3.4rem;

      .article-meta {
        justify-content: flex-start;
      }
    }

    .article-category {
      left: 1rem;
      right: auto;
    }

    .article-btn {
      right: 1rem;
      left: auto;
    }

    &:nth-child(even) {
      flex-direction: column;

      .article-cover {
        width: 100%;
        margin: 0;
        clip-path: polygon(0 0, 100% 0, 100% 100%, 0 96%);
        border-radius: 0.95rem 0.95rem 0 0;
      }

      .article-info {
        padding: 0.95rem 1rem 3.4rem;
      }

      .article-category {
        left: 1rem;
        right: auto;
      }

      .article-btn {
        right: 1rem;
        left: auto;
      }
    }
  }
```

- [ ] **Step 4: Build check**

Run:

```bash
npm run build
```

Expected: build passes.

- [ ] **Step 5: Commit**

```bash
git add src/views/Article/ArticleItem.vue
git commit -m "style: refine alternating article cards"
```

## Task 7: Final Verification And Polish

**Files:**
- Modify only files already touched in Tasks 1-6 if verification reveals layout issues.

- [ ] **Step 1: Run production build**

Run:

```bash
npm run build
```

Expected: build passes.

- [ ] **Step 2: Start local dev server**

Run:

```bash
npm run dev -- --host 127.0.0.1
```

Expected: Vite prints a local URL, usually `http://127.0.0.1:5173/`.

- [ ] **Step 3: Browser-check desktop**

Open the local URL and check:

- At about 1440px wide, the hero title is centered and readable.
- The talk strip and recommendation carousel look supporting, not dominant.
- The article cards are the strongest content element below the hero.
- Alternating card clipping is visible but softer than before.
- Sidebar cards match the calmer surface language.

- [ ] **Step 4: Browser-check responsive widths**

Check around 991px and 767px widths:

- Sidebar hides at the existing breakpoint.
- Toolbar controls stack without horizontal overflow.
- Article card text does not overlap category or read button.
- Cover images keep stable height.
- Long article titles truncate cleanly.

- [ ] **Step 5: Browser-check dark theme**

Toggle dark theme if available in the UI and check:

- Card surfaces remain readable.
- Metadata and excerpts are not too faint.
- Hero overlay still keeps title legible.
- Images are not excessively dim.

- [ ] **Step 6: Commit any verification polish**

If no changes are needed, do not commit. If small polish changes are needed, commit only those files:

```bash
git add src/assets/styles/theme-shoka.scss src/assets/styles/common.scss src/views/Home/index.vue src/views/Home/Brand/index.vue src/views/Home/Swiper/Images.vue src/views/Home/Swiper/TalkSwiper.vue src/views/Home/Swiper/Recommend.vue src/views/Article/ArticleItem.vue
git commit -m "style: polish homepage magazine flow"
```

## Self-Review

- Spec coverage: Tasks cover theme tokens, content area composition, hero, talk strip, recommendation carousel, filter toolbar, alternating cards, sidebar surface styling, responsive checks, dark mode checks, and build verification.
- Scope control: The plan does not change APIs, routing, backend behavior, pagination logic, article fetching, or carousel libraries.
- Placeholder scan: No placeholder steps remain; each task names concrete files, commands, and expected outcomes.
- Type consistency: No new TypeScript types or data contracts are introduced.
