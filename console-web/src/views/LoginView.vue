<template>
  <el-container class="page">
    <el-main>
      <el-card class="card" header="登录">
        <el-form label-width="80px" @submit.prevent="submit">
          <el-form-item label="邮箱">
            <el-input v-model="email" type="email" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="password" type="password" autocomplete="current-password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading">登录</el-button>
            <el-button type="button" @click="goRegister">注册</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { apiJson } from "../api/client";
import { useSessionStore } from "../stores/session";

const router = useRouter();
const route = useRoute();
const session = useSessionStore();
const email = ref("");
const password = ref("");
const loading = ref(false);

watch(
  () => route.query.email,
  (q) => {
    if (typeof q === "string" && q) {
      email.value = q;
    }
  },
  { immediate: true }
);

function goRegister() {
  router.push("/register");
}

async function submit() {
  loading.value = true;
  try {
    const data = await apiJson<{ accessToken: string }>("/user/login", {
      method: "POST",
      body: JSON.stringify({ email: email.value, password: password.value }),
    });
    session.setToken(data.accessToken);
    ElMessage.success("已登录");
    const redir = typeof route.query.redirect === "string" ? route.query.redirect : "";
    await router.push(redir && redir.startsWith("/") ? redir : "/console/dashboard");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "登录失败");
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
