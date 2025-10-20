<template>
  <div class="login">
    <h3 class="title">博客后台管理系统</h3>
    <el-form ref="ruleFormRef" :model="loginForm" :rules="rules" class="login-form">
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" type="text" size="large" placeholder="账号">
          <template #prefix>
            <svg-icon icon-class="user"></svg-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="text" show-password size="large" placeholder="密码"
                  @keyup.enter="handleLogin()">
          <template #prefix>
            <svg-icon icon-class="password"></svg-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button class="custom-input-bg" :loading="loading" type="primary" @click.prevent="handleLogin()"
                   style="width:100%;">
          <span v-if="!loading">登 录</span>
          <span v-else>登 录中...</span>
        </el-button>

      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>Copyright © 2022 - {{ new Date().getFullYear() }} By w&p</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import router from "@/router";
import useStore from '@/store';
import {ElMessage, FormInstance, FormRules} from 'element-plus';
import {reactive, ref} from 'vue';
import {login} from "@/api/login";
import {setToken} from "@/utils/token";

const {user} = useStore();
const ruleFormRef = ref<FormInstance>();
const loading = ref(false);
const loginForm = reactive({
  username: "admin@qq.com",
  password: "123456",
});
const rules = reactive<FormRules>({
  username: [{required: true, message: "请输入用户名", trigger: "blur"}],
  password: [{required: true, message: "请输入密码", trigger: "blur"}, {
    min: 6,
    message: "密码不能少于6位",
    trigger: "blur"
  }],
});

const handleLogin = () => {
  const reg = /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
  if (!reg.test(loginForm.username)) {
    ElMessage({
      type: 'warning',
      message: '邮箱格式不正确，请重新输入',
      offset: 100 // 弹窗位置，避免遮挡输入框
    });
    return;
  }
  loading.value = true;
  login(loginForm)
    .then(({ data }) => {
      if (data.flag) {
        console.log(data.data);
        setToken(data.data);
        router.push("/");
      }
      loading.value = false;
    })
    .catch((err) => {
      loading.value = false;
      ElMessage({
        type: 'error',
        message: '登录失败，请检查账号密码或网络',
        offset: 100
      });
      console.error('登录错误:', err);
    });
};

</script>
<style lang="scss" scoped>

// 给根容器添加动画
.login {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url('../../assets/bg.png');
  background-size: cover;
  background-position: center; // 背景居中
  animation: fadeIn 0.3s ease-in-out;
}

// 新增动画定义
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-family: Arial,serif;
  font-size: 12px;
  letter-spacing: 1px;
  background: rgba(0, 0, 0, 0.1);
}

// 样式中更新 .title 类
.title {
  margin: -40px auto 35px auto;
  text-align: center;
  font-size: 36px;
  font-weight: 600;
  // 新增：蓝色渐变文字+阴影
  background: linear-gradient(120deg, #b8d2ff, #ffffff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.login-form {
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 300px;
  box-shadow: 20px 20px 50px rgba(0, 0, 0, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, .9);

  @supports (backdrop-filter: blur(10px) brightness(150%)) {
    background: rgba(255, 255, 255, .1);
    backdrop-filter: blur(20px) brightness(90%);
  }
  border-radius: 6px;
  width: 400px;
  padding: 25px 25px 5px 25px;


  .el-form-item {
    margin: 30px 30px 10px 30px;
  }

  .el-button {
    height: 45px;
    font-size: 16px;
    background-color: rgba(29, 83, 189, 0.9);
    border-color: #2757bd;
    color: #ffffff;
  }
}

:deep(.el-input__wrapper) {
  background-color: rgba(176, 197, 221, 0.8);
  height: 48px;
  border: 1px solid transparent;
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper.is-focus) {
  background-color: rgba(209, 229, 241, 0.8);
  border-color: #4096ff;
  box-shadow: 0 0 0 3px rgba(64, 150, 255, 0.2);
}

:deep(.el-input__icon) {
  color: rgb(44, 51, 65, 0.6);
  transition: color 0.3s ease;
}

:deep(.el-input__wrapper.is-focus .el-input__icon) {
  color: #4096ff;
}

:deep(.el-input__inner) {
  color: rgb(30, 35, 45);
  font-family: sans-serif;
  font-size: 15px;
}

.login-form .el-button {
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(39, 87, 189, 0.92), rgba(64, 150, 255, 0.89));
  border-color: transparent;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.login-form .el-button:hover {
  background: linear-gradient(135deg, rgba(30, 74, 168, 0.91), rgba(54, 136, 255, 0.91));
  box-shadow: 0 4px 12px rgba(7, 109, 236, 0.3);
}

.login-form .el-button:active {
  box-shadow: 0 2px 8px rgba(64, 150, 255, 0.2);
}

:deep(.el-button--loading .el-loading-spinner) {
  margin-right: 8px;
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial,serif;
  font-size: 12px;
  letter-spacing: 1px;
}
</style>
