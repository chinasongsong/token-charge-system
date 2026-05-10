<template>
  <div class="home">
    <section class="hero">
      <div class="hero-inner">
        <h1>
          全球大模型稳定直连<span class="dot"> · </span>分钟级接入<span class="dot"> · </span>
          <span class="purple">配额可观测</span>
        </h1>
        <p class="sub">
          标准 OpenAI 协议，开发与 C 端同一套账务；专线低延迟、按量计费。参考
          <a href="https://taotoken.net/" target="_blank" rel="noopener noreferrer">TaoToken</a>
          产品形态，当前站点仅保留 <strong>首页 · 模型体验 · 控制台</strong> 三模块。
        </p>
        <div class="cta">
          <RouterLink custom v-slot="{ navigate }" to="/experience">
            <el-button type="primary" size="large" class="btn-main" @click="navigate">立即体验模型</el-button>
          </RouterLink>
          <RouterLink custom v-slot="{ navigate }" to="/console/dashboard">
            <el-button size="large" class="btn-sec" @click="navigate">进入控制台</el-button>
          </RouterLink>
        </div>
      </div>
    </section>

    <section class="models">
      <div class="section-head">
        <h2>
          热门模型
          <span class="badge">开发价</span>
        </h2>
        <RouterLink to="/experience" class="more">去模型体验 &gt;</RouterLink>
      </div>
      <div class="model-grid">
        <article v-for="m in hotModels" :key="m.id" class="model-card">
          <div class="m-head">
            <span class="m-name th-mono">{{ m.id }}</span>
            <span class="m-brand">{{ m.brand }}</span>
          </div>
          <p class="m-price">
            输入 {{ m.in }} · 输出 {{ m.out }}
          </p>
          <RouterLink custom v-slot="{ navigate }" to="/register">
            <el-button type="primary" class="m-btn" @click="navigate">免费体验</el-button>
          </RouterLink>
        </article>
      </div>
    </section>

    <section class="quick">
      <div class="quick-inner">
        <div class="quick-title">
          <h2>快速接入</h2>
          <p>三步完成 OpenAI 兼容调用</p>
        </div>
        <RouterLink custom v-slot="{ navigate }" to="/console/recharge">
          <el-button size="small" type="primary" plain @click="navigate">模拟充值</el-button>
        </RouterLink>
        <ol class="steps">
          <li>
            <strong>注册 / 登录账号</strong>
            <span class="st">{{ session.accessToken ? "已登录" : "未登录" }}</span>
          </li>
          <li>
            <strong>创建 API Key</strong>
            <RouterLink to="/console/api-keys">前往 KEY 管理</RouterLink>
          </li>
          <li>
            <strong>复制代码调用</strong>
            <span class="th-mono tiny">POST /v1/chat/completions</span>
          </li>
        </ol>
        <pre class="code"><code>curl -H "Authorization: Bearer &lt;JWT 或 sk_&gt;" \
  -H "Content-Type: application/json" \
  -d '{"model":"deepseek-v4-flash","messages":[{"role":"user","content":"hi"}]}' \
  {{ apiBase }}/v1/chat/completions</code></pre>
      </div>
    </section>

    <footer class="foot">Token Hub · plan.md §15 设计令牌 · 仅供研发联调</footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useSessionStore } from "../stores/session";

const session = useSessionStore();
const apiBase = computed(() => (import.meta.env.VITE_API_BASE ? `${import.meta.env.VITE_API_BASE}` : "http://localhost:8080"));

const hotModels = [
  { id: "deepseek-v4-flash", brand: "DeepSeek", in: "按 model_prices", out: "按 model_prices" },
  { id: "deepseek-v4-pro", brand: "DeepSeek", in: "按 model_prices", out: "按 model_prices" },
  { id: "glm-4-flash", brand: "智谱", in: "按 model_prices", out: "按 model_prices" },
  { id: "多线路 failover", brand: "平台路由", in: "DeepSeek 主", out: "智谱备" },
];
</script>

<style scoped>
.home {
  background: #f7f8fa;
}

.hero {
  padding: 56px 20px 48px;
  background: radial-gradient(ellipse 90% 60% at 50% -30%, rgba(99, 102, 241, 0.2), transparent);
}

.hero-inner {
  max-width: 880px;
  margin: 0 auto;
  text-align: center;
}

.hero h1 {
  margin: 0 0 16px;
  font-size: clamp(26px, 4vw, 38px);
  line-height: 1.25;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.dot {
  color: #94a3b8;
  font-weight: 500;
}

.purple {
  color: #6366f1;
}

.sub {
  margin: 0 auto 28px;
  max-width: 620px;
  font-size: 16px;
  line-height: 1.65;
  color: #475569;
}

.sub a {
  color: #6366f1;
  font-weight: 500;
}

.cta {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-main {
  border-radius: 10px !important;
  padding: 12px 28px !important;
  background: linear-gradient(135deg, #6366f1, #5b52e8) !important;
  border: none !important;
}

.btn-sec {
  border-radius: 10px !important;
  padding: 12px 24px !important;
}

.models {
  max-width: 1160px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-head h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.badge {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #b45309;
  background: #fef3c7;
  padding: 2px 8px;
  border-radius: 6px;
  vertical-align: middle;
}

.more {
  font-size: 14px;
  color: #6366f1;
  font-weight: 500;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.model-card {
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.m-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.m-name {
  font-weight: 700;
  font-size: 15px;
  color: #0f172a;
}

.m-brand {
  font-size: 12px;
  color: #64748b;
}

.m-price {
  margin: 0 0 14px;
  font-size: 13px;
  color: #64748b;
}

.m-btn {
  width: 100%;
  border-radius: 10px !important;
}

.quick {
  padding: 0 20px 48px;
}

.quick-inner {
  max-width: 960px;
  margin: 0 auto;
  background: linear-gradient(180deg, #eef2ff 0%, #f8fafc 100%);
  border: 1px solid #e0e7ff;
  border-radius: 16px;
  padding: 24px 28px 28px;
  position: relative;
}

.quick-title {
  margin-bottom: 16px;
}
.quick-title h2 {
  margin: 0 0 6px;
  font-size: 20px;
}
.quick-title p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.quick-inner > .el-button {
  position: absolute;
  top: 22px;
  right: 24px;
}

.steps {
  margin: 0 0 16px;
  padding-left: 20px;
  color: #334155;
  line-height: 1.9;
}

.steps a {
  color: #6366f1;
  margin-left: 6px;
}

.st {
  margin-left: 8px;
  font-size: 13px;
  color: #16a34a;
}

.tiny {
  font-size: 12px;
  margin-left: 8px;
  color: #6366f1;
}

.code {
  margin: 0;
  padding: 16px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 12px;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.5;
}

.foot {
  text-align: center;
  padding: 28px 16px;
  font-size: 13px;
  color: #94a3b8;
}
</style>
