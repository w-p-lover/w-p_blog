<template>
  <div class="page-header">
    <h1 class="page-title">音乐库</h1>
    <img class="page-cover" :src="pageCover" alt="">
    <Waves></Waves>
  </div>

  <div class="bg">
    <main class="page-container music-container">
      <section class="music-intro">
        <div>
          <span class="section-kicker">MUSIC COLLECTION</span>
          <h2>我的歌曲收藏</h2>
          <p>{{ heroDescription }}</p>
        </div>
        <div class="intro-actions">
          <el-button type="primary" @click="showImportDialog = true">导入歌曲</el-button>
          <el-button @click="openManualPanel">手动收藏</el-button>
        </div>
      </section>

      <section v-if="playlistGroups.length" class="playlist-shelf" aria-label="来源歌单">
        <article
          v-for="group in playlistGroups"
          :key="group.id"
          class="playlist-card"
          :class="{ active: playlistFilter === group.id }"
          @click="selectPlaylistGroup(group.id)"
        >
          <img :src="group.cover" :alt="group.name">
          <div>
            <span>{{ group.count }} 首歌</span>
            <h3>{{ group.name }}</h3>
            <p>{{ group.preview }}</p>
          </div>
        </article>
      </section>

      <section class="music-toolbar">
        <div class="toolbar-search">
          <span>SEARCH</span>
          <el-input
            v-model="keyword"
            class="music-search"
            clearable
            placeholder="搜索歌名、歌手、标签或备注"
            @clear="music.setFilter({ keyword: '' })"
          />
        </div>
        <div class="toolbar-filters">
          <label>
            <span>来源</span>
            <el-select v-model="playlistFilter" class="music-select" placeholder="歌单">
              <el-option label="全部来源" value="all" />
              <el-option v-for="playlist in music.playlists" :key="playlist.id" :label="playlist.name" :value="playlist.id" />
            </el-select>
          </label>
          <label>
            <span>标签</span>
            <el-select v-model="tagFilter" class="music-select" placeholder="标签">
              <el-option label="全部标签" value="all" />
              <el-option v-for="tag in tags" :key="tag" :label="tag" :value="tag" />
            </el-select>
          </label>
          <label>
            <span>排序</span>
            <el-select v-model="sortType" class="music-select" placeholder="排序">
              <el-option label="精选优先" value="curated" />
              <el-option label="最新导入" value="newest" />
              <el-option label="评分最高" value="rating" />
              <el-option label="标题排序" value="title" />
            </el-select>
          </label>
        </div>
      </section>

      <section class="song-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">SONGS</span>
            <h3>歌曲展示</h3>
          </div>
          <div class="collection-stats">
            <span>{{ stats.total }} 首歌</span>
            <span>{{ stats.playlists }} 个歌单</span>
            <span>{{ stats.tagCount }} 个标签</span>
          </div>
        </div>

        <div v-if="visibleItems.length" class="song-grid">
          <article
            v-for="item in visibleItems"
            :key="item.id"
            class="song-card"
            @click="openDetail(item.id)"
          >
            <div class="song-card__cover">
              <img :src="item.cover" :alt="item.title">
            </div>
            <div class="song-card__body">
              <span>{{ item.playlistName }}</span>
              <h4>{{ item.title }}</h4>
              <p>{{ item.artist }}</p>
              <small>{{ item.summary || "等待补充收藏理由。" }}</small>
            </div>
          </article>
        </div>

        <div v-else class="music-empty">
          <h3>没有匹配的歌曲</h3>
          <p>换个筛选条件，或者先导入一个网易云公开歌单里的歌曲。</p>
          <el-button type="primary" @click="clearFilters">清空筛选</el-button>
        </div>
      </section>
    </main>

    <el-dialog v-model="showImportDialog" title="导入歌曲" width="520px" class="music-dialog">
      <div class="music-import">
        <el-input
          v-model="importSource"
          type="textarea"
          :rows="5"
          placeholder="每行一个公开歌单链接或 ID，可一次导入多个歌单"
        />
        <p v-if="music.importError">{{ music.importError }}</p>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="music.importLoading" @click="importPlaylist">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showManualDialog" title="手动收藏" width="620px" class="music-dialog">
      <div class="manual-form">
        <el-input v-model="manualForm.title" placeholder="歌名" />
        <el-input v-model="manualForm.artist" placeholder="歌手" />
        <el-input v-model="manualForm.cover" placeholder="封面链接" />
        <el-input v-model="manualForm.tags" placeholder="标签，用逗号分隔" />
        <el-input v-model="manualForm.summary" type="textarea" :rows="3" placeholder="一句话备注" />
      </div>
      <template #footer>
        <el-button @click="showManualDialog = false">取消</el-button>
        <el-button type="primary" @click="addManualItem">收藏</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import pageCover from "@/assets/images/bg.jpg";
import Waves from "@/components/Waves/index.vue";
import useStore from "@/store";
import {
  filterMusicItems,
  getAllMusicTags,
  getMusicStats,
  sortMusicItems,
  type MusicItem,
} from "@/views/Music/musicModel";

const { music } = useStore();
const router = useRouter();

const showImportDialog = ref(false);
const showManualDialog = ref(false);
const importSource = ref(music.importSource);

const manualForm = reactive({
  title: "",
  artist: "",
  cover: "",
  tags: "",
  summary: "",
});

const visibleItems = computed(() => sortMusicItems(filterMusicItems(music.items, music.filter), music.sortType));
const stats = computed(() => getMusicStats(music.items));
const tags = computed(() => getAllMusicTags(music.items));
const playlistGroups = computed(() => {
  return music.playlists
    .map((playlist) => {
      const songs = music.items.filter((item) => item.playlistId === playlist.id);
      return {
        id: playlist.id,
        name: playlist.name,
        cover: playlist.cover || songs[0]?.cover || pageCover,
        count: songs.length,
        songs,
        preview: songs.slice(0, 3).map((item) => item.title).join(" / ") || "等待添加歌曲",
      };
    })
    .filter((group) => group.count > 0)
    .sort((left, right) => right.count - left.count);
});
const heroDescription = computed(() => {
  return stats.value.total ? `现在收藏 ${stats.value.total} 首歌，来自 ${stats.value.playlists} 个歌单，适合慢慢补上自己的备注。` : "从一个公开歌单开始，把里面的歌曲变成自己的收藏线索。";
});

const keyword = computed({
  get: () => music.filter.keyword,
  set: (value: string) => music.setFilter({ keyword: value }),
});

const playlistFilter = computed({
  get: () => music.filter.playlistId,
  set: (value: string) => music.setFilter({ playlistId: value }),
});

const tagFilter = computed({
  get: () => music.filter.tag,
  set: (value: string) => music.setFilter({ tag: value }),
});

const sortType = computed({
  get: () => music.sortType,
  set: (value: string) => music.setSortType(value),
});

const openDetail = (id: string) => {
  music.selectItem(id);
  router.push(`/music/${id}`);
};

const selectPlaylistGroup = (id: string) => {
  music.setFilter({ playlistId: id });
  const firstSong = music.items.find((item: MusicItem) => item.playlistId === id);
  if (firstSong) {
    music.selectItem(firstSong.id);
  }
};

const importPlaylist = async () => {
  await music.importNeteasePlaylists(importSource.value);
  if (!music.importError) {
    showImportDialog.value = false;
    window.$message?.success("歌曲已导入");
  }
};

const openManualPanel = () => {
  manualForm.title = "";
  manualForm.artist = "";
  manualForm.cover = "";
  manualForm.tags = "";
  manualForm.summary = "";
  showManualDialog.value = true;
};

const addManualItem = () => {
  if (!manualForm.title.trim()) {
    window.$message?.warning("请输入歌名");
    return;
  }
  music.addManualItem({
    ...manualForm,
    playlistId: "manual",
    playlistName: "手动收藏",
    note: manualForm.summary,
  });
  showManualDialog.value = false;
  window.$message?.success("已加入音乐库");
};

const clearFilters = () => {
  music.setFilter({ keyword: "", tag: "all", playlistId: "all", mood: "all" });
};

onMounted(() => {
  document.body.classList.add("music-library-page");
  music.ensureDefaultSongs();
});

onBeforeUnmount(() => {
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
