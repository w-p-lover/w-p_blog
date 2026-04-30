import assert from "node:assert/strict";
import { build } from "esbuild";
import { tmpdir } from "node:os";
import { pathToFileURL } from "node:url";
import { mkdirSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";

const outdir = join(tmpdir(), "chat-model-test");
rmSync(outdir, { recursive: true, force: true });
mkdirSync(outdir, { recursive: true });

await build({
  entryPoints: ["src/components/ChatHome/chatModel.ts"],
  bundle: true,
  platform: "node",
  format: "esm",
  outfile: resolve(outdir, "chatModel.mjs"),
});

const {
  createOutgoingMessage,
  filterConversations,
  formatFileSize,
  getFileTypeByMime,
  getMessagePreview,
  mergeOlderMessages,
  updateConversationState,
  renderEmojiContent,
  shouldCompressUpload,
} = await import(pathToFileURL(resolve(outdir, "chatModel.mjs")).href);

const emojiMap = {
  "[emoji1]": "https://example.com/smile.png",
};

assert.equal(
  renderEmojiContent(" hi [emoji1] [missing] <b>x</b> ", emojiMap),
  "hi <img src=\"https://example.com/smile.png\" width=\"21\" height=\"21\" style=\"margin: 0 1px;vertical-align: text-bottom\"/> [missing] &lt;b&gt;x&lt;/b&gt;"
);

assert.equal(renderEmojiContent("   ", emojiMap), "");

const textMessage = createOutgoingMessage({
  content: "hello",
  messageType: "text",
  senderId: "1",
  receiveId: "2",
  senderName: "Me",
  receiverName: "You",
  senderAvatar: "/avatar.png",
  localId: "local-1",
  now: new Date("2026-04-30T08:00:00.000Z"),
});

assert.deepEqual(textMessage, {
  id: "local-1",
  localId: "local-1",
  content: "hello",
  messageType: "text",
  senderName: "Me",
  name: "You",
  createTime: new Date("2026-04-30T08:00:00.000Z"),
  senderId: "1",
  receiveId: "2",
  senderAvatar: "/avatar.png",
  clientStatus: "sending",
});

assert.equal(getFileTypeByMime("application/pdf", "paper.pdf"), 4);
assert.equal(getFileTypeByMime("", "archive.zip"), 5);
assert.equal(getFileTypeByMime("application/octet-stream", "unknown.bin"), 0);
assert.equal(formatFileSize(1024), "1.00KB");
assert.equal(formatFileSize(3 * 1024 * 1024), "3.00MB");

assert.equal(shouldCompressUpload({ type: "image/png", size: 300 * 1024 }, "img"), true);
assert.equal(shouldCompressUpload({ type: "image/png", size: 40 * 1024 }, "img"), false);
assert.equal(shouldCompressUpload({ type: "application/pdf", size: 300 * 1024 }, "file"), false);

assert.equal(getMessagePreview({ messageType: "text", content: "hello <img src=\"x\"/> world" }), "hello world");
assert.equal(getMessagePreview({ messageType: "image", content: "https://example.com/a.png" }), "[图片]");
assert.equal(
  getMessagePreview({
    messageType: "file",
    content: "https://example.com/a.pdf",
    fileInfo: { fileName: "paper.pdf", fileSize: "12.00KB", fileType: 4 },
  }),
  "[文件] paper.pdf"
);
assert.equal(getMessagePreview({ messageType: "text", content: "012345678901234567890123456789" }, 12), "01234567890...");

const conversations = [
  { id: "1", name: "林间", detail: "写代码", lastMsg: "今晚修聊天室", unreadCount: 0 },
  { id: "2", name: "星野", detail: "摄影", lastMsg: "发了一张照片", unreadCount: 2 },
];

assert.deepEqual(filterConversations(conversations, "聊天室").map((item) => item.id), ["1"]);
assert.deepEqual(filterConversations(conversations, " 星 ").map((item) => item.id), ["2"]);
assert.deepEqual(filterConversations(conversations, "").map((item) => item.id), ["1", "2"]);

const updatedUnread = updateConversationState(conversations, {
  id: "2",
  lastMsg: "新的消息",
  lastTime: "2026-04-30 09:00",
  activeConversationId: "1",
  incoming: true,
});

assert.deepEqual(updatedUnread.map((item) => item.id), ["2", "1"]);
assert.equal(updatedUnread[0].lastMsg, "新的消息");
assert.equal(updatedUnread[0].unreadCount, 3);

const updatedActive = updateConversationState(conversations, {
  id: "1",
  lastMsg: "当前会话消息",
  activeConversationId: "1",
  incoming: true,
});

assert.equal(updatedActive[0].unreadCount, 0);
assert.equal(updatedActive[0].lastMsg, "当前会话消息");

const mergedMessages = mergeOlderMessages(
  [
    { id: "3", content: "third", createTime: "2026-04-30T12:03:00" },
    { id: "4", content: "fourth", createTime: "2026-04-30T12:04:00" },
  ],
  [
    { id: "1", content: "first", createTime: "2026-04-30T12:01:00" },
    { id: "2", content: "second", createTime: "2026-04-30T12:02:00" },
    { id: "3", content: "third duplicate", createTime: "2026-04-30T12:03:00" },
  ]
);

assert.deepEqual(mergedMessages.map((item) => item.id), ["1", "2", "3", "4"]);
assert.equal(mergedMessages[2].content, "third");
