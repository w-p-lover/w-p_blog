<template>
  <n-popover
      trigger="click"
      placement="top-start"
      content-style="padding: 10px; background: rgba(31, 36, 45, 0.98); border: 1px solid rgba(218, 225, 214, 0.12); border-radius: 12px; box-shadow: 0 20px 46px rgba(0, 0, 0, 0.34);"
      :width="292"
      scrollable
  >
    <template #trigger>
      <span class="emoji-trigger"><svg-icon :icon-class= "emojiIco" size="1.25rem"></svg-icon></span>
    </template>
    <div class="emoji-content">
            <span class="emoji-item" v-for="(value, key, index) of emojiList" :key="index" @click="addEmoji(key)">
                <img :src="value" :title="key" class="emoji" width="26" height="26"/>
            </span>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
import emojiList from "@/utils/emoji";

const props = defineProps({
  emojiIco: {
    type: String,
    default: 'emoji'
  },
});

const emit = defineEmits(['addEmoji']);
const addEmoji = (key: string) => {
  emit("addEmoji", key);
};
</script>

<style lang="scss" scoped>
.emoji-trigger {
  width: 100%;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(225, 230, 221, 0.78);
}

.emoji-content {
  max-height: 212px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 5px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(210, 218, 211, 0.2);
    border-radius: 10px;
  }
}

.emoji-item {
  cursor: pointer;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: 0.18s ease;

  .emoji {
    user-select: none;
    display: block;
    vertical-align: middle;
  }

  &:hover {
    background: rgba(214, 224, 216, 0.12);
    transform: translateY(-1px);
  }
}
</style>
