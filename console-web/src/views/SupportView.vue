<template>
  <div>
    <h2>工单</h2>
    <el-form inline @submit.prevent="create">
      <el-form-item label="标题">
        <el-input v-model="title" placeholder="新问题标题" style="width: 280px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">创建</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="tickets" border v-loading="loadingList">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="updatedAt" label="更新" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { apiJson } from "../api/client";

type Ticket = {
  id: number;
  title: string;
  status: string;
  priority: string;
  createdAt: string;
  updatedAt: string;
};

const tickets = ref<Ticket[]>([]);
const title = ref("");
const loading = ref(false);
const loadingList = ref(false);

async function load() {
  loadingList.value = true;
  try {
    tickets.value = await apiJson<Ticket[]>("/user/support/tickets");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "加载失败");
  } finally {
    loadingList.value = false;
  }
}

async function create() {
  if (!title.value.trim()) {
    ElMessage.warning("请填写标题");
    return;
  }
  loading.value = true;
  try {
    await apiJson<Ticket>("/user/support/tickets", {
      method: "POST",
      body: JSON.stringify({ title: title.value.trim() }),
    });
    title.value = "";
    ElMessage.success("已创建");
    await load();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "失败");
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
h2 {
  margin-top: 0;
}
</style>
