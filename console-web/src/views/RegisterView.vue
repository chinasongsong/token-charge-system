<template>
  <el-container class="page">
    <el-main>
      <el-card class="card" header="注册">
        <el-form label-width="80px" @submit.prevent="submit">
          <el-form-item label="邮箱">
            <el-input v-model="email" type="email" autocomplete="username" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="displayName" maxlength="255" autocomplete="nickname" placeholder="可选" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="password" type="password" autocomplete="new-password" show-password />
          </el-form-item>
          <el-form-item label="确认">
            <el-input v-model="password2" type="password" autocomplete="new-password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading">注册</el-button>
          </el-form-item>
          <el-form-item>
            <el-button link type="primary" @click="goLogin">已有账号？去登录</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiJson } from "../api/client";

const router = useRouter();
const email = ref("");
const displayName = ref("");
const password = ref("");
const password2 = ref("");
const loading = ref(false);

function goLogin() {
  router.push("/login");
}

async function submit() {
  if (password.value.length < 8) {
    ElMessage.warning("密码至少 8 位");
    return;
  }
  if (password.value !== password2.value) {
    ElMessage.warning("两次输入的密码不一致");
    return;
  }
  loading.value = true;
  try {
    const body: { email: string; password: string; displayName?: string } = {
      email: email.value.trim(),
      password: password.value,
    };
    const nick = displayName.value.trim();
    if (nick) {
      body.displayName = nick;
    }
    await apiJson("/user/register", {
      method: "POST",
      body: JSON.stringify(body),
    });
    ElMessage.success("注册成功，请登录");
    await router.push({ path: "/login", query: { email: email.value.trim() } });
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "注册失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  align-items: center;
  justify-content: center;
}
.card {
  max-width: 420px;
  margin: 4rem auto;
}
</style>
