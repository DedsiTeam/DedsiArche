# DedsiArche 前端

独立的 React 管理端基础项目，技术栈参考 DedsiNatives 的 react-admin。依赖版本于 2026-10-28 从 npm 官方仓库核实，使用稳定版并提交 package-lock.json。

## 启动

使用 Node.js 22.13+（22.x）或 24+，推荐 Node.js 24 LTS。在仓库根目录执行：

```powershell
cd src/react
npm ci
npm run dev
```

访问 http://localhost:5173。首页不依赖后端服务。

需要覆盖默认配置时，将 `.env.example` 复制为 `.env.local`。开发服务器将 `/dedsi-arche-api` 请求代理到 `http://localhost:11518`，可通过 `API_PROXY_TARGET` 修改。Axios 客户端位于 `src/services/http.ts`。

## 目录

```text
src/
  layouts/       公共布局
  pages/         首页、404 及后续业务页面
  router/        路由配置
  services/      HTTP 客户端及后续业务接口
  styles/        全局样式
  main.tsx       应用入口
```

## 检查与构建

```powershell
npm run lint
npm run typecheck
npm run build
npm run preview
```

提供 development、prototype、test、production 模式，使用 `dev:prototype`、`dev:test`、`dev:prod` 以及对应的 `build:*` 命令。这里的 test 是环境模式，不是自动化测试。按需创建 `.env.[mode]`，所有模式默认使用相同基础配置。

构建产物位于 `dist`。生产服务器需要为前端路由配置回退到 `index.html`，并将 `/dedsi-arche-api` 转发到后端；Vite 开发代理不包含在构建产物中。也可在构建时设置 `VITE_API_BASE_URL` 为接口地址，并在后端配置对应 CORS。所有 `VITE_` 变量都会公开给浏览器，不应填写密钥。

TypeScript 沿用参考项目的双版本方案：`@typescript/native` 别名安装 TypeScript 7，使用其 `tsc` 构建；`typescript` 别名安装 TypeScript 6，供尚要求 `<6.1` API 的 typescript-eslint 使用。

保留 `oidc-client-ts` 依赖以对齐参考技术栈，尚未接入身份提供方或登录流程。当前 Java 后端使用自己的 JWT 接口，后续需按实际认证方案接入。
