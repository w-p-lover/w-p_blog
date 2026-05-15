# Music Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a visually polished personal music library that imports public NetEase playlists, stores editable collection metadata locally, and displays songs as a gallery-style archive.

**Architecture:** Add a dedicated `src/views/Music` feature folder with a typed model, a persisted Pinia store, focused page components, and isolated SCSS. Route and menu changes wire the feature into the existing app without touching global styles.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, Pinia persisted state, Element Plus, existing SVG icon system, existing `meting-js` playback support, direct public Meting API fetch for NetEase playlist import.

---

## File Structure

- Create `src/views/Music/musicModel.ts`: pure types and helpers for playlist ID parsing, import normalization, filtering, stats, and seed data.
- Create `src/views/Music/musicModel.test.mjs`: node/esbuild tests for all pure helper behavior.
- Create `src/store/modules/music.ts`: Pinia store persisted in `localStorage`, with import, edit, filter, select, and reset actions.
- Modify `src/store/index.ts`: expose `music` store through the existing `useStore()` pattern.
- Create `src/views/Music/index.vue`: music library gallery, hero, filters, import dialog, quick edit panel, selected song detail panel.
- Create `src/views/Music/css/music.scss`: all page-specific visual styling; no global styles touched.
- Modify `src/router/index.ts`: add `/music` route.
- Modify `src/router/menu.ts`: add `音乐库` menu entry under `动态` and include `/music` in active matching.

## Task 1: Model And Tests

**Files:**
- Create: `src/views/Music/musicModel.ts`
- Create: `src/views/Music/musicModel.test.mjs`

- [ ] **Step 1: Create failing model tests**

Add tests covering:

```js
assert.equal(extractNeteasePlaylistId("https://music.163.com/#/playlist?id=7573319758"), "7573319758");
assert.equal(extractNeteasePlaylistId("7573319758"), "7573319758");
assert.equal(extractNeteasePlaylistId("bad"), "");
assert.deepEqual(splitMusicTags("夜晚，民谣, 雨天；收藏"), ["夜晚", "民谣", "雨天", "收藏"]);
assert.equal(joinMusicTags([" 夜晚 ", "", "民谣"]), "夜晚,民谣");
assert.equal(normalizeMetingSong({ title: "九三", author: "李莎旻子", pic: "cover", url: "song" }, "7573319758", 0).title, "九三");
assert.deepEqual(filterMusicItems(seedMusicItems, { keyword: "雨", tag: "雨天", playlistId: "all", mood: "all" }).map((item) => item.id), ["seed-rain"]);
assert.equal(getMusicStats(seedMusicItems).total, seedMusicItems.length);
```

Run:

```bash
node src/views/Music/musicModel.test.mjs
```

Expected: FAIL because `musicModel.ts` does not exist yet.

- [ ] **Step 2: Implement model helpers**

Implement exported types and helpers:

```ts
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
  sourceType: "netease" | "manual";
  sourceUrl?: string;
  tags: string;
  mood: string;
  rating: number;
  note: string;
  summary: string;
  favoriteLevel: "spark" | "loop" | "classic";
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
```

Helpers must include `extractNeteasePlaylistId`, `splitMusicTags`, `joinMusicTags`, `normalizeMetingSong`, `filterMusicItems`, `sortMusicItems`, `getMusicStats`, `getFeaturedMusic`, and `seedMusicItems`.

- [ ] **Step 3: Verify tests**

Run:

```bash
node src/views/Music/musicModel.test.mjs
```

Expected: PASS.

## Task 2: Persisted Music Store

**Files:**
- Create: `src/store/modules/music.ts`
- Modify: `src/store/index.ts`

- [ ] **Step 1: Add store module**

Create a Pinia store named `useMusicStore` with:

```ts
state: () => ({
  items: seedMusicItems,
  playlists: seedMusicPlaylists,
  activeItemId: seedMusicItems[0]?.id || "",
  importSource: "",
  importLoading: false,
  importError: "",
  filter: { keyword: "", tag: "all", playlistId: "all", mood: "all" },
  sortType: "curated",
})
```

Actions:

- `selectItem(id: string)`
- `setFilter(partial: Partial<MusicFilter>)`
- `setSortType(sortType: string)`
- `updateItem(id: string, patch: Partial<MusicItem>)`
- `addManualItem(item: Partial<MusicItem>)`
- `importNeteasePlaylist(source: string)`
- `resetLibrary()`

`importNeteasePlaylist` should:

1. Parse a public NetEase playlist ID with `extractNeteasePlaylistId`.
2. Fetch `https://api.i-meto.com/meting/api?server=netease&type=playlist&id=${playlistId}`.
3. Normalize returned songs with `normalizeMetingSong`.
4. Merge by `title + artist + playlistId` to avoid duplicates.
5. Set a readable error when the ID is invalid or the request fails.

- [ ] **Step 2: Register store**

Update `src/store/index.ts`:

```ts
import useMusicStore from "./modules/music";

const useStore = () => ({
  app: useAppStore(),
  blog: useBlogStore(),
  user: useUserStore(),
  music: useMusicStore(),
});
```

- [ ] **Step 3: Type-check through build**

Run:

```bash
npm run build
```

Expected: store imports compile or show the next concrete type error to fix.

## Task 3: Music Library Page

**Files:**
- Create: `src/views/Music/index.vue`
- Create: `src/views/Music/css/music.scss`

- [ ] **Step 1: Build page shell**

The page should include:

- existing `page-header` pattern with `Waves`
- music-specific hero inside `.music-gallery`
- stats strip
- import form
- filter controls
- pinned feature area
- cover wall
- selected song detail panel

- [ ] **Step 2: Bind store data**

Use computed values from the music store:

```ts
const visibleItems = computed(() => sortMusicItems(filterMusicItems(music.items, music.filter), music.sortType));
const stats = computed(() => getMusicStats(music.items));
const featuredItems = computed(() => getFeaturedMusic(music.items, 3));
const selectedItem = computed(() => music.items.find((item) => item.id === music.activeItemId) || visibleItems.value[0]);
```

- [ ] **Step 3: Add isolated visual design**

`music.scss` must stay scoped to `.music-page`, `.music-gallery`, and descendants. Do not modify `src/assets/styles/*.scss`.

The visual style should use:

- cover-first cards
- layered artwork frames
- asymmetrical hero composition
- compact controls
- responsive grid with stable card dimensions
- tasteful hover motion

- [ ] **Step 4: Add editing interactions**

Selected item panel should allow editing:

- note
- summary
- tags
- mood
- rating

Use existing Element Plus inputs and buttons. Save by calling `music.updateItem`.

## Task 4: Route And Menu Integration

**Files:**
- Modify: `src/router/index.ts`
- Modify: `src/router/menu.ts`

- [ ] **Step 1: Add route**

Add:

```ts
{
  path: "/music",
  component: () => import("@/views/Music/index.vue"),
  meta: {
    title: "音乐库",
  },
}
```

- [ ] **Step 2: Add menu item**

Add to `activityMenuItems`:

```ts
{
  name: "音乐库",
  icon: "laba",
  path: "/music",
}
```

Update dynamic group match paths:

```ts
matchPaths: ["/talk", "/album", "/message", "/friend", "/music"]
```

## Task 5: Verification And Merge Prep

**Files:**
- Review all touched files.

- [ ] **Step 1: Run model test**

```bash
node src/views/Music/musicModel.test.mjs
```

Expected: PASS.

- [ ] **Step 2: Run production build**

```bash
npm run build
```

Expected: PASS.

- [ ] **Step 3: Inspect git diff**

```bash
git diff --stat
git diff -- src/views/Music src/store src/router
```

Expected: only music feature, route, menu, and plan files changed.

- [ ] **Step 4: Commit feature branch**

```bash
git add shoka-blog/docs/superpowers/plans/2026-05-15-music-library.md shoka-blog/src/views/Music shoka-blog/src/store shoka-blog/src/router
git commit -m "feat: add personal music library"
```

