import assert from "node:assert/strict";
import { build } from "esbuild";
import { tmpdir } from "node:os";
import { pathToFileURL } from "node:url";
import { mkdirSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";

const outdir = join(tmpdir(), "life-route-model-test");
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await build({
  entryPoints: ["src/views/Life/routeModel.ts"],
  bundle: true,
  platform: "node",
  format: "esm",
  outfile: resolve(outdir, "routeModel.mjs"),
});

const {
  enrichRouteNodes,
  getRouteStats,
  getFilteredRouteElements,
  getPlaybackSequence,
} = await import(pathToFileURL(resolve(outdir, "routeModel.mjs")).href);

const rawElements = [
  {
    id: "2",
    elemType: "node",
    label: "Vue3 实战",
    positionX: 240,
    positionY: 120,
    elemClass: "stage-work",
    description: "做一个完整项目",
    keyPoints: ["Vue3", "Pinia", "部署"],
  },
  {
    id: "1",
    elemType: "node",
    label: "基础补课",
    positionX: 40,
    positionY: 120,
    elemClass: "stage-birth",
    description: "补齐基础",
    keyPoints: ["HTML", "CSS"],
  },
  {
    id: "e1-2",
    elemType: "edge",
    sourceId: "1",
    targetId: "2",
    animated: true,
    color: "#FAAD14",
    elemClass: "edge-work",
  },
];

const enriched = enrichRouteNodes(rawElements);

assert.equal(enriched[0].data.status, "doing");
assert.deepEqual(enriched[0].data.tags, ["Vue3", "Pinia", "部署"]);
assert.equal(enriched[1].data.status, "done");

assert.deepEqual(getRouteStats(enriched), {
  total: 2,
  done: 1,
  doing: 1,
  planned: 0,
  linked: 1,
});

assert.deepEqual(getPlaybackSequence(enriched).map((node) => node.id), ["1", "2"]);

const filtered = getFilteredRouteElements(enriched, "stage-birth");
assert.deepEqual(filtered.map((item) => item.id), ["1"]);

const all = getFilteredRouteElements(enriched, "all");
assert.equal(all.length, 3);
