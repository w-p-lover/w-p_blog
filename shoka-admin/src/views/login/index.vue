<template>
    <div class="login">
        <el-form ref="ruleFormRef" :model="loginForm" :rules="rules" class="login-form">
            <h3 class="title">博客后台管理系统</h3>
            <el-form-item prop="username">
                <el-input v-model="loginForm.username" type="text" size="large" placeholder="账号">
                    <template #prefix><svg-icon icon-class="user"></svg-icon></template>
                </el-input>
            </el-form-item>
            <el-form-item prop="password">
                <el-input v-model="loginForm.password" type="password" show-password size="large" placeholder="密码"
                    @keyup.enter="handleLogin()">
                    <template #prefix><svg-icon icon-class="password"></svg-icon></template>
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
import { FormInstance, FormRules } from 'element-plus';
import { reactive, ref } from 'vue';
import {login} from "@/api/login";
import {setToken} from "@/utils/token";
const { user } = useStore();
const ruleFormRef = ref<FormInstance>();
const loading = ref(false);
const loginForm = reactive({
    username: "admin@qq.com",
    password: "123456",
});
const rules = reactive<FormRules>({
    username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
    password: [{ required: true, message: "请输入密码", trigger: "blur" }, { min: 6, message: "密码不能少于6位", trigger: "blur" }],
});

const handleLogin = () => {
  let reg = /^[A-Za-z0-9\u4e00-\u9fa5]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;
  if (!reg.test(loginForm.username)) {
    console.log("邮箱格式不正确");
    return;
  }
  loading.value = true;
  login(loginForm)
      .then(({ data }) => {
        console.log("sdsds")
        if (data.flag) {
          console.log(data.data)
          setToken(data.data);
          router.push("/")
        } else {
        }
        loading.value = false;
      }).catch(() => {
    loading.value = true;
  });
}

</script>
<style lang="scss" scoped>

.login {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    background-image: url("https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img/202408131724486.png");
    background-size: cover;
}

.title {
    margin: 25px auto 45px auto;
    text-align: center;
    color: #ffffff;
    font-size: 30px;
}

.login-form {
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 400px;
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
  .el-form-item
  {
    margin: 30px 30px 10px 30px;
  }
  .el-button{
    height: 45px;
    font-size: 16px;
    background-color: rgba(29, 83, 189, 0.9);
    border-color: #2757bd;
    color: #ffffff;
  }
}

:deep(.el-input__wrapper)  {
  background-color: rgba(176, 197, 221,0.7);
  height: 46px;
  border: thin  #ffffff;
}
//输入框颜色
:deep(.el-input__inner) {
  color: rgb(44, 51, 65);

}
//提示框的字体颜色
:deep(input::-webkit-input-placeholder) {
  color: #fff;
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
    font-family: Arial;
    font-size: 12px;
    letter-spacing: 1px;
}
</style>
