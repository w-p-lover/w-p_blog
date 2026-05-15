import assert from "node:assert/strict";
import { build } from "esbuild";
import { tmpdir } from "node:os";
import { pathToFileURL } from "node:url";
import { mkdirSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";

const outdir = join(tmpdir(), "music-model-test");
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await build({
  entryPoints: ["src/views/Music/musicModel.ts"],
  bundle: true,
  platform: "node",
  format: "esm",
  outfile: resolve(outdir, "musicModel.mjs"),
});

const {
  extractNeteasePlaylistId,
  filterMusicItems,
  getFeaturedMusic,
  getMusicStats,
  joinMusicTags,
  normalizeMetingSong,
  parseLyricText,
  sortMusicItems,
  splitMusicTags,
  splitPlaylistSources,
} = await import(pathToFileURL(resolve(outdir, "musicModel.mjs")).href);

assert.equal(extractNeteasePlaylistId("https://music.163.com/#/playlist?id=7573319758"), "7573319758");
assert.equal(extractNeteasePlaylistId("https://music.163.com/playlist?id=7573319758&userid=1"), "7573319758");
assert.equal(extractNeteasePlaylistId("7573319758"), "7573319758");
assert.equal(extractNeteasePlaylistId("bad"), "");

assert.deepEqual(splitMusicTags("夜晚，民谣, 雨天；收藏"), ["夜晚", "民谣", "雨天", "收藏"]);
assert.equal(joinMusicTags([" 夜晚 ", "", "民谣"]), "夜晚,民谣");
assert.deepEqual(splitPlaylistSources("7573319758\nhttps://music.163.com/#/playlist?id=12345678，87654321"), [
  "7573319758",
  "https://music.163.com/#/playlist?id=12345678",
  "87654321",
]);
assert.deepEqual(parseLyricText("[00:01.20]第一句\n[00:03]第二句").map((line) => line.text), ["第一句", "第二句"]);
assert.equal(parseLyricText("[00:01.20]第一句")[0].time, 1.2);

const normalized = normalizeMetingSong(
  { title: "九三", author: "李莎旻子", pic: "cover", url: "song", lrc: "lyric" },
  "7573319758",
  0,
);

assert.equal(normalized.title, "九三");
assert.equal(normalized.artist, "李莎旻子");
assert.equal(normalized.cover, "cover");
assert.equal(normalized.url, "song");
assert.equal(normalized.lyricUrl, "lyric");
assert.equal(normalized.playlistId, "7573319758");
assert.equal(normalized.playlistName, "我的网易云歌曲");
assert.equal(normalized.sourceType, "netease");

const fixtureItems = [
  {
    ...normalized,
    id: "fixture-rain",
    title: "雨夜",
    artist: "某位歌手",
    playlistId: "playlist-night",
    playlistName: "夜晚收藏",
    tags: "雨天,夜晚",
    mood: "夜行",
    rating: 5,
    isPinned: true,
    importedAt: "2026-01-03T00:00:00.000Z",
    updatedAt: "2026-01-03T00:00:00.000Z",
  },
  {
    ...normalized,
    id: "fixture-sea",
    title: "海边",
    artist: "另一位歌手",
    playlistId: "playlist-day",
    playlistName: "午后收藏",
    tags: "晴天",
    mood: "松弛",
    rating: 4,
    isPinned: false,
    importedAt: "2026-01-02T00:00:00.000Z",
    updatedAt: "2026-01-02T00:00:00.000Z",
  },
  {
    ...normalized,
    id: "fixture-city",
    title: "城市",
    artist: "第三位歌手",
    playlistId: "playlist-night",
    playlistName: "夜晚收藏",
    tags: "通勤",
    mood: "夜行",
    rating: 4,
    isPinned: false,
    importedAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
];

assert.deepEqual(
  filterMusicItems(fixtureItems, { keyword: "雨", tag: "雨天", playlistId: "all", mood: "all" }).map((item) => item.id),
  ["fixture-rain"],
);
assert.deepEqual(
  filterMusicItems(fixtureItems, { keyword: "", tag: "all", playlistId: "playlist-night", mood: "夜行" }).map((item) => item.playlistId),
  ["playlist-night", "playlist-night"],
);

assert.equal(getMusicStats(fixtureItems).total, fixtureItems.length);
assert.equal(getMusicStats(fixtureItems).pinned, fixtureItems.filter((item) => item.isPinned).length);
assert.equal(getMusicStats(fixtureItems).tagCount, 4);

assert.equal(getFeaturedMusic(fixtureItems, 2).length, 2);
assert.equal(getFeaturedMusic(fixtureItems, 2)[0].isPinned, true);
assert.deepEqual(sortMusicItems(fixtureItems, "rating").map((item) => item.rating), [5, 4, 4]);
