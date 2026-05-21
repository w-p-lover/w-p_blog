import assert from "node:assert/strict";
import { build } from "esbuild";
import { mkdirSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const outdir = join(tmpdir(), "ai-writer-assistant-model-test");
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await build({
  entryPoints: ["src/components/AiWriterFloat/assistantModel.ts"],
  bundle: true,
  platform: "node",
  format: "esm",
  outfile: resolve(outdir, "assistantModel.mjs"),
});

const { buildAssistantQuestion, writingActions } = await import(
  pathToFileURL(resolve(outdir, "assistantModel.mjs")).href
);

assert.deepEqual(
  writingActions.map((item) => item.action),
  ["expand", "polish", "summary"],
);

assert.equal(
  buildAssistantQuestion(
    [
      { role: "assistant", content: "先说说你想写什么。" },
      { role: "user", content: "我想写一篇雨夜散步。" },
      { role: "assistant", content: "可以从气味和脚步声切入。" },
    ],
    "那开头怎么写？",
  ),
  [
    "你是博客里的 AI 写作与对话助手。请自然回应用户，必要时给出写作建议。",
    "以下是最近对话：",
    "助手：先说说你想写什么。",
    "用户：我想写一篇雨夜散步。",
    "助手：可以从气味和脚步声切入。",
    "用户：那开头怎么写？",
  ].join("\n"),
);

assert.match(buildAssistantQuestion([], "随便聊聊"), /用户：随便聊聊$/);
