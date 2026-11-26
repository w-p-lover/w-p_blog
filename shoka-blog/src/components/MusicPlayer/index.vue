<template>
  <meting-js v-if="enabled && ready" :key="playlistId" server="netease" type="playlist"
             :id="playlistId" fixed="true" list-folded="true" autoplay="false" preload="none" theme="#e9546b"></meting-js>
</template>

<script setup lang="ts">
import useStore from "@/store";

const {blog} = useStore();
const enabled = computed(() => !!blog.blogInfo.siteConfig.isMusic);
const playlistId = computed(() => String(blog.blogInfo.siteConfig.musicId || "7573319758").trim());
const ready = ref(false);
let loading = false;

const loadScript = (src: string) => new Promise<void>((resolve, reject) => {
  const script = document.createElement("script");
  script.src = src;
  script.onload = () => resolve();
  script.onerror = () => { script.remove(); reject(new Error("音乐播放器资源加载失败")); };
  document.head.appendChild(script);
});

watch(enabled, async (value) => {
  if (!value || ready.value || loading) return;
  loading = true;
  try {
    if (!document.getElementById("blog-player-style")) {
      const style = document.createElement("link");
      style.id = "blog-player-style";
      style.rel = "stylesheet";
      style.href = "https://cdn.jsdelivr.net/npm/aplayer@1.10.1/dist/APlayer.min.css";
      document.head.appendChild(style);
    }
    if (!(window as Window & {APlayer?: unknown}).APlayer) await loadScript("/APlayer.min.js");
    if (!customElements.get("meting-js")) await loadScript("https://cdn.jsdelivr.net/npm/meting@2.0.1/dist/Meting.min.js");
    ready.value = true;
  } catch (error) {
    console.warn("音乐播放器暂时不可用", error);
  } finally {
    loading = false;
  }
}, {immediate: true});
</script>
