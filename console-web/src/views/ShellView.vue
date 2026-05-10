<template>
  <el-container class="shell">
    <el-aside width="220px" class="aside">
      <div class="aside-title">控制台</div>
      <el-menu
        router
        :default-active="activePath"
        class="aside-menu"
        background-color="#eef2ff"
        text-color="#334155"
        active-text-color="#4338ca"
      >
        <el-menu-item :index="'/console/dashboard'">
          <el-icon><Odometer /></el-icon>
          <span>总览</span>
        </el-menu-item>
        <el-menu-item :index="'/console/api-keys'">
          <el-icon><Key /></el-icon>
          <span>KEY 管理</span>
        </el-menu-item>
        <el-menu-item :index="'/console/recharge'">
          <el-icon><CreditCard /></el-icon>
          <span>账户充值</span>
        </el-menu-item>
        <el-menu-item :index="'/console/recharge-records'">
          <el-icon><List /></el-icon>
          <span>充值明细</span>
        </el-menu-item>
        <el-menu-item :index="'/console/usage'">
          <el-icon><Histogram /></el-icon>
          <span>用量明细</span>
        </el-menu-item>
        <el-menu-item :index="'/console/support'">
          <el-icon><Tickets /></el-icon>
          <span>工单中心</span>
        </el-menu-item>
      </el-menu>
      <div class="biz">
        <p class="biz-t">商务合作</p>
        <el-button class="biz-btn" size="small" plain>成为供应商</el-button>
      </div>
    </el-aside>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { CreditCard, Histogram, Key, List, Odometer, Tickets } from "@element-plus/icons-vue";
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const activePath = computed(() => route.path);
</script>

<style scoped>
.shell { min-height: calc(100vh - 56px); background: #f6f8ff; }
.aside { background: #eef2ff; display: flex; flex-direction: column; border-right: 1px solid #dfe6fb; }
.aside-title { padding: 20px 18px 8px; font-size: 14px; font-weight: 600; color: #334155; letter-spacing: 0.02em; }
.aside-menu { border-right: none !important; flex: 1; }
.aside-menu :deep(.el-menu-item) { border-radius: 8px; margin: 2px 8px; }
.aside-menu :deep(.el-menu-item.is-active) { background: rgba(99, 102, 241, 0.14) !important; }
.biz { padding: 16px; border-top: 1px solid #dfe6fb; }
.biz-t { margin: 0 0 8px; font-size: 12px; color: #64748b; }
.biz-btn { width: 100%; border-color: #c7d2fe !important; color: #4338ca !important; }
.main { padding: 0; overflow: auto; }
.main > :deep(*) { padding: 22px 24px 40px; max-width: 1200px; }
</style>
