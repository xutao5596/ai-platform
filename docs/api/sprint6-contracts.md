# Sprint 6 API 契约

> 启动日期: 2026-06-10
> 3 Agent 并行: A(详情页 tabs) / B(缺失页面) / C(聊天+编辑器增强)
> 严格监督:每个 Agent 完成必须**端到端冒烟测试**

## 1. 强制编码规范(继承)
- `docs/OPERATIONS-ENCODING.md` 必读
- 读文件: `[System.IO.File]::ReadAllBytes` + 显式 Encoding
- 写文件: `[IO.File]::WriteAllText(path, content, (New-Object System.Text.UTF8Encoding($False)))`
- 严禁: `Get-Content` / `Set-Content` / `Out-File` / `>` 重定向

## 2. 当前后端 API 全清单(已就绪,Agent 只读不改)

### 流程
- `GET  /api/v1/flow/page?projectId=&keyword=&isAssistant=&current=&size=` — 分页
- `GET  /api/v1/flow/list?projectId=` — 列表(无分页)
- `GET  /api/v1/flow/{id}` — 详情
- `POST /api/v1/flow` — 创建
- `PUT  /api/v1/flow` — 更新
- `DELETE /api/v1/flow/{id}`
- `POST /api/v1/flow/{id}/run` — 触发(body: `{"input":{...}}`)
- `GET  /api/v1/flow/{id}/runs?current=&size=` — 运行历史
- `GET  /api/v1/flow/run/{runId}` — 单次运行(含 steps)
- `GET  /api/v1/flow/{id}/versions` — 版本
- `POST /api/v1/flow/{id}/versions` — 创建版本
- `GET  /api/v1/flow/{id}/triggers` — 触发器
- `POST /api/v1/flow/{id}/triggers` — 创建触发器
- `DELETE /api/v1/flow/{id}/triggers/{tid}`

### 知识库
- `GET  /api/v1/ai/knowledge/list?projectId=` — 列表
- `GET  /api/v1/ai/knowledge/page?projectId=&keyword=&current=&size=` — 分页
- `GET  /api/v1/ai/knowledge/{id}` — 详情
- `POST /api/v1/ai/knowledge` — 创建
- `PUT  /api/v1/ai/knowledge` — 更新
- `DELETE /api/v1/ai/knowledge/{id}`
- `POST /api/v1/ai/knowledge/{id}/doc/upload` — **上传文档**(multipart,字段 `file`)
- `GET  /api/v1/ai/knowledge/{id}/doc/list` — 文档列表
- `POST /api/v1/ai/knowledge/{id}/doc/index` — 重建索引

### 助手
- `GET  /api/v1/assistant/list?projectId=`
- `GET  /api/v1/assistant/page?projectId=&keyword=&current=&size=`
- `GET  /api/v1/assistant/{id}`
- `POST /api/v1/assistant` — 创建
- `PUT  /api/v1/assistant` — 更新
- `DELETE /api/v1/assistant/{id}`
- `POST /api/v1/assistant/{id}/chat/stream` — **SSE 流式**
- `POST /api/v1/assistant/{id}/chat` — 同步

### 系统
- `GET  /api/v1/system/log/page` — 审计日志
- `GET  /actuator/prometheus` — 监控指标(无认证)
- `GET  /actuator/health`

## 3. Agent A — 详情页 tabs 补全(改前端 2 个详情页)

### 文件
- `ai-frontend/src/views/project/detail.vue` — **4 个空 tab 全部接 API**
- `ai-frontend/src/views/flow/detail.vue` — 加 4 个有内容的 tab

### 契约 — project/detail.vue tab

| Tab | 数据来源 | 组件 |
|---|---|---|
| overview | `projectApi.get(id)` | el-descriptions (已有) |
| **flow** | `flowApi.page({ projectId: id, current: 1, size: 20 })` | el-table(名称点击跳详情)+ 状态标签 + "新建"按钮 |
| **knowledge** | `aiKnowledgeApi.list({ projectId: id })` | el-table(名称 + 文档数 + 大小) + "新建知识库"按钮 |
| **assistant** | `assistantApi.list({ projectId: id })` | el-table(名称 + 模型 + 工具数) + "新建助手"按钮 |
| **settings** | 当前项目编辑表单 | el-form(项目名/描述/状态) + 保存按钮 |

### 契约 — flow/detail.vue tab

| Tab | 数据来源 | 组件 |
|---|---|---|
| overview | `flowApi.get(id)` | el-descriptions (已有) |
| **runs** | `flowApi.runs(id, current=1, size=20)` | el-table(状态 + 耗时 + 触发类型 + 时间) + 步骤展开 |
| **triggers** | `flowApi.triggers(id)` | el-table + 新建/删除/启用切换 |
| **versions** | `versionApi.list(id)` | el-table + 切换/对比 |

### 涉及文件
- 修改:`ai-frontend/src/views/project/detail.vue`
- 修改:`ai-frontend/src/views/flow/detail.vue`
- 可能新建:`ai-frontend/src/api/flow.ts` 增 runs/triggers/versions 方法(若未)
- 可能新建:`ai-frontend/src/components/EmptyHint.vue`(可选,空状态统一组件)

### 不修改
- 任何 .vue 之外的文件
- 任何后端文件

### 验证步骤(必做)
1. `cd ai-frontend; npm run build` BUILD SUCCESS
2. **端到端**:启动后端 + 前端 dev,进入任一项目详情 → 看到 5 个 tab 都有内容(不是 el-empty)
3. 进入任一流程详情 → 看到 4 个 tab 都有内容
4. 在 flow tab 新建流程 → 跳到 flow/editor
5. 写 commit: `Sprint 6 Agent A: project + flow detail tabs`

---

## 4. Agent B — 缺失页面 + Dashboard 真实化(改前端 4 个文件)

### 文件
- `ai-frontend/src/views/monitor/index.vue` — 完整监控页
- `ai-frontend/src/views/ai/mcp.vue` — MCP CRUD
- `ai-frontend/src/views/ai/knowledge.vue` — 加文件上传 dialog
- `ai-frontend/src/views/dashboard/index.vue` — 4 个统计改调 API

### 契约 — monitor/index.vue
- 4 个图表(ECharts):
  - 流程执行量(按小时,折线图) — 从 `/actuator/prometheus` 拉 `flow_run_count_total`
  - 登录次数(按天,折线图) — `login_count_total`
  - 节点执行分布(饼图) — `flow_node_execute_total`
  - Webhook 派发(按状态,堆叠柱) — `webhook_dispatch_count_total`
- 顶部 4 个统计卡片(总流程 / 总助手 / 今日聊天 / 错误率)
- 实时刷新(每 30s 拉一次)

### 契约 — ai/mcp.vue
- 表格 + CRUD:名称 / URL / 类型 / 状态 / 创建时间
- 创建/编辑 dialog(参考 model.vue)
- 测试连接按钮(`POST /api/v1/ai/mcp/test` — 检查后端有没有此端点,没有就跳过)
- 后端实体 AiMcp 已存在,检查 controller

### 契约 — ai/knowledge.vue 改造
- 现有 CRUD 基础上,加"上传文档"按钮 → 弹 dialog → 选择文件 → 上传到 `POST /api/v1/ai/knowledge/{id}/doc/upload`
- 上传后显示文档列表(`doc/list`)
- 进度条 + 错误处理

### 契约 — dashboard/index.vue
- 当前只调 `projectApi.mine()`,**调齐 4 个**:
  - projectCount: `projectApi.mine().length`
  - flowCount: `flowApi.list({ projectId: ... })` 或 `flowApi.page({ current: 1, size: 1 }).total`
  - knowledgeCount: `aiKnowledgeApi.list({ projectId: ... }).length`
  - chatToday: 后端缺这个端点 → 显示 0 + "聊天统计 待实现" 提示

### 涉及文件
- 修改:`ai-frontend/src/views/monitor/index.vue`
- 修改:`ai-frontend/src/views/ai/mcp.vue`
- 修改:`ai-frontend/src/views/ai/knowledge.vue`
- 修改:`ai-frontend/src/views/dashboard/index.vue`
- 可能新建:`ai-frontend/src/api/ai/mcp.ts` 增方法

### 不修改
- 任何后端文件
- 任何无关前端文件

### 验证步骤
1. `cd ai-frontend; npm run build` BUILD SUCCESS
2. **端到端**:访问 /monitor → 看到 4 个图表(数据可能为 0,但图表渲染)
3. 访问 /ai/mcp → CRUD 可用
4. 进入知识库 → 上传文件 → 文档列表更新
5. Dashboard 4 个卡片显示真实数字
6. 写 commit: `Sprint 6 Agent B: monitor + mcp + KB upload + dashboard`

---

## 5. Agent C — Chat Markdown + 编辑器增强(改前端 2 个文件)

### 文件
- `ai-frontend/src/views/ai/chat.vue` — Markdown 渲染 + 代码高亮
- `ai-frontend/src/views/flow/editor/index.vue` — 撤销重做 + 节点搜索 + 自动保存

### 契约 — chat.vue
- 引入 `markdown-it` + `highlight.js`(查 package.json 是否已有,没有就 npm install)
- 助手消息渲染 Markdown + 代码高亮
- 用户消息纯文本
- 加"停止生成"按钮(SSE 期间显示,触发 AbortController)
- 加"重新生成"按钮(最近 1 条助手消息)

### 契约 — editor/index.vue
- 自动保存:编辑后 3s 防抖,自动调 `flowApi.update(design)`
- 撤销/重做:用 history stack,Ctrl+Z / Ctrl+Shift+Z 绑定
- 节点搜索:按名称过滤,高亮匹配节点
- 复制粘贴:选中节点后 Ctrl+C / Ctrl+V

### 涉及文件
- 修改:`ai-frontend/src/views/ai/chat.vue`
- 修改:`ai-frontend/src/views/flow/editor/index.vue`
- 可能:`ai-frontend/package.json` 加依赖(markdown-it, highlight.js)

### 不修改
- 任何后端
- 其他 vue 文件

### 验证步骤
1. `cd ai-frontend; npm run build` BUILD SUCCESS
2. **端到端**:访问 /ai/chat → 助手消息带 Markdown 渲染(代码块高亮等)
3. 访问 /flow/{id}/editor → 编辑 3s 后自动保存(看 network 请求)
4. 测试 Ctrl+Z 撤销
5. 写 commit: `Sprint 6 Agent C: chat markdown + editor autosave`

---

## 6. 主线程监督清单(联调阶段)

| 检查 | 命令 / 方法 |
|---|---|
| Agent A 详情 tabs | 访问 http://localhost:5174/project/10 → 看 5 个 tab 都有内容 |
| Agent B monitor | 访问 /monitor → 看到 4 个 ECharts 图表 |
| Agent B mcp CRUD | 访问 /ai/mcp → 新建/编辑/删除 |
| Agent B KB 上传 | 知识库详情 → 上传 docx 文件 |
| Agent B dashboard | 首页 4 个卡片显示真实数字 |
| Agent C chat | /ai/chat → 给助手发消息带 markdown 的,看渲染 |
| Agent C editor | /flow/405/editor → 等 3s 看 PUT 请求 |

如果任何一项不达标,**必须让 Agent 修到达标再合并**。
