<template>
  <div>
    <div class="cards">
      <el-card shadow="never" class="metric">
        <div class="label">账单余额</div>
        <div class="big" v-if="summary">{{ summary.balance }} {{ summary.currency }}</div>
        <div class="big" v-else>—</div>
        <div class="foot">
          累计账单订单 {{ summary?.billingOrderCount ?? "—" }}
          <RouterLink custom v-slot="{ navigate }" to="/console/recharge">
            <el-button type="primary" link size="small" @click="navigate">充值</el-button>
          </RouterLink>
        </div>
      </el-card>
      <el-card shadow="never" class="metric">
        <div class="label">活跃 API Key</div>
        <div class="big">{{ apiKeyStats.active }}</div>
        <div class="foot">
          共 {{ apiKeyStats.total }} 个
          <RouterLink custom v-slot="{ navigate }" to="/console/api-keys">
            <el-button type="primary" link size="small" @click="navigate">管理</el-button>
          </RouterLink>
        </div>
      </el-card>
      <el-card shadow="never" class="metric">
        <div class="label">用量流水样本</div>
        <div class="big">{{ usagePreview }}</div>
        <div class="foot">
          <RouterLink custom v-slot="{ navigate }" to="/console/usage">
            <el-button type="primary" link size="small" @click="navigate">明细</el-button>
          </RouterLink>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <span class="ttl">近期账单订单</span>
      </template>
      <el-table v-loading="loadOrders" :data="recentOrders" size="small" stripe border>
        <el-table-column prop="modelName" label="模型" min-width="120" show-overflow-tooltip />
        <el-table-column prop="providerCode" label="供应商" width="88" />
        <el-table-column prop="amount" label="金额" width="80" align="right" />
        <el-table-column prop="billingStatus" label="状态" width="88" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatApiDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <p v-if="!loadOrders && recentOrders.length === 0" class="empty-hint">暂无调用记录（调一次对话后即可在此看到）</p>
    </el-card>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <span class="ttl">近期用量流水</span>
      </template>
      <el-table v-loading="loadUsage" :data="recentUsage" size="small" stripe border>
        <el-table-column prop="entryType" label="类型" width="120" />
        <el-table-column prop="quantity" label="Quantity" width="100" align="right" />
        <el-table-column prop="requestOrderId" label="订单 ID" width="100" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatApiDate(row.recordedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-alert v-if="error" type="error" :title="error" show-icon />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { apiJson } from "../api/client";
import { formatApiDate } from "../utils/formatApiDate";

type Summary = { balance: number; currency: string; billingOrderCount: number };
type ApiKeyRow = { id: number; status: string };
type OrderRow = {
  modelName?: string;
  providerCode?: string;
  amount?: number;
  billingStatus?: string;
  createdAt?: unknown;
};
type UsageRow = { entryType?: string; quantity?: number; requestOrderId?: number; recordedAt?: unknown };

const summary = ref<Summary | null>(null);
const apiKeys = ref<ApiKeyRow[]>([]);
const recentOrders = ref<OrderRow[]>([]);
const recentUsage = ref<UsageRow[]>([]);
const loadOrders = ref(false);
const loadUsage = ref(false);
const error = ref("");

const apiKeyStats = computed(() => {
  const rows = apiKeys.value;
  return {
    total: rows.length,
    active: rows.filter((r) => r.status === "ACTIVE").length,
  };
});

const usagePreview = computed(() => (recentUsage.value.length ? `${recentUsage.value.length} 条` : "暂无"));

onMounted(async () => {
  try {
    summary.value = await apiJson<Summary>("/dashboard/summary");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "总览加载失败";
  }

  loadOrders.value = true;
  loadUsage.value = true;
  try {
    const [keys, orders, usage] = await Promise.all([
      apiJson<ApiKeyRow[]>("/apikeys"),
      apiJson<OrderRow[]>("/billing/orders?limit=10"),
      apiJson<UsageRow[]>("/v1/usage?limit=10"),
    ]);
    apiKeys.value = keys;
    recentOrders.value = orders;
    recentUsage.value = usage;
  } catch (e) {
    if (!error.value) error.value = e instanceof Error ? e.message : "列表加载失败";
  } finally {
    loadOrders.value = false;
    loadUsage.value = false;
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

.ttl {
  font-weight: 600;
  font-size: 15px;
}

.empty-hint {
  margin: 12px 0 0;
  font-size: 13px;
  color: #94a3b8;
}
</style>
