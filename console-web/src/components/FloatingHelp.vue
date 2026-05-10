<template>
  <div class="fab-wrap" aria-label="客服与帮助">
    <el-tooltip content="联系客服（请替换为实际企业微信二维码）" placement="left">
      <button type="button" class="fab" @click="tip">
        <el-icon :size="20"><Service /></el-icon>
      </button>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { Service } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";

function tip() {
  ElMessage.info("可在 deploy 或运营配置中挂载企业微信二维码链接");
}
</script>

<style scoped>
.fab-wrap {
  position: fixed;
  right: 20px;
  bottom: 88px;
  z-index: 90;
}
.fab {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #5b52e8);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
}
.fab:hover {
  filter: brightness(1.05);
}
</style>
