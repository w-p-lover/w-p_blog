import assert from "node:assert/strict";
import {
  getArticleWritingStats,
  stripArticleContent,
} from "../src/utils/article-writing.js";

assert.equal(stripArticleContent("<h1>标题</h1><p>Hello world</p>```js\nconst a = 1\n```"), "标题 Hello world const a = 1");

assert.deepEqual(getArticleWritingStats("你好，世界。Hello world"), {
  wordCount: 6,
  readMinutes: 1,
});

assert.deepEqual(getArticleWritingStats(""), {
  wordCount: 0,
  readMinutes: 0,
});
