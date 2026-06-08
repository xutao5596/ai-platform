# 架构设计 (ARCHITECTURE)

> **版本**: v1.0
> **日期**: 2026-06-08
> **项目**: AI-Platform

---

## 1. 总体架构

### 1.1 系统分层

```
┌────────────────────────────────────────────────────────────────┐
│                     表示层 (Frontend)                           │
│  Vue 3 + Vite + Element Plus + Pinia + LogicFlow + ECharts   │
├────────────────────────────────────────────────────────────────┤
│                     网关层 (Nginx)                              │
│  反向代理 + 静态资源 + SSL 终止 + 限流                          │
├────────────────────────────────────────────────────────────────┤
│                     应用层 (Backend)                            │
│  Spring Boot 3.5 + Java 21                                    │
│  ├── Controller (REST API + SSE)                                │
│  ├── Service (业务逻辑)                                         │
│  ├── Flow Engine (LiteFlow)                                     │
│  ├── AI Engine (LangChain4j)                                   │
│  └── Assistant Engine (LLM + Tools + Events)                   │
├────────────────────────────────────────────────────────────────┤
│                     数据层 (Data)                                │
│  ├── MariaDB 12.2 (主存储)                                      │
│  ├── Hnswlib (向量索引,文件存储)                                │
│  ├── Caffeine (本地缓存)                                        │
│  └── 本地文件系统 (上传文件、日志)                              │
└────────────────────────────────────────────────────────────────┘
```

### 1.2 部署拓扑

```
┌─────────────────────────────────────────────────────────────┐
│                   Internet / Users                           │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTPS
┌────────────────────▼────────────────────────────────────────┐
│              Nginx (80/443) - 1 instance                     │
│  ├─ /api/*  → proxy_pass http://127.0.0.1:8080              │
│  └─ /*      → serve /opt/ai-platform/frontend/dist          │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│         ai-platform-backend.jar - 1-2 instances              │
│         Port: 8080  (systemd 管理)                            │
└────┬──────────────────┬────────────────────┬────────────────┘
     │                  │                    │
┌────▼─────┐    ┌───────▼──────┐    ┌───────▼────────┐
│ MariaDB  │    │  Hnswlib     │    │  File System   │
│ :3306    │    │  /data/hnsw/ │    │  /data/upload/ │
│ root/root│    │  *.bin       │    │  /data/logs/   │
└──────────┘    └──────────────┘    └────────────────┘
```

---

## 2. 后端架构

### 2.1 模块划分

```
ai-backend/                        # Maven 多模块
├── ai-common/                     # 公共工具、异常、常量
│   ├── constant/                  # 常量定义
│   ├── exception/                 # 业务异常
│   ├── util/                      # 工具类
│   └── api/                       # 通用 DTO
│
├── ai-framework/                  # 基础框架(可复用)
│   ├── shiro/                     # 权限认证
│   ├── jwt/                       # JWT 工具
│   ├── web/                       # Web 配置(全局异常/响应包装)
│   ├── cache/                     # Caffeine 缓存
│   ├── log/                       # 日志 + 操作日志切面
│   ├── security/                  # 安全相关
│   └── signature/                 # 接口签名
│
├── ai-system/                     # 系统管理
│   ├── controller/                # 用户/角色/部门/菜单/字典/日志
│   ├── service/
│   ├── entity/
│   ├── mapper/
│   └── dto/
│
├── ai-project/                    # 项目域
│   ├── controller/
│   ├── service/
│   ├── entity/                    # Project / Member / ApiKey / Webhook
│   ├── interceptor/               # 项目角色拦截器
│   └── annotation/                # @PreProjectRole
│
├── ai-flow/                       # 流程引擎
│   ├── spi/                       # FlowNode 接口
│   ├── registry/                  # NodeRegistry
│   ├── executor/                  # FlowExecutor + ChainBuilder
│   ├── trigger/                   # 5 种触发器实现
│   ├── controller/
│   ├── service/
│   ├── entity/                    # Flow / Version / Run / Step / Trigger / CustomNode
│   └── nodes/                     # 12 个内置节点
│
├── ai-ai/                         # AI 业务
│   ├── model/                     # 大模型管理
│   ├── mcp/                       # MCP 服务
│   ├── knowledge/                 # 知识库
│   ├── prompt/                    # 提示词
│   ├── chat/                      # AI 对话
│   └── embedding/                 # Embedding 抽象
│
├── ai-assistant/                  # AI 助手(可选独立)
│   ├── controller/
│   ├── service/
│   ├── tools/                     # 系统工具集
│   └── events/                    # 事件订阅
│
├── ai-start/                      # 启动模块
│   ├── Application.java
│   ├── config/                    # MyBatis Plus / Shiro / Swagger
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── db/migration/          # Flyway 脚本
│
└── pom.xml
```

### 2.2 核心架构图

#### 2.2.1 整体架构

```
┌──────────────────────────────────────────────────────────────┐
│                     HTTP Client (Frontend)                     │
└────────────────────────┬─────────────────────────────────────┘
                         │ JSON over HTTPS
┌────────────────────────▼─────────────────────────────────────┐
│                Shiro Filter + Spring MVC                       │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  @RestController (REST API)                              │ │
│  │  ↓                                                       │ │
│  │  @PreProjectRole("project:developer")  (项目权限校验)   │ │
│  │  ↓                                                       │ │
│  │  Service Layer (业务逻辑)                                 │ │
│  │  ↓                                                       │ │
│  │  FlowEngine (LiteFlow) / AIEngine (LangChain4j)          │ │
│  │  ↓                                                       │ │
│  │  MyBatis Plus → MariaDB                                  │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

#### 2.2.2 AI 对话流程

```
┌──────────┐     ┌────────────┐     ┌────────────┐     ┌─────────────┐
│ Browser  │────>│ Controller │────>│ ChatService│────>│ LLMHandler  │
│ (SSE)    │<────│  (SSE)     │<────│            │<────│ (LangChain4j)│
└──────────┘     └────────────┘     └────────────┘     └─────────────┘
                        │                  │                     │
                        │                  ↓                     ↓
                        │           ┌────────────┐       ┌──────────────┐
                        │           │ ChatPersist│       │  LLM API     │
                        │           │  (MariaDB) │       │ (OpenAI/...) │
                        │           └────────────┘       └──────────────┘
                        │
                        ↓
                  ┌─────────────┐
                  │ Response    │
                  │ SseEmitter  │
                  └─────────────┘
```

#### 2.2.3 流程执行流程

```
┌────────┐
│ Trigger│ (Manual/Cron/Webhook/Event/Chained)
└───┬────┘
    │
    ↓
┌────────────────┐
│ FlowExecutor   │
│  ├ load flow   │
│  ├ create run  │
│  └ execute     │
└───┬────────────┘
    │
    ↓
┌────────────────┐
│ ChainBuilder   │
│  design → chain│
└───┬────────────┘
    │
    ↓
┌────────────────┐
│ LiteFlow Engine│
│  execute chain │
└───┬────────────┘
    │
    ├─→ Node 1 (Start)
    │     └→ step log
    ├─→ Node 2 (Knowledge Search)
    │     └→ Hnswlib query
    ├─→ Node 3 (LLM)
    │     └→ LangChain4j call
    │     └→ SSE stream back
    └─→ Node 4 (End)
          └→ final result
    │
    ↓
┌────────────────┐
│ Run Recorder   │
│  save run +    │
│  step history  │
└────────────────┘
```

#### 2.2.4 AI 助手架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Assistant Dialog                            │
│                                                              │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐  │
│  │ User Msg │───>│ Assistant    │───>│ LLM (with       │  │
│  │ + History│    │ Service      │    │ Function Call)  │  │
│  └──────────┘    └──────┬───────┘    └────────┬────────┘  │
│                          │                     │            │
│                          ↓                     ↓            │
│                  ┌──────────────┐    ┌─────────────────┐  │
│                  │ Context      │    │ Tool Router     │  │
│                  │ - System     │    │ ├─ HTTP Tool     │  │
│                  │   Prompt     │    │ ├─ MCP Tool      │  │
│                  │ - History    │    │ ├─ Subflow       │  │
│                  │ - Memory     │    │ ├─ System Tool   │  │
│                  │ - Variables  │    │ └─ Knowledge     │  │
│                  └──────────────┘    └─────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Event Bus (ApplicationEvent)                          │  │
│  │  ├─ flow.run.failed → trigger assistant               │  │
│  │  ├─ knowledge.doc.parsed → trigger assistant         │  │
│  │  └─ ...                                                │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 关键技术点

#### 2.3.1 SSE 流式响应

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter chatStream(@RequestParam String sessionId, @RequestBody ChatRequest req) {
    SseEmitter emitter = new SseEmitter(60_000L);
    
    // 异步执行 LLM
    chatService.streamChat(sessionId, req)
        .subscribe(
            chunk -> emitter.send(SseEmitter.event().data(chunk)),
            error -> emitter.completeWithError(error),
            () -> emitter.complete()
        );
    
    return emitter;
}
```

#### 2.3.2 项目角色拦截器

```java
@PreProjectRole("project:developer")  // 需要 developer 及以上角色
@PostMapping("/project/{projectId}/flow")
public Result<AiFlow> createFlow(@PathVariable Long projectId, @RequestBody FlowDTO dto) {
    // 业务逻辑
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreProjectRole {
    String value() default "project:viewer";  // 最低角色要求
}
```

#### 2.3.3 流程节点 SPI

```java
public interface FlowNode {
    String getTypeKey();
    String getName();
    String getCategory();
    String getIcon();
    NodeSchema getInputSchema();
    NodeSchema getOutputSchema();
    NodeExecuteResult execute(NodeContext ctx) throws Exception;
}

@Component
public class LLMNode implements FlowNode {
    @Override
    public String getTypeKey() { return "llm"; }
    
    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        // 复用 LLMHandler
        ChatResponse response = llmHandler.chat(ctx.getModelId(), ctx.getMessages());
        return NodeExecuteResult.success(response.getData());
    }
}
```

#### 2.3.4 Hnswlib 集成

```java
@Component
public class HnswlibVectorStore {
    private final Map<Long, HnswIndex> indexes = new ConcurrentHashMap<>();
    
    public void addVectors(Long kbId, List<float[]> vectors, List<Long> ids) {
        HnswIndex index = getOrCreateIndex(kbId);
        for (int i = 0; i < vectors.size(); i++) {
            index.addItem(vectors.get(i), ids.get(i));
        }
    }
    
    public List<SearchResult> search(Long kbId, float[] query, int topK) {
        HnswIndex index = getOrCreateIndex(kbId);
        return index.search(query, topK);
    }
    
    public void save(Long kbId) {
        HnswIndex index = indexes.get(kbId);
        if (index != null) {
            index.save(getIndexFile(kbId));  // 持久化到 /data/hnsw/kb_xxx.bin
        }
    }
}
```

---

## 3. 前端架构

### 3.1 目录结构

```
ai-frontend/
├── src/
│   ├── api/                       # API 接口定义
│   │   ├── system/                # 系统管理
│   │   ├── project/               # 项目域
│   │   ├── flow/                  # 流程
│   │   ├── assistant/             # 助手
│   │   ├── knowledge/             # 知识库
│   │   ├── model/                 # 模型
│   │   ├── mcp/                   # MCP
│   │   └── prompt/                # 提示词
│   │
│   ├── components/                # 公共组件
│   │   ├── common/                # 通用(Button、Input 等封装)
│   │   ├── flow/                  # 流程相关
│   │   │   ├── NodeCard.vue
│   │   │   ├── NodeProperty.vue
│   │   │   ├── ParamMapping.vue
│   │   │   └── JsonSchemaForm.vue
│   │   ├── assistant/             # 助手相关
│   │   │   ├── AssistantDialog.vue
│   │   │   ├── AssistantMessage.vue
│   │   │   └── ToolCallDisplay.vue
│   │   └── theme/                 # 主题切换
│   │       └── ThemeSwitcher.vue
│   │
│   ├── views/                     # 页面
│   │   ├── login/                 # 登录
│   │   ├── dashboard/             # 工作台
│   │   ├── project/               # 项目域
│   │   ├── flow/                  # 流程
│   │   │   ├── list/
│   │   │   └── editor/
│   │   │       ├── FlowEditor.vue
│   │   │       ├── NodePanel.vue
│   │   │       ├── Canvas.vue
│   │   │       ├── PropertyPanel.vue
│   │   │       ├── DebugPanel.vue
│   │   │       └── nodes/         # 12+ 节点配置组件
│   │   ├── assistant/             # 助手
│   │   ├── knowledge/             # 知识库
│   │   ├── prompt/                # 提示词
│   │   ├── model/                 # 模型
│   │   ├── mcp/                   # MCP
│   │   ├── monitor/               # 监控
│   │   ├── system/                # 系统管理
│   │   └── embed/                 # iframe 嵌入页
│   │
│   ├── layouts/                   # 布局
│   │   ├── index.vue
│   │   ├── components/
│   │   │   ├── SideBar.vue        # 左侧栏
│   │   │   ├── TopBar.vue         # 顶栏
│   │   │   ├── ProjectDropdown.vue
│   │   │   ├── AssistantFloat.vue # 助手浮窗
│   │   │   └── ...
│   │   └── composables/
│   │
│   ├── router/                    # 路由
│   │   ├── index.ts
│   │   ├── routes.ts
│   │   └── guards.ts
│   │
│   ├── store/                     # Pinia
│   │   ├── modules/
│   │   │   ├── user.ts            # 用户、Token
│   │   │   ├── permission.ts      # 权限、动态路由
│   │   │   ├── project.ts         # 当前项目
│   │   │   ├── theme.ts           # 主题
│   │   │   ├── assistant.ts       # 助手状态
│   │   │   └── ...
│   │   └── index.ts
│   │
│   ├── utils/                     # 工具
│   │   ├── http/                  # Axios 封装
│   │   ├── auth/                  # 认证
│   │   ├── sse/                   # SSE 客户端
│   │   ├── format/                # 格式化
│   │   └── ...
│   │
│   ├── types/                     # TypeScript 类型
│   │   ├── api.d.ts
│   │   ├── flow.d.ts
│   │   └── ...
│   │
│   ├── locales/                   # i18n
│   │   ├── zh-CN.ts
│   │   └── en-US.ts
│   │
│   ├── styles/                    # 全局样式
│   │   ├── index.scss
│   │   ├── variables.scss         # CSS 变量(主题)
│   │   └── dark.scss              # 暗色模式
│   │
│   ├── App.vue
│   ├── main.ts
│   └── env.d.ts
│
├── public/                        # 静态资源
├── index.html
├── vite.config.ts
├── package.json
├── tsconfig.json
└── .env.development
```

### 3.2 状态管理 (Pinia)

```typescript
// store/modules/user.ts
export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null as UserInfo | null,
    permissions: [] as string[],
    currentProjectId: null as number | null,
  }),
  actions: {
    async login(credentials) { ... },
    async fetchUserInfo() { ... },
    logout() { ... },
  },
  persist: true,  // localStorage 持久化
});

// store/modules/theme.ts
export const useThemeStore = defineStore('theme', {
  state: () => ({
    colorScheme: 'blue' as 'blue' | 'purple' | 'green',
    mode: 'light' as 'light' | 'dark' | 'auto',
    accentColor: '#409EFF',
  }),
  actions: {
    setColorScheme(scheme) { ... },
    setMode(mode) { ... },
    applyTheme() { /* 修改 CSS 变量 */ },
  },
});

// store/modules/assistant.ts
export const useAssistantStore = defineStore('assistant', {
  state: () => ({
    floatVisible: false,
    currentAssistantId: null as number | null,
    currentSessionId: null as string | null,
    messages: [] as AssistantMessage[],
  }),
  actions: {
    async sendMessage(content) {
      // SSE 流式接收
    },
  },
});
```

### 3.3 路由设计

```typescript
// router/routes.ts
export const routes = [
  // 静态路由
  { path: '/login', component: () => import('@/views/login/index.vue') },
  { path: '/403', component: () => import('@/views/error/403.vue') },
  { path: '/404', component: () => import('@/views/error/404.vue') },
  
  // 主布局(动态路由)
  {
    path: '/',
    component: () => import('@/layouts/index.vue'),
    redirect: '/dashboard',
    children: [
      // 工作台
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue') },
      
      // 项目
      { path: 'project', component: () => import('@/views/project/list.vue') },
      { 
        path: 'project/:id', 
        component: () => import('@/views/project/detail.vue'),
        children: [
          { path: '', redirect: 'overview' },
          { path: 'overview', component: () => import('@/views/project/tabs/overview.vue') },
          { path: 'flow', component: () => import('@/views/flow/list.vue') },
          { path: 'flow/:flowId', component: () => import('@/views/flow/editor/index.vue') },
          { path: 'assistant', component: () => import('@/views/assistant/list.vue') },
          { path: 'knowledge', component: () => import('@/views/knowledge/list.vue') },
          { path: 'prompt', component: () => import('@/views/prompt/list.vue') },
          { path: 'member', component: () => import('@/views/project/member.vue') },
          { path: 'apikey', component: () => import('@/views/project/apikey.vue') },
          { path: 'webhook', component: () => import('@/views/project/webhook.vue') },
          { path: 'settings', component: () => import('@/views/project/settings.vue') },
        ]
      },
      
      // AI 资源
      { path: 'ai/model', component: () => import('@/views/model/list.vue') },
      { path: 'ai/mcp', component: () => import('@/views/mcp/list.vue') },
      
      // 监控
      { path: 'monitor', component: () => import('@/views/monitor/index.vue') },
      
      // 系统管理
      { 
        path: 'system', 
        component: () => import('@/views/system/index.vue'),
        children: [
          { path: 'user', component: () => import('@/views/system/user.vue') },
          { path: 'role', component: () => import('@/views/system/role.vue') },
          { path: 'dept', component: () => import('@/views/system/dept.vue') },
          { path: 'menu', component: () => import('@/views/system/menu.vue') },
          { path: 'dict', component: () => import('@/views/system/dict.vue') },
          { path: 'log', component: () => import('@/views/system/log.vue') },
        ]
      },
    ]
  },
  
  // 助手对话(独立全屏)
  { path: '/chat/:sessionId', component: () => import('@/views/chat/fullscreen.vue') },
  
  // iframe 嵌入
  { path: '/embed/assistant/:id', component: () => import('@/views/embed/assistant.vue') },
];
```

### 3.4 SSE 客户端

```typescript
// utils/sse/index.ts
export class SSEClient {
  private eventSource: EventSource | null = null;
  
  connect(url: string, options: {
    onMessage: (data: any) => void;
    onError?: (error: any) => void;
    onComplete?: () => void;
    headers?: Record<string, string>;
  }) {
    // event-source-polyfill 支持自定义 headers
    this.eventSource = new EventSourcePolyfill(url, {
      headers: options.headers || {},
    });
    
    this.eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      options.onMessage(data);
    };
    
    this.eventSource.onerror = (error) => {
      options.onError?.(error);
    };
    
    this.eventSource.onopen = () => {
      // 连接建立
    };
  }
  
  close() {
    this.eventSource?.close();
    this.eventSource = null;
  }
}
```

---

## 4. 数据架构

### 4.1 数据库表关系图

参见 [SOW.md §5.2](SOW.md)

### 4.2 关键表结构

#### ai_project
```sql
CREATE TABLE ai_project (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  name         VARCHAR(100) NOT NULL,
  code         VARCHAR(50)  UNIQUE,
  description  VARCHAR(500),
  icon         VARCHAR(255),
  status       VARCHAR(20)  DEFAULT 'active',
  owner_id     BIGINT,
  settings     LONGTEXT,  -- JSON
  create_by    VARCHAR(50),
  create_time  DATETIME,
  update_by    VARCHAR(50),
  update_time  DATETIME
);
```

#### ai_flow
```sql
CREATE TABLE ai_flow (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id          BIGINT NOT NULL,
  name                VARCHAR(100) NOT NULL,
  description         VARCHAR(500),
  icon                VARCHAR(255),
  is_assistant        TINYINT DEFAULT 0,
  design              LONGTEXT,  -- LogicFlow JSON
  chain               LONGTEXT,  -- LiteFlow Chain
  status              VARCHAR(20) DEFAULT 'draft',
  current_version_id  BIGINT,
  create_by           VARCHAR(50),
  create_time         DATETIME,
  update_by           VARCHAR(50),
  update_time         DATETIME,
  INDEX idx_project (project_id)
);
```

#### ai_knowledge_chunk
```sql
CREATE TABLE ai_knowledge_chunk (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id       BIGINT NOT NULL,
  kb_id        BIGINT NOT NULL,
  chunk_index  INT,
  content      LONGTEXT,
  content_len  INT,
  vector_id    BIGINT NOT NULL,  -- Hnswlib 内部 ID
  metadata     LONGTEXT,
  create_time  DATETIME,
  INDEX idx_doc (doc_id),
  INDEX idx_kb (kb_id)
);
```

### 4.3 Hnswlib 存储

每个知识库对应一个 Hnswlib 索引文件:

```
/data/hnsw/
├── knowledge_001.bin
├── knowledge_002.bin
└── knowledge_xxx.bin
```

文件加载时机:
- 应用启动:不预加载(懒加载)
- 首次查询某知识库:从 .bin 加载到内存
- 文档新增:实时更新索引
- 文档删除:重建索引
- 应用关闭:序列化到磁盘

---

## 5. 关键技术决策

### 5.1 为什么不用 Redis

| 场景 | 替代方案 |
|---|---|
| Session 存储 | JWT 无状态,不需要 |
| Token 黑名单 | Caffeine + TTL |
| 字典缓存 | @Cacheable + Caffeine |
| 接口限流 | Bucket4j 内存令牌桶 |
| 在线用户 | ConcurrentHashMap |
| SSE 会话 | 内存 Map |

**结论**:Caffeine 完全够用,单机部署无需 Redis 分布式缓存。

### 5.2 为什么不用 PostgreSQL + pgvector

| 维度 | PostgreSQL + pgvector | Hnswlib + MariaDB |
|---|---|---|
| 部署 | 需 PG 服务 | 嵌入式,无外部服务 |
| 性能 | 亿级毫秒级 | 亿级毫秒级 |
| 运维 | 中(需调优) | 简单(库文件持久化) |
| 数据一致性 | 强 | 应用层保证 |
| 扩展 | 可横向 | 暂不支持 |

**结论**:单机部署场景下,Hnswlib 更简单。如未来需分布式,可平滑迁移到 Milvus。

### 5.3 为什么助手复用 Flow

| 方案 | 优势 | 劣势 |
|---|---|---|
| 助手复用 Flow | 共享流程引擎、触发器、版本管理 | 助手状态字段需在 Flow 中扩展 |
| 助手独立实体 | 更灵活 | 需重复实现大量基础设施 |

**结论**:复用 Flow 是更经济的选择,代价是 `is_assistant` 字段 + `ai_assistant_*` 扩展表的少量额外复杂度。

### 5.4 节点自定义的两层设计

| 层级 | 适用 | 实现 |
|---|---|---|
| 开发者扩展 | 复杂自定义(数据库、内部服务) | Java 类 + Vue 组件 |
| 用户自定义 | 简单自定义(HTTP、SQL、脚本) | UI 配置,无代码 |

**结论**:两层覆盖 95%+ 场景,平衡了灵活性和易用性。

---

## 6. 安全设计

### 6.1 认证流程

```
┌──────────┐     POST /auth/login       ┌──────────┐
│  Browser │───────────────────────────>│ Backend  │
│          │<────── { token, user } ────│          │
│          │                             │          │
│          │   GET /api/xxx              │          │
│          │   Header: Authorization:    │          │
│          │     Bearer {token}          │          │
│          │───────────────────────────>│          │
│          │                             │ Shiro    │
│          │                             │ Filter   │
│          │                             │ ↓        │
│          │                             │ Validate │
│          │                             │ JWT      │
│          │<────── 200 / 401 ──────────│          │
└──────────┘                             └──────────┘
```

### 6.2 授权层级

```
1. 认证(Authentication):JWT 验证
   ↓
2. 系统角色(Authorization):@RequiresRoles
   ↓
3. 项目角色(Authorization):@PreProjectRole
   ↓
4. 数据权限(Authorization):@DataScope
   ↓
5. 资源归属(Ownership):检查资源 owner
```

### 6.3 接口签名(防重放)

```java
// 请求签名规则
// sign = md5(timestamp + nonce + secret + params)
```

### 6.4 操作日志

使用 AOP 自动记录:
- 方法 + URL + 参数 + 响应
- 用户 + IP + UA
- 耗时
- 异常信息

---

## 7. 性能设计

### 7.1 性能指标

| 指标 | 目标 |
|---|---|
| 单实例 QPS | 100+ AI 对话 |
| AI 首字响应 | ≤ 3s |
| 知识库检索 P95 | ≤ 500ms |
| 启动时间 | < 30s |
| 内存占用 | < 2GB |
| CPU 占用(空闲) | < 5% |

### 7.2 性能优化

| 优化点 | 方案 |
|---|---|
| DB 查询 | 索引 + 分页 + 批量 |
| 缓存 | Caffeine(本地) + 字典/权限缓存 |
| 异步 | @Async 异步任务 |
| SSE | 直接代理,无中间层 |
| 向量检索 | Hnswlib 内存索引 |
| 静态资源 | Nginx 压缩 + 缓存 |
| JVM | G1GC + 4GB heap |

### 7.3 限流

```java
// Bucket4j 限流(按 IP 和按用户)
@RateLimiter(key = "user", limit = 60, period = 60)  // 每用户 60 次/分
```

---

## 8. 可扩展性设计

### 8.1 横向扩展

当前为单机部署,设计预留扩展点:

| 维度 | 当前 | 扩展方案 |
|---|---|---|
| 应用 | 1-2 实例 | 改用外部 Session |
| 向量 | Hnswlib 单机 | 迁移到 Milvus |
| 缓存 | Caffeine | Redis Cluster |
| 事件 | ApplicationEvent | Kafka / RocketMQ |

### 8.2 插件化

通过 SPI 机制支持第三方扩展:

- 节点类型(Java SPI)
- LLM 提供商(LangChain4j 抽象)
- MCP 服务(标准协议)
- 触发器(接口实现)

---

## 9. 监控与可观测性

### 9.1 应用监控

- Spring Boot Actuator
- Druid 监控(/druid)
- springdoc-openapi(/swagger-ui)

### 9.2 业务监控

- 操作日志(全链路)
- 运行记录(流程 + 助手)
- 审计日志(API 调用)
- 告警中心(失败率、超时)

### 9.3 资源监控

- JVM(Micrometer)
- 系统(CPU/内存/磁盘)
- 数据库(MariaDB Performance Schema)

---

## 10. 部署架构

### 10.1 单机部署

```bash
# 1. 安装 JDK 21
sudo apt install openjdk-21-jdk

# 2. 安装 MariaDB 12.2
sudo apt install mariadb-server
sudo mysql_secure_installation

# 3. 创建数据库
mysql -u root -p
CREATE DATABASE ai_platform DEFAULT CHARACTER SET utf8mb4;

# 4. 安装 Nginx
sudo apt install nginx

# 5. 部署后端
sudo cp ai-platform-backend.jar /opt/ai-platform/
sudo systemctl enable ai-platform

# 6. 部署前端
sudo tar -xzf ai-platform-frontend.tar.gz -C /var/www/ai-platform/

# 7. 配置 Nginx
sudo cp nginx.conf /etc/nginx/sites-available/ai-platform
sudo ln -s /etc/nginx/sites-available/ai-platform /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 10.2 Docker 部署(可选)

```yaml
# docker-compose.yml
version: '3.8'
services:
  mariadb:
    image: mariadb:12.2
    volumes:
      - ./data/mariadb:/var/lib/mysql
    environment:
      MARIADB_ROOT_PASSWORD: root
      MARIADB_DATABASE: ai_platform
  
  backend:
    image: ai-platform-backend:1.0.0
    ports:
      - "8080:8080"
    depends_on:
      - mariadb
    volumes:
      - ./data/hnsw:/data/hnsw
      - ./data/upload:/data/upload
  
  frontend:
    image: nginx:alpine
    volumes:
      - ./dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    ports:
      - "80:80"
    depends_on:
      - backend
```

---

**ARCHITECTURE 终**
