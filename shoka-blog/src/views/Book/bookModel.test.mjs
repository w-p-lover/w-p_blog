import assert from "node:assert/strict";
import { build } from "esbuild";
import { tmpdir } from "node:os";
import { pathToFileURL } from "node:url";
import { mkdirSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";

const outdir = join(tmpdir(), "book-model-test");
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await build({
  entryPoints: ["src/views/Book/bookModel.ts"],
  bundle: true,
  platform: "node",
  format: "esm",
  outfile: resolve(outdir, "bookModel.mjs"),
});

const {
  filterBooks,
  getAvailableResourceCount,
  getBookStats,
  getPrimaryResource,
  getStatusLabel,
  joinTags,
  normalizeBook,
  parseResources,
  sortBooks,
  splitTags,
} = await import(pathToFileURL(resolve(outdir, "bookModel.mjs")).href);

const rawBooks = [
  {
    id: 1,
    title: "三体",
    author: "刘慈欣",
    status: "read",
    tags: "科幻, 长篇",
    addTime: "2026-04-01T10:00:00",
    resource: '[{"name":"PDF","url":"https://example.com/3.pdf","type":"pdf"}]',
  },
  {
    id: 2,
    title: "Vue 设计与实现",
    author: "霍春阳",
    status: "reading",
    tags: "技术，前端",
    addTime: "2026-04-03T10:00:00",
    resource: [],
  },
  {
    id: 3,
    title: "无效状态书",
    status: "draft",
    tags: "",
    addTime: "2026-03-20T10:00:00",
    resource: "{bad json",
  },
];

const books = rawBooks.map(normalizeBook);

assert.equal(books[2].status, "wish");
assert.deepEqual(books[0].resource, [{ name: "PDF", url: "https://example.com/3.pdf", type: "pdf" }]);
assert.deepEqual(parseResources({ name: "笔记", url: "https://example.com/note", type: "" }), [
  { name: "笔记", url: "https://example.com/note", type: "other" },
]);
assert.deepEqual(getPrimaryResource([
  { name: "空资源", url: "", type: "pdf" },
  { name: "在线阅读", url: "https://example.com/read", type: "other" },
]), { name: "在线阅读", url: "https://example.com/read", type: "other" });
assert.equal(getPrimaryResource([{ name: "空资源", url: "", type: "pdf" }]), null);
assert.equal(getAvailableResourceCount([
  { name: "PDF", url: "https://example.com/book.pdf", type: "pdf" },
  { name: "缺链接", url: "", type: "other" },
]), 1);

assert.deepEqual(splitTags("技术，前端, Vue；工程化"), ["技术", "前端", "Vue", "工程化"]);
assert.equal(joinTags([" 技术 ", "", "前端"]), "技术,前端");

assert.deepEqual(getBookStats(books), {
  total: 3,
  wish: 1,
  reading: 1,
  read: 1,
  resources: 1,
});

assert.deepEqual(filterBooks(books, "前端", "all").map((book) => book.id), [2]);
assert.deepEqual(filterBooks(books, "刘慈欣", "read").map((book) => book.id), [1]);
assert.deepEqual(sortBooks(books, "nameAsc").map((book) => book.title), [
  "Vue 设计与实现",
  "三体",
  "无效状态书",
]);
assert.deepEqual(sortBooks(books, "newest").map((book) => book.id), [2, 1, 3]);
assert.equal(getStatusLabel("reading"), "在读");
assert.equal(getStatusLabel("unknown"), "想读");
