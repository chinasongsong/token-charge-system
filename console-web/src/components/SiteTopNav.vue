<template>
  <header class="nav">
    <div class="nav-inner">
      <RouterLink to="/" class="logo"><span class="logo-icon" aria-hidden="true">*</span>Token Hub</RouterLink>
      <nav class="links" aria-label="主导航">
        <RouterLink to="/" :class="{ active: isHome }">首页</RouterLink>
        <RouterLink to="/experience" :class="{ active: isExperience }">模型体验</RouterLink>
        <RouterLink to="/console" :class="{ active: isConsole }">控制台</RouterLink>
      </nav>
      <div class="right">
        <RouterLink custom v-slot="{ navigate }" to="/console/recharge">
          <el-button type="primary" size="small" class="recharge-btn" @click="navigate">充值</el-button>
        </RouterLink>
        <template v-if="session.accessToken">
          <el-dropdown trigger="click" @command="onUserCmd">
            <span class="user-chip"><el-icon><UserFilled /></el-icon>{{ displayName }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="console">控制台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <RouterLink custom v-slot="{ navigate }" to="/login"><el-button text @click="navigate">登录</el-button></RouterLink>
          <RouterLink custom v-slot="{ navigate }" to="/register"><el-button type="primary" link @click="navigate">注册</el-button></RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { UserFilled } from "@element-plus/icons-vue";
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { apiJson } from "../api/client";
import { useSessionStore } from "../stores/session";

const route = useRoute();
const router = useRouter();
const session = useSessionStore();
const profileName = ref("");

const isHome = computed(() => route.path === "/");
const isExperience = computed(() => route.path === "/experience");
const isConsole = computed(() => route.path.startsWith("/console"));
const displayName = computed(() => profileName.value || "已登录");

onMounted(async () => {
  if (!session.accessToken) return;
  try {
    const p = await apiJson<{ displayName: string | null; email: string }>("/user/me");
    profileName.value = (p.displayName && p.displayName.trim()) || p.email || "";
  } catch {
    profileName.value = "";
  }
});

function onUserCmd(cmd: string) {
  if (cmd === "console") router.push("/console/dashboard");
  if (cmd === "logout") {
    session.clear();
    profileName.value = "";
    router.push("/");
  }
}
</script>

<style scoped>
.nav { position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,.94); backdrop-filter: blur(12px); border-bottom: 1px solid #e8ecf1; }
.nav-inner { max-width: 1200px; margin: 0 auto; padding: 0 20px; height: 56px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.logo { display: inline-flex; align-items: center; gap: 8px; font-weight: 800; font-size: 18px; letter-spacing: -.03em; color: var(--th-text-primary,#1e293b); }
.logo-icon { color: #6366f1; font-size: 20px; line-height: 1; }
.links { display: flex; align-items: center; gap: 4px; flex: 1; justify-content: center; }
.links a { padding: 8px 16px; border-radius: 8px; font-size: 14px; font-weight: 500; color: #475569; text-decoration: none; }
.links a:hover { color: #6366f1; background: rgba(99,102,241,.08); }
.links a.router-link-active,.links a.active { color: #6366f1; }
.right { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.recharge-btn { border-radius: 8px !important; background: linear-gradient(135deg,#6366f1,#5b52e8) !important; border: none !important; }
.user-chip { display:inline-flex; align-items:center; gap:6px; cursor:pointer; font-size:14px; color:var(--th-text-primary,#1e293b); padding:6px 10px; border-radius:8px; }
.user-chip:hover { background: rgba(99,102,241,.08); }
@media (max-width: 720px) { .links { display: none; } }
</style>
