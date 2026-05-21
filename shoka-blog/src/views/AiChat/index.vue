<template>
  <div class="page-header">
    <div class="page-title">
      <h1>AI 问答</h1>
      <p class="page-subtitle">工程化思维 · 结构化回答 · 可落地建议</p>
    </div>
    <div
      class="page-cover-placeholder"
      :class="{ 'page-cover-placeholder--hidden': coverLoaded }"
      :style="{ backgroundImage: `url(${lowResCoverUrl})` }"
      aria-hidden="true"
    ></div>
    <img
      class="page-cover"
      :class="{ 'page-cover--ready': coverLoaded }"
      :src="coverUrl"
      alt=""
      loading="eager"
      fetchpriority="high"
      decoding="async"
      @load="handleCoverLoad"
    />
    <Waves></Waves>
  </div>

  <div class="bg">
    <div class="page-container">
      <section class="ai-shell">
        <header class="ai-hero">
          <div class="hero-glow hero-glow--left"></div>
          <div class="hero-glow hero-glow--right"></div>
          <p class="hero-badge">{{ currentMode.badge }}</p>
          <h2>{{ currentMode.headline }}</h2>
          <p class="hero-desc">{{ currentMode.description }}</p>

          <div class="mode-switch" role="tablist" aria-label="AI 对话方向">
            <button
              v-for="item in aiChatModes"
              :key="item.key"
              type="button"
              class="mode-switch__item"
              :class="{ active: item.key === activeMode }"
              :aria-selected="item.key === activeMode"
              role="tab"
              @click="changeMode(item.key)"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-label">输入字数</span>
              <span class="stat-value">{{ question.length }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最近回答</span>
              <span class="stat-value">{{ answer ? "已生成" : "等待中" }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">来源数量</span>
              <span class="stat-value">{{ sources.length }}</span>
            </div>
          </div>

          <div class="hero-meter">
            <div class="hero-meter-track">
              <span class="hero-meter-fill" :style="{ width: `${questionLengthRatio}%` }"></span>
            </div>
            <span class="hero-meter-text">提问完整度 {{ questionLengthRatio }}%</span>
          </div>
        </header>

        <section class="qa-card">
          <div class="card-title-row">
            <h3>{{ currentMode.inputTitle }}</h3>
            <span class="card-title-tip">{{ currentMode.inputTip }}</span>
          </div>

          <el-input
            v-model="question"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            :placeholder="currentMode.placeholder"
          />

          <div class="prompt-list">
            <button
              v-for="item in currentMode.quickPrompts"
              :key="item"
              type="button"
              class="prompt-chip"
              @click="applyPrompt(item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="actions">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ currentMode.submitText }}</el-button>
            <el-button :disabled="submitting" @click="handleClear">重置内容</el-button>
          </div>
        </section>

        <section v-if="answer" class="answer-card">
          <div class="card-title-row">
            <h3>AI 回答</h3>
            <button class="ghost-btn" type="button" @click="handleCopyAnswer">复制内容</button>
          </div>
          <v-md-preview class="answer-content" :text="normalizeAiMarkdown(answer)"></v-md-preview>
        </section>

        <section v-if="sources.length" class="source-card">
          <div class="card-title-row">
            <h3>参考来源</h3>
            <span class="card-title-tip">共 {{ sources.length }} 条</span>
          </div>
          <ul>
            <li v-for="(item, index) in sources" :key="item.url">
              <span class="source-index">{{ index + 1 }}</span>
              <a :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.title || item.url }}</a>
            </li>
          </ul>
        </section>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { aiKnowledgeChat, aiWriteAssistStream } from "@/api/ai";
import type { AiSource } from "@/api/ai/types";
import Waves from "@/components/Waves/index.vue";
import { normalizeAiMarkdown } from "@/utils/markdown";
import { aiChatModes, getAiChatMode } from "./aiChatModel";

const question = ref("");
const submitting = ref(false);
const answer = ref("");
const sources = ref<AiSource[]>([]);
const activeMode = ref("chat");
const coverLoaded = ref(false);
const coverUrl = "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png";
const lowResCoverUrl = `${coverUrl}?x-oss-process=image/resize,w_320/quality,q_35`;

const questionLengthRatio = computed(() => Math.min(100, Math.round((question.value.length / 500) * 100)));
const currentMode = computed(() => getAiChatMode(activeMode.value));

const applyPrompt = (value: string) => {
  question.value = value;
};

const changeMode = (value: string) => {
  activeMode.value = value;
};

const handleCoverLoad = () => {
  coverLoaded.value = true;
};

const handleSubmit = async () => {
  const content = question.value.trim();
  if (!content) {
    window.$message?.warning("请输入问题");
    return;
  }

  submitting.value = true;
  try {
    if (currentMode.value.provider === "knowledge") {
      const { data: result } = await aiKnowledgeChat({ question: content });
      if (!result?.flag || !result.data) {
        throw new Error(result?.msg || "提问失败");
      }
      answer.value = result.data.answer ?? "";
      sources.value = result.data.sources ?? [];
      return;
    }
    answer.value = "";
    sources.value = [];
    await aiWriteAssistStream(
      { action: "chat", content },
      {
        onMessage: (chunk) => {
          answer.value += chunk;
        },
        onComplete: () => {
          if (!answer.value.trim()) {
            answer.value = "这次没有拿到回答，可以换个说法再试一次。";
          }
        },
      },
    );
  } catch (error: any) {
    window.$message?.error(error?.message || "提问失败，请稍后重试");
  } finally {
    submitting.value = false;
  }
};

const handleCopyAnswer = async () => {
  if (!answer.value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(answer.value);
    window.$message?.success("已复制回答");
  } catch {
    window.$message?.warning("复制失败，请手动复制");
  }
};

const handleClear = () => {
  question.value = "";
  answer.value = "";
  sources.value = [];
};
</script>

<style scoped>
.page-cover-placeholder {
  position: fixed;
  inset: 0 auto auto 0;
  width: 100%;
  height: clamp(22rem, 68vh, 44rem);
  background-size: cover;
  background-position: center;
  filter: blur(10px) saturate(1.1);
  opacity: 0.92;
  transform: scale(1.02);
  transition: opacity 0.42s ease;
  pointer-events: none;
}

.page-cover-placeholder--hidden {
  opacity: 0;
}

.page-cover {
  opacity: 0.06;
  transition: opacity 0.45s ease;
}

.page-cover--ready {
  opacity: 1;
}

.page-title h1 {
  margin: 0;
  font-size: clamp(1rem, 2.8vw, 2rem);
  letter-spacing: 0.04em;
}

.page-subtitle {
  margin: 0.7rem 0 0;
  font-size: clamp(0.95rem, 1.4vw, 1.1rem);
  letter-spacing: 0.08em;
  color: rgba(233, 242, 255, 0.88);
}

.ai-shell {
  width: min(980px, 94vw);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ai-hero {
  position: relative;
  overflow: hidden;
  padding: 24px 24px 20px;
  border-radius: 16px;
  border: 1px solid rgba(127, 152, 207, 0.2);
  background:
    radial-gradient(circle at 8% 16%, rgba(95, 141, 255, 0.22), transparent 35%),
    radial-gradient(circle at 85% 22%, rgba(85, 224, 199, 0.2), transparent 35%),
    linear-gradient(160deg, rgba(247, 250, 255, 0.98), rgba(236, 244, 255, 0.95));
  box-shadow: 0 16px 34px rgba(15, 35, 74, 0.14);
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  pointer-events: none;
}

.hero-glow--left {
  width: 180px;
  height: 180px;
  left: -58px;
  top: -80px;
  background: rgba(122, 162, 255, 0.2);
}

.hero-glow--right {
  width: 140px;
  height: 140px;
  right: -34px;
  bottom: -42px;
  background: rgba(88, 227, 197, 0.2);
}

.hero-badge {
  position: relative;
  z-index: 1;
  display: inline-block;
  margin: 0 0 10px;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(31, 84, 188, 0.12);
  color: #1b4c9f;
  font-size: 12px;
  letter-spacing: 0.12em;
  font-weight: 600;
}

.ai-hero h2 {
  position: relative;
  z-index: 1;
  margin: 0;
  font-size: 1.55rem;
  color: #172f57;
  line-height: 1.4;
}

.hero-desc {
  position: relative;
  z-index: 1;
  margin: 10px 0 0;
  color: #405982;
  line-height: 1.85;
}

.mode-switch {
  position: relative;
  z-index: 1;
  width: fit-content;
  margin-top: 14px;
  padding: 4px;
  display: inline-flex;
  gap: 4px;
  border: 1px solid rgba(127, 152, 207, 0.2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.52);
}

.mode-switch__item {
  min-height: 34px;
  border: 0;
  border-radius: 7px;
  padding: 0 13px;
  color: #55709b;
  background: transparent;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.mode-switch__item.active {
  color: #173e7b;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 5px 14px rgba(35, 67, 126, 0.14);
}

.hero-stats {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stat-item {
  border: 1px solid rgba(127, 152, 207, 0.22);
  border-radius: 12px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.62);
}

.stat-label {
  display: block;
  color: #5e7297;
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  display: block;
  color: #1f3865;
  font-size: 14px;
  font-weight: 600;
}

.hero-meter {
  margin-top: 12px;
}

.hero-meter-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: rgba(76, 118, 197, 0.18);
  overflow: hidden;
}

.hero-meter-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4b74d2, #58dec6);
  transition: width 0.2s ease;
}

.hero-meter-text {
  margin-top: 7px;
  display: inline-block;
  color: #4f6790;
  font-size: 12px;
}

.qa-card,
.answer-card,
.source-card {
  border-radius: 16px;
  border: 1px solid rgba(131, 156, 209, 0.15);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.96));
  box-shadow: 0 16px 34px rgba(16, 31, 64, 0.1);
  padding: 20px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title-row h3 {
  margin: 0;
  color: #1d3560;
  font-size: 1.08rem;
}

.card-title-tip {
  color: #60779e;
  font-size: 12px;
}

.prompt-list {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.prompt-chip {
  border: 1px solid rgba(66, 116, 209, 0.24);
  background: rgba(66, 116, 209, 0.08);
  color: #2d569e;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.prompt-chip:hover {
  background: rgba(66, 116, 209, 0.16);
  transform: translateY(-1px);
}

.actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.ghost-btn {
  border: 1px solid rgba(72, 112, 188, 0.26);
  border-radius: 8px;
  padding: 4px 10px;
  background: rgba(72, 112, 188, 0.08);
  color: #2d5395;
  font-size: 12px;
  cursor: pointer;
}

.answer-content {
  color: #2b426a;
  white-space: normal;
}

.answer-content :deep(.v-md-editor-preview) {
  padding: 0;
}

.answer-content :deep(.vuepress-markdown-body) {
  color: inherit;
  background: transparent;
  font-size: 15px;
  line-height: 1.9;
}

.answer-content :deep(.vuepress-markdown-body:not(.custom)) {
  padding: 0;
}

.answer-content :deep(.vuepress-markdown-body > :first-child) {
  margin-top: 0;
}

.answer-content :deep(.vuepress-markdown-body > :last-child) {
  margin-bottom: 0;
}

.answer-content :deep(.vuepress-markdown-body h1),
.answer-content :deep(.vuepress-markdown-body h2),
.answer-content :deep(.vuepress-markdown-body h3),
.answer-content :deep(.vuepress-markdown-body h4),
.answer-content :deep(.vuepress-markdown-body h5),
.answer-content :deep(.vuepress-markdown-body h6) {
  position: relative;
  margin-top: 1.05em;
  margin-bottom: 0.55em;
  padding: 0 0 0 0.72em;
  border: 0;
  color: #183d70;
  line-height: 1.45;
  letter-spacing: 0;
}

.answer-content :deep(.vuepress-markdown-body h1::before),
.answer-content :deep(.vuepress-markdown-body h2::before),
.answer-content :deep(.vuepress-markdown-body h3::before),
.answer-content :deep(.vuepress-markdown-body h4::before),
.answer-content :deep(.vuepress-markdown-body h5::before),
.answer-content :deep(.vuepress-markdown-body h6::before) {
  position: absolute;
  left: 0;
  top: 0.28em;
  bottom: 0.28em;
  width: 4px;
  border-radius: 999px;
  background: linear-gradient(180deg, #4b74d2, #58dec6);
  content: "";
}

.answer-content :deep(.vuepress-markdown-body h1) {
  font-size: 1.35rem;
}

.answer-content :deep(.vuepress-markdown-body h2) {
  font-size: 1.2rem;
}

.answer-content :deep(.vuepress-markdown-body h3) {
  font-size: 1.08rem;
}

.answer-content :deep(.vuepress-markdown-body h4),
.answer-content :deep(.vuepress-markdown-body h5),
.answer-content :deep(.vuepress-markdown-body h6) {
  font-size: 1rem;
}

.answer-content :deep(.vuepress-markdown-body pre),
.answer-content :deep(.vuepress-markdown-body table) {
  max-width: 100%;
  overflow-x: auto;
}

.answer-content :deep(.vuepress-markdown-body a) {
  color: #2f5eb4;
  font-weight: 700;
  text-decoration: underline;
  text-decoration-color: rgba(47, 94, 180, 0.28);
  text-decoration-thickness: 2px;
  text-underline-offset: 3px;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.answer-content :deep(.vuepress-markdown-body a:hover) {
  color: #163f88;
  text-decoration-color: rgba(22, 63, 136, 0.42);
}

.answer-content :deep(.vuepress-markdown-body img) {
  display: block;
  width: auto;
  max-width: 100%;
  max-height: min(520px, 70vh);
  margin: 1rem auto;
  border: 1px solid rgba(131, 156, 209, 0.18);
  border-radius: 10px;
  object-fit: contain;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 14px 30px rgba(16, 31, 64, 0.12);
}

.source-card ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.source-card li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(83, 111, 164, 0.16);
}

.source-card li:last-child {
  border-bottom: none;
}

.source-index {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: rgba(64, 109, 204, 0.12);
  color: #315aa4;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.source-card a {
  color: #2f5eb4;
  text-decoration: none;
  transition: color 0.2s ease;
  line-height: 1.6;
}

.source-card a:hover {
  color: #163f88;
}

@media (max-width: 768px) {
  .page-subtitle {
    letter-spacing: 0.03em;
  }

  .ai-hero,
  .qa-card,
  .answer-card,
  .source-card {
    padding: 15px;
  }

  .ai-hero h2 {
    font-size: 1.24rem;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: flex-start;
  }

  .source-card li {
    align-items: flex-start;
  }
}
</style>

<style>
[theme="dark"] .ai-hero {
  border-color: var(--home-border);
  background:
    radial-gradient(circle at 8% 16%, rgba(143, 207, 203, 0.16), transparent 35%),
    radial-gradient(circle at 85% 22%, rgba(213, 109, 88, 0.14), transparent 35%),
    linear-gradient(160deg, rgba(33, 40, 52, 0.96), rgba(28, 35, 46, 0.92));
  box-shadow: var(--home-shadow);
}

[theme="dark"] .ai-hero .hero-glow--left {
  background: rgba(143, 207, 203, 0.16);
}

[theme="dark"] .ai-hero .hero-glow--right {
  background: rgba(213, 109, 88, 0.14);
}

[theme="dark"] .ai-hero .hero-badge {
  background: var(--note-bg);
  color: var(--home-accent);
}

[theme="dark"] .ai-hero h2,
[theme="dark"] .card-title-row h3 {
  color: var(--grey-7);
}

[theme="dark"] .ai-hero .hero-desc,
[theme="dark"] .ai-hero .stat-label,
[theme="dark"] .ai-hero .hero-meter-text,
[theme="dark"] .card-title-tip {
  color: var(--grey-5);
}

[theme="dark"] .ai-hero .mode-switch {
  border-color: var(--home-border);
  background: rgba(20, 25, 34, 0.46);
}

[theme="dark"] .ai-hero .mode-switch__item {
  color: var(--grey-5);
}

[theme="dark"] .ai-hero .mode-switch__item.active {
  color: var(--grey-7);
  background: var(--note-bg);
  box-shadow: var(--home-shadow);
}

[theme="dark"] .ai-hero .stat-item,
[theme="dark"] .qa-card,
[theme="dark"] .answer-card,
[theme="dark"] .source-card {
  border-color: var(--home-border);
  background: var(--home-surface);
  box-shadow: var(--home-shadow), var(--home-glow);
}

[theme="dark"] .ai-hero .stat-value,
[theme="dark"] .answer-content {
  color: var(--grey-6);
}

[theme="dark"] .ai-hero .hero-meter-track {
  background: rgba(143, 207, 203, 0.14);
}

[theme="dark"] .ai-hero .hero-meter-fill {
  background: linear-gradient(90deg, var(--home-accent), var(--home-accent-warm));
}

[theme="dark"] .prompt-chip,
[theme="dark"] .ghost-btn,
[theme="dark"] .source-index {
  border-color: var(--home-border);
  background: var(--note-bg);
  color: var(--home-accent);
}

[theme="dark"] .prompt-chip:hover {
  background: var(--home-accent-soft);
}

[theme="dark"] .source-card li {
  border-bottom-color: rgba(143, 207, 203, 0.16);
}

[theme="dark"] .source-card a {
  color: var(--home-accent-cool);
}

[theme="dark"] .source-card a:hover {
  color: var(--home-accent);
}

[theme="dark"] .qa-card .el-textarea__inner {
  border-color: var(--home-border);
  background-color: rgba(24, 29, 38, 0.72);
  color: var(--grey-6);
  box-shadow: none;
}

[theme="dark"] .qa-card .el-textarea__inner::placeholder {
  color: var(--grey-5);
}

[theme="dark"] .qa-card .el-input__count {
  background: transparent;
  color: var(--grey-5);
}
</style>
