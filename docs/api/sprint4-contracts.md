# Sprint 4 API 契约

> 启动日期: 2026-06-10
> 3 Agent 并行: A(API Key) / B(Webhook) / C(部署+压测+文档)

## 1. 强制编码规范(继承 Sprint 3.1)
- 完整规范: `docs/OPERATIONS-ENCODING.md`
- 记忆: `.memory/CLAUDE.md` 第 15 节
- 简明:
  - 读文件: `[System.IO.File]::ReadAllBytes` + `[Text.Encoding]::UTF8.GetString`
  - 写文件: `[IO.File]::WriteAllText(path, content, (New UTF8Encoding($False)))`
  - 严禁: `Get-Content` / `Set-Content` / `Out-File` / `>` 重定向
  - Git 读: `git cat-file blob <hash>` → 字节流

## 2. 已有表(无需新 migration)

### ai_project_api_key
- id / project_id / name / api_key / api_secret / scopes / rate_limit / expires_at / status / last_used_time / last_used_ip
- Entity: `AiProjectApiKey.java` 已存在
- Mapper: `AiProjectApiKeyMapper.java` 已存在

### ai_project_webhook
- id / project_id / name / url / secret / events / status / description
- Entity + Mapper 已存在

### ai_project_webhook_log
- 接收日志

## 3. Agent A — API Key(改 `ai-project` + `ai-framework` + 前端)

### 涉及文件
**新建**:
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/controller/ProjectApiKeyController.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/dto/ApiKeySaveRequest.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/dto/ApiKeyVO.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/service/ApiKeyService.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/security/ApiKeyAuthFilter.java`
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/security/ApiKeyContext.java`(用于 ApiKey 鉴权的上下文)
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/ratelimit/RateLimiter.java` + `RateLimitFilter.java`
- `ai-frontend/src/views/project/apikeys.vue`

**修改**:
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/mapper/AiProjectApiKeyMapper.java` — 加 `selectByApiKey`
- `ai-backend/ai-start/src/main/java/com/aiplatform/security/JwtAuthenticationFilter.java` — 跳过 `/api/v1/ext/**`(API Key 认证路径)
- `ai-backend/ai-start/src/main/java/com/aiplatform/AiPlatformApplication.java` — 注册 ApiKeyAuthFilter

### API 契约
```
GET    /api/v1/project/{projectId}/apikeys           列表
POST   /api/v1/project/{projectId}/apikeys           创建(返回明文 secret,仅此一次)
PUT    /api/v1/project/{projectId}/apikeys/{id}     更新(name/scopes/rate_limit/status)
DELETE /api/v1/project/{projectId}/apikeys/{id}     删除
POST   /api/v1/project/{projectId}/apikeys/{id}/reset 重置 secret

# 外部调用(API Key 认证,非 JWT)
GET    /api/v1/ext/flow/list?projectId=1
POST   /api/v1/ext/flow/{id}/run
POST   /api/v1/ext/assistant/{id}/chat
```

### 鉴权
- Header: `X-API-Key: <apiKey>` + `X-API-Secret: <apiSecret>`(或合并为 `Authorization: ApiKey <key>:<secret>`)
- ApiKeyAuthFilter 拦截 `/api/v1/ext/**`,查 `ai_project_api_key` 验证 + 设置 `ApiKeyContext` + 限流计数
- admin 跳过 API Key 鉴权

### 限流
- 60 req/min 默认,可配 `rate_limit` 字段
- 用 Caffeine 内存计数(简单滑窗),不用 Redis
- 超出返回 429 + Retry-After header

### 数据库
- 不需要新表
- 不需要新 migration

---

## 4. Agent B — Webhook 外部触发(改 `ai-project` + `ai-flow` + 前端)

### 涉及文件
**新建**:
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/controller/ProjectWebhookController.java`(管理 CRUD)
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/dto/WebhookSaveRequest.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/dto/WebhookVO.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/dto/WebhookTestRequest.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/service/WebhookService.java`
- `ai-backend/ai-project/src/main/java/com/aiplatform/project/service/WebhookDispatcher.java`(异步重试)
- `ai-frontend/src/views/project/webhooks.vue`

**修改**:
- `JwtAuthenticationFilter.java` — 跳过 `/api/v1/webhook/receive/**`(接收端点匿名访问)
- `ai-project/.../mapper/AiProjectWebhookMapper.java` — 加分页查询
- `ai-project/.../mapper/AiProjectWebhookLogMapper.java` — 加按 webhookId 查询

### API 契约
```
# 管理(走 JWT)
GET    /api/v1/project/{projectId}/webhooks               列表
POST   /api/v1/project/{projectId}/webhooks               创建(返回 secret)
PUT    /api/v1/project/{projectId}/webhooks/{id}         更新
DELETE /api/v1/project/{projectId}/webhooks/{id}         删除
POST   /api/v1/project/{projectId}/webhooks/{id}/test    发送测试 payload
GET    /api/v1/project/{projectId}/webhooks/{id}/logs     投递日志

# 接收(匿名,签名验证)
POST   /api/v1/webhook/receive/{id}                       接收外部事件
```

### 事件订阅
- 支持事件类型:
  - `flow.run.success` / `flow.run.failed`
  - `assistant.chat.completed`
  - `kb.doc.indexed`
- Webhook 订阅 = `events` 字段存 JSON 数组 `["flow.run.success"]`

### 签名验证
- Header: `X-Webhook-Signature: sha256=<HMAC-SHA256(secret, body)>`
- Header: `X-Webhook-Timestamp: <unix-seconds>`(防重放,5 分钟内有效)

### 异步重试
- 用 `@Async` + 线程池
- 失败重试策略: 1s, 5s, 30s, 5min(最多 4 次)
- 每次结果写入 `ai_project_webhook_log`

### 数据库
- 不需要新表
- 不需要新 migration

---

## 5. Agent C — 部署 + 压测 + 文档

### 涉及文件
**新建**:
- `ai-deploy/deploy.sh` — 单机部署脚本(检查环境、初始化 DB、构建 jar、启动)
- `ai-deploy/stop.sh` — 停止服务
- `ai-deploy/restart.sh` — 重启
- `ai-deploy/backup.sh` — 数据库备份(mysqldump)
- `ai-deploy/restore.sh` — 恢复
- `ai-deploy/upgrade.sh` — 升级(拉代码 + 重构 + 重启)
- `ai-deploy/nginx/ai-platform.conf` — Nginx 反向代理 + 静态资源
- `ai-deploy/systemd/ai-platform.service` — systemd unit
- `tests/load/jmeter/test-plan.jmx` — JMeter 压测脚本
- `tests/load/jmeter/run-load-test.sh` — 压测启动脚本
- `docs/USER-MANUAL.md` — 用户手册
- `docs/DEPLOY.md` — 部署文档

### 部署要求
- 端口: 8080(后端),5173(前端 dev),80/443(nginx)
- 数据库: MariaDB 12.2
- 内存: ≥2GB
- 启动顺序: DB → 后端 jar → 前端构建产物

### 压测目标
- 单机 100 QPS(简单 API 如登录/查询)
- 流程执行 50 QPS(复杂路径)
- 响应 P99 < 1s

### 用户手册
- 项目初始化(注册 admin → 创建项目 → 添加成员)
- 流程创建 → 触发器配置 → 试运行
- 助手使用 → 工具启用
- API Key 使用示例
- Webhook 配置

---

## 6. 公共边界检查清单

| 检查项 | Agent A | Agent B | Agent C |
|---|---|---|---|
| `JwtAuthenticationFilter` 跳过路径 | 加 `/api/v1/ext/**` | 加 `/api/v1/webhook/receive/**` | 不动 |
| `ApiKeyContext` 不污染其他模块 | ✅ 自己维护 | ✅ 不依赖 | ✅ 不动 |
| `WebhookDispatcher` 不依赖 LiteFlow | ✅ 不动 | ✅ 调 FlowRunner 已有 API | ✅ 不动 |
| 前端路由 | `apikeys.vue` | `webhooks.vue` | 不写前端 |
| 数据库新表 | 不需要 | 不需要 | 不需要 |
| V8 migration | 不用 | 不用 | 不用 |

---

## 7. 主线程联调步骤
1. 各 Agent 在 `feature/sprint4-{a/b/c}` 分支开发
2. 主线程 merge 三分支到 develop
3. 跑 `mvn -o -pl ai-start -am package -DskipTests` 编译
4. 启动后端,跑 `tests/e2e_sprint3.ps1` 确认无回归
5. 跑新增 E2E(API Key CRUD + Webhook CRUD + 接收验签)
6. 跑 JMeter 压测
7. 打 tag `v0.6.0`
