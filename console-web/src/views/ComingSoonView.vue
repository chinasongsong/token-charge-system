<template>
  <div class="wrap">
    <el-card shadow="never" class="card">
      <template #header>
        <span>{{ title }}</span>
      </template>
      <p class="desc">{{ description }}</p>
      <div class="actions">
        <RouterLink v-slot="{ navigate }" custom to="/console/dashboard">
          <el-button type="primary" plain @click="navigate">返回总览</el-button>
        </RouterLink>
        <RouterLink v-slot="{ navigate }" custom to="/experience">
          <el-button text type="primary" @click="navigate">模型体验</el-button>
        </RouterLink>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const title = computed(() => (route.meta.title as string) || "即将上线");
const description = computed(
  () =>
    (route.meta.description as string) ||
    "该页与 plan.md §15.5 信息架构一致，接口与交互正在按阶段接入。"
);
</script>

<style scoped>
.wrap {
  max-width: 720px;
}
.card {
  border-radius: var(--th-radius-md);
  border: 1px solid var(--th-border);
}
.desc {
  margin: 0 0 20px;
  color: var(--th-text-secondary);
  line-height: 1.65;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
