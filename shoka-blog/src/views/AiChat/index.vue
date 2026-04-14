<template>
  <div class="page-header">
    <h1 class="page-title">AI问答</h1>
    <img class="page-cover" src="@/assets/images/bg.jpg" alt="" />
  </div>

  <div class="bg">
    <div class="ai-card card">
      <el-input
        v-model="question"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请输入你的问题，例如：Redis缓存击穿怎么解决？"
      />

      <div class="actions">
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提问
        </el-button>
        <el-button :disabled="submitting" @click="handleClear">清空</el-button>
      </div>

      <div v-if="answer" class="answer-block">
        <h3>回答</h3>
        <p class="answer-text">{{ answer }}</p>
      </div>

      <div v-if="sources.length" class="sources-block">
        <h3>参考来源</h3>
        <ul>
          <li v-for="item in sources" :key="item.url">
            <a :href="item.url" target="_blank" rel="noopener noreferrer">
              {{ item.title || item.url }}
            </a>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { aiChat } from "@/api/ai";
import type { AiSource } from "@/api/ai/types";
import { ref } from "vue";

const question = ref("");
const submitting = ref(false);
const answer = ref("");
const sources = ref<AiSource[]>([]);

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

const handleClear = () => {
  question.value = "";
  answer.value = "";
  sources.value = [];
};
</script>

<style scoped lang="scss">
.page-header {
  position: relative;
  height: 280px;
  overflow: hidden;

  .page-title {
    position: absolute;
    left: 50%;
    top: 45%;
    transform: translate(-50%, -50%);
    z-index: 2;
    color: #fff;
    font-size: 2rem;
  }

  .page-cover {
    width: 100%;
    height: 100%;
    object-fit: cover;
    filter: brightness(0.75);
  }
}

.bg {
  padding: 24px 0 40px;
}

.ai-card {
  width: min(900px, 92vw);
  margin: 0 auto;
  padding: 20px;
}

.actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.answer-block,
.sources-block {
  margin-top: 18px;
}

.answer-text {
  white-space: pre-wrap;
  line-height: 1.7;
}

.sources-block ul {
  margin: 0;
  padding-left: 18px;
}
</style>
