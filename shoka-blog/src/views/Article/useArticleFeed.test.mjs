import assert from "node:assert/strict";
import { build } from "esbuild";
import { resolve } from "node:path";

const result = await build({
  stdin: {
    contents: `export { createRenderer, nextTick, h } from "vue";
      export { useArticleFeed } from "./src/views/Article/useArticleFeed.ts";
      export { pending, tagRequests } from "@/api/article";
      export { default as bus } from "@/eventBus";`,
    resolveDir: process.cwd(),
  },
  bundle: true, platform: "node", format: "esm", write: false,
  plugins: [{
    name: "feed-api-fixtures",
    setup(build) {
      build.onResolve({ filter: /^@\/api\/(article|tag)$/ }, () => ({ path: "api", namespace: "fixture" }));
      build.onResolve({ filter: /^@\/eventBus$/ }, () => ({ path: resolve("src/eventBus.ts"), namespace: "file", external: false }));
      build.onLoad({ filter: /.*/, namespace: "fixture" }, () => ({ contents: `
        export const pending = [];
        export const tagRequests = [];
        export function getArticleList(params) {
          return new Promise((resolve, reject) => pending.push({params, resolve, reject}));
        }
        export function getTagList() {
          return new Promise((resolve, reject) => tagRequests.push({resolve, reject}));
        }`, loader: "js" }));
    },
  }],
});
const { createRenderer, nextTick, h, useArticleFeed, pending, tagRequests, bus } =
    await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString("base64")}`);
const renderer = createRenderer({
  createComment: () => ({}), insert() {}, remove() {}, parentNode() {}, nextSibling() {},
});
let feed;
const app = renderer.createApp({ setup() { feed = useArticleFeed(); return () => null; } });
app.mount({});
const settle = async () => { await Promise.resolve(); await Promise.resolve(); await nextTick(); };
const success = (id, records = [{ id }], count = 12) => ({ data: { flag: true, data: { recordList: records, count } } });
assert.equal(pending.length, 1);
assert.equal(feed.dateRange.value, null);
assert.equal(pending[0].params.start, null);
assert.equal(feed.loading.value, true);
pending[0].resolve(success(1));
tagRequests[0].reject(new Error("标签不可用"));
await settle();
assert.equal(feed.tagsFailed.value, true);
assert.equal(feed.loading.value, false);
feed.queryParams.current = 3;
await nextTick();
assert.equal(pending.length, 2);
feed.selectedTag.value = 7;
feed.filterByTag();
await nextTick();
assert.equal(pending.length, 3, "切换标签只产生一个请求");
assert.equal(pending[2].params.current, 1);
assert.equal(pending[2].params.tagId, 7);
pending[2].resolve(success(3));
await settle();
pending[1].resolve(success(2));
await settle();
assert.equal(feed.articleList.value[0].id, 3, "旧响应不得覆盖新筛选");
feed.queryParams.current = 2;
await nextTick();
feed.changeSort("views");
await nextTick();
assert.equal(pending.length, 5);
assert.equal(pending[4].params.current, 1);
assert.equal(pending[4].params.sort, "views");
feed.dateRange.value = [new Date(2026, 9, 1), new Date(2026, 9, 16)];
feed.filterByDate();
await nextTick();
assert.equal(pending.length, 6);
assert.equal(pending[5].params.start, "2026-10-01 00:00:00");
assert.equal(pending[5].params.end, "2026-10-16 23:59:59");
pending[5].reject(new Error("请求失败"));
await settle();
assert.equal(feed.failed.value, true);
assert.equal(feed.loading.value, false);
void feed.loadArticles();
assert.equal(pending[6].params.end, "2026-10-16 23:59:59");
pending[6].resolve(success(7, [], 0));
await settle();
assert.equal(feed.failed.value, false);
assert.equal(feed.count.value, 0);
feed.clearFilters();
await nextTick();
assert.equal(pending.length, 8);
assert.equal(pending[7].params.tagId, null);
assert.equal(pending[7].params.start, null);
assert.equal(feed.hasFilters.value, false);
const tagsRetry = feed.loadTags();
tagRequests[1].resolve({ data: { flag: true, data: [{ id: 7, tagName: "Java" }] } });
await tagsRetry;
assert.equal(feed.tagsFailed.value, false);
const beforeRefresh = pending.length;
bus.emit("refresh-articles");
assert.equal(pending.length, beforeRefresh + 1);
app.unmount();
bus.emit("refresh-articles");
assert.equal(pending.length, beforeRefresh + 1, "卸载后应移除刷新监听");
pending.at(-1).resolve(success(99));
await settle();
assert.equal(feed.articleList.value.length, 0, "卸载后的响应不再更新状态");
console.log("PASS: 首次加载、筛选单次请求、分页重置、响应逆序、结束日、失败重试、标签恢复、事件清理");
