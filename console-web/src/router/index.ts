import { createRouter, createWebHistory } from "vue-router";
import ShellView from "../views/ShellView.vue";

export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      component: ShellView,
    },
  ],
});
