<script setup lang="ts">
import { lifeMenuItems } from "@/router/menu";
import cover from "@/assets/images/bg.jpg";
</script>

<template>
  <div class="page-header living-header">
    <h1 class="page-title">生活</h1>
    <img class="page-cover" :src="cover" alt="" />
    <Waves />
  </div>
  <div class="bg">
    <div class="page-container living-page">
      <nav class="interest-list" aria-label="生活与兴趣">
        <router-link v-for="item in lifeMenuItems" :key="item.path" :to="item.path" class="interest-link">
          <svg-icon :icon-class="item.icon" size="1.5rem" />
          <span class="interest-copy">
            <span class="interest-name">{{ item.name }}</span>
            <span class="interest-description">{{ item.description }}</span>
          </span>
          <span class="interest-arrow" aria-hidden="true">›</span>
        </router-link>
      </nav>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.living-header,
.living-header .page-cover { height: 22rem; }
.living-header .page-title { height: 16rem; }

.living-page {
  max-width: 52rem;
}

.interest-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 2.5rem;
}

.interest-link {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  min-width: 0;
  padding: 1.35rem 0;
  border-bottom: 1px solid var(--home-border);
  color: var(--text-color);

  &:hover .interest-name { color: var(--home-accent-cool); }
  &:focus-visible {
    outline: 2px solid var(--home-accent-cool);
    outline-offset: 4px;
  }

  > .svg-icon { flex-shrink: 0; color: var(--home-accent-cool); }
}

.interest-copy { display: flex; flex: 1; flex-direction: column; min-width: 0; }
.interest-name { font-size: 1.05rem; font-weight: 600; }
.interest-description { margin-top: 0.3rem; font-size: 0.8125rem; line-height: 1.6; color: var(--home-muted); }
.interest-arrow { font-size: 1.4rem; color: var(--home-muted); }

@media (max-width: 600px) {
  .interest-list { grid-template-columns: 1fr; }
  .interest-link { padding: 1.1rem 0.25rem; }
}
</style>
