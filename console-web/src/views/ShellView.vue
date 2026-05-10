<template>
  <el-container class="shell">
    <el-header height="auto">
      <h1>Token Hub 控制台（P0 骨架）</h1>
      <p class="hint">后端契约见 <code>docs/openapi/unified-api.yaml</code></p>
    </el-header>
    <el-main>
      <el-alert type="info" title="后续阶段将实现登录 / Dashboard / API Keys / 计费 等路由。" />
    </el-main>
  </el-container>
</template>

<script setup lang="ts"></script>

<style scoped>
.shell {
  min-height: 100vh;
}
.hint code {
  font-size: 0.95em;
}
</style>
