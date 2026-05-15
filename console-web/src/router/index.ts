import { createRouter, createWebHistory } from "vue-router";
import ShellView from "../views/ShellView.vue";
import LandingView from "../views/LandingView.vue";
import DashboardView from "../views/DashboardView.vue";
import RechargeView from "../views/RechargeView.vue";
import SupportView from "../views/SupportView.vue";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import ExperienceView from "../views/ExperienceView.vue";
import UsageDetailView from "../views/UsageDetailView.vue";
import ApiKeysView from "../views/ApiKeysView.vue";
import BillingOrdersView from "../views/BillingOrdersView.vue";
import { useSessionStore } from "../stores/session";

export const ROUTE_CONSOLE_PREFIX = "/console";

function isPublicRoute(path: string): boolean {
  if (path === "/" || path === "/experience") {
    return true;
  }
  if (path.startsWith("/login") || path.startsWith("/register")) {
    return true;
  }
  return false;
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", name: "landing", component: LandingView },
    { path: "/experience", name: "experience", component: ExperienceView },
    { path: "/login", name: "login", component: LoginView },
    { path: "/register", name: "register", component: RegisterView },
    {
      path: ROUTE_CONSOLE_PREFIX,
      component: ShellView,
      redirect: `${ROUTE_CONSOLE_PREFIX}/dashboard`,
      children: [
        { path: "dashboard", name: "console-dashboard", component: DashboardView },
        { path: "api-keys", name: "console-api-keys", component: ApiKeysView },
        { path: "recharge", name: "console-recharge", component: RechargeView },
        { path: "recharge-records", name: "console-billing-orders", component: BillingOrdersView },
        {
          path: "usage",
          name: "console-usage",
          component: UsageDetailView,
        },
        {
          path: "support",
          name: "console-support",
          component: SupportView,
        },
      ],
    },
    { path: "/:pathMatch(.*)*", name: "not-found", redirect: "/" },
  ],
});

router.beforeEach((to) => {
  const session = useSessionStore();
  const token =
    session.accessToken || (typeof localStorage !== "undefined" ? localStorage.getItem("accessToken") : null);
  const isConsole = to.path === ROUTE_CONSOLE_PREFIX || to.path.startsWith(`${ROUTE_CONSOLE_PREFIX}/`);
  const pathOnly = to.path.split("?")[0] ?? to.path;
  if (isPublicRoute(pathOnly)) {
    return true;
  }
  if (isConsole && !token) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  return true;
});

export default router;
