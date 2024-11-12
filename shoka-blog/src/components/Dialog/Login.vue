<template>
  <n-modal class="bg" v-model:show="dialogVisible" preset="dialog" :show-icon="false" transform-origin="center"
           :block-scroll="false">
    <div v-if="showCaptcha" id="captcha-overlay" @click="closeCaptcha">
      <div id="captcha-box" ref="captchaBox"></div> <!-- 验证码框 -->
    </div>
    <n-input class="mt-11" v-model:value="loginForm.username" placeholder="邮箱号"
             @keyup.enter="handleCaptcha"></n-input>
    <n-input class="mt-11" v-model:value="loginForm.password" type="password" show-password-on="click"
             placeholder="密码"
             @keyup.enter="handleCaptcha"></n-input>
    <n-button class="mt-11" color="#ed6ea0" style="width: 100%" :loading="loading" @click="handleCaptcha">
      登 录
    </n-button>
    <div class="mt-10 login-tip">
      <span class="colorFlag" @click="handleRegister">立即注册</span>
      <span class="colorFlag" @click="handleForget">忘记密码?</span>
    </div>
    <div>
      <div class="social-login-title">社交账号登录</div>
      <div class="social-login-wrapper">
        <svg-icon class="icon" icon-class="qq" size="2rem" color="#00aaee"></svg-icon>
        <svg-icon class="icon" icon-class="gitee" size="2rem" v-if="showLogin('gitee')" @click="giteeLogin"></svg-icon>
        <svg-icon class="icon" icon-class="github" size="2rem" v-if="showLogin('github')"
                  @click="githubLogin"></svg-icon>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import {login} from "@/api/login";
import {LoginForm} from "@/api/login/types";
import config from "@/assets/js/config";
import useStore from "@/store";
import {setToken} from "@/utils/token";

//TODO，我搞定的神奇东西，要学习
import {ref, computed} from "vue";
import EventBus from "@/eventBus";

const {app, user, blog} = useStore();
const route = useRoute();
const loading = ref(false);
const loginForm = ref<LoginForm>({
  username: "",
  password: "",
});
const dialogVisible = computed({
  get: () => app.loginFlag,
  set: (value) => (app.loginFlag = value),
});
const showLogin = computed(
    () => (type: string) => blog.blogInfo.siteConfig.loginList.includes(type)
);
const showCaptcha = ref(false);
//TODO，我搞定的神奇东西，要学习
const captchaBox = ref<HTMLElement | null>(null);

const handleRegister = () => {
  app.setLoginFlag(false);
  app.setRegisterFlag(true);
};
const handleForget = () => {
  app.setLoginFlag(false);
  app.setForgetFlag(true);
};

let globalTAC: any;
const captchaConfig = {
  requestCaptchaDataUrl: "http://localhost:8080/gen?type=RANDOM",
  validCaptchaUrl: "http://localhost:8080/check",
  bindEl: "#captcha-box",
  validSuccess: (res: any, c: any, tac: any) => {
    tac.destroyWindow();
    handlelogin(res.data.id);
  },
};
const captchaStyle = {
  logoUrl: "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img/202410311645295.png",
};

//TODO，我搞定的神奇东西，要学习
const closeCaptcha = () => {
  const width = captchaBox.value?.offsetWidth; // 获取 captchaBox 的宽度
  console.log(width); // 打印宽度
  if (width === 0) {
    console.log('验证码框宽度为0，执行关闭操作');
    showCaptcha.value = false
  } else {
    console.log('验证码框宽度不为0，不关闭操作');
  }
};

const handleCaptcha = () => {
  let reg = /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
  if (!reg.test(loginForm.value.username)) {
    window.$message?.warning("邮箱格式不正确");
    return;
  }
  if (loginForm.value.password.trim().length == 0) {
    window.$message?.warning("密码不能为空");
    return;
  }
  showCaptcha.value = true;
  // 创建 TAC 启动验证码服务
  if (globalTAC) {
    globalTAC.destroyWindow();
  }
  // @ts-ignore
  setTimeout(() => {
    // @ts-ignore
    globalTAC = new TAC(captchaConfig, captchaStyle).init();
  }, 0);
};

const handlelogin = (token: string) => {

  loading.value = true;
  login(loginForm.value).then(({data}) => {
    if (data.flag) {
      setToken(data.data);
      user.GetUserInfo();
      window.$message?.success("登录成功");
      loginForm.value = {
        username: "",
        password: "",
      };
      app.setLoginFlag(false);
    }
    showCaptcha.value = false; // 隐藏验证码
    loading.value = false;
    EventBus.emit("refresh-articles");
    return;
  });
};

const giteeLogin = () => {
  //保留当前路径
  user.savePath(route.path);
  app.setLoginFlag(false);
  window.open(
      "https://gitee.com/oauth/authorize?client_id=" +
      config.GITEE_APP_ID +
      "&response_type=code&redirect_uri=" +
      config.GITEE_REDIRECT_URI,
      "_self"
  );
};
const githubLogin = () => {
  //保留当前路径
  user.savePath(route.path);
  app.setLoginFlag(false);
  window.open(
      "https://github.com/login/oauth/authorize?client_id=" +
      config.GITHUB_APP_ID +
      "&redirect_uri=" +
      config.GITHUB_REDIRECT_URL +
      "&scope=user",
      "_self"
  );
};

</script>

<style scoped>

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
  z-index: 9999;
}

.login-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.social-login-title {
  margin-top: 1rem;
  color: #b5b5b5;
  font-size: 0.75rem;
  text-align: center;
}

.social-login-title::before {
  content: "";
  display: inline-block;
  background-color: #d8d8d8;
  width: 60px;
  height: 1px;
  margin: 0 12px;
  vertical-align: middle;
}

.social-login-title::after {
  content: "";
  display: inline-block;
  background-color: #d8d8d8;
  width: 60px;
  height: 1px;
  margin: 0 12px;
  vertical-align: middle;
}

.social-login-wrapper {
  text-align: center;
  margin-top: 1.4rem;
}

.icon {
  margin: 0 0.3rem;
}
</style>
