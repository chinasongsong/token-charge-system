# console-web 文档

C 端用户控制台：Vue 3 + Vite + Pinia + Element Plus；经**网关**访问 user-center / billing / payment。

## 阅读顺序

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [00-前端总览.md](./00-前端总览.md) | 技术栈、与后端边界 |
| 2 | [pages/01-路由与鉴权.md](./pages/01-路由与鉴权.md) | `router`、登录守卫 |
| 3 | [pages/02-API客户端.md](./pages/02-API客户端.md) | `api/client.ts`、`gateway.ts` |
| 4 | [pages/03-会话状态.md](./pages/03-会话状态.md) | Pinia `session` |
| 5 | [pages/04-控制台页面.md](./pages/04-控制台页面.md) | Dashboard、Key、充值等 |
| 6 | [08-构建与配置.md](./08-构建与配置.md) | Vite、`.env` |

## 源码入口

| 类型 | 路径 |
|------|------|
| 入口 | `src/main.ts`, `src/App.vue` |
| 路由 | `src/router/index.ts` |
| API | `src/api/` |
| 页面 | `src/views/` |
