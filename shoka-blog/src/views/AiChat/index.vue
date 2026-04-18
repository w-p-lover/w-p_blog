<template>
  <div class="page-header">
    <h1 class="page-title">AI 问答</h1>
    <img
      class="page-cover"
      src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png"
      alt=""
    />
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <div class="ai-container">
        <el-input
          v-model="question"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入你的问题，例如：Redis缓存击穿怎么解决？"
        />

        <div class="actions">
          <el-button type="primary" :loading="submitting" @click="handleSubmit"> 提问 </el-button>
          <el-button :disabled="submitting" @click="handleClear">清空</el-button>
        </div>

        <div v-if="answer" class="answer-box">
          <h3>回答</h3>
          <div class="answer-content">{{ answer }}</div>
        </div>

        <div v-if="sources.length" class="source-box">
          <h3>参考来源</h3>
          <ul>
            <li v-for="item in sources" :key="item.url">
              <a :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.title || item.url }}</a>
            </li>
          </ul>
        </div>

        <div class="write-assist-box">
          <h3>AI写作助手</h3>
          <el-input
            v-model="writeContent"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            :disabled="writing"
            placeholder="请输入待处理文本，例如：Redis缓存优化实践总结"
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

          <div class="write-result" :class="{ streaming: writing }">
            {{ writeResult || "生成结果会在这里实时显示" }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { aiChat, aiWriteAssistStream } from "@/api/ai";
import type { AiSource, AiWriteAssistAction } from "@/api/ai/types";
import { onBeforeUnmount, ref } from "vue";
import Waves from "@/components/Waves/index.vue";

const question = ref("");
const submitting = ref(false);
const answer = ref("");
const sources = ref<AiSource[]>([]);

const writeContent = ref("");
const writeResult = ref("");
const writing = ref(false);
const currentAction = ref<AiWriteAssistAction | "">("");
let writeAbortController: AbortController | null = null;

const handleSubmit = async () => {
  const content = question.value.trim();
  if (!content) {
    window.$message?.warning("请输入问题");
    return;
  }

  submitting.value = true;
  try {
    const { data } = await aiChat({ question: content });
    answer.value = data?.data?.answer ?? "";
    sources.value = data?.data?.sources ?? [];
  } catch {
    window.$message?.error("提问失败，请稍后重试");
  } finally {
    submitting.value = false;
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
        onMessage: (chunk) => {
          writeResult.value += chunk;
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
      window.$message?.info("已停止生成");
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

const handleClear = () => {
  question.value = "";
  answer.value = "";
  sources.value = [];
};

onBeforeUnmount(() => {
  stopWriteStream();
});
</script>

<style scoped>
.ai-container {
  width: min(900px, 92vw);
  margin: 0 auto;
}

.actions,
.write-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.answer-box,
.source-box,
.write-assist-box {
  margin-top: 24px;
  text-align: left;
}

.answer-box h3,
.source-box h3,
.write-assist-box h3 {
  font-size: 1.2rem;
  font-weight: 500;
  margin-bottom: 12px;
  text-align: left;
}

.answer-content {
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  padding: 14px;
  border-radius: 8px;
  background: #f9f9f9;
}

.source-box ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.source-box li {
  padding: 8px 0;
}

.source-box a {
  color: #409eff;
  text-decoration: none;
}

.write-result {
  margin-top: 14px;
  min-height: 180px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  background: linear-gradient(180deg, #ffffff 0%, #f7f8fa 100%);
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
  transition: box-shadow 0.25s ease, border-color 0.25s ease;
}

.write-result.streaming {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
}
</style>