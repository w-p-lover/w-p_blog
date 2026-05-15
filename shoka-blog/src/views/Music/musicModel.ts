export type MusicSourceType = "netease" | "manual";
export type MusicFavoriteLevel = "spark" | "loop" | "classic";

export interface MetingSong {
  title?: string;
  author?: string;
  url?: string;
  pic?: string;
  lrc?: string;
}

export interface MusicItem {
  id: string;
  title: string;
  artist: string;
  album?: string;
  cover: string;
  url?: string;
  lyricUrl?: string;
  playlistId: string;
  playlistName: string;
  sourceType: MusicSourceType;
  sourceUrl?: string;
  tags: string;
  mood: string;
  rating: number;
  note: string;
  summary: string;
  favoriteLevel: MusicFavoriteLevel;
  isPinned?: boolean;
  importedAt: string;
  updatedAt: string;
}

export interface MusicPlaylist {
  id: string;
  name: string;
  sourceUrl?: string;
  importedAt: string;
  cover?: string;
}

export interface MusicFilter {
  keyword: string;
  tag: string;
  playlistId: string;
  mood: string;
}

export interface MusicStats {
  total: number;
  playlists: number;
  pinned: number;
  tagCount: number;
  avgRating: number;
}

export const METING_PLAYLIST_API = "https://api.i-meto.com/meting/api?server=netease&type=playlist&id=";
export const DEFAULT_NETEASE_PLAYLIST_ID = "7573319758";
export const DEFAULT_NETEASE_PLAYLIST_NAME = "我的网易云歌曲";

const DEFAULT_COVER =
  "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=900&q=80";

export const seedMusicPlaylists: MusicPlaylist[] = [];
export const seedMusicItems: MusicItem[] = [];

export const extractNeteasePlaylistId = (source: string) => {
  const value = source.trim();
  if (/^\d{4,}$/.test(value)) {
    return value;
  }
  const match = value.match(/[?&#]id=(\d{4,})/) || value.match(/playlist\/(\d{4,})/);
  return match?.[1] || "";
};

export const splitMusicTags = (tags?: string) => {
  if (!tags) {
    return [];
  }
  return tags
    .split(/[,，、;；]/)
    .map((tag) => tag.trim())
    .filter(Boolean);
};

export const splitPlaylistSources = (source: string) => {
  return source
    .split(/[\n,，;；\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
};

export const joinMusicTags = (tags: string[]) => {
  return tags.map((tag) => tag.trim()).filter(Boolean).join(",");
};

export interface LyricLine {
  time: number;
  text: string;
}

export const parseLyricText = (lyric: string): LyricLine[] => {
  return lyric
    .split(/\r?\n/)
    .flatMap((line) => {
      const timeMatches = [...line.matchAll(/\[(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?]/g)];
      const text = line.replace(/\[[^\]]+]/g, "").trim();
      if (!timeMatches.length || !text) {
        return [];
      }
      return timeMatches.map((match) => {
        const minutes = Number(match[1]);
        const seconds = Number(match[2]);
        const fraction = Number((match[3] || "0").padEnd(3, "0"));
        return {
          time: minutes * 60 + seconds + fraction / 1000,
          text,
        };
      });
    })
    .sort((left, right) => left.time - right.time);
};

export const createMusicItemId = (value: string) => {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = value.charCodeAt(index) + ((hash << 5) - hash);
  }
  return Math.abs(hash).toString(36);
};

export const normalizeMetingSong = (
  song: MetingSong,
  playlistId: string,
  index: number,
  playlistName = playlistId === DEFAULT_NETEASE_PLAYLIST_ID ? DEFAULT_NETEASE_PLAYLIST_NAME : `网易云歌曲 ${playlistId}`,
): MusicItem => {
  const title = String(song.title || "未命名歌曲").trim();
  const artist = String(song.author || "未知歌手").trim();
  const id = `netease-${playlistId}-${createMusicItemId(`${title}-${artist}-${index}`)}`;
  const importedAt = new Date().toISOString();

  return {
    id,
    title,
    artist,
    cover: String(song.pic || DEFAULT_COVER).trim(),
    url: String(song.url || "").trim(),
    lyricUrl: String(song.lrc || "").trim(),
    playlistId,
    playlistName,
    sourceType: "netease",
    sourceUrl: `https://music.163.com/#/playlist?id=${playlistId}`,
    tags: "",
    mood: "待整理",
    rating: 3,
    note: "",
    summary: "从网易云导入的歌曲，等待补充收藏理由。",
    favoriteLevel: "spark",
    importedAt,
    updatedAt: importedAt,
  };
};

export const filterMusicItems = (items: MusicItem[], filter: MusicFilter) => {
  const keyword = filter.keyword.trim().toLowerCase();
  return items.filter((item) => {
    if (filter.tag !== "all" && !splitMusicTags(item.tags).includes(filter.tag)) {
      return false;
    }
    if (filter.playlistId !== "all" && item.playlistId !== filter.playlistId) {
      return false;
    }
    if (filter.mood !== "all" && item.mood !== filter.mood) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    const searchable = `${item.title} ${item.artist} ${item.playlistName} ${item.tags} ${item.mood} ${item.note} ${item.summary}`;
    return searchable.toLowerCase().includes(keyword);
  });
};

export const sortMusicItems = (items: MusicItem[], sortType: string) => {
  return [...items].sort((left, right) => {
    if (sortType === "rating") {
      return right.rating - left.rating || getTime(right.updatedAt) - getTime(left.updatedAt);
    }
    if (sortType === "title") {
      return left.title.localeCompare(right.title, "zh-CN");
    }
    if (sortType === "newest") {
      return getTime(right.importedAt) - getTime(left.importedAt);
    }
    return Number(Boolean(right.isPinned)) - Number(Boolean(left.isPinned)) || right.rating - left.rating;
  });
};

export const getMusicStats = (items: MusicItem[]): MusicStats => {
  const tags = new Set<string>();
  const playlists = new Set<string>();
  const totalRating = items.reduce((sum, item) => {
    splitMusicTags(item.tags).forEach((tag) => tags.add(tag));
    playlists.add(item.playlistId);
    return sum + item.rating;
  }, 0);

  return {
    total: items.length,
    playlists: playlists.size,
    pinned: items.filter((item) => item.isPinned).length,
    tagCount: tags.size,
    avgRating: items.length ? Number((totalRating / items.length).toFixed(1)) : 0,
  };
};

export const getFeaturedMusic = (items: MusicItem[], limit = 3) => {
  return sortMusicItems(items, "curated").slice(0, limit);
};

export const getAllMusicTags = (items: MusicItem[]) => {
  return [...new Set(items.flatMap((item) => splitMusicTags(item.tags)))];
};

export const getAllMusicMoods = (items: MusicItem[]) => {
  return [...new Set(items.map((item) => item.mood).filter(Boolean))];
};

export const createManualMusicItem = (item: Partial<MusicItem>): MusicItem => {
  const createdAt = new Date().toISOString();
  const title = item.title?.trim() || "未命名歌曲";
  const artist = item.artist?.trim() || "未知歌手";

  return {
    id: item.id || `manual-${createMusicItemId(`${title}-${artist}-${createdAt}`)}`,
    title,
    artist,
    album: item.album,
    cover: item.cover || DEFAULT_COVER,
    url: item.url,
    lyricUrl: item.lyricUrl,
    playlistId: item.playlistId || "manual",
    playlistName: item.playlistName || "手动收藏",
    sourceType: item.sourceType || "manual",
    sourceUrl: item.sourceUrl,
    tags: item.tags || "",
    mood: item.mood || "待整理",
    rating: clampRating(item.rating ?? 3),
    note: item.note || "",
    summary: item.summary || "手动添加的收藏条目。",
    favoriteLevel: item.favoriteLevel || "spark",
    isPinned: item.isPinned,
    importedAt: item.importedAt || createdAt,
    updatedAt: item.updatedAt || createdAt,
  };
};

export const clampRating = (value: number) => {
  return Math.min(5, Math.max(1, Number.isFinite(value) ? value : 3));
};

const getTime = (value?: string) => {
  const time = value ? new Date(value).getTime() : 0;
  return Number.isNaN(time) ? 0 : time;
};

