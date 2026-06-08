# AI-Platform Frontend

> Vue 3 + Vite + Element Plus 前端工程

## 技术栈

| 组件 | 版本 |
|---|---|
| Vue | 3.5.x |
| Vite | 6.x |
| TypeScript | 5.x |
| Element Plus | 2.8.x |
| Pinia | 2.1.x |
| Vue Router | 4.5.x |
| Axios | 1.x |
| LogicFlow | 2.0.x |
| ECharts | 5.6.x |
| markdown-it | 14.x |
| highlight.js | 11.x |
| CodeMirror | 5.x |
| Iconify | 3.x |

## 目录结构

```
ai-frontend/
├── src/
│   ├── api/                  # API 定义
│   │   ├── system/
│   │   ├── project/
│   │   ├── flow/
│   │   ├── assistant/
│   │   ├── knowledge/
│   │   ├── model/
│   │   ├── mcp/
│   │   └── prompt/
│   │
│   ├── components/           # 公共组件
│   │   ├── common/
│   │   ├── flow/             # 流程组件
│   │   ├── assistant/        # 助手组件
│   │   └── theme/            # 主题
│   │
│   ├── views/                # 页面
│   │   ├── login/
│   │   ├── dashboard/
│   │   ├── project/
│   │   ├── flow/             # 含 editor
│   │   ├── assistant/
│   │   ├── knowledge/
│   │   ├── prompt/
│   │   ├── model/
│   │   ├── mcp/
│   │   ├── monitor/
│   │   ├── system/
│   │   └── embed/
│   │
│   ├── layouts/              # 布局
│   │   ├── index.vue
│   │   └── components/
│   │
│   ├── router/               # 路由
│   ├── store/                # Pinia
│   ├── utils/                # 工具
│   ├── types/                # TypeScript 类型
│   ├── locales/              # i18n
│   ├── styles/               # 样式
│   ├── App.vue
│   └── main.ts
│
├── public/                   # 静态资源
├── index.html
├── vite.config.ts
├── package.json
├── tsconfig.json
└── .env.development
```

## 快速开始

### 环境要求

- Node.js 24.x
- npm 10.x(使用 `--legacy-peer-deps`)

### 安装依赖

```bash
npm install --legacy-peer-deps
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产

```bash
npm run build
```

产物在 `dist/` 目录。

### 预览生产构建

```bash
npm run preview
```

## 环境变量

### .env.development

```env
VITE_GLOB_API_URL=http://localhost:8080
VITE_GLOB_APP_TITLE=AI Platform
VITE_GLOB_TENANT_MODE=false
```

### .env.production

```env
VITE_GLOB_API_URL=/
VITE_GLOB_APP_TITLE=AI Platform
VITE_GLOB_TENANT_MODE=false
```

## 关键设计

### 状态管理

```typescript
// store/modules/user.ts
export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
    permissions: [],
    currentProjectId: null,
  }),
  persist: true,
});
```

### 路由守卫

```typescript
router.beforeEach(async (to, from) => {
  const userStore = useUserStore();
  
  if (!userStore.token && to.path !== '/login') {
    return '/login';
  }
  
  if (to.meta.requiresAuth && !userStore.permissions.includes(to.meta.permission)) {
    return '/403';
  }
});
```

### SSE 客户端

```typescript
import { SSEClient } from '@/utils/sse';

const sse = new SSEClient();
sse.connect('/api/v1/chat/stream', {
  onMessage: (chunk) => console.log(chunk),
  onError: (err) => console.error(err),
  onComplete: () => console.log('done'),
});
```

## 关键页面

详见 [docs/UI-DESIGN.md](../docs/UI-DESIGN.md)

## 状态

🟡 **待开发** - Sprint 0 启动前
