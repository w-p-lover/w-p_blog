<template>
  <div class="page-header">
    <h1 class="page-title">音乐库</h1>
    <img class="page-cover" :src="pageCover" alt="">
    <Waves></Waves>
  </div>

  <div class="bg">
    <main class="page-container music-container music-detail-container">
      <section v-if="song" class="music-detail">
        <button class="detail-back" type="button" @click="router.push('/music')">返回音乐库</button>

        <article class="lyric-stage" :class="{ playing: isPlaying }">
          <div class="stage-backdrop" :style="{ backgroundImage: `url(${song.cover})` }"></div>
          <div class="stage-vignette" aria-hidden="true"></div>
          <audio
            v-if="song.url"
            ref="audioRef"
            :src="song.url"
            preload="metadata"
            @loadedmetadata="syncAudioDuration"
            @timeupdate="syncAudioTime"
            @ended="handlePlaybackEnded"
          ></audio>

          <section class="stage-identity">
            <div class="stage-cover-wrap">
              <div class="stage-disc" aria-hidden="true"></div>
              <img class="stage-cover" :src="song.cover" :alt="song.title">
            </div>

            <div class="stage-title">
              <span>{{ song.playlistName }}</span>
              <h2>{{ song.title }}</h2>
              <p>{{ song.artist }}</p>
            </div>

            <p class="stage-note">{{ song.note || song.summary || "这里还可以慢慢补上这首歌被收藏的理由。" }}</p>

            <div class="sound-tags">
              <span>{{ isPlaying ? "播放中" : "已暂停" }}</span>
              <span>{{ song.rating }} / 5</span>
              <span>{{ song.mood || "待整理" }}</span>
              <span>{{ favoriteLevelLabel[song.favoriteLevel] }}</span>
            </div>

            <button class="stage-play" type="button" @click="togglePlayback">
              <span>{{ isPlaying ? "PAUSE" : "PLAY" }}</span>
            </button>

            <div class="stage-actions">
              <el-button type="primary" @click="openEditPanel">编辑收藏</el-button>
              <el-button v-if="song.sourceUrl" @click="openSource(song.sourceUrl)">网易云来源</el-button>
            </div>
          </section>

          <section class="stage-lyrics">
            <div class="stage-lyrics__head">
              <span class="section-kicker">LYRICS</span>
              <small>{{ currentTimeLabel }} / {{ lyricDurationLabel }}</small>
            </div>
            <div class="stage-lyrics__body">
              <p v-if="lyricLoading">歌词读取中...</p>
              <p v-else-if="lyricError">{{ lyricError }}</p>
              <template v-else-if="lyricLines.length">
                <p
                  v-for="(line, index) in lyricLines"
                  :key="`${line.time}-${line.text}`"
                  :class="{ active: index === activeLyricIndex }"
                >
                  {{ line.text }}
                </p>
              </template>
              <p v-else>这首歌暂时没有可展示的歌词。</p>
            </div>
            <div class="stage-player" aria-hidden="true">
              <span>{{ currentTimeLabel }}</span>
              <i><b :style="{ width: `${playProgress}%` }"></b></i>
              <span>{{ lyricDurationLabel }}</span>
            </div>
          </section>
        </article>
      </section>

      <section v-else class="music-empty">
        <h3>还没有找到这首歌</h3>
        <p>可能歌曲还没有导入，或者本地收藏数据被清空了。</p>
        <el-button type="primary" @click="router.push('/music')">返回音乐库</el-button>
      </section>
    </main>

    <el-dialog v-model="showEditDialog" title="编辑歌曲展示" width="680px" class="music-dialog">
      <div class="edit-form music-edit-panel">
        <label class="edit-field edit-field--wide">
          <span>一句话展示</span>
          <el-input v-model="editForm.summary" type="textarea" :rows="2" maxlength="80" show-word-limit />
        </label>
        <label class="edit-field edit-field--wide">
          <span>收藏理由</span>
          <el-input v-model="editForm.note" type="textarea" :rows="4" maxlength="160" show-word-limit />
        </label>
        <div class="edit-grid edit-control-panel">
          <label class="edit-field">
            <span>标签</span>
            <el-input v-model="editForm.tags" placeholder="用逗号分隔" />
          </label>
          <label class="edit-field">
            <span>心情</span>
            <el-input v-model="editForm.mood" />
          </label>
          <label class="edit-field edit-rate-field">
            <span>评分</span>
            <el-rate v-model="editForm.rating" :max="5" />
          </label>
          <label class="edit-field">
            <span>等级</span>
            <el-select v-model="editForm.favoriteLevel">
              <el-option label="灵光一闪" value="spark" />
              <el-option label="适合循环" value="loop" />
              <el-option label="长期收藏" value="classic" />
            </el-select>
          </label>
        </div>
      </div>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSong">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import pageCover from "@/assets/images/bg.jpg";
import Waves from "@/components/Waves/index.vue";
import useStore from "@/store";
import {
  parseLyricText,
  splitMusicTags,
  type LyricLine,
  type MusicFavoriteLevel,
} from "@/views/Music/musicModel";

const route = useRoute();
const router = useRouter();
const { music } = useStore();

const showEditDialog = ref(false);
const lyricLines = ref<LyricLine[]>([]);
const lyricLoading = ref(false);
const lyricError = ref("");
const audioRef = ref<HTMLAudioElement>();
const isPlaying = ref(false);
const currentTime = ref(0);
const audioDuration = ref(0);
let playbackTimer: number | undefined;
let lyricController: AbortController | null = null;

const favoriteLevelLabel: Record<MusicFavoriteLevel, string> = {
  spark: "灵光一闪",
  loop: "适合循环",
  classic: "长期收藏",
};

const editForm = reactive({
  summary: "",
  note: "",
  tags: "",
  mood: "",
  rating: 3,
  favoriteLevel: "spark" as MusicFavoriteLevel,
});

const song = computed(() => music.items.find((item) => item.id === route.params.id));
const lyricDuration = computed(() => audioDuration.value || lyricLines.value[lyricLines.value.length - 1]?.time || 0);
const activeLyricIndex = computed(() => {
  if (!lyricLines.value.length) {
    return -1;
  }
  const index = lyricLines.value.findIndex((line) => line.time > currentTime.value);
  return index === -1 ? lyricLines.value.length - 1 : Math.max(0, index - 1);
});
const currentTimeLabel = computed(() => formatTime(currentTime.value));
const lyricDurationLabel = computed(() => {
  return lyricDuration.value ? formatTime(lyricDuration.value) : "--:--";
});
const playProgress = computed(() => {
  return lyricDuration.value ? Math.min(100, (currentTime.value / lyricDuration.value) * 100) : 0;
});

const syncEditForm = () => {
  if (!song.value) {
    return;
  }
  editForm.summary = song.value.summary;
  editForm.note = song.value.note;
  editForm.tags = song.value.tags;
  editForm.mood = song.value.mood;
  editForm.rating = song.value.rating;
  editForm.favoriteLevel = song.value.favoriteLevel;
};

const openEditPanel = () => {
  syncEditForm();
  showEditDialog.value = true;
};

const saveSong = () => {
  if (!song.value) {
    return;
  }
  music.updateItem(song.value.id, { ...editForm });
  showEditDialog.value = false;
  window.$message?.success("已保存歌曲展示");
};

const openSource = (url: string) => {
  window.open(url, "_blank", "noopener,noreferrer");
};

const loadLyric = async () => {
  lyricController?.abort();
  lyricLines.value = [];
  lyricError.value = "";

  if (!song.value?.lyricUrl) {
    return;
  }

  const controller = new AbortController();
  lyricController = controller;
  lyricLoading.value = true;
  try {
    const response = await fetch(song.value.lyricUrl, { signal: controller.signal });
    if (!response.ok) {
      throw new Error(`lyric request failed: ${response.status}`);
    }
    lyricLines.value = parseLyricText(await response.text());
    currentTime.value = 0;
  } catch (error) {
    if ((error as Error).name !== "AbortError") {
      lyricError.value = "歌词暂时读取失败。";
    }
  } finally {
    if (lyricController === controller) {
      lyricLoading.value = false;
    }
  }
};

const formatTime = (value: number) => {
  const minutes = Math.floor(value / 60);
  const seconds = Math.floor(value % 60);
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
};

const startSimulatedPlayback = () => {
  window.clearInterval(playbackTimer);
  playbackTimer = window.setInterval(() => {
    if (!isPlaying.value) {
      return;
    }
    const limit = lyricDuration.value || 180;
    currentTime.value = currentTime.value >= limit ? 0 : currentTime.value + 0.5;
  }, 500);
};

const togglePlayback = async () => {
  if (isPlaying.value) {
    audioRef.value?.pause();
    window.clearInterval(playbackTimer);
    isPlaying.value = false;
    return;
  }
  isPlaying.value = true;
  if (!audioRef.value) {
    startSimulatedPlayback();
    return;
  }
  try {
    await audioRef.value.play();
    window.clearInterval(playbackTimer);
  } catch {
    startSimulatedPlayback();
  }
};

const syncAudioDuration = () => {
  audioDuration.value = audioRef.value?.duration || 0;
};

const syncAudioTime = () => {
  currentTime.value = audioRef.value?.currentTime || currentTime.value;
};

const handlePlaybackEnded = () => {
  window.clearInterval(playbackTimer);
  isPlaying.value = false;
  currentTime.value = 0;
};

watch(song, syncEditForm, { immediate: true });
watch(() => song.value?.id, loadLyric, { immediate: true });
onMounted(() => {
  document.body.classList.add("music-library-page");
  music.ensureDefaultSongs();
});

onBeforeUnmount(() => {
  window.clearInterval(playbackTimer);
  lyricController?.abort();
  document.body.classList.remove("music-library-page");
});
</script>

<style scoped>
@import "@/views/Music/css/music.scss";
</style>

<style>
body.music-library-page .aplayer-lrc,
body.music-library-page .aplayer-list {
  display: none !important;
}
</style>
