import assert from "node:assert/strict";
import { build } from "esbuild";
import { tmpdir } from "node:os";
import { pathToFileURL } from "node:url";
import { mkdirSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";

const outdir = join(tmpdir(), "hitokoto-model-test");
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await build({
  entryPoints: ["src/views/Home/Swiper/hitokotoModel.ts"],
  bundle: true,
  platform: "node",
  format: "esm",
  outfile: resolve(outdir, "hitokotoModel.mjs"),
});

const {
  DEFAULT_HITOKOTO_TYPE,
  HITOKOTO_TYPE_CHANGE_EVENT,
  HITOKOTO_TYPE_STORAGE_KEY,
  HITOKOTO_TYPES,
  buildHitokotoUrl,
  normalizeHitokotoType,
} = await import(pathToFileURL(resolve(outdir, "hitokotoModel.mjs")).href);

assert.equal(DEFAULT_HITOKOTO_TYPE, "d");
assert.equal(HITOKOTO_TYPE_STORAGE_KEY, "home-hitokoto-type");
assert.equal(HITOKOTO_TYPE_CHANGE_EVENT, "home-hitokoto-type-change");
assert.deepEqual(
  HITOKOTO_TYPES.map((item) => `${item.value}:${item.label}`),
  [
    "a:动画",
    "b:漫画",
    "c:游戏",
    "d:文学",
    "e:原创",
    "f:来自网络",
    "g:其他",
    "h:影视",
    "i:诗词",
    "j:网易云",
    "k:哲学",
    "l:抖机灵",
  ]
);
assert.equal(normalizeHitokotoType("a"), "a");
assert.equal(normalizeHitokotoType("l"), "l");
assert.equal(normalizeHitokotoType("unknown"), "d");
assert.equal(normalizeHitokotoType(null), "d");
assert.equal(buildHitokotoUrl("i"), "https://international.v1.hitokoto.cn/?c=i");
assert.equal(buildHitokotoUrl("bad"), "https://international.v1.hitokoto.cn/?c=d");
