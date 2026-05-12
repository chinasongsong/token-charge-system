<template>
  <div>
    <h2 class="h2">账户充值（Mock）</h2>
    <p class="hint">
      入账走支付服务：<code class="th-mono">POST /payments/mock/recharge</code>，与计费余额联动。也可在「账单订单」查看模型调用产生的扣费记录。
    </p>

    <el-card shadow="never" class="card-block">
      <template #header><span class="card-ttl">模拟充值</span></template>
      <el-form inline @submit.prevent="submit">
        <el-form-item label="金额 (TOKEN)">
          <el-input-number v-model="amount" :min="1" :step="100" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading">充值</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="result" type="success" :title="`订单 ${result.orderNo} 状态 ${result.status}`" show-icon />
    </el-card>

    <el-card shadow="never" class="card-block">
      <template #header>
        <span class="card-ttl">套餐列表</span>
        <span class="card-sub">GET /billing/plans</span>
      </template>
      <el-alert v-if="planErr" type="error" :title="planErr" show-icon class="mb" />
      <el-table v-loading="planLoading" :data="plans" stripe border size="small">
        <el-table-column prop="code" label="套餐码" width="140" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="price" label="价格" width="100" align="right" />
        <el-table-column prop="cycle" label="周期" width="100" />
        <el-table-column prop="status" label="状态" width="88" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :loading="subscribing === row.code" @click="subscribe(row.code)">
              订阅
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!planLoading && plans.length === 0" class="muted">暂无上架套餐（可在库内种子数据或运营后台配置）。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { apiJson } from "../api/client";

const amount = ref(1000);
const loading = ref(false);
const result = ref<{ orderNo: string; status: string } | null>(null);

type Plan = { id: number; code: string; name: string; price: number; cycle: string; status: string };
const plans = ref<Plan[]>([]);
const planLoading = ref(false);
const planErr = ref("");
const subscribing = ref<string | null>(null);

async function loadPlans() {
  planLoading.value = true;
  planErr.value = "";
  try {
    plans.value = await apiJson<Plan[]>("/billing/plans");
  } catch (e) {
    planErr.value = e instanceof Error ? e.message : "套餐加载失败";
  } finally {
    planLoading.value = false;
  }
}

async function submit() {
  loading.value = true;
  result.value = null;
  try {
    const data = await apiJson<{ orderNo: string; status: string }>("/payments/mock/recharge", {
      method: "POST",
      body: JSON.stringify({ amount: amount.value }),
    });
    result.value = data;
    ElMessage.success("已提交充值");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "失败");
  } finally {
    loading.value = false;
  }
}

async function subscribe(planCode: string) {
  try {
    await ElMessageBox.confirm(`确认订阅套餐「${planCode}」？（将调用 POST /billing/subscribe）`, "订阅确认", {
      type: "info",
    });
  } catch {
    return;
  }
  subscribing.value = planCode;
  try {
    await apiJson("/billing/subscribe", {
      method: "POST",
      body: JSON.stringify({ planCode }),
    });
    ElMessage.success("订阅成功");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "订阅失败");
  } finally {
    subscribing.value = null;
  }
}

onMounted(loadPlans);
</script>

<style scoped>
.h2 {
  margin-top: 0;
  font-size: 20px;
  font-weight: 700;
}
.hint {
  color: #64748b;
  margin-bottom: 16px;
  font-size: 13px;
  line-height: 1.6;
}
.card-block {
  margin-bottom: 20px;
  border-radius: 12px !important;
  border: 1px solid #e8ecf1 !important;
}
.card-ttl {
  font-weight: 600;
}
.card-sub {
  margin-left: 12px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
}
.mb {
  margin-bottom: 12px;
}
.muted {
  margin: 12px 0 0;
  font-size: 13px;
  color: #94a3b8;
}
</style>
