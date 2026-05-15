import {
  DEFAULT_NETEASE_PLAYLIST_ID,
  DEFAULT_NETEASE_PLAYLIST_NAME,
  METING_PLAYLIST_API,
  createManualMusicItem,
  extractNeteasePlaylistId,
  normalizeMetingSong,
  seedMusicItems,
  seedMusicPlaylists,
  splitPlaylistSources,
  type MetingSong,
  type MusicFilter,
  type MusicItem,
  type MusicPlaylist,
} from "@/views/Music/musicModel";

interface MusicState {
  items: MusicItem[];
  playlists: MusicPlaylist[];
  activeItemId: string;
  importSource: string;
  importLoading: boolean;
  importError: string;
  filter: MusicFilter;
  sortType: string;
}

interface ImportPlaylistOptions {
  silent?: boolean;
  keepFilter?: boolean;
}

const initialFilter = (): MusicFilter => ({
  keyword: "",
  tag: "all",
  playlistId: "all",
  mood: "all",
});

const useMusicStore = defineStore("useMusicStore", {
  state: (): MusicState => ({
    items: seedMusicItems,
    playlists: seedMusicPlaylists,
    activeItemId: seedMusicItems[0]?.id || "",
    importSource: "",
    importLoading: false,
    importError: "",
    filter: initialFilter(),
    sortType: "curated",
  }),
  actions: {
    selectItem(id: string) {
      this.activeItemId = id;
    },
    setFilter(partial: Partial<MusicFilter>) {
      this.filter = {
        ...this.filter,
        ...partial,
      };
    },
    setSortType(sortType: string) {
      this.sortType = sortType;
    },
    updateItem(id: string, patch: Partial<MusicItem>) {
      this.items = this.items.map((item) =>
        item.id === id
          ? {
              ...item,
              ...patch,
              updatedAt: new Date().toISOString(),
            }
          : item,
      );
    },
    addManualItem(item: Partial<MusicItem>) {
      const musicItem = createManualMusicItem(item);
      this.items = [musicItem, ...this.items];
      if (!this.playlists.some((playlist) => playlist.id === musicItem.playlistId)) {
        this.playlists = [
          {
            id: musicItem.playlistId,
            name: musicItem.playlistName,
            importedAt: musicItem.importedAt,
            cover: musicItem.cover,
          },
          ...this.playlists,
        ];
      }
      this.activeItemId = musicItem.id;
    },
    async ensureDefaultSongs() {
      const hasDefaultSongs = this.items.some((item) => item.playlistId === DEFAULT_NETEASE_PLAYLIST_ID);
      const hasOldDemoSongs = this.items.some((item) => item.id.startsWith("seed-"));

      if (hasDefaultSongs || this.importLoading) {
        return;
      }
      if (hasOldDemoSongs) {
        this.items = this.items.filter((item) => !item.id.startsWith("seed-"));
        this.playlists = this.playlists.filter((playlist) => !playlist.id.startsWith("seed-"));
        this.activeItemId = "";
        this.filter = initialFilter();
      }
      await this.importNeteasePlaylist(DEFAULT_NETEASE_PLAYLIST_ID, { silent: true, keepFilter: true });
    },
    async importNeteasePlaylist(source: string, options: ImportPlaylistOptions = {}) {
      const playlistId = extractNeteasePlaylistId(source);
      this.importSource = source;
      this.importError = "";

      if (!playlistId) {
        this.importError = "请输入公开网易云歌单链接或歌单 ID，系统会导入其中的歌曲";
        return;
      }

      this.importLoading = true;
      try {
        const response = await fetch(`${METING_PLAYLIST_API}${playlistId}`);
        if (!response.ok) {
          throw new Error(`request failed: ${response.status}`);
        }
        const result = (await response.json()) as MetingSong[];
        if (!Array.isArray(result) || !result.length) {
          throw new Error("empty playlist");
        }

        const playlistName = playlistId === DEFAULT_NETEASE_PLAYLIST_ID ? DEFAULT_NETEASE_PLAYLIST_NAME : `网易云歌曲 ${playlistId}`;
        const importedItems = result.map((song, index) => normalizeMetingSong(song, playlistId, index, playlistName));
        const existingKeys = new Set(this.items.map(getDuplicateKey));
        const nextItems = importedItems.filter((item) => !existingKeys.has(getDuplicateKey(item)));

        this.items = [...nextItems, ...this.items];
        this.playlists = upsertPlaylist(this.playlists, {
          id: playlistId,
          name: playlistName,
          sourceUrl: `https://music.163.com/#/playlist?id=${playlistId}`,
          importedAt: new Date().toISOString(),
          cover: nextItems[0]?.cover || importedItems[0]?.cover,
        });
        this.activeItemId = nextItems[0]?.id || importedItems[0]?.id || this.activeItemId;
        if (!options.keepFilter) {
          this.setFilter({ playlistId });
        }
        this.importSource = "";
      } catch {
        if (!options.silent) {
          this.importError = "歌曲读取失败，可以稍后重试，或先手动添加收藏。";
        }
      } finally {
        this.importLoading = false;
      }
    },
    async importNeteasePlaylists(source: string) {
      const sources = splitPlaylistSources(source);
      if (!sources.length) {
        this.importError = "请输入公开网易云歌单链接或歌单 ID，系统会导入其中的歌曲";
        return;
      }
      this.importError = "";
      for (const item of sources) {
        await this.importNeteasePlaylist(item, { keepFilter: sources.length > 1 });
        if (this.importError) {
          return;
        }
      }
      if (sources.length > 1) {
        this.setFilter({ playlistId: "all" });
      }
    },
    resetLibrary() {
      this.items = seedMusicItems;
      this.playlists = seedMusicPlaylists;
      this.activeItemId = seedMusicItems[0]?.id || "";
      this.filter = initialFilter();
      this.sortType = "curated";
      this.importSource = "";
      this.importError = "";
    },
  },
  persist: {
    key: "music-library",
    storage: localStorage,
  },
});

const getDuplicateKey = (item: MusicItem) => {
  return `${item.playlistId}-${item.title.trim().toLowerCase()}-${item.artist.trim().toLowerCase()}`;
};

const upsertPlaylist = (playlists: MusicPlaylist[], playlist: MusicPlaylist) => {
  const exists = playlists.some((item) => item.id === playlist.id);
  if (!exists) {
    return [playlist, ...playlists];
  }
  return playlists.map((item) => (item.id === playlist.id ? { ...item, ...playlist } : item));
};

export default useMusicStore;

