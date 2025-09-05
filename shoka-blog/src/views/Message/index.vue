<template>
  <div class="message-container">
    <h1 class="message-title">留言板</h1>
    <div class="message-input">
      <input
          class="input"
          v-model="messageContent"
          @click="show = true"
          @keyup.enter="send"
          placeholder="说点什么吧"
      />
      <button class="send" @click="send" v-show="show">发送</button>
    </div>
  </div>

  <div class="danmaku-container" ref="danmakuContainer">
    <img class="background" src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-d6eq6o_5640x2400.png" />
    <div
        v-for="(dm, index) in messageList"
        :key="index"
        class="danmaku-item"
        :style="{
        left: dm.x + 'px',
        top: dm.top + 'px',
      }"
        @mouseenter="dm.paused = true"
        @mouseleave="dm.paused = false"
    >
      <img :src="dm.avatar" width="35" height="35" style="border-radius:50%" />
      <span class="ml">{{ dm.nickname }} :</span>
      <span class="ml">{{ dm.messageContent }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { addMessage, getMessageList } from "@/api/message";
import useStore from "@/store";

const { blog, user } = useStore();
const messageContent = ref("");
const show = ref(false);
const messageList = ref<any[]>([]);
const danmakuContainer = ref<HTMLElement | null>(null);

const getRandomSpeed = () => Math.random() + Math.random() * 1.5 + 1;

const getRandomTop = () => {
  const containerHeight = danmakuContainer.value?.offsetHeight || 500;
  const margin = 50;
  const avoidTop = containerHeight * 0.35;
  const avoidBottom = containerHeight * 0.5;

  // 随机选择在上区或下区
  if (Math.random() > 0.5) {
    return Math.random() * (avoidTop - margin) + margin;
  } else {
    return Math.random() * (containerHeight - avoidBottom - margin ) + avoidBottom;
  }
};


/** 初始化弹幕 */
onMounted(async () => {
  const { data } = await getMessageList();
  messageList.value = data.data.map((msg) => ({
    ...msg,
    x: -200,
    top: getRandomTop(),
    speed: getRandomSpeed(),
    paused: false,
  }));
  animateDanmaku();
});

/** 发送弹幕 */
const send = () => {
  if (!messageContent.value.trim()) {
    window.$message?.warning("留言内容不能为空");
    return;
  }
  const message = {
    avatar: user.avatar || blog.blogInfo.siteConfig.touristAvatar,
    nickname: user.nickname || "游客",
    messageContent: messageContent.value,
    x: -200,
    top: getRandomTop(),
    speed: getRandomSpeed(),
    paused: false,
  };
  addMessage(message).then(({ data }) => {
    if (data.flag) {
      if (!blog.blogInfo.siteConfig.messageCheck) {
        messageList.value.push(message);
        window.$message?.success("留言成功");
      } else {
        window.$message?.warning("留言成功，正在审核中");
      }
      messageContent.value = "";
    }
  });
};

/** 弹幕动画循环 */
const animateDanmaku = () => {
  const step = () => {
    const width = danmakuContainer.value?.offsetWidth || window.innerWidth;
    messageList.value.forEach((dm) => {
      if (!dm.paused) {
        dm.x -= dm.speed;
        if (dm.x < -200) dm.x = width; // 到左边界回到右边

      }
    });
    requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
};
</script>

<style scoped lang="scss">
.message-container {
  position: fixed;
  top: 35%;
  left: 0;
  right: 0;
  width: 22.5rem;
  margin: 0 auto;
  text-align: center;
  color: #ffffff;
  z-index: 5;
}

.message-title {
  animation: titleScale 1s;
}

.message-input {
  display: flex;
  justify-content: center;
  height: 2.5rem;
  margin-top: 2rem;
}

.message-input .input {
  width: 70%;
  height: 100%;
  border-radius: 1.25rem;
  padding: 0 1.25rem;
  outline: none;
  color: #eee;
  border: #fff 1px solid;
}

.message-input .input::-webkit-input-placeholder {
  color: #eeee;
}

.message-input .send {
  height: 100%;
  padding: 0 1.25rem;
  color: #eee;
  border: #fff 1px solid;
  border-radius: 20px;
  outline: none;
  animation: slideUpIn 0.3s;
}

.danmaku-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  overflow: hidden;
}

.background {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(85%);
  z-index: 0;
}

.danmaku-item {
  display: flex;
  align-items: center;
  padding: 2px 20px;
  border-radius: 1.5rem;
  background-color: rgba(141, 149, 148, 0.18);
  color: #fff;
  position: absolute;
  white-space: nowrap;
  font-size: 17px;
  pointer-events: auto;
  z-index: 1;
}

.ml {
  margin-left: 0.5rem;
}
</style>
