import { defineStore } from "pinia";
import { computed, ref } from "vue";

const STORAGE_KEY = "accessToken";

export const useSessionStore = defineStore("session", () => {
  const accessToken = ref(localStorage.getItem(STORAGE_KEY) ?? "");

  const isAuthenticated = computed(() => Boolean(accessToken.value));

  function setToken(token: string) {
    accessToken.value = token;
    localStorage.setItem(STORAGE_KEY, token);
  }

  function clear() {
    accessToken.value = "";
    localStorage.removeItem(STORAGE_KEY);
  }

  return { accessToken, isAuthenticated, setToken, clear };
});
