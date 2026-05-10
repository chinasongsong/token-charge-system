<template>
  <div>
    <div class="cards">
      <el-card shadow="never" class="metric">
        <div class="label">账单余额</div>
        <div class="big" v-if="summary">{{ summary.balance }} {{ summary.currency }}</div>
        <div class="big" v-else>—</div>
        <div class="foot">
          本月账单订单 {{ summary?.billingOrderCount ?? "—" }}
          <RouterLink custom v-slot="{ navigate }" to="/console/recharge">
            <el-button type="primary" link size="small" @click="navigate">充值</el-button>
          </RouterLink>
        </div>
      </el-card>
      <el-card shadow="never" class="metric muted-card">
        <div class="label">邀请用户数</div>
        <div class="big">0</div>
        <div class="foot">累计邀请用户（未接增长模块）</div>
      </el-card>
      <el-card shadow="never" class="metric muted-card">
        <div class="label">邀请分佣金额</div>
        <div class="big">¥0</div>
        <div class="foot">累计分佣收益（占位）</div>
      </el-card>
    </div>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="chart-head">
          <span class="ttl">每日消费</span>
          <el-radio-group v-model="consMode" size="small">
            <el-radio-button value="daily">每日消费</el-radio-button>
            <el-radio-button value="month">每月消费</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div class="chart-placeholder">
        <p>图表待接 request_orders 聚合接口；当前为 TaoToken 式布局占位。</p>
      </div>
    </el-card>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="chart-head">
          <span class="ttl">模型用量</span>
          <div class="filters">
            <span class="f-label">统计月份</span>
            <span class="f-mono th-mono">{{ monthStr }}</span>
          </div>
        </div>
      </template>
      <div class="chart-placeholder short">
        <p>按月 / 按日切换与下拉模型筛选可后续接 usage_ledger。</p>
      </div>
    </el-card>

    <el-alert v-if="error" type="error" :title="error" show-icon />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { apiJson } from "../api/client";

type Summary = { balance: number; currency: string; billingOrderCount: number };

const summary = ref<Summary | null>(null);
const error = ref("");
const consMode = ref<"daily" | "month">("daily");
const monthStr = ref("本年累计");

onMounted(async () => {
  try {
    summary.value = await apiJson<Summary>("/dashboard/summary");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  }
});
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.metric {
  border-radius: 12px !important;
  border: 1px solid #e8ecf1 !important;
}

.muted-card .big {
  color: #94a3b8;
}

.label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.big {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.foot {
  margin-top: 12px;
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.chart-card {
  margin-bottom: 16px;
  border-radius: 12px !important;
  border: 1px solid #e8ecf1 !important;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.ttl {
  font-weight: 600;
  font-size: 15px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.f-mono {
  color: #6366f1;
}

.chart-placeholder {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #f8fafc, #fff);
  border-radius: 8px;
  color: #64748b;
  font-size: 14px;
  text-align: center;
  padding: 16px;
}

.chart-placeholder.short {
  min-height: 140px;
}
</style>
