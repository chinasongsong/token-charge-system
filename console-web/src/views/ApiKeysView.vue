<template>
  <div class="page">
    <div class="head">
      <div>
        <h2 class="h2">API Key 管理</h2>
        <p class="sub">对接 billing：<code class="th-mono">GET/POST/PATCH /apikeys</code>，密钥仅创建时完整展示一次。</p>
      </div>
      <el-button type="primary" :loading="creating" @click="openCreate">创建新 Key</el-button>
    </div>

    <el-alert type="info" show-icon :closable="false" class="mb">
      调用开放平台 <code class="th-mono">/v1/chat/completions</code> 时，可使用
      <code class="th-mono">Authorization: Bearer sk_tokenhub_…</code>（网关将按指纹解析用户并计费）；与登录 JWT 二选一。
    </el-alert>

    <el-table v-loading="loading" :data="rows" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatApiDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ACTIVE'"
            link
            type="danger"
            size="small"
            :loading="disablingId === row.id"
            @click="disable(row.id)"
          >
            禁用
          </el-button>
          <span v-else class="muted">已禁用</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="nameDialog" title="创建 API Key" width="420px" @close="newKeyName = ''">
      <el-form label-width="72px">
        <el-form-item label="备注名">
          <el-input v-model="newKeyName" maxlength="191" placeholder="可选，便于区分用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nameDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { apiJson } from "../api/client";
import { formatApiDate } from "../utils/formatApiDate";

type Row = { id: number; name: string; status: string; createdAt: string };

const rows = ref<Row[]>([]);
const loading = ref(false);
const creating = ref(false);
const disablingId = ref<number | null>(null);
const nameDialog = ref(false);
const newKeyName = ref("");

async function load() {
  loading.value = true;
  try {
    rows.value = await apiJson<Row[]>("/apikeys");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  newKeyName.value = "";
  nameDialog.value = true;
}

async function confirmCreate() {
  creating.value = true;
  try {
    const body: { name?: string } = {};
    if (newKeyName.value.trim()) body.name = newKeyName.value.trim();
    const created = await apiJson<{ id: number; name: string; plaintextKey: string; status: string; createdAt: string }>(
      "/apikeys",
      { method: "POST", body: JSON.stringify(Object.keys(body).length ? body : {}) }
    );
    nameDialog.value = false;
    await ElMessageBox.alert(
      `请立即复制保存，关闭后无法再次查看完整密钥：\n\n${created.plaintextKey}`,
      "新密钥（仅展示一次）",
      {
        confirmButtonText: "已复制",
        type: "success",
        customClass: "wide-secret-box",
      }
    );
    await load();
  } catch (e) {
    if (e !== "close" && e !== "cancel") {
      ElMessage.error(e instanceof Error ? e.message : "创建失败");
    }
  } finally {
    creating.value = false;
  }
}

async function disable(id: number) {
  try {
    await ElMessageBox.confirm("禁用后该 Key 无法再调用网关，确认？", "禁用 API Key", { type: "warning" });
  } catch {
    return;
  }
  disablingId.value = id;
  try {
    await apiJson(`/apikeys/${id}`, {
      method: "PATCH",
      body: JSON.stringify({ status: "DISABLED" }),
    });
    ElMessage.success("已禁用");
    await load();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "操作失败");
  } finally {
    disablingId.value = null;
  }
}

onMounted(load);
</script>

<style scoped>
.page {
  max-width: 1100px;
}
.h2 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
}
.sub {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.mb {
  margin-bottom: 16px;
}
.muted {
  color: #94a3b8;
  font-size: 13px;
}
</style>
