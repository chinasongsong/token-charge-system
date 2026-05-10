<template>
  <div class="brand">
    <RouterLink class="logo" to="/">Token Hub</RouterLink>
    <p class="tagline">
      Java 中转平台<span class="th-mono"> · OpenAI 兼容 · </span>开发者与 C 端一套账户
    </p>
    <ul class="bullets">
      <li><span class="dot" /><span><strong>统一网关</strong>：鉴权 / 路由 / SSE 占位</span></li>
      <li><span class="dot" /><span><strong>混合计费</strong>：余额 + 套餐并存</span></li>
      <li><span class="dot" /><span><strong>多供应商</strong>：低成本与可用率兜底</span></li>
      <li><span class="dot" /><span><strong>账务闭环</strong>：账单、工单与安全基线</span></li>
    </ul>
    <footer class="foot">
      <span class="th-mono">/v1/chat/completions</span>
      <span class="muted">· 与你的 PRD §15 设计令牌对齐</span>
    </footer>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.brand {
  padding: clamp(24px, 6vw, 48px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100%;
  color: rgba(248, 250, 252, 0.98);
}

.logo {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: inherit;
  margin-bottom: 14px;
}
.logo:hover {
  color: rgba(255, 255, 255, 0.9);
}

.tagline {
  margin: 0 0 26px;
  font-size: 15px;
  line-height: 1.65;
  color: rgba(226, 232, 240, 0.88);
  max-width: 360px;
}

.bullets {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 380px;
}

.bullets li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 14px;
  line-height: 1.55;
  color: rgba(226, 232, 240, 0.92);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #a5b4fc, #e0e7ff);
}

.foot {
  margin-top: 40px;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.9);
}

.muted {
  display: inline-block;
  margin-left: 6px;
  color: rgba(148, 163, 184, 0.75);
}
</style>
