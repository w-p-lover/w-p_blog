<template>
  <div class="ai-writer-float">
    <button class="entry-btn" type="button" aria-label="打开AI写作助手" @click="openPanel">
      <span class="entry-btn__dot" :class="{ writing }"></span>
      AI 写作
    </button>

    <transition name="panel-fade-up">
      <section v-if="visible" class="writer-panel" :class="{ fullscreen: isFullscreen }">
        <header class="panel-header">
          <h3>AI 写作助手</h3>
          <div class="panel-header-actions">
            <button type="button" @click="toggleFullscreen">{{ isFullscreen ? "退出全屏" : "全屏" }}</button>
            <button type="button" @click="closePanel">关闭</button>
          </div>
        </header>

        <div class="panel-content">
          <el-input
            v-model="writeContent"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            resize="none"
            :disabled="writing"
            placeholder="请输入待处理文本"
          />

          <div class="write-actions">
            <el-button
              type="primary"
              :loading="writing && currentAction === 'expand'"
              :disabled="writing"
              @click="handleWriteAssist('expand')"
            >
              扩写
            </el-button>
            <el-button
              type="primary"
              :loading="writing && currentAction === 'polish'"
              :disabled="writing"
              @click="handleWriteAssist('polish')"
            >
              润色
            </el-button>
            <el-button
              type="primary"
              :loading="writing && currentAction === 'summary'"
              :disabled="writing"
              @click="handleWriteAssist('summary')"
            >
              总结
            </el-button>
            <el-button :disabled="!writing" @click="handleStopWrite">停止</el-button>
            <el-button :disabled="writing" @click="handleClearWrite">清空</el-button>
          </div>

          <div ref="resultContainer" class="write-result" :class="{ streaming: writing }">
            {{ writeResult || "生成结果会在这里实时显示" }}
          </div>
        </div>
      </section>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { aiWriteAssistStream } from "@/api/ai";
import type { AiWriteAssistAction } from "@/api/ai/types";
import { nextTick, onBeforeUnmount, ref } from "vue";

const visible = ref(false);
const isFullscreen = ref(false);
const writeContent = ref("");
const writeResult = ref("");
const writing = ref(false);
const currentAction = ref<AiWriteAssistAction | "">("");
const resultContainer = ref<HTMLElement | null>(null);
let writeAbortController: AbortController | null = null;

const scrollResultToBottom = async () => {
  await nextTick();
  if (resultContainer.value) {
    resultContainer.value.scrollTop = resultContainer.value.scrollHeight;
  }
};

const stopWriteStream = () => {
  if (writeAbortController) {
    writeAbortController.abort();
    writeAbortController = null;
  }
  writing.value = false;
  currentAction.value = "";
};

const openPanel = () => {
  visible.value = true;
};

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value;
};

const closePanel = () => {
  if (writing.value) {
    stopWriteStream();
    window.$message?.info("已停止生成");
  }
  visible.value = false;
  isFullscreen.value = false;
};

const handleWriteAssist = async (action: AiWriteAssistAction) => {
  const content = writeContent.value.trim();
  if (!content) {
    window.$message?.warning("请输入写作内容");
    return;
  }

  stopWriteStream();
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
          await scrollResultToBottom();
        },
        onComplete: () => {
          writing.value = false;
          currentAction.value = "";
          writeAbortController = null;
        },
      },
    );
  } catch (error: any) {
    if (error?.name === "AbortError") {
      return;
    }
    window.$message?.error("写作助手请求失败，请稍后重试");
  } finally {
    if (writing.value) {
      writing.value = false;
      currentAction.value = "";
      writeAbortController = null;
    }
  }
};

const handleStopWrite = () => {
  if (!writing.value) {
    return;
  }
  stopWriteStream();
  window.$message?.info("已停止生成");
};

const handleClearWrite = () => {
  writeContent.value = "";
  writeResult.value = "";
};

onBeforeUnmount(() => {
  stopWriteStream();
});
</script>

<style scoped>
.ai-writer-float {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 20;
}

.entry-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 999px;
  padding: 11px 16px;
  background: linear-gradient(135deg, rgba(36, 52, 84, 0.86), rgba(23, 34, 58, 0.94));
  color: #eef3ff;
  box-shadow: 0 10px 24px rgba(8, 16, 33, 0.36);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.entry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(8, 16, 33, 0.42);
}

.entry-btn__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9fc3ff;
}

.entry-btn__dot.writing {
  animation: pulse 1.4s ease-in-out infinite;
}

.writer-panel {
  position: absolute;
  right: 0;
  bottom: 56px;
  width: 420px;
  height: 560px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: linear-gradient(165deg, rgba(19, 29, 48, 0.95), rgba(14, 22, 37, 0.95));
  color: #eef3ff;
  box-shadow: 0 22px 45px rgba(8, 16, 33, 0.45);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.writer-panel.fullscreen {
  position: fixed;
  left: 24px;
  right: 24px;
  top: 24px;
  bottom: 24px;
  width: auto;
  height: auto;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
}

.panel-header-actions {
  display: flex;
  gap: 8px;
}

.panel-header-actions button {
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 8px;
  padding: 5px 10px;
  color: #eef3ff;
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
}

.panel-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px 16px;
}

.write-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.write-result {
  flex: 1;
  min-height: 160px;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  padding: 14px;
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.04));
}

.write-result.streaming {
  border-color: rgba(159, 195, 255, 0.7);
  box-shadow: 0 0 0 2px rgba(159, 195, 255, 0.18);
}

.panel-fade-up-enter-active,
.panel-fade-up-leave-active {
  transition: all 0.2s ease;
}

.panel-fade-up-enter-from,
.panel-fade-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(159, 195, 255, 0.6);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(159, 195, 255, 0);
  }
}

@media (max-width: 768px) {
  .ai-writer-float {
    right: 16px;
    bottom: 16px;
  }

  .writer-panel,
  .writer-panel.fullscreen {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 12px;
    top: auto;
    width: auto;
    height: 82vh;
    border-radius: 14px;
  }
}
</style>
