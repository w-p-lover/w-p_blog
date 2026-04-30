<template>
  <div class="home">
    <el-container height="100%">
      <el-aside width="100px">
        <Nav></Nav>
      </el-aside>
      <el-main>
        <router-view to="/chatHome"></router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import Nav from "@/components/ChatHome/Chat/Nav.vue";
import useStore from "@/store";
import EventBus from "@/eventBus.ts";
import Waves from "@/components/Waves/index.vue";
const { user, app } = useStore();

export default {
  name: "App",
  components: {
    Nav,
  },
  mounted() {
    if (!user.id || user.id === "") {
      app.setLoginFlag(true);
    }
    EventBus.on("refresh-chat", () => {
      if(window.location.pathname === "/chat/ChatHome") {
        window.location.reload(); // 触发页面刷新
      }
    });
  },
};
</script>

<style lang="scss" scoped>
:deep(.el-container) {
  height: 100%;
}

:deep(.el-aside) {
  overflow: visible;
}

:deep(.el-main) {
  min-width: 0;
  padding: 0;
  overflow: hidden;
}

.home {
  width: min(1180px, 92vw);
  height: min(820px, 90vh);
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(43, 48, 61, 0.96), rgba(18, 21, 29, 0.98)),
    radial-gradient(circle at 12% 12%, rgba(116, 142, 150, 0.16), transparent 30%),
    radial-gradient(circle at 92% 88%, rgba(138, 112, 116, 0.14), transparent 34%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.42);
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

@media (max-width: 760px) {
  .home {
    width: 100vw;
    height: 100vh;
    border-radius: 0;
  }

  :deep(.el-aside) {
    width: 72px !important;
  }
}
</style>
