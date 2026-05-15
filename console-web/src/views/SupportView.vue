<template>
  <div>
    <h2 class="h2">工单中心</h2>
    <p class="sub">对接 <code class="th-mono">/user/support/tickets</code> 与消息线程接口。</p>

    <el-form inline @submit.prevent="create" class="form-row">
      <el-form-item label="标题">
        <el-input v-model="title" placeholder="新问题标题" style="width: 280px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">创建</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="tickets"
      border
      stripe
      v-loading="loadingList"
      highlight-current-row
      @row-click="openTicket"
      class="table-click"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column label="更新" width="180">
        <template #default="{ row }">{{ formatApiDate(row.updatedAt) }}</template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawer" :title="drawerTitle" size="420px" destroy-on-close @closed="onDrawerClosed">
      <div v-loading="msgLoading" class="drawer-body">
        <div v-for="m in messages" :key="m.id" class="msg" :class="m.role === 'USER' ? 'user' : 'other'">
          <div class="msg-meta">
            <span class="role">{{ m.role === "USER" ? "我" : m.role }}</span>
            <span class="time">{{ formatApiDate(m.createdAt) }}</span>
          </div>
          <div class="msg-body">{{ m.body }}</div>
        </div>
        <p v-if="!msgLoading && messages.length === 0" class="muted">暂无消息</p>
      </div>
      <template #footer>
        <div class="reply-row">
          <el-input v-model="replyBody" type="textarea" :rows="3" placeholder="输入回复..." />
          <el-button type="primary" class="send-btn" :loading="replying" :disabled="!activeId" @click="sendReply">
            发送
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { apiJson } from "../api/client";
import { formatApiDate } from "../utils/formatApiDate";

type Ticket = {
  id: number;
  title: string;
  status: string;
  priority: string;
  createdAt: unknown;
  updatedAt: unknown;
};

type Msg = { id: number; ticketId: number; userId: number; role: string; body: string; createdAt: unknown };

const tickets = ref<Ticket[]>([]);
const title = ref("");
const loading = ref(false);
const loadingList = ref(false);
const drawer = ref(false);
const activeId = ref<number | null>(null);
const drawerTitle = computed(() => (activeId.value ? `工单 #${activeId.value}` : "工单"));
const messages = ref<Msg[]>([]);
const msgLoading = ref(false);
const replyBody = ref("");
const replying = ref(false);

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

async function openTicket(row: Ticket) {
  activeId.value = row.id;
  drawer.value = true;
  replyBody.value = "";
  await loadMessages();
}

async function loadMessages() {
  if (!activeId.value) return;
  msgLoading.value = true;
  try {
    messages.value = await apiJson<Msg[]>(`/user/support/tickets/${activeId.value}/messages`);
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "消息加载失败");
    messages.value = [];
  } finally {
    msgLoading.value = false;
  }
}

async function sendReply() {
  if (!activeId.value || !replyBody.value.trim()) {
    ElMessage.warning("请输入回复内容");
    return;
  }
  replying.value = true;
  try {
    await apiJson(`/user/support/tickets/${activeId.value}/messages`, {
      method: "POST",
      body: JSON.stringify({ body: replyBody.value.trim() }),
    });
    replyBody.value = "";
    await loadMessages();
    await load();
    ElMessage.success("已发送");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "发送失败");
  } finally {
    replying.value = false;
  }
}

function onDrawerClosed() {
  activeId.value = null;
  messages.value = [];
}

onMounted(load);
</script>

<style scoped>
.h2 {
  margin-top: 0;
  font-size: 20px;
  font-weight: 700;
}
.sub {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
}
.form-row {
  margin-bottom: 16px;
}
.table-click :deep(.el-table__row) {
  cursor: pointer;
}
.drawer-body {
  min-height: 200px;
  max-height: calc(100vh - 260px);
  overflow-y: auto;
}
.msg {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e8ecf1;
}
.msg.user {
  background: rgba(99, 102, 241, 0.08);
  border-color: #c7d2fe;
}
.msg-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 6px;
}
.msg-body {
  white-space: pre-wrap;
  line-height: 1.55;
  font-size: 14px;
  color: #334155;
}
.reply-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.send-btn {
  align-self: flex-end;
}
.muted {
  color: #94a3b8;
  font-size: 13px;
}
</style>
