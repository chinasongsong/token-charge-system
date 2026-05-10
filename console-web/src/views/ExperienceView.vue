<template>
  <div class="page">
    <div class="side-type"><div class="side-type-item active"><el-icon><ChatDotRound /></el-icon><span>文本对话</span></div></div>
    <div class="panel-params">
      <h3 class="panel-title">参数设置</h3>
      <el-form label-position="top" class="param-form">
        <el-form-item label="System Prompt"><el-input v-model="systemPrompt" type="textarea" :rows="4" placeholder="可选系统提示词" /></el-form-item>
        <el-form-item label="Temperature"><el-slider v-model="temperature" :min="0" :max="2" :step="0.1" show-input /></el-form-item>
        <el-form-item label="Top P"><el-slider v-model="topP" :min="0" :max="1" :step="0.05" show-input /></el-form-item>
        <el-form-item label="Frequency Penalty"><el-slider v-model="freqPenalty" :min="-2" :max="2" :step="0.1" show-input /></el-form-item>
        <el-form-item label="Presence Penalty"><el-slider v-model="presPenalty" :min="-2" :max="2" :step="0.1" show-input /></el-form-item>
        <el-form-item label="Max Tokens"><el-input-number v-model="maxTokens" :min="256" :max="128000" :step="256" style="width:100%" /></el-form-item>
      </el-form>
    </div>

    <div class="chat-wrap">
      <div class="chat-toolbar">
        <el-select v-model="modelId" placeholder="选择模型" filterable style="width:240px" :loading="loadingModels">
          <el-option v-for="m in models" :key="m.id" :label="m.id" :value="m.id" />
        </el-select>
        <span class="muted th-mono">{{ modelId || "-" }}</span>
      </div>

      <div class="chat-scroll" ref="scrollRef">
        <div v-if="!session.accessToken" class="empty-login">
          <p>登录后才可调用 <code class="th-mono">/v1/chat/completions</code>。</p>
          <RouterLink to="/login"><el-button type="primary">去登录</el-button></RouterLink>
        </div>
        <template v-else-if="messages.filter((m) => m.role !== 'system').length === 0">
          <div class="empty-chat"><div class="empty-logo">*</div><p class="model-hint">{{ modelId || "选择模型后开始对话" }}</p></div>
        </template>
        <div v-else class="bubble-list">
          <div v-for="(m, i) in bubbles" :key="i" class="bubble" :class="m.role">
            <span class="role">{{ m.role === "user" ? "你" : "模型" }}</span>
            <div class="content">{{ m.content }}</div>
          </div>
        </div>
      </div>

      <div class="hints" v-if="session.accessToken">
        <el-button round size="small" @click="input='用三句话概括 Spring Cloud Gateway 的职责'">网关总结</el-button>
        <el-button round size="small" @click="input='写一首关于晚霞的短诗'">晚霞短诗</el-button>
        <el-button round size="small" @click="input='把这句话改成更礼貌的表达：为什么这个还没好？'">语气润色</el-button>
      </div>

      <div class="composer">
        <el-input v-model="input" type="textarea" :rows="2" :disabled="!session.accessToken || sending" placeholder="输入你的问题" @keydown.enter.ctrl="send" />
        <el-button type="primary" class="send" :loading="sending" :disabled="!session.accessToken" @click="send">发送</el-button>
      </div>
      <p class="shortcut-hint th-mono">Ctrl + Enter 发送</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound } from "@element-plus/icons-vue";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { chatCompletions, fetchModels } from "../api/gateway";
import { useSessionStore } from "../stores/session";

type Msg = { role: "system" | "user" | "assistant"; content: string };

const session = useSessionStore();
const models = ref<{ id: string }[]>([]);
const loadingModels = ref(false);
const modelId = ref("");
const systemPrompt = ref("你是一个有帮助的助手。");
const temperature = ref(1);
const topP = ref(0.7);
const freqPenalty = ref(0);
const presPenalty = ref(0);
const maxTokens = ref(8192);
const messages = ref<Msg[]>([]);
const input = ref("");
const sending = ref(false);
const scrollRef = ref<HTMLElement | null>(null);

const bubbles = computed(() => messages.value.filter((m) => m.role !== "system"));

async function loadModels() {
  if (!session.accessToken) return;
  loadingModels.value = true;
  try {
    const list = await fetchModels();
    models.value = list;
    if (list.length && !modelId.value) modelId.value = list[0].id;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "加载模型失败");
    models.value = [{ id: "deepseek-v4-flash" }];
    modelId.value = "deepseek-v4-flash";
  } finally {
    loadingModels.value = false;
  }
}

function buildPayload() {
  const core: Msg[] = [];
  const sys = systemPrompt.value.trim();
  if (sys) core.push({ role: "system", content: sys });
  core.push(...messages.value.filter((m) => m.role !== "system"));
  return { model: modelId.value, messages: core, temperature: temperature.value, top_p: topP.value, frequency_penalty: freqPenalty.value, presence_penalty: presPenalty.value, max_tokens: maxTokens.value };
}

async function send() {
  if (!session.accessToken) return;
  const text = input.value.trim();
  if (!text || !modelId.value) {
    ElMessage.warning("请选择模型并输入消息");
    return;
  }
  sending.value = true;
  messages.value.push({ role: "user", content: text });
  input.value = "";
  await nextTick();
  scrollBottom();
  try {
    const resp = (await chatCompletions(buildPayload())) as { choices?: Array<{ message?: { content?: string } }> };
    const content = resp?.choices?.[0]?.message?.content ?? "(无内容)";
    messages.value.push({ role: "assistant", content });
  } catch (e) {
    messages.value.pop();
    ElMessage.error(e instanceof Error ? e.message : "请求失败");
  } finally {
    sending.value = false;
    await nextTick();
    scrollBottom();
  }
}

function scrollBottom() {
  const el = scrollRef.value;
  if (el) el.scrollTop = el.scrollHeight;
}

watch(() => session.accessToken, loadModels);
onMounted(loadModels);
</script>

<style scoped>
.page { display:grid; grid-template-columns:72px minmax(220px,280px) 1fr; min-height:calc(100vh - 56px); background:#f6f8ff; }
.side-type,.panel-params { background:#fff; border-right:1px solid #dfe6fb; }
.side-type { padding-top:16px; display:flex; justify-content:center; }
.side-type-item { display:flex; flex-direction:column; align-items:center; gap:6px; padding:12px 8px; border-radius:10px; font-size:12px; color:#4f46e5; background:rgba(99,102,241,.1); width:56px; text-align:center; }
.panel-params { padding:16px; overflow-y:auto; }
.panel-title { margin:0 0 12px; font-size:15px; font-weight:600; }
.chat-wrap { display:flex; flex-direction:column; min-height:calc(100vh - 56px); padding:12px 20px 20px; }
.chat-toolbar { display:flex; align-items:center; gap:12px; margin-bottom:10px; }
.muted { color:#64748b; font-size:13px; }
.chat-scroll { flex:1; overflow-y:auto; background:#fff; border:1px solid #dfe6fb; border-radius:12px; padding:20px; min-height:280px; }
.empty-login,.empty-chat { height:100%; min-height:240px; display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; gap:12px; color:#64748b; }
.empty-logo { font-size:48px; color:#c7d2fe; }
.model-hint { font-weight:600; color:#4f46e5; }
.bubble-list { display:flex; flex-direction:column; gap:16px; }
.bubble .role { font-size:12px; color:#94a3b8; }
.bubble .content { margin-top:4px; white-space:pre-wrap; line-height:1.65; }
.bubble.user .content { background:rgba(99,102,241,.08); padding:10px 14px; border-radius:12px; display:inline-block; max-width:92%; }
.hints { display:flex; flex-wrap:wrap; gap:8px; margin:10px 0; }
.composer { display:flex; gap:12px; align-items:flex-end; }
.composer :deep(.el-textarea__inner) { border-radius:12px; }
.send { border-radius:10px !important; align-self:stretch; padding:0 22px !important; }
.shortcut-hint { margin:6px 0 0; font-size:12px; color:#94a3b8; }
@media (max-width:960px) { .page { grid-template-columns:1fr; grid-template-rows:auto auto 1fr; } .side-type { flex-direction:row; justify-content:flex-start; padding:8px; } .panel-params { max-height:220px; } }
</style>

