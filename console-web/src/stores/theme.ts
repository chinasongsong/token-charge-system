import { defineStore } from "pinia";
import { ref } from "vue";

export const useThemeStore = defineStore("theme", () => {
  const dark = ref(false);

  function initFromStorage() {
    dark.value = false;
    document.documentElement.classList.remove("dark");
  }

  function setDark(_value: boolean) {
    dark.value = false;
    document.documentElement.classList.remove("dark");
  }

  function toggle() {
    dark.value = false;
    document.documentElement.classList.remove("dark");
  }

  return { dark, initFromStorage, setDark, toggle };
});
