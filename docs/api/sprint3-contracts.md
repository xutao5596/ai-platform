# Sprint 3 API 契约

> 锁定: 2026-06-09  
> 目的: 三个 Agent 并行开发的接口对齐基准  
> 变更需在群组声明

## 1. 流程引擎 (Agent A)

### 1.1 流程 CRUD

```
GET    /api/v1/flow/page?projectId=&keyword=&current=&size=
GET    /api/v1/flow/list?projectId=           # 项目内所有 flow
GET    /api/v1/flow/{id}
POST   /api/v1/flow                          # body: {projectId,name,description,design,chain}
PUT    /api/v1/flow                          # body: 同上 + id
DELETE /api/v1/flow/{id}
```

### 1.2 流程版本

```
GET    /api/v1/flow/{id}/versions
POST   /api/v1/flow/{id}/versions            # body: {design,chain,changelog}
POST   /api/v1/flow/{id}/publish/{versionId}
```

### 1.3 流程执行

```
POST   /api/v1/flow/{id}/run                # body: {input,async:false}
GET    /api/v1/flow/{id}/runs               # 执行历史
GET    /api/v1/flow/run/{runId}             # 详情(含步骤)
POST   /api/v1/flow/{id}/trigger/test       # 触发器测试
```

### 1.4 触发器

```
GET    /api/v1/flow/{id}/triggers
POST   /api/v1/flow/{id}/triggers            # body: {type,config}
PUT    /api/v1/flow/{id}/triggers/{triggerId}
DELETE /api/v1/flow/{id}/triggers/{triggerId}
```

Trigger types: `manual` / `cron` / `webhook` / `event` / `chained`

### 1.5 节点定义(SPI)

每个节点的输入/输出 schema:
```ts
type NodeSchema = {
  typeKey: string         // 'llm' / 'start' / 'end' / 'if_else' / 'http' / ...
  category: 'basic' | 'ai' | 'control' | 'tool' | 'data'
  displayName: string
  description: string
  icon: string
  inputs: Property[]       // 配置项
  outputs: Property[]      // 输出变量
  color: string            // 节点卡片颜色
}

type Property = {
  key: string
  label: string
  type: 'string' | 'number' | 'boolean' | 'select' | 'json' | 'textarea' | 'model' | 'kb' | 'prompt'
  required: boolean
  default?: any
  options?: {label, value}[]   // for select
  description?: string
}
```

```
GET    /api/v1/flow/node-definitions        # 全部可用节点
```

### 1.6 自定义节点

```
GET    /api/v1/flow/custom-nodes?projectId=
POST   /api/v1/flow/custom-nodes
PUT    /api/v1/flow/custom-nodes/{id}
DELETE /api/v1/flow/custom-nodes/{id}
```

## 2. AI 助手 (Agent B)

### 2.1 助手配置

```
GET    /api/v1/assistant/page?projectId=&keyword=&current=&size=
GET    /api/v1/assistant/list?projectId=
GET    /api/v1/assistant/{id}
POST   /api/v1/assistant                      # body: {projectId,name,persona,modelId,kbIds,toolsEnabled,...}
PUT    /api/v1/assistant
DELETE /api/v1/assistant/{id}
```

### 2.2 助手事件订阅

```
GET    /api/v1/assistant/{id}/events
POST   /api/v1/assistant/{id}/events          # body: {eventType,filter,enabled}
DELETE /api/v1/assistant/{id}/events/{eventId}
```

Event types: `flow.run.failed` / `flow.run.success` / `knowledge.doc.parsed` / ...

### 2.3 助手工具

```
GET    /api/v1/assistant/tools                # 全部可用工具
POST   /api/v1/assistant/{id}/tools/test      # body: {toolName,args}
```

### 2.4 助手对话(SSE)

```
POST   /api/v1/assistant/{id}/chat/stream    # body: {message,sessionId,toolsEnabled}
                                          # 返回: text/event-stream
```

事件格式:
```
event: content
data: {"chunk":"Hello","sessionId":1}

event: tool_call
data: {"name":"web_search","args":{...},"callId":"tc-001"}

event: tool_result
data: {"callId":"tc-001","result":"..."}

event: done
data: {"sessionId":1,"messageId":42,"inputTokens":120,"outputTokens":85}
```

## 3. 共享响应

`Result<T>` 包装 + 业务码 200/400/401/403/404/500/5001-5003 (见 ErrorCode)

## 4. 类型定义共享

前端类型来源:
- 后端 Java 实体转 TS(后续做 codegen,本期手写)
- API 文件由前端基于契约定义,后端实现
- 类型变更:改 `docs/api/sprint3-contracts.md` + 群组通知
