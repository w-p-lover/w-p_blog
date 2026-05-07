<template>
  <main class="message-page">
    <section class="message-hero">
      <div class="message-heading">
        <span class="message-kicker">Guestbook</span>
        <h1 class="message-title">留言板</h1>
      </div>

      <form class="message-input" @submit.prevent="send">
        <input
          v-model="messageContent"
          class="input"
          maxlength="120"
          placeholder="写一张便签留在这里"
        />
        <button class="send" type="submit">贴上去</button>
      </form>
    </section>

    <section class="notes-wall" aria-label="留言便签墙">
      <article
        v-for="(message, index) in messageList"
        :key="message.localId || message.id || index"
        class="note-card"
        :class="[message.tone, message.noteStyle, message.fastener, { 'is-new': message.isNew }]"
        :style="{
          '--note-rotate': message.rotate + 'deg',
          '--note-delay': message.delay + 'ms',
        }"
      >
        <div class="note-fastener"></div>
        <div class="note-author">
          <img class="note-avatar" :src="message.avatar" :alt="message.nickname" />
          <div>
            <strong>{{ message.nickname }}</strong>
            <span>#{{ String(index + 1).padStart(2, "0") }}</span>
          </div>
        </div>
        <p class="note-content">{{ message.messageContent }}</p>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { addMessage, getMessageList } from "@/api/message";
import type { Message, MessageForm } from "@/api/message/types";
import useStore from "@/store";

type NoteTone = "tone-mint" | "tone-paper" | "tone-honey" | "tone-coral";
type NoteStyle = "note-plain" | "note-strip" | "note-postcard" | "note-clipped";
type NoteFastener = "with-tape" | "with-pin" | "with-clip";

interface NoteMessage extends Message {
  localId?: string;
  tone: NoteTone;
  noteStyle: NoteStyle;
  fastener: NoteFastener;
  rotate: number;
  delay: number;
  isNew?: boolean;
}

const { blog, user } = useStore();
const messageContent = ref("");
const messageList = ref<NoteMessage[]>([]);

const noteTones: NoteTone[] = ["tone-mint", "tone-paper", "tone-honey", "tone-coral"];
const noteStyles: NoteStyle[] = [
  "note-plain",
  "note-plain",
  "note-postcard",
  "note-plain",
  "note-strip",
  "note-clipped",
  "note-plain",
];
const noteFasteners: NoteFastener[] = ["with-tape", "with-pin", "with-tape", "with-tape"];
const noteRotations = [-1.6, 1.1, -0.7, 1.4, -1, 0.6];

const decorateMessage = (message: Message, index: number, isNew = false): NoteMessage => ({
  ...message,
  tone: noteTones[index % noteTones.length],
  noteStyle: noteStyles[index % noteStyles.length],
  fastener:
    noteStyles[index % noteStyles.length] === "note-clipped"
      ? "with-clip"
      : noteFasteners[index % noteFasteners.length],
  rotate: noteRotations[index % noteRotations.length],
  delay: Math.min(index * 18, 240),
  isNew,
});

onMounted(async () => {
  try {
    const { data } = await getMessageList();
    messageList.value = data.data.map((message, index) => decorateMessage(message, index));
  } catch {
    messageList.value = [];
    window.$message?.warning("留言加载失败，请稍后刷新重试");
  }
});

const buildMessage = (): MessageForm => ({
  avatar: user.avatar || blog.blogInfo.siteConfig.touristAvatar,
  nickname: user.nickname || "游客",
  messageContent: messageContent.value.trim(),
});

const send = () => {
  if (!messageContent.value.trim()) {
    window.$message?.warning("留言内容不能为空");
    return;
  }

  const message = buildMessage();
  addMessage(message)
    .then(({ data }) => {
      if (data.flag) {
        if (!blog.blogInfo.siteConfig.messageCheck) {
          const note = decorateMessage(
            {
              id: Date.now(),
              ...message,
            },
            messageList.value.length,
            true
          );
          note.localId = `local-${note.id}`;
          messageList.value.unshift(note);
          window.$message?.success("留言成功");
        } else {
          window.$message?.warning("留言成功，正在审核中");
        }
        messageContent.value = "";
      }
    })
    .catch(() => {
      window.$message?.error("留言发送失败，请稍后再试");
    });
};
</script>

<style scoped lang="scss">
.message-page {
  position: relative;
  min-height: 100vh;
  padding: 6.25rem clamp(1rem, 4vw, 4rem) 4.5rem;
  overflow-x: hidden;
  color: var(--text-color);
  background:
    radial-gradient(circle at 15% 12%, rgba(85, 162, 160, 0.16), transparent 28rem),
    radial-gradient(circle at 86% 24%, rgba(200, 85, 64, 0.12), transparent 24rem),
    linear-gradient(rgba(85, 162, 160, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(85, 162, 160, 0.045) 1px, transparent 1px),
    var(--grey-0);
  background-size: auto, auto, 2.25rem 2.25rem, 2.25rem 2.25rem, auto;
}

.message-page::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(120deg, rgba(255, 255, 255, 0.36), transparent 42%),
    repeating-linear-gradient(
      0deg,
      rgba(124, 119, 112, 0.045) 0,
      rgba(124, 119, 112, 0.045) 1px,
      transparent 1px,
      transparent 9px
    );
  mix-blend-mode: multiply;
}

.message-hero {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(10rem, 15rem) minmax(18rem, 30rem);
  gap: 1.5rem;
  align-items: center;
  max-width: 72rem;
  margin: 0 auto 2.35rem;
}

.message-kicker {
  display: inline-flex;
  align-items: center;
  margin-bottom: 0.35rem;
  color: var(--home-accent-warm);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.message-kicker::before {
  content: "";
  width: 2.2rem;
  height: 1px;
  margin-right: 0.65rem;
  background: var(--home-accent-warm);
}

.message-title {
  margin: 0;
  color: var(--home-title);
  font-size: clamp(1.85rem, 3.6vw, 3.25rem);
  line-height: 1;
  font-weight: 800;
  letter-spacing: 0;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.76);
  animation: titleScale 0.8s ease both;
}

.message-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
  padding: 0.45rem;
  border: 1px solid var(--home-border);
  border-radius: 8px;
  background: var(--home-surface);
  box-shadow: var(--home-shadow), var(--home-glow);
  backdrop-filter: blur(14px);
}

.input {
  min-width: 0;
  height: 2.65rem;
  padding: 0 0.85rem;
  border: 1px solid transparent;
  border-radius: 6px;
  outline: none;
  color: var(--text-color);
  background: rgba(255, 254, 250, 0.76);
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.input::placeholder {
  color: var(--home-muted);
}

.input:focus {
  border-color: var(--home-border-strong);
  background: var(--home-surface-strong);
  box-shadow: 0 0 0 3px var(--home-accent-soft);
}

.send {
  height: 2.65rem;
  padding: 0 1rem;
  border: 0;
  border-radius: 6px;
  color: var(--grey-1);
  font-weight: 700;
  background: linear-gradient(135deg, var(--home-accent), var(--home-accent-cool));
  box-shadow: 0 8px 18px rgba(85, 162, 160, 0.26);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.send:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(85, 162, 160, 0.3);
  filter: saturate(1.06);
}

.notes-wall {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr));
  gap: 1.1rem;
  max-width: 72rem;
  margin: 0 auto;
  padding: 1.35rem;
  border: 1px solid rgba(200, 181, 137, 0.34);
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(122, 89, 48, 0.035) 1px, transparent 1px),
    linear-gradient(rgba(122, 89, 48, 0.035) 1px, transparent 1px),
    rgba(255, 252, 242, 0.44);
  background-size: 1.4rem 1.4rem;
  box-shadow: 0 18px 34px rgba(89, 78, 56, 0.06) inset;
}

.note-card {
  position: relative;
  min-width: 0;
  min-height: 9.4rem;
  padding: 1.1rem 1rem 1rem;
  border: 1px solid rgba(227, 223, 212, 0.8);
  border-radius: 5px;
  transform: rotate(var(--note-rotate));
  box-shadow:
    0 12px 20px rgba(89, 78, 56, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.76) inset;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
  animation: noteSettle 0.46s ease both;
  animation-delay: var(--note-delay);
}

.note-card:hover {
  z-index: 20;
  transform: translateY(-0.2rem) rotate(0deg);
  box-shadow:
    0 16px 26px rgba(89, 78, 56, 0.12),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
}

.note-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background:
    linear-gradient(rgba(255, 255, 255, 0.23) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.16) 1px, transparent 1px);
  background-size: 1rem 1rem;
  opacity: 0.28;
}

.tone-mint {
  background: linear-gradient(145deg, #e8f3ef, #f9fbf7);
}

.tone-paper {
  background: linear-gradient(145deg, #fffefa, #f7f4ed);
}

.tone-honey {
  background: linear-gradient(145deg, #fff5cf, #fffefa);
}

.tone-coral {
  background: linear-gradient(145deg, #f9e5dd, #fff8f2);
}

.note-plain {
  min-height: 9.4rem;
}

.note-strip {
  min-height: 7.7rem;
  border-left: 4px solid rgba(85, 162, 160, 0.42);
}

.note-strip .note-content {
  -webkit-line-clamp: 3;
}

.note-postcard {
  min-height: 9.2rem;
  padding-right: 1.15rem;
}

.note-postcard::after {
  content: "";
  position: absolute;
  right: 0.88rem;
  bottom: 0.82rem;
  width: 2.1rem;
  height: 2.1rem;
  border: 1px dashed rgba(200, 85, 64, 0.34);
  border-radius: 50%;
  opacity: 0.72;
  transform: rotate(-12deg);
}

.note-clipped {
  min-height: 9.8rem;
  padding-top: 1.25rem;
}

.note-clipped::after {
  content: "";
  position: absolute;
  left: 1rem;
  right: 1rem;
  top: 0.92rem;
  height: 1px;
  background: rgba(85, 162, 160, 0.16);
}

.note-fastener {
  position: absolute;
  pointer-events: none;
}

.with-pin .note-fastener {
  top: 0.55rem;
  right: 0.75rem;
  width: 0.58rem;
  height: 0.58rem;
  border: 2px solid rgba(255, 255, 255, 0.72);
  border-radius: 50%;
  background: var(--home-accent-warm);
  box-shadow: 0 2px 6px rgba(120, 77, 58, 0.22);
}

.with-tape .note-fastener {
  top: -0.42rem;
  left: 50%;
  width: 2.8rem;
  height: 0.82rem;
  border-radius: 2px;
  background: rgba(255, 252, 231, 0.62);
  box-shadow: 0 2px 5px rgba(120, 110, 85, 0.09);
  transform: translateX(-50%) rotate(-2deg);
}

.with-clip .note-fastener {
  top: -0.36rem;
  right: 1rem;
  width: 1rem;
  height: 1.55rem;
  border: 2px solid rgba(85, 162, 160, 0.48);
  border-bottom: 0;
  border-radius: 0.58rem 0.58rem 0 0;
  transform: rotate(6deg);
}

.note-author {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
  margin-bottom: 0.68rem;
}

.note-avatar {
  width: 2.1rem;
  height: 2.1rem;
  flex: 0 0 auto;
  border: 3px solid rgba(255, 255, 255, 0.82);
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 6px 14px rgba(89, 78, 56, 0.14);
}

.note-author strong,
.note-author span {
  display: block;
  max-width: 8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-author strong {
  color: var(--home-title);
  font-size: 0.82rem;
}

.note-author span {
  margin-top: 0.08rem;
  color: var(--home-muted);
  font-size: 0.64rem;
}

.note-content {
  position: relative;
  z-index: 1;
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: #4a443d;
  font-size: 0.88rem;
  line-height: 1.65;
  word-break: break-word;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.is-new {
  animation: notePop 0.46s cubic-bezier(0.2, 1.35, 0.32, 1) both;
}

@keyframes notePop {
  from {
    opacity: 0;
    transform: translateY(0.8rem) scale(0.96) rotate(-1.5deg);
  }
}

@keyframes noteSettle {
  from {
    opacity: 0;
    transform: translateY(0.45rem) rotate(calc(var(--note-rotate) - 0.8deg));
  }
}

@media (max-width: 720px) {
  .message-page {
    padding: 5.8rem 1rem 3rem;
  }

  .message-hero {
    grid-template-columns: 1fr;
    gap: 1rem;
    margin-bottom: 1.5rem;
  }

  .message-title {
    font-size: 2.35rem;
  }

  .message-input {
    grid-template-columns: 1fr;
  }

  .send {
    width: 100%;
  }

  .notes-wall {
    grid-template-columns: 1fr;
    gap: 0.95rem;
    padding: 0.9rem;
  }

  .note-card,
  .note-card:hover {
    transform: none;
  }
}
</style>

<style lang="scss">
[theme="dark"] .message-page {
  background:
    radial-gradient(circle at 15% 12%, rgba(143, 207, 203, 0.16), transparent 28rem),
    radial-gradient(circle at 86% 24%, rgba(213, 109, 88, 0.13), transparent 24rem),
    linear-gradient(rgba(143, 207, 203, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(143, 207, 203, 0.05) 1px, transparent 1px),
    var(--grey-0);
}

[theme="dark"] .message-page::before {
  mix-blend-mode: screen;
  opacity: 0.42;
}

[theme="dark"] .message-title {
  text-shadow: none;
}

[theme="dark"] .message-input {
  background: rgba(33, 40, 52, 0.76);
}

[theme="dark"] .message-input .input {
  border-color: rgba(118, 154, 148, 0.2);
  background: rgba(29, 35, 46, 0.7);
}

[theme="dark"] .message-input .send {
  color: #142223;
}

[theme="dark"] .notes-wall {
  border-color: var(--home-border);
  background:
    linear-gradient(90deg, rgba(143, 207, 203, 0.04) 1px, transparent 1px),
    linear-gradient(rgba(143, 207, 203, 0.04) 1px, transparent 1px),
    rgba(29, 35, 46, 0.46);
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.18) inset;
}

[theme="dark"] .notes-wall .note-card {
  border-color: rgba(118, 154, 148, 0.26);
  box-shadow:
    0 14px 24px rgba(0, 0, 0, 0.3),
    0 1px 0 rgba(255, 255, 255, 0.08) inset;
}

[theme="dark"] .notes-wall .tone-mint,
[theme="dark"] .notes-wall .tone-paper,
[theme="dark"] .notes-wall .tone-honey,
[theme="dark"] .notes-wall .tone-coral {
  background: linear-gradient(145deg, rgba(37, 45, 59, 0.94), rgba(28, 35, 46, 0.9));
}

[theme="dark"] .notes-wall .note-content {
  color: rgba(246, 248, 255, 0.82);
}

[theme="dark"] .notes-wall .note-strip {
  border-left-color: rgba(143, 207, 203, 0.42);
}

[theme="dark"] .notes-wall .note-postcard::after {
  border-color: rgba(213, 109, 88, 0.42);
}
</style>
