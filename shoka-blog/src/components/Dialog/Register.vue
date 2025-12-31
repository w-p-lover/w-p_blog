<template>
</template>

<script setup lang="ts">
import {getCode, login, register} from "@/api/login";
import {LoginForm} from "@/api/login/types";
import {UserForm} from "@/model";
import useStore from "@/store";
import {setToken} from "@/utils/token";
import {useIntervalFn} from '@vueuse/core';

const {app, user} = useStore();
const registerRef = ref();
const data = reactive({
  timer: 0,
  flag: false,
  loading: false,
  registerForm: {
    username: "",
    password: "",
    code: "",
  } as UserForm,
});
const {timer, flag, loading, registerForm} = toRefs(data);
const {pause, resume} = useIntervalFn(() => {
  timer.value--;
  if (timer.value <= 0) {
    // 停止定时器
    pause();
    flag.value = false;
  }
}, 1000, {immediate: false});
const start = (time: number) => {
  flag.value = true;
  timer.value = time;
  // 启动定时器
  resume();
};
const sendCode = () => {
  let reg = /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
  if (!reg.test(registerForm.value.username)) {
    window.$message?.warning("邮箱格式不正确");
    return;
  }
  start(60);
  getCode(registerForm.value.username).then(({data}) => {
    if (data.flag) {
      window.$message?.success("发送成功");
    }
  });
};
const handleRegister = () => {
  if (registerForm.value.password.trim().length < 6) {
    window.$message?.warning("密码不能少于6位");
    return;
  }
  loading.value = true;
  register(registerForm.value).then(({data}) => {
    if (data.flag) {
      let loginForm: LoginForm = {
        username: registerForm.value.username,
        password: registerForm.value.password,
      }
      login(loginForm).then(({data}) => {
        if (data.flag) {
          registerForm.value = {
            username: "",
            password: "",
            code: "",
          }
          setToken(data.data);
          user.GetUserInfo();
          window.$message?.success("登录成功");
          app.setRegisterFlag(false);
        }
      });
    }
    loading.value = false;
  });
};
const dialogVisible = computed({
  get: () => app.registerFlag,
  set: (value) => app.registerFlag = value,
});
const handleLogin = () => {
  app.setRegisterFlag(false);
  app.setLoginFlag(true);
};
</script>

<style scoped></style>
