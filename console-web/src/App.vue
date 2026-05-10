<template>
  <div class="app-root">
    <SiteTopNav />
    <main class="with-nav">
      <router-view />
    </main>
    <FloatingHelp />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import FloatingHelp from "./components/FloatingHelp.vue";
import SiteTopNav from "./components/SiteTopNav.vue";
import { useThemeStore } from "./stores/theme";

const theme = useThemeStore();

onMounted(() => {
  theme.initFromStorage();
});
</script>

<style>
.app-root {
  min-height: 100vh;
}
main.with-nav {
  min-height: calc(100vh - 56px);
}
</style>
