<template>
  <div>
    <h2>模拟充值</h2>
    <p class="hint">调用 <code>POST /payments/mock/recharge</code>，入账走 billing 幂等。</p>
    <el-form inline @submit.prevent="submit">
      <el-form-item label="金额 (TOKEN)">
        <el-input-number v-model="amount" :min="1" :step="100" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">充值</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="result" type="success" :title="`订单 ${result.orderNo} 状态 ${result.status}`" show-icon />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { apiJson } from "../api/client";

const amount = ref(1000);
const loading = ref(false);
const result = ref<{ orderNo: string; status: string } | null>(null);

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
</script>

<style scoped>
.hint {
  color: #666;
  margin-bottom: 16px;
}
h2 {
  margin-top: 0;
}
</style>
