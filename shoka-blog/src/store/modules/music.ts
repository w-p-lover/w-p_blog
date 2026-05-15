import {
  DEFAULT_NETEASE_PLAYLIST_ID,
  DEFAULT_NETEASE_PLAYLIST_NAME,
  createManualMusicItem,
  extractNeteasePlaylistId,
  normalizeMusicItemPayload,
  normalizeMusicLibraryPayload,
  seedMusicItems,
  seedMusicPlaylists,
  splitPlaylistSources,
  type MusicFilter,
  type MusicItem,
  type MusicPlaylist,
} from "@/views/Music/musicModel";
import {
  addMusicItem,
  getMusicLibrary,
  importNeteasePlaylist as importNeteasePlaylistApi,
  resetMusicLibrary,
  updateMusicItem,
  type MusicItemForm,
} from "@/api/music";
import { getToken } from "@/utils/token";

interface MusicState {
  items: MusicItem[];
  playlists: MusicPlaylist[];
  activeItemId: string;
  libraryLoading: boolean;
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

type PermissionChecker = (permission: string) => boolean;

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
    libraryLoading: false,
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
    replaceLibrary(payload: { items?: MusicItem[]; playlists?: MusicPlaylist[] }) {
      this.items = payload.items || [];
      this.playlists = payload.playlists || [];
      if (!this.items.some((item) => item.id === this.activeItemId)) {
        this.activeItemId = this.items[0]?.id || "";
      }
    },
    async fetchLibrary() {
      this.libraryLoading = true;
      try {
        const response = await getMusicLibrary({
          keyword: this.filter.keyword || undefined,
          playlistId: this.filter.playlistId !== "all" ? this.filter.playlistId : undefined,
          tag: this.filter.tag !== "all" ? this.filter.tag : undefined,
          mood: this.filter.mood !== "all" ? this.filter.mood : undefined,
          sortType: this.sortType,
        });
        if (response.data.flag) {
          this.replaceLibrary(normalizeMusicLibraryPayload(response.data.data));
        }
      } finally {
        this.libraryLoading = false;
      }
    },
    async updateItem(id: string, patch: Partial<MusicItem>) {
      const oldItem = this.items.find((item) => item.id === id);
      if (!oldItem) {
        return;
      }
      const oldItems = [...this.items];
      this.items = this.items.map((item) =>
        item.id === id
          ? {
              ...item,
              ...patch,
              updatedAt: new Date().toISOString(),
            }
          : item,
      );
      const response = await updateMusicItem(id, {
        ...oldItem,
        ...patch,
      } as MusicItemForm);
      if (!response.data.flag) {
        this.items = oldItems;
        return;
      }
      if (response.data.flag && response.data.data) {
        const savedItem = normalizeMusicItemPayload(response.data.data);
        this.items = this.items.map((item) => (item.id === id ? savedItem : item));
      }
    },
    async updateItemWithPermission(id: string, patch: Partial<MusicItem>, hasPermission: PermissionChecker) {
      if (!this.canMutate("music:item:update", hasPermission)) {
        return false;
      }
      await this.updateItem(id, patch);
      return true;
    },
    canMutate(permission: string, hasPermission: PermissionChecker) {
      if (!getToken()) {
        this.importError = "请先登录后再维护音乐库";
        window.$message?.warning(this.importError);
        return false;
      }
      if (!hasPermission(permission)) {
        this.importError = "当前账号没有音乐库维护权限";
        window.$message?.warning(this.importError);
        return false;
      }
      return true;
    },
    async addManualItem(item: Partial<MusicItem>, hasPermission: PermissionChecker) {
      if (!this.canMutate("music:item:add", hasPermission)) {
        return;
      }
      const draftItem = createManualMusicItem(item);
      const response = await addMusicItem(draftItem as MusicItemForm);
      if (!response.data.flag || !response.data.data) {
        return;
      }
      const musicItem = normalizeMusicItemPayload(response.data.data);
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
    async importNeteasePlaylist(source: string, options: ImportPlaylistOptions = {}, hasPermission: PermissionChecker = () => false) {
      if (!this.canMutate("music:library:import", hasPermission)) {
        return;
      }
      const playlistId = extractNeteasePlaylistId(source);
      this.importSource = source;
      this.importError = "";

      if (!playlistId) {
        this.importError = "请输入公开网易云歌单链接或歌单 ID，系统会导入其中的歌曲";
        return;
      }

      this.importLoading = true;
      try {
        const playlistName = playlistId === DEFAULT_NETEASE_PLAYLIST_ID ? DEFAULT_NETEASE_PLAYLIST_NAME : `网易云歌曲 ${playlistId}`;
        const response = await importNeteasePlaylistApi({
          source,
          playlistName,
        });
        if (!response.data.flag) {
          throw new Error(response.data.msg);
        }
        const library = normalizeMusicLibraryPayload(response.data.data);
        const importedIds = new Set(library.items.map((item) => item.id));
        const remainingItems = this.items.filter((item) => !importedIds.has(item.id));
        this.items = [...library.items, ...remainingItems];
        this.playlists = upsertPlaylists(this.playlists, library.playlists);
        this.activeItemId = library.items[0]?.id || this.activeItemId;
        if (!options.keepFilter) {
          this.setFilter({ playlistId });
        }
        this.importSource = "";
      } catch (error) {
        if (!options.silent) {
          this.importError = error instanceof Error && error.message ? error.message : "歌曲读取失败，可以稍后重试，或先手动添加收藏。";
        }
      } finally {
        this.importLoading = false;
      }
    },
    async importNeteasePlaylists(source: string, hasPermission: PermissionChecker) {
      const sources = splitPlaylistSources(source);
      if (!sources.length) {
        this.importError = "请输入公开网易云歌单链接或歌单 ID，系统会导入其中的歌曲";
        return;
      }
      this.importError = "";
      for (const item of sources) {
        await this.importNeteasePlaylist(item, { keepFilter: sources.length > 1 }, hasPermission);
        if (this.importError) {
          return;
        }
      }
      if (sources.length > 1) {
        this.setFilter({ playlistId: "all" });
      }
    },
    async resetLibrary(hasPermission: PermissionChecker) {
      if (!this.canMutate("music:library:reset", hasPermission)) {
        return;
      }
      const response = await resetMusicLibrary();
      if (response.data.flag) {
        this.items = seedMusicItems;
        this.playlists = seedMusicPlaylists;
        this.activeItemId = seedMusicItems[0]?.id || "";
        this.filter = initialFilter();
        this.sortType = "curated";
        this.importSource = "";
        this.importError = "";
      }
    },
  },
  persist: {
    key: "music-library",
    paths: ["activeItemId", "filter", "sortType"],
    storage: localStorage,
  },
});

const upsertPlaylists = (playlists: MusicPlaylist[], incomingPlaylists: MusicPlaylist[]) => {
  const incomingIds = new Set(incomingPlaylists.map((playlist) => playlist.id));
  return [...incomingPlaylists, ...playlists.filter((playlist) => !incomingIds.has(playlist.id))];
};

export default useMusicStore;

