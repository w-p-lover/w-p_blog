<template>
  <!-- Live2D 模型容器 -->
  <div id="live2d-container" class="live2d-container">
    <!-- Canvas 会自动插入或手动创建 -->
    <canvas id="live2d-canvas"></canvas>

    <!-- 自定义消息框 -->
    <div id="live2dMessageBox" class="message-box">
      <div
          id="live2dMessageBox-content"
          :class="{'message-content-visible': isMessageVisible, 'message-content-hidden': !isMessageVisible}"
      >
        {{ currentMessage }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

// 兼容 import 方式
import * as Live2DModule from 'live2d-render';
const initializeLive2D = Live2DModule.initializeLive2D || Live2DModule.default;

const isMessageVisible = ref(false);
const currentMessage = ref('');
const messageTimer = ref(null);
const route = useRoute();

// 页面消息配置
const pageMessages = {
  '/': '愿您在此寻得片刻安宁。',
  '/message': '笔墨传情，静待您的留言。',
  '/chat': '此处可畅所欲言，以言会友。',
  '/chat/ChatHome': '聊天主页，期待您的分享。',
  '/about': '关于我们，些许薄见，望君雅正。',
  '/category': '万物有序，分类而聚，便于君寻。',
  '/category/:id': '此分类下的文章，或合您意。',
  '/tag': '标签汇聚，皆是心意所向。',
  '/tag/:id': '此标签下的内容，盼能共鸣。',
  '/friend': '友链相连，皆是同道中人。',
  '/archive': '时光归档，每一刻都值得回味。',
  '/user': '个人中心，记录您的点滴足迹。',
  '/album': '相册集萃，定格美好瞬间。',
  '/album/:id': '这些照片，藏着一段故事。',
  '/talk': '闲言碎语，皆是生活感悟。',
  '/talk/:id': '这则说说，道尽当时心境。',
  '/article/:id': '此篇文章，愿能与君共勉。',
  '/404': '页面暂未寻得，不妨换条路径。',
  '/weather': '天气多云转晴，爱会一直放晴',
  '/book': '看书，看的就是书，我看看看看',
  '/oauth/login/gitee': '正在通过Gitee登录，稍候片刻。',
  '/oauth/login/github': '正在通过Github登录，稍候片刻。'
};

const setMessageBox = (message, duration = 3000) => {
  currentMessage.value = message;
  isMessageVisible.value = true;
  if (messageTimer.value) clearTimeout(messageTimer.value);
  messageTimer.value = setTimeout(() => (isMessageVisible.value = false), duration);
};

// 监听路由变化
watch(() => route.path, (newPath) => {
  const regex = /\/(\d+)$/;
  const newMessage = pageMessages[newPath.replace(regex, '/:id')];
  if (newMessage) setMessageBox(newMessage, 5000);
});

onMounted(async () => {
  const container = document.getElementById('live2d-container');
  if (!container) {
    console.error('Live2D 容器不存在！');
    return;
  }

  if (!initializeLive2D || typeof initializeLive2D !== 'function') {
    console.error('initializeLive2D 未导入成功，请检查 live2d-render 版本或导入方式！');
    return;
  }

  try {
    // 初始化 Live2D
    await initializeLive2D({
      Container: container,
      CanvasId: 'live2d-canvas',
      BackgroundRGBA: [0, 0, 0, 0],
      ResourcesPath: '/whitecatfree_vts/SDwhite_cat_free.model3.json', // 注意斜杠
      CanvasSize: { width: 300, height: 400 },
      ShowToolBox: false,
      LoadFromCache: true,
      Scale: 0.5,
      Offset: { x: 0, y: 0 }
    });

    // 当前页面显示消息
    const regex = /\/(\d+)$/;
    const currentPageMessage = pageMessages[route.path.replace(regex, '/:id')];
    if (currentPageMessage) setMessageBox(currentPageMessage, 5000);

    console.log('Live2D 模型加载完成');
  } catch (err) {
    console.error('Live2D 初始化失败：', err);
  }
});
</script>


<style scoped>
.live2d-container {
  position: fixed;
  z-index: 999;
  pointer-events: auto;
  right: -105px;
  bottom: 0;
  width: 340px;
  height: 400px;
}

.message-box {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(100%);
  max-width: 100%;
  padding: 8px 0;
  z-index: 10;
}

#live2dMessageBox-content {
  font-family: "新宋体", sans-serif;
  font-size: 14px;
  font-weight: bold;
  padding: 10px 18px;
  border-radius: 20px;
  width: 300px;
  word-wrap: break-word;
  display: flex;
  justify-content: center
}

.message-content-hidden {
  opacity: 0;
  transform: translate(-50%, 15px);
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  background-color: rgba(255, 255, 255, 0.92);
  color: #333;
}

.message-content-visible {
  opacity: 1;
  transform: translate(-50%, 0);
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  background-color: rgba(202, 201, 201, 0.51);
  color: #234161;
  border: 1px solid rgba(220, 220, 220, 0.3);
}

#live2dMessageBox-content:hover {
  box-shadow: 0 5px 20px rgba(2, 13, 85, 0.2);
  transition: all 0.3s ease;
}
</style>