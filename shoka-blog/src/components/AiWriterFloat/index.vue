<template>
  <div class="assistant-dock" :class="{ open: visible }">
    <button class="assistant-launcher" type="button" aria-label="打开 AI 助手" title="AI 助手" @click="openPanel">
      <svg-icon icon-class="ai" size="1.18rem"></svg-icon>
      <span>AI</span>
      <i :class="{ active: busy }"></i>
    </button>

    <transition name="assistant-rise">
      <section v-if="visible" class="assistant-panel" :class="{ fullscreen: isFullscreen }" aria-label="AI 助手">
        <header class="assistant-header">
          <div class="assistant-brand">
            <span class="assistant-brand__mark">
              <svg-icon icon-class="ai" size="1.05rem"></svg-icon>
            </span>
            <div>
              <h3>AI 助手</h3>
              <p>{{ activeWorkspace === "chat" ? "自由对话" : "写作处理" }}</p>
            </div>
          </div>

          <div class="assistant-window-actions">
            <button type="button" :aria-label="isFullscreen ? '退出全屏' : '全屏'" @click="toggleFullscreen">
              {{ isFullscreen ? "收" : "展" }}
            </button>
            <button type="button" aria-label="关闭 AI 助手" @click="closePanel">×</button>
          </div>
        </header>

        <nav class="workspace-tabs" aria-label="AI 助手模式">
          <button type="button" :class="{ active: activeWorkspace === 'chat' }" @click="activeWorkspace = 'chat'">
            对话
          </button>
          <button type="button" :class="{ active: activeWorkspace === 'write' }" @click="activeWorkspace = 'write'">
            写作
          </button>
        </nav>

        <section v-if="activeWorkspace === 'chat'" class="chat-workspace">
          <div ref="chatContainer" class="chat-thread">
            <article v-for="(item, index) in chatMessages" :key="`${item.role}-${index}`" class="chat-message" :class="item.role">
              <span>{{ item.role === "assistant" ? "AI" : "我" }}</span>
              <div>
                <v-md-preview class="assistant-md" :text="normalizeAiMarkdown(item.content)"></v-md-preview>
              </div>
            </article>

            <article v-if="chatPending" class="chat-message assistant pending">
              <span>AI</span>
              <div><p>正在想一想...</p></div>
            </article>
          </div>

          <form class="chat-composer" @submit.prevent="handleChatSubmit">
            <textarea
              v-model="chatDraft"
              maxlength="500"
              :disabled="chatting"
              placeholder="随便聊聊，或把想写的东西告诉我"
              @keydown.enter.exact.prevent="handleChatSubmit"
            ></textarea>
            <div class="composer-actions">
              <button class="quiet" type="button" :disabled="!canStop" @click="handleStop">停止</button>
              <button class="primary" type="submit" :disabled="chatting || !chatDraft.trim()">发送</button>
            </div>
          </form>
        </section>

        <section v-else class="write-workspace">
          <label class="write-field">
            <span>待处理文本</span>
            <textarea
              v-model="writeContent"
              maxlength="2000"
              :disabled="writing"
              placeholder="粘贴一段文字，再选择扩写、润色或总结"
            ></textarea>
          </label>

          <div class="writing-actions">
            <button
              v-for="item in writingActions"
              :key="item.action"
              type="button"
              :class="{ active: writing && currentAction === item.action }"
              :disabled="writing || !writeContent.trim()"
              @click="handleWriteAssist(item.action)"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="write-toolbar">
            <button type="button" :disabled="!canStop" @click="handleStop">停止</button>
            <button type="button" :disabled="writing" @click="handleClearWrite">清空</button>
          </div>

          <div ref="resultContainer" class="write-result" :class="{ streaming: writing }">
            <v-md-preview v-if="writeResult" class="assistant-md" :text="normalizeAiMarkdown(writeResult)"></v-md-preview>
            <span v-else>处理结果会显示在这里</span>
          </div>
        </section>
      </section>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { aiWriteAssistStream } from "@/api/ai";
import type { AiWriteAssistAction } from "@/api/ai/types";
import { normalizeAiMarkdown } from "@/utils/markdown";
import { nextTick, onBeforeUnmount, ref } from "vue";
import { buildAssistantQuestion, writingActions } from "./assistantModel";

interface ChatMessage {
  role: "assistant" | "user";
  content: string;
}

const visible = ref(false);
const isFullscreen = ref(false);
const activeWorkspace = ref<"chat" | "write">("chat");
const chatDraft = ref("");
const chatting = ref(false);
const chatPending = ref(false);
const chatMessages = ref<ChatMessage[]>([
  { role: "assistant", content: "想聊什么都可以。也可以把一个还没成形的写作想法丢给我。" },
]);
const chatContainer = ref<HTMLElement | null>(null);
const writeContent = ref("");
const writeResult = ref("");
const writing = ref(false);
const currentAction = ref<AiWriteAssistAction | "">("");
const resultContainer = ref<HTMLElement | null>(null);
const busy = computed(() => chatting.value || writing.value);
const canStop = computed(() => chatting.value || writing.value);
let chatAbortController: AbortController | null = null;
let writeAbortController: AbortController | null = null;

const scrollToBottom = async (container: typeof chatContainer | typeof resultContainer) => {
  await nextTick();
  if (container.value) {
    container.value.scrollTop = container.value.scrollHeight;
  }
};

const stopChat = () => {
  chatAbortController?.abort();
  chatAbortController = null;
  chatting.value = false;
  chatPending.value = false;
};

const stopWrite = () => {
  writeAbortController?.abort();
  writeAbortController = null;
  writing.value = false;
  currentAction.value = "";
};

const stopActiveTask = () => {
  const hadTask = canStop.value;
  stopChat();
  stopWrite();
  return hadTask;
};

const openPanel = () => {
  visible.value = true;
};

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value;
};

const closePanel = () => {
  if (stopActiveTask()) {
    window.$message?.info("已停止生成");
  }
  visible.value = false;
  isFullscreen.value = false;
};

const handleChatSubmit = async () => {
  const content = chatDraft.value.trim();
  if (!content || chatting.value) {
    return;
  }

  const recentMessages = chatMessages.value.slice(-6);
  chatMessages.value.push({ role: "user", content });
  chatDraft.value = "";
  chatting.value = true;
  chatPending.value = true;
  chatAbortController = new AbortController();
  await scrollToBottom(chatContainer);

  try {
    let assistantMessage: ChatMessage | null = null;
    await aiWriteAssistStream(
      { action: "chat", content: buildAssistantQuestion(recentMessages, content) },
      {
        signal: chatAbortController.signal,
        onMessage: async (chunk) => {
          if (!assistantMessage) {
            assistantMessage = { role: "assistant", content: "" };
            chatMessages.value.push(assistantMessage);
            chatPending.value = false;
          }
          assistantMessage.content += chunk;
          await scrollToBottom(chatContainer);
        },
        onComplete: () => {
          chatPending.value = false;
          if (!assistantMessage?.content.trim()) {
            chatMessages.value.push({
              role: "assistant",
              content: "这次没有拿到回答，可以换个说法再试一次。",
            });
          }
          chatting.value = false;
          chatAbortController = null;
        },
      },
    );
  } catch (error: any) {
    if (error?.name !== "AbortError" && error?.name !== "CanceledError") {
      window.$message?.error(error?.message || "对话请求失败，请稍后重试");
    }
  } finally {
    chatting.value = false;
    chatPending.value = false;
    chatAbortController = null;
    await scrollToBottom(chatContainer);
  }
};

const handleWriteAssist = async (action: AiWriteAssistAction) => {
  const content = writeContent.value.trim();
  if (!content || writing.value) {
    return;
  }

  stopWrite();
  writeResult.value = "";
  writing.value = true;
  currentAction.value = action;
  writeAbortController = new AbortController();

  try {
    await aiWriteAssistStream(
      { action, content },
      {
        signal: writeAbortController.signal,
        onMessage: async (chunk) => {
          writeResult.value += chunk;
          await scrollToBottom(resultContainer);
        },
        onComplete: () => {
          writing.value = false;
          currentAction.value = "";
          writeAbortController = null;
        },
      },
    );
  } catch (error: any) {
    if (error?.name !== "AbortError") {
      window.$message?.error("写作助手请求失败，请稍后重试");
    }
  } finally {
    if (writing.value) {
      writing.value = false;
      currentAction.value = "";
      writeAbortController = null;
    }
  }
};

const handleStop = () => {
  if (stopActiveTask()) {
    window.$message?.info("已停止生成");
  }
};

const handleClearWrite = () => {
  writeContent.value = "";
  writeResult.value = "";
};

onBeforeUnmount(() => {
  stopActiveTask();
});
</script>

<style scoped>
.assistant-dock {
  position: fixed;
  left: 24px;
  bottom: 24px;
  z-index: 10020;
}

.assistant-launcher {
  position: relative;
  width: 62px;
  height: 48px;
  border: 1px solid rgba(74, 86, 101, 0.22);
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #26384a;
  background:
    linear-gradient(140deg, rgba(255, 255, 255, 0.96), rgba(232, 244, 244, 0.88));
  box-shadow: 0 14px 30px rgba(31, 39, 52, 0.18);
  backdrop-filter: blur(14px);
  cursor: pointer;
  font-weight: 700;
  letter-spacing: 0;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.assistant-launcher:hover {
  border-color: rgba(74, 86, 101, 0.42);
  box-shadow: 0 18px 34px rgba(31, 39, 52, 0.24);
  transform: translateY(-2px);
}

.assistant-launcher i {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #5b9e91;
}

.assistant-launcher i.active {
  animation: assistant-pulse 1.35s ease-in-out infinite;
}

.assistant-panel {
  position: absolute;
  left: 0;
  bottom: 60px;
  width: min(470px, calc(100vw - 32px));
  height: min(680px, calc(100vh - 52px));
  border: 1px solid rgba(75, 88, 104, 0.2);
  border-radius: 8px;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
  color: #27384a;
  background:
    linear-gradient(180deg, rgba(255, 253, 248, 0.98), rgba(241, 247, 250, 0.97));
  box-shadow:
    0 28px 68px rgba(25, 34, 48, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(18px);
}

.assistant-panel.fullscreen {
  position: fixed;
  left: 24px;
  top: 24px;
  bottom: 24px;
  width: min(860px, calc(100vw - 48px));
  height: auto;
}

.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 68px;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(75, 88, 104, 0.12);
  background: rgba(255, 249, 238, 0.72);
}

.assistant-brand {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 11px;
}

.assistant-brand__mark {
  width: 36px;
  height: 36px;
  flex: none;
  border: 1px solid rgba(75, 88, 104, 0.18);
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #275268;
  background: rgba(221, 241, 238, 0.88);
}

.assistant-brand h3,
.assistant-brand p {
  margin: 0;
  letter-spacing: 0;
}

.assistant-brand h3 {
  color: #1d3042;
  font-size: 1rem;
  line-height: 1.2;
}

.assistant-brand p {
  margin-top: 3px;
  color: #6e5a4b;
  font-size: 12px;
}

.assistant-window-actions {
  display: flex;
  gap: 6px;
}

.assistant-window-actions button {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(75, 88, 104, 0.16);
  border-radius: 8px;
  color: #536478;
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  line-height: 1;
}

.workspace-tabs {
  margin: 12px 14px 0;
  padding: 4px;
  border: 1px solid rgba(75, 88, 104, 0.12);
  border-radius: 8px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  background: rgba(230, 238, 242, 0.74);
}

.workspace-tabs button {
  height: 36px;
  border: 0;
  border-radius: 6px;
  color: #5d6c7d;
  background: transparent;
  cursor: pointer;
  font-weight: 600;
  white-space: nowrap;
}

.workspace-tabs button.active {
  color: #20374b;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 18px rgba(42, 52, 66, 0.12);
}

.chat-workspace,
.write-workspace {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 14px 14px;
}

.chat-thread {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  border: 1px solid rgba(75, 88, 104, 0.12);
  border-radius: 8px;
  padding: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(239, 246, 247, 0.78));
}

.chat-message {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 9px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.chat-message:last-child {
  margin-bottom: 0;
}

.chat-message > span {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #275268;
  background: rgba(204, 235, 230, 0.86);
  font-size: 12px;
  font-weight: 700;
}

.chat-message > div {
  min-width: 0;
  width: fit-content;
  max-width: 100%;
  border: 1px solid rgba(75, 88, 104, 0.12);
  border-radius: 8px;
  padding: 10px 11px;
  color: #304255;
  background: rgba(255, 255, 255, 0.88);
  line-height: 1.75;
  word-break: break-word;
}

.chat-message p {
  margin: 0;
  white-space: pre-wrap;
}

.assistant-md {
  color: inherit;
  white-space: normal;
}

.assistant-md :deep(.v-md-editor-preview) {
  padding: 0;
}

.assistant-md :deep(.vuepress-markdown-body) {
  color: inherit;
  background: transparent;
  font-size: 14px;
  line-height: 1.75;
}

.assistant-md :deep(.vuepress-markdown-body:not(.custom)) {
  padding: 0;
}

.assistant-md :deep(.vuepress-markdown-body > :first-child) {
  margin-top: 0;
}

.assistant-md :deep(.vuepress-markdown-body > :last-child) {
  margin-bottom: 0;
}

.assistant-md :deep(.vuepress-markdown-body p),
.assistant-md :deep(.vuepress-markdown-body ul),
.assistant-md :deep(.vuepress-markdown-body ol),
.assistant-md :deep(.vuepress-markdown-body pre),
.assistant-md :deep(.vuepress-markdown-body blockquote),
.assistant-md :deep(.vuepress-markdown-body table) {
  margin-top: 0.55em;
  margin-bottom: 0.55em;
}

.assistant-md :deep(.vuepress-markdown-body h1),
.assistant-md :deep(.vuepress-markdown-body h2),
.assistant-md :deep(.vuepress-markdown-body h3),
.assistant-md :deep(.vuepress-markdown-body h4),
.assistant-md :deep(.vuepress-markdown-body h5),
.assistant-md :deep(.vuepress-markdown-body h6) {
  position: relative;
  margin-top: 0.85em;
  margin-bottom: 0.45em;
  padding: 0 0 0 0.62em;
  border: 0;
  color: #1f4054;
  line-height: 1.45;
  letter-spacing: 0;
}

.assistant-md :deep(.vuepress-markdown-body h1::before),
.assistant-md :deep(.vuepress-markdown-body h2::before),
.assistant-md :deep(.vuepress-markdown-body h3::before),
.assistant-md :deep(.vuepress-markdown-body h4::before),
.assistant-md :deep(.vuepress-markdown-body h5::before),
.assistant-md :deep(.vuepress-markdown-body h6::before) {
  position: absolute;
  left: 0;
  top: 0.28em;
  bottom: 0.28em;
  width: 3px;
  border-radius: 999px;
  background: #5b9e91;
  content: "";
}

.assistant-md :deep(.vuepress-markdown-body h1) {
  font-size: 1.08rem;
}

.assistant-md :deep(.vuepress-markdown-body h2) {
  font-size: 1.02rem;
}

.assistant-md :deep(.vuepress-markdown-body h3),
.assistant-md :deep(.vuepress-markdown-body h4),
.assistant-md :deep(.vuepress-markdown-body h5),
.assistant-md :deep(.vuepress-markdown-body h6) {
  font-size: 1rem;
}

.assistant-md :deep(.vuepress-markdown-body pre) {
  max-width: 100%;
  border-radius: 8px;
  overflow-x: auto;
}

.assistant-md :deep(.vuepress-markdown-body code) {
  white-space: pre-wrap;
  word-break: break-word;
}

.assistant-md :deep(.vuepress-markdown-body table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
}

.assistant-md :deep(.vuepress-markdown-body a) {
  color: #246d86;
  font-weight: 700;
  text-decoration: underline;
  text-decoration-color: rgba(36, 109, 134, 0.28);
  text-decoration-thickness: 2px;
  text-underline-offset: 3px;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.assistant-md :deep(.vuepress-markdown-body a:hover) {
  color: #8b5239;
  text-decoration-color: rgba(139, 82, 57, 0.42);
}

.assistant-md :deep(.vuepress-markdown-body img) {
  display: block;
  width: auto;
  max-width: 100%;
  max-height: 260px;
  margin: 0.7em auto;
  border: 1px solid rgba(75, 88, 104, 0.16);
  border-radius: 8px;
  object-fit: contain;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 10px 22px rgba(25, 34, 48, 0.13);
}

.chat-message.user {
  grid-template-columns: minmax(0, 1fr) 28px;
}

.chat-message.user > span {
  grid-column: 2;
  color: #75543f;
  background: rgba(247, 222, 196, 0.9);
}

.chat-message.user > div {
  grid-column: 1;
  grid-row: 1;
  justify-self: end;
  color: #45372f;
  background: rgba(255, 245, 230, 0.92);
}

.chat-message.pending > div {
  color: #617284;
}

.chat-composer {
  flex: none;
  border: 1px solid rgba(75, 88, 104, 0.14);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.78);
}

.chat-composer textarea,
.write-field textarea {
  width: 100%;
  border: 1px solid rgba(75, 88, 104, 0.14);
  border-radius: 8px;
  box-sizing: border-box;
  resize: none;
  color: #27384a;
  background: rgba(248, 251, 252, 0.92);
  outline: none;
  font: inherit;
  line-height: 1.65;
}

.chat-composer textarea {
  height: 86px;
  padding: 10px 11px;
}

.chat-composer textarea:focus,
.write-field textarea:focus {
  border-color: rgba(53, 128, 137, 0.46);
  box-shadow: 0 0 0 3px rgba(53, 128, 137, 0.12);
}

.composer-actions {
  margin-top: 9px;
  display: grid;
  grid-template-columns: minmax(86px, 1fr) minmax(110px, 1.2fr);
  gap: 8px;
}

.composer-actions button,
.writing-actions button,
.write-toolbar button {
  min-width: 0;
  height: 38px;
  border: 1px solid rgba(75, 88, 104, 0.16);
  border-radius: 8px;
  color: #425668;
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  font-weight: 600;
  white-space: nowrap;
}

.composer-actions .primary,
.writing-actions button.active {
  border-color: rgba(41, 97, 123, 0.28);
  color: #f7fbfb;
  background: #29617b;
}

.composer-actions button:disabled,
.writing-actions button:disabled,
.write-toolbar button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.write-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #5c4d40;
  font-size: 12px;
  font-weight: 700;
}

.write-field textarea {
  height: 144px;
  padding: 11px;
  color: #27384a;
  font-size: 14px;
  font-weight: 400;
}

.writing-actions {
  flex: none;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.writing-actions button:not(.active):hover,
.write-toolbar button:hover,
.composer-actions .quiet:hover,
.assistant-window-actions button:hover {
  border-color: rgba(53, 128, 137, 0.36);
  color: #20374b;
}

.write-toolbar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.write-result {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  border: 1px solid rgba(75, 88, 104, 0.12);
  border-radius: 8px;
  padding: 13px;
  color: #304255;
  background: rgba(238, 246, 247, 0.82);
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;
}

.write-result.streaming {
  border-color: rgba(53, 128, 137, 0.42);
  box-shadow: 0 0 0 3px rgba(53, 128, 137, 0.1);
}

.assistant-rise-enter-active,
.assistant-rise-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.assistant-rise-enter-from,
.assistant-rise-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes assistant-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(91, 158, 145, 0.5);
  }
  50% {
    box-shadow: 0 0 0 7px rgba(91, 158, 145, 0);
  }
}

@media (max-width: 768px) {
  .assistant-dock {
    left: 12px;
    bottom: 12px;
  }

  .assistant-panel,
  .assistant-panel.fullscreen {
    position: fixed;
    left: 12px;
    right: 12px;
    top: 12px;
    bottom: 12px;
    width: auto;
    height: auto;
  }

  .assistant-header {
    min-height: 62px;
  }

  .chat-workspace,
  .write-workspace {
    padding-left: 10px;
    padding-right: 10px;
  }

  .chat-thread {
    padding: 11px;
  }
}
</style>

<style>
[theme="dark"] .assistant-dock .assistant-launcher,
[theme="dark"] .assistant-dock .assistant-panel {
  border-color: var(--home-border);
  color: var(--grey-6);
  background: rgba(28, 35, 46, 0.96);
  box-shadow: var(--home-shadow), var(--home-glow);
}

[theme="dark"] .assistant-dock .assistant-header {
  border-bottom-color: var(--home-border);
  background: rgba(20, 26, 35, 0.82);
}

[theme="dark"] .assistant-dock .assistant-brand h3 {
  color: var(--grey-7);
}

[theme="dark"] .assistant-dock .assistant-brand p,
[theme="dark"] .assistant-dock .write-field {
  color: var(--grey-5);
}

[theme="dark"] .assistant-dock .assistant-brand__mark,
[theme="dark"] .assistant-dock .workspace-tabs,
[theme="dark"] .assistant-dock .chat-thread,
[theme="dark"] .assistant-dock .chat-composer,
[theme="dark"] .assistant-dock .write-result {
  border-color: var(--home-border);
  background: rgba(18, 24, 33, 0.72);
}

[theme="dark"] .assistant-dock .workspace-tabs button,
[theme="dark"] .assistant-dock .assistant-window-actions button,
[theme="dark"] .assistant-dock .composer-actions button,
[theme="dark"] .assistant-dock .writing-actions button,
[theme="dark"] .assistant-dock .write-toolbar button {
  border-color: var(--home-border);
  color: var(--grey-5);
  background: var(--note-bg);
}

[theme="dark"] .assistant-dock .workspace-tabs button.active,
[theme="dark"] .assistant-dock .writing-actions button.active,
[theme="dark"] .assistant-dock .composer-actions .primary {
  color: #f6faf8;
  background: var(--home-accent);
}

[theme="dark"] .assistant-dock .chat-message > div,
[theme="dark"] .assistant-dock .chat-composer textarea,
[theme="dark"] .assistant-dock .write-field textarea {
  border-color: var(--home-border);
  color: var(--grey-6);
  background: rgba(30, 37, 49, 0.88);
}

[theme="dark"] .assistant-dock .chat-message.user > div {
  color: var(--grey-7);
  background: rgba(94, 63, 46, 0.34);
}
</style>
