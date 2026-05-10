<template>
  <div>
    <h2 class="h2">用量明细</h2>
    <p class="sub">数据来源：GET /v1/usage（usage_ledger 流水）</p>
    <el-alert v-if="err" type="error" :title="err" show-icon class="mb" />
    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="requestOrderId" label="请求订单 ID" width="110" />
      <el-table-column prop="entryType" label="类型" width="100" />
      <el-table-column prop="quantity" label="Quantity" width="100" />
      <el-table-column prop="idempotencyKey" label="幂等键" min-width="160" show-overflow-tooltip />
      <el-table-column prop="recordedAt" label="时间" min-width="170" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { apiJson } from "../api/client";

type Row = {
  id: number;
  requestOrderId: number;
  entryType: string;
  quantity: number;
  idempotencyKey: string;
  detailJson?: string | null;
  recordedAt: string;
};

const rows = ref<Row[]>([]);
const loading = ref(false);
const err = ref("");

onMounted(async () => {
  loading.value = true;
  try {
    rows.value = await apiJson<Row[]>("/v1/usage?limit=100");
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
}
.mb {
  margin-bottom: 12px;
}
</style>
