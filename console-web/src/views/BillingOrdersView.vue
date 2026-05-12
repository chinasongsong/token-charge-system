<template>
  <div class="page">
    <h2 class="h2">账单订单</h2>
    <p class="sub">
      数据来源：<code class="th-mono">GET /billing/orders</code>（<code class="th-mono">request_orders</code>，模型调用产生的计费单）。充值入账走
      <code class="th-mono">/payments/mock/recharge</code>，当前仅总览余额变化；如需支付单列表需后续提供查询接口。
    </p>

    <el-alert v-if="err" type="error" :title="err" show-icon class="mb" />

    <el-table v-loading="loading" :data="rows" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="modelName" label="模型" min-width="140" show-overflow-tooltip />
      <el-table-column prop="providerCode" label="供应商" width="100" />
      <el-table-column prop="inputTokens" label="输入 Token" width="110" align="right" />
      <el-table-column prop="outputTokens" label="输出 Token" width="110" align="right" />
      <el-table-column prop="amount" label="金额" width="100" align="right" />
      <el-table-column prop="billingStatus" label="状态" width="100" />
      <el-table-column prop="traceId" label="Trace" min-width="120" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">{{ formatApiDate(row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { apiJson } from "../api/client";
import { formatApiDate } from "../utils/formatApiDate";

type Row = {
  id: number;
  traceId: string;
  modelName: string;
  providerCode: string;
  billingStatus: string;
  inputTokens: number;
  outputTokens: number;
  amount: number;
  createdAt: unknown;
};

const rows = ref<Row[]>([]);
const loading = ref(false);
const err = ref("");

onMounted(async () => {
  loading.value = true;
  try {
    rows.value = await apiJson<Row[]>("/billing/orders?limit=100");
  } catch (e) {
    err.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.h2 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
}
.sub {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
.mb {
  margin-bottom: 12px;
}
</style>
