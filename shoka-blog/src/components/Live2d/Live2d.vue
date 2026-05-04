<template>
  <div class="live2d-host"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

import * as Live2DModule from 'live2d-render';

const initializeLive2D = Live2DModule.initializeLive2D || Live2DModule.default;
const setLive2DMessage = Live2DModule.setMessageBox;

const route = useRoute();
let removeGlobalPointerFollow = null;

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

const showRouteMessage = (path, duration = 5000) => {
  if (typeof setLive2DMessage !== 'function') return;
  const regex = /\/(\d+)$/;
  const message = pageMessages[path.replace(regex, '/:id')];
  if (message) setLive2DMessage(message, duration);
};

const setupRuntimeLayerBehavior = () => {
  const canvas = document.getElementById('live2d-canvas');
  if (!canvas) return null;

  canvas.style.pointerEvents = 'none';

  const toolbox = document.getElementById('live2d-toolbox') || document.querySelector('.live2d-toolbox');
  if (toolbox) toolbox.style.pointerEvents = 'auto';

  const messageBox = document.getElementById('live2dMessageBox');
  if (messageBox) messageBox.style.pointerEvents = 'auto';

  const messageContent = document.getElementById('live2dMessageBox-content');
  if (messageContent) messageContent.style.pointerEvents = 'auto';

  return canvas;
};

const setupGlobalPointerFollow = (canvas) => {
  if (!canvas) return;

  const forwardPointer = (event) => {
    const rect = canvas.getBoundingClientRect();
    const normalizedX = Math.min(Math.max(event.clientX / window.innerWidth, 0), 1);
    const normalizedY = Math.min(Math.max(event.clientY / window.innerHeight, 0), 1);
    const mappedClientX = rect.left + rect.width * normalizedX;
    const mappedClientY = rect.top + rect.height * normalizedY;

    if (typeof canvas.onmousemove === 'function') {
      canvas.onmousemove({
        target: canvas,
        clientX: mappedClientX,
        clientY: mappedClientY
      });
      return;
    }

    canvas.dispatchEvent(new MouseEvent('mousemove', {
      clientX: mappedClientX,
      clientY: mappedClientY,
      bubbles: true,
      cancelable: true,
      view: window
    }));
  };

  window.addEventListener('mousemove', forwardPointer, { passive: true });
  window.addEventListener('pointermove', forwardPointer, { passive: true });
  removeGlobalPointerFollow = () => {
    window.removeEventListener('mousemove', forwardPointer);
    window.removeEventListener('pointermove', forwardPointer);
  };
};

watch(() => route.path, (newPath) => {
  showRouteMessage(newPath, 5000);
});

onMounted(async () => {
  if (!initializeLive2D || typeof initializeLive2D !== 'function') {
    console.error('initializeLive2D 未导入成功，请检查 live2d-render 版本或导入方式！');
    return;
  }

  try {
    await initializeLive2D({
      CanvasId: 'live2d-canvas',
      BackgroundRGBA: [0, 0, 0, 0],
      ResourcesPath: '/whitecatfree_vts/SDwhite_cat_free.model3.json',
      CanvasSize: { width: 300, height: 400 },
      ShowToolBox: true,
      LoadFromCache: true,
      Scale: 0.5,
      Offset: { x: 0, y: 0 }
    });

    const canvas = setupRuntimeLayerBehavior();
    setupGlobalPointerFollow(canvas);
    showRouteMessage(route.path, 5000);

    console.log('Live2D 模型加载完成');
  } catch (err) {
    console.error('Live2D 初始化失败：', err);
  }
});

onBeforeUnmount(() => {
  if (removeGlobalPointerFollow) removeGlobalPointerFollow();
});
</script>

<style>
.live2d-host {
  display: none;
}

#live2d-canvas {
  pointer-events: none !important;
}

#live2d-toolbox,
.live2d-toolbox,
#live2dMessageBox,
#live2dMessageBox-content {
  pointer-events: auto !important;
}

#live2dMessageBox-content {
  position: relative;
  overflow: hidden;
  font-family: "新宋体", "STSong", serif;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.45;
  width: 308px;
  max-width: min(76vw, 308px);
  padding: 11px 18px;
  border-radius: 18px;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  word-wrap: break-word;
  backdrop-filter: blur(7px) saturate(1.06);
  -webkit-backdrop-filter: blur(7px) saturate(1.06);
  border: 1px solid rgba(226, 202, 168, 0.55);
  box-shadow:
    0 12px 30px rgba(128, 92, 58, 0.18),
    inset 0 1px 0 rgba(255, 252, 245, 0.72);
  opacity: 0;
  transform: translateY(12px) scale(0.985);
  transition: all 0.42s cubic-bezier(0.22, 0.78, 0.26, 1);
  background:
    linear-gradient(145deg, rgba(255, 248, 236, 0.95), rgba(247, 233, 209, 0.82));
  color: #6a4c32;
}

#live2dMessageBox-content.live2dMessageBox-content-visible {
  opacity: 1;
  transform: translateY(0) scale(1);
  background:
    linear-gradient(148deg, rgba(255, 244, 225, 0.94), rgba(245, 224, 187, 0.84));
  color: #5b3f26;
  border: 1px solid rgba(210, 172, 123, 0.62);
}

#live2dMessageBox-content.live2dMessageBox-content-hidden {
  opacity: 0;
  transform: translateY(12px) scale(0.985);
}

#live2dMessageBox-content::before {
  content: "";
  position: absolute;
  inset: 1px;
  border-radius: 17px;
  pointer-events: none;
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0));
}

#live2dMessageBox-content:hover {
  transform: translateY(-1px) scale(1.01);
  box-shadow:
    0 16px 36px rgba(120, 84, 52, 0.25),
    inset 0 1px 0 rgba(255, 252, 246, 0.8);
  border-color: rgba(196, 151, 92, 0.74);
}

@media (max-width: 768px) {
  #live2d-canvas,
  #live2d-toolbox,
  .live2d-toolbox,
  .__live2d-toolbox-item,
  .__live2d-toolbox-item.button-item,
  .__live2d-toolbox-item.expression-item,
  #live2dMessageBox {
    display: none !important;
  }

  body > div[style*="right: 265px"][style*="z-index: 10000"] {
    display: none !important;
  }
}
</style>
