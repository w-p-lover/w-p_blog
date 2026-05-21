import assert from "node:assert/strict";
import { build } from "esbuild";
import { mkdirSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const outdir = join(tmpdir(), "ai-chat-model-test");
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await build({
  entryPoints: ["src/views/AiChat/aiChatModel.ts"],
  bundle: true,
  platform: "node",
  format: "esm",
  outfile: resolve(outdir, "aiChatModel.mjs"),
});

const { aiChatModes, getAiChatMode } = await import(pathToFileURL(resolve(outdir, "aiChatModel.mjs")).href);

assert.deepEqual(
  aiChatModes.map((item) => item.key),
  ["chat", "qa"],
);

const chatMode = getAiChatMode("chat");
assert.equal(chatMode.label, "自由对话");
assert.equal(chatMode.provider, "write-assist");
assert.match(chatMode.description, /想法|聊天/);
assert.match(chatMode.placeholder, /聊聊|想法/);
assert.ok(chatMode.quickPrompts.some((item) => item.includes("聊聊")));

const qaMode = getAiChatMode("qa");
assert.equal(qaMode.label, "专业问答");
assert.equal(qaMode.provider, "knowledge");
assert.match(qaMode.placeholder, /SpringBoot|Redis/);
assert.equal(getAiChatMode("unknown").key, "chat");
