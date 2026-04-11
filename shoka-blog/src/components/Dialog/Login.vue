<template>
  <n-modal
      class="login-modal-wrapper"
      v-model:show="dialogVisible"
      :show-icon="false"
      transform-origin="center"
      :block-scroll="false"
  >
    <!-- 核心修复：将Modal内所有内容包裹在单个根div中 -->
    <div class="modal-inner-wrapper">
      <!-- 验证码遮罩 -->
      <div v-if="showCaptcha" id="captcha-overlay" @click="closeCaptcha">
        <div id="captcha-box" ref="captchaBox"></div>
      </div>

      <!-- 核心玻璃拟态容器 -->
      <div class="container" :class="{ 'register-active': app.registerFlag }">
        <!-- 表单区域（登录+注册） -->
        <div class="form-wrap">
          <!-- 登录表单（包含账号/社交登录切换） -->
          <div class="login-form">
            <div class="title">登入W&P</div>

            <!-- 账号/社交登录tab切换 -->
            <div class="tab-switch">
              <button class="tab-btn" :class="{active: activeSlide === 0}" @click="activeSlide = 0">账号登录</button>
              <button class="tab-btn" :class="{active: activeSlide === 1}" @click="activeSlide = 1">社交登录</button>
            </div>

            <!-- 滑块容器（账号/社交登录） -->
            <div class="slider">
              <div class="slides" :style="{ transform: `translateX(-${activeSlide * 50}%)` }">
                <!-- 账号登录表单 -->
                <div class="slide">
                  <div class="input-group">
                    <n-input
                        v-model:value="loginForm.username"
                        placeholder="邮箱号"
                        @keyup.enter="handleCaptcha"
                        :style="inputStyle"
                    />
                  </div>
                  <div class="input-group">
                    <n-input
                        v-model:value="loginForm.password"
                        type="password"
                        show-password-on="click"
                        placeholder="密码"
                        @keyup.enter="handleCaptcha"
                        :style="inputStyle"
                    />
                  </div>
                  <n-button
                      class="btn login-btn"
                      :loading="loginLoading"
                      @click="handleCaptcha"
                      :style="btnStyle"
                  >
                    登 录
                  </n-button>
                  <div class="mt-10 login-tip">
                    <span class="colorFlag" @click="handleForget">忘记密码?</span>
                  </div>
                </div>

                <!-- 社交登录表单 -->
                <div class="slide social-slide">
                  <div class="brand-title">欢迎回来</div>
                  <div class="brand-subtitle">选择你的社交账号快速登录</div>
                  <div class="social-login-wrapper">
                    <svg-icon class="icon" icon-class="qq" size="2rem" color="#00aaee"></svg-icon>
                    <svg-icon class="icon" icon-class="gitee" size="2rem" v-if="showLogin('gitee')" @click="giteeLogin"></svg-icon>
                    <svg-icon class="icon" icon-class="github" size="2rem" v-if="showLogin('github')" @click="githubLogin"></svg-icon>
                  </div>
                </div>
              </div>
            </div>

            <!-- 账号/社交登录切换圆点 -->
            <div class="nav-dots">
              <span class="dot" :class="{active: activeSlide === 0}" @click="activeSlide = 0"></span>
              <span class="dot" :class="{active: activeSlide === 1}" @click="activeSlide = 1"></span>
            </div>
          </div>

          <!-- 注册表单（完整功能 + 玻璃拟态样式） -->
          <div class="register-form">
            <div class="title">创建账号</div>

            <!-- 邮箱输入框 -->
            <div class="input-group">
              <n-input
                  v-model:value="registerForm.username"
                  placeholder="邮箱号"
                  :style="inputStyle"
              />
            </div>

            <!-- 验证码输入组 -->
            <div class="input-group captcha-group">
              <n-input
                  placeholder="验证码"
                  v-model:value="registerForm.code"
                  :style="{
                  ...inputStyle,
                  width: '70%',
                  marginRight: '8px',
                  padding: '16px 18px'
                }"
              />
              <n-button
                  :disabled="flag"
                  @click="sendCode"
                  :style="{
                  width: '28%',
                  padding: '16px 0',
                  borderRadius: '16px',
                  border: '1px solid rgba(255,255,255,.4)',
                  background: 'linear-gradient(135deg,#49b1f5,#3a9ef0)',
                  color: '#fff',
                  backdropFilter: 'blur(6px)',
                  '&:disabled': {
                    background: 'rgba(73, 177, 245, 0.5)',
                    cursor: 'not-allowed'
                  }
                }"
              >
                {{ timer == 0 ? '发送' : `${timer}s` }}
              </n-button>
            </div>

            <!-- 密码输入框 -->
            <div class="input-group">
              <n-input
                  v-model:value="registerForm.password"
                  type="password"
                  show-password-on="click"
                  placeholder="密码"
                  :style="inputStyle"
              />
            </div>

            <!-- 注册按钮 -->
            <n-button
                ref="registerRef"
                class="btn register-btn"
                :loading="registerLoading"
                @click="handleRegister"
                :style="{
                ...btnStyle,
                background: 'linear-gradient(135deg,#e9546b,#d6485e)',
                boxShadow: '0 10px 30px rgba(233, 84, 107, 0.35)'
              }"
            >
              注册
            </n-button>

            <!-- 已有账号跳转 -->
            <div class="register-tip mt-10">
              <span class="dialog-text">已有账号？</span>
              <span class="colorFlag" @click="switchToLogin">登录</span>
            </div>
          </div>
        </div>

        <!-- 滑块标题面板（核心动画区域） -->
        <div class="title-panel">
          <div class="login-title">
            <h2>欢迎来到W&P</h2>
            <p class="hint-text">在安静与专注中，记录你的世界</p>
            <button class="btn title-btn" @click="switchToRegister">注册</button>
          </div>
          <div class="register-title">
            <div class="slogan">Stay Hungry</div>
            <div class="slogan">Stay Foolish</div>
            <p class="hint-text">很高兴再次见到你</p>
            <button class="btn title-btn" @click="switchToLogin">登录</button>
          </div>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, reactive, toRefs, nextTick } from "vue";
import { useRoute } from "vue-router";
import { useIntervalFn } from '@vueuse/core';
import { login, register, getCode } from "@/api/login";
import { LoginForm } from "@/api/login/types";
import { UserForm } from "@/model";
import config from "@/assets/js/config";
import useStore from "@/store";
import { setToken } from "@/utils/token";
import EventBus from "@/eventBus";

// Store 与路由
const { app, user, blog } = useStore();
const route = useRoute();

// 样式常量（玻璃拟态基础样式）
const inputStyle = {
  width: "100%",
  padding: "16px 18px",
  borderRadius: "16px",
  border: "1px solid rgba(255,255,255,.9)",
  background: "rgb(227,1,1)",
  color: "#fff",
  backdropFilter: "blur(10px)",
  fontSize: "15px"
};

const btnStyle = {
  width: "100%",
  padding: "16px",
  borderRadius: "18px",
  border: "1px solid rgba(255,255,255,.4)",
  color: "#fff",
  backdropFilter: "blur(6px)",
  transition: "transform 0.25s ease, box-shadow 0.25s ease"
};

// 登录相关响应式数据
const loginLoading = ref(false);
const activeSlide = ref(0); // 账号/社交登录切换
const loginForm = ref<LoginForm>({
  username: "",
  password: "",
});

// 注册相关响应式数据（完整复用原有注册逻辑）
const registerRef = ref();
const registerData = reactive({
  timer: 0,
  flag: false,
  registerLoading: false,
  registerForm: {
    username: "",
    password: "",
    code: "",
  } as UserForm,
});
const { timer, flag, registerLoading, registerForm } = toRefs(registerData);

// 验证码倒计时定时器
const { pause, resume } = useIntervalFn(() => {
  timer.value--;
  if (timer.value <= 0) {
    pause();
    flag.value = false;
  }
}, 1000, { immediate: false });

// 社交登录配置
const showLogin = computed(
    () => (type: string) => blog.blogInfo.siteConfig.loginList.includes(type)
);

// 弹窗显隐（关联登录/注册Flag）
const dialogVisible = computed({
  get: () => app.loginFlag || app.registerFlag,
  set: (value) => {
    app.setLoginFlag(value);
    app.setRegisterFlag(false);
  },
});

// ========== 注册核心逻辑 ==========
const start = (time: number) => {
  flag.value = true;
  timer.value = time;
  resume();
};

// 发送验证码
const sendCode = () => {
  const reg = /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
  if (!reg.test(registerForm.value.username)) {
    window.$message?.warning("邮箱格式不正确");
    return;
  }
  start(60);
  getCode(registerForm.value.username).then(({ data }) => {
    if (data.flag) {
      window.$message?.success("发送成功");
    }
  });
};

// 注册提交（含自动登录逻辑）
const handleRegister = () => {
  if (registerForm.value.password.trim().length < 6) {
    window.$message?.warning("密码不能少于6位");
    return;
  }
  registerLoading.value = true;
  register(registerForm.value).then(({ data }) => {
    if (data.flag) {
      const autoLoginForm: LoginForm = {
        username: registerForm.value.username,
        password: registerForm.value.password,
      };
      login(autoLoginForm).then(({ data }) => {
        if (data.flag) {
          // 重置注册表单
          registerForm.value = {
            username: "",
            password: "",
            code: "",
          };
          setToken(data.data);
          user.GetUserInfo();
          window.$message?.success("登录成功");
          app.setRegisterFlag(false);
        }
      });
    }
    registerLoading.value = false;
  });
};

// ========== 登录核心逻辑 ==========
// 验证码相关
const showCaptcha = ref(false);
const captchaBox = ref<HTMLElement | null>(null);
let globalTAC: any;
const captchaConfig = {
  requestCaptchaDataUrl: "http://localhost:8080/gen?type=RANDOM",
  validCaptchaUrl: "http://localhost:8080/check",
  bindEl: "#captcha-box",
  validSuccess: (res: any, c: any, tac: any) => {
    tac.destroyWindow();
    handleLoginSubmit(res.data.id);
  },
};
const captchaStyle = {
  logoUrl: "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img/202410311645295.png",
};

// 修复验证码关闭逻辑
const closeCaptcha = () => {
  const width = captchaBox.value?.offsetWidth;
  console.log("验证码容器宽度:", width);
  if (width === 0 || !width) {
    console.log('验证码框宽度为0，执行关闭操作');
    showCaptcha.value = false;
    // 销毁TAC实例
    if (globalTAC) {
      globalTAC.destroyWindow();
      globalTAC = null;
    }
  } else {
    console.log('验证码框宽度不为0，不关闭操作');
  }
};

// 修复TAC初始化逻辑（核心修复）
const handleCaptcha = () => {
  const reg = /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
  if (!reg.test(loginForm.value.username)) {
    window.$message?.warning("邮箱格式不正确");
    return;
  }
  if (loginForm.value.password.trim().length === 0) {
    window.$message?.warning("密码不能为空");
    return;
  }

  showCaptcha.value = true;

  // 先销毁旧实例
  if (globalTAC) {
    globalTAC.destroyWindow();
    globalTAC = null;
  }

  // 等待DOM完全渲染后再初始化TAC（替换setTimeout为nextTick）
  nextTick(() => {
    // 校验容器是否存在
    const captchaEl = document.querySelector("#captcha-box");
    if (!captchaEl) {
      window.$message?.error("验证码容器加载失败，请重试");
      showCaptcha.value = false;
      return;
    }
    // 校验容器尺寸
    const rect = captchaEl.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) {
      window.$message?.error("验证码容器尺寸异常，请重试");
      showCaptcha.value = false;
      return;
    }
    // 初始化TAC
    try {
      // @ts-ignore
      globalTAC = new TAC(captchaConfig, captchaStyle).init();
    } catch (e) {
      console.error("TAC初始化失败:", e);
      window.$message?.error("验证码加载失败，请刷新页面重试");
      showCaptcha.value = false;
    }
  });
};

// 登录请求
const handleLoginSubmit = (token: string) => {
  loginLoading.value = true;
  login(loginForm.value)
      .then(({ data }) => {
        if (data.flag) {
          setToken(data.data);
          user.GetUserInfo();
          window.$message?.success("登录成功");
          loginForm.value = { username: "", password: "" };
          app.setLoginFlag(false);
        }
        showCaptcha.value = false;
        loginLoading.value = false;
        EventBus.emit("refresh-articles");
        EventBus.emit("refresh-chat");
      })
      .catch((err) => {
        console.error("登录失败:", err);
        window.$message?.error("登录失败，请检查账号密码");
        loginLoading.value = false;
        showCaptcha.value = false;
      });
};

// ========== 切换逻辑 ==========
// 切换到注册
const switchToRegister = () => {
  app.setLoginFlag(false);
  app.setRegisterFlag(true);
};

// 切换到登录
const switchToLogin = () => {
  app.setRegisterFlag(false);
  app.setLoginFlag(true);
};

// 忘记密码
const handleForget = () => {
  app.setLoginFlag(false);
  app.setForgetFlag(true);
};

// 社交登录
const giteeLogin = () => {
  user.savePath(route.path);
  app.setLoginFlag(false);
  window.open(
      `https://gitee.com/oauth/authorize?client_id=${config.GITEE_APP_ID}&response_type=code&redirect_uri=${config.GITEE_REDIRECT_URI}`,
      "_self"
  );
};

const githubLogin = () => {
  user.savePath(route.path);
  app.setLoginFlag(false);
  window.open(
      `https://github.com/login/oauth/authorize?client_id=${config.GITHUB_APP_ID}&redirect_uri=${config.GITHUB_REDIRECT_URL}&scope=user`,
      "_self"
  );
};
</script>

<style scoped>
/* 新增：Modal内部根容器样式（确保布局不变） */
.modal-inner-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 重置Modal默认样式 */
.login-modal-wrapper {
  display: flex !important;
  align-items: center;
  justify-content: center;
}

.login-modal-wrapper .n-modal {
  margin: 0 !important;
}

/* 核心玻璃拟态容器 */
.container {
  width: 860px;
  height: 540px;
  position: relative;
  border-radius: 26px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(147, 136, 136, 0.55), rgba(188, 143, 143, 0.35));
  backdrop-filter: blur(22px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

/* 表单区域布局 */
.form-wrap {
  display: flex;
  height: 100%;
}

.login-form,
.register-form {
  width: 50%;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  transition: opacity 0.55s ease, transform 0.55s ease;
}

.register-form {
  opacity: 0;
  transform: translateX(50px);
  pointer-events: none;
}

.container.register-active .login-form {
  opacity: 0;
  transform: translateX(-50px);
  pointer-events: none;
}

.container.register-active .register-form {
  opacity: 1;
  transform: translateX(0);
  pointer-events: auto;
}

/* 滑块标题面板（核心动画） */
.title-panel {
  position: absolute;
  inset: 0;
  left: 50%;
  width: 50%;
  padding: 60px;
  background: linear-gradient(105deg, rgba(226, 242, 243, 0.65), rgba(227, 153, 188, 0.58));
  backdrop-filter: blur(26px);
  -webkit-backdrop-filter: blur(26px);
  border-left: 1px solid rgba(255, 255, 255, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  transition: left 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 5;
}

.container.register-active .title-panel {
  left: 0;
}

/* 标题内容动画 */
.title-panel > div {
  position: absolute;
  transition: opacity 0.45s ease, transform 0.45s ease;
}

.login-title {
  opacity: 1;
  transform: scale(1);
  pointer-events: auto;
}

.register-title {
  opacity: 0;
  transform: translateY(30px) scale(0.94);
  pointer-events: none;
}

.container.register-active .login-title {
  opacity: 0;
  transform: translateY(-30px) scale(0.94);
  pointer-events: none;
}

.container.register-active .register-title {
  opacity: 1;
  transform: translateY(0) scale(1);
  pointer-events: auto;
}

/* 标题区按钮动画 */
.title-btn {
  color: #1f2937;
  width: 85px;
  height: 40px;
  border-radius: 16px;
  background: linear-gradient(90deg, rgba(133, 237, 255, 0.34), rgba(60, 106, 209, 0.38));
  box-shadow: 0 6px 16px rgba(159, 122, 234, 0.25);
  margin-top: 22px;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.login-title .title-btn {
  opacity: 1;
  transform: translateY(0);
  transition-delay: 0.12s;
}

.container.register-active .register-title .title-btn {
  opacity: 1;
  transform: translateY(0);
  transition-delay: 0.12s;
}

.container.register-active .login-title .title-btn {
  opacity: 0;
  transform: translateY(-8px);
  transition-delay: 0s;
}

/* 基础样式 */
.title {
  font-size: 26px;
  margin-bottom: 30px;
  color: #2f2f2f;
}

.input-group {
  margin-bottom: 20px;
  width: 100%;
}

/* 验证码输入组布局 */
.captcha-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn {
  cursor: pointer;
  font-size: 15px;
  backdrop-filter: blur(6px);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  border: none;
}

.btn:hover {
  transform: translateY(-2px);
}

.login-btn:hover {
  box-shadow: 0 14px 34px rgba(217, 123, 90, 0.35);
}

.register-btn:hover {
  box-shadow: 0 14px 34px rgba(233, 84, 107, 0.35);
}

.hint-text {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
  margin-bottom: 20px;
}

.slogan {
  font-size: 30px;
  font-style: italic;
  color: #333;
}

/* 验证码遮罩（修复层级和尺寸） */
#captcha-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999; /* 提高层级避免被遮挡 */
  transform: translateZ(0); /* 脱离父容器渲染层 */
  pointer-events: auto;
}

/* 验证码容器强制设置尺寸，避免初始化时尺寸为0 */
#captcha-box {
  width: 320px;
  height: 200px;
  min-width: 320px;
  min-height: 200px;
}

/* 账号/社交登录tab样式（适配玻璃拟态） */
.tab-switch {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 20px;
}

.tab-btn {
  border: 0;
  padding: 8px 14px;
  border-radius: 999px;
  font-weight: 600;
  color: #6b7280;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.25s ease;
  backdrop-filter: blur(8px);
}

.tab-btn.active {
  color: #1f2937;
  background: linear-gradient(90deg, #ff85a2, #9f7aea);
  box-shadow: 0 6px 16px rgba(159, 122, 234, 0.25);
}

/* 滑块容器 */
.slider {
  width: 100%;
  overflow: hidden;
  border-radius: 14px;
  margin-bottom: 16px;
}

.slides {
  display: flex;
  width: 200%;
  transition: transform 360ms ease;
}

.slide {
  width: 50%;
  padding: 10px 6px 16px;
}

.social-slide {
  text-align: center;
}

/* 社交登录样式 */
.brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
  margin-top: 8px;
}

.brand-subtitle {
  color: #718096;
  font-size: 14px;
  margin: 6px 0 10px;
}

.social-login-wrapper {
  text-align: center;
  margin-top: 1.2rem;
}

.icon {
  margin: 0 0.3rem;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.icon:hover {
  transform: scale(1.1);
}

/* 切换圆点 */
.nav-dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(159, 122, 234, 0.35);
  cursor: pointer;
  transition: all 0.2s ease;
}

.dot.active {
  background: #7a90ea;
  box-shadow: 0 4px 10px rgba(159, 122, 234, 0.35);
}

/* 登录/注册提示文本 */
.login-tip, .register-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 14px;
}

.dialog-text {
  color: #1e62ec;
}

.colorFlag {
  color: #1e62ec;
  cursor: pointer;
  transition: color 0.2s ease;
}

.colorFlag:hover {
  color: #805ad5;
  text-decoration: underline;
}

:deep(.n-dialog__content--last) {
  margin-bottom: 30px;
}

/* 限制样式重置作用域，避免污染TAC组件 */
.login-form :deep(.n-input__input),
.register-form :deep(.n-input__input) {
  background: transparent !important;
  border: none !important;
  outline: none !important;
  padding: 0 !important;
  height: auto !important;
}

.login-form :deep(.n-input),
.register-form :deep(.n-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.login-form :deep(.n-button),
.register-form :deep(.n-button) {
  border: none !important;
  outline: none !important;
}

.login-form :deep(.n-button__content),
.register-form :deep(.n-button__content) {
  justify-content: center !important;
}

.n-modal {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>