# Sprint 5 API 契约

> 启动日期: 2026-06-10
> 4 Agent 并行: A(审计日志) / B(可观测性) / E(国际化) / F(暗色主题)

## 1. 强制编码规范(继承)
- `docs/OPERATIONS-ENCODING.md` 必读
- 读文件: `[System.IO.File]::ReadAllBytes` + 显式 Encoding
- 写文件: `[System.IO.File]::WriteAllText(path, content, (New-Object System.Text.UTF8Encoding($False)))`
- 严禁: `Get-Content` / `Set-Content` / `Out-File` / `>` 重定向

## 2. 已有资产
- `OperationLog` 注解(`ai-framework/log/OperationLog.java`)— 注解已存在,0 个 Controller 用
- `sys_log` 表 + SysLogController/Service/Mapper/Entity — 完整骨架,0 条记录
- Spring Actuator 基础端点(health/info/metrics)

## 3. Agent A — 审计日志激活

### 涉及文件
**新建**:
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/log/OperationLogAspect.java`
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/log/RequestResponseCapture.java`(辅助类,流可重复读)
- `ai-frontend/src/views/system/logs.vue`(查询页)

**修改**:
- `ai-backend/ai-start/src/main/java/com/aiplatform/security/JwtAuthenticationFilter.java`(已在加 ApiKey 跳过路径,本任务不动)
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/log/SysLog.java`(看是否需要扩字段)
- `ai-frontend/src/router/index.ts`(加路由)
- `ai-frontend/src/api/log.ts`(新建)

### 行为契约
- 拦截所有 `@RestController` + `@RequestMapping`(自动,不需注解)
- 写 `sys_log`:module=类名(去 Controller 后缀)/action=方法名/request_url/method/params(POST body,前 2KB)/response(可选,前 2KB)/user_id/username/ip/user_agent/cost_ms/status(0=失败/1=成功)/error_msg
- 异步写库(`@Async` + 线程池),不阻塞业务
- 排除路径:`/api/v1/auth/login`、`/api/v1/auth/captcha`、`/actuator/**`、`/v3/api-docs/**`、`/api/v1/webhook/receive/**`(接收端无用户上下文)
- 失败时仅 log warn,**不抛异常**(审计失败不能影响业务)
- 列表分页:`GET /api/v1/system/logs?current=1&size=20&module=&username=&status=`
- 详情:`GET /api/v1/system/logs/{id}`(返回完整 params + response)

### 不修改
- `OperationLog` 注解(保留给未来精细标注)
- `sys_log` 表结构
- 任何 Controller 代码

## 4. Agent B — 可观测性

### 涉及文件
**新建**:
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/observability/MetricsConfig.java`
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/observability/BusinessMetrics.java`(业务指标)
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/observability/TraceIdFilter.java`(MDC traceId)
- `ai-backend/ai-framework/src/main/java/com/aiplatform/framework/observability/MetricsAspect.java`(AOP 记录耗时)

**修改**:
- `ai-backend/ai-start/pom.xml` — 加 `micrometer-registry-prometheus` + `spring-boot-starter-actuator`(已有)
- `ai-backend/ai-start/src/main/resources/application.yml` — 暴露 prometheus 端点,加 traceId 日志格式

### 行为契约
- 暴露 `/actuator/prometheus` 端点(无需认证,内网白名单)
- 业务指标(Micrometer Counter/Timer):
  - `flow.run.count{flow_id, status}` — 流程执行次数
  - `flow.run.duration{flow_id}` — 流程执行耗时(Timer)
  - `flow.node.execute{node_type, status}` — 节点执行
  - `api_key.call.count{api_key_id, path, status}` — API Key 调用
  - `webhook.dispatch.count{webhook_id, event, status}` — Webhook 派发
  - `login.count{status}` — 登录次数
  - `assistant.chat.count{assistant_id, status}` — 助手对话
- `TraceIdFilter` 给每个 HTTP 请求生成 `traceId`(UUID),放入 MDC + 响应 header `X-Trace-Id`
- 日志格式加 `[traceId=%X{traceId}]`
- AOP 记录 Service 方法耗时(可选,只对标了 `@Timed` 的)

### 不修改
- 任何 Controller 代码
- 任何 Service 代码(除非用 AOP 自动包装)

## 5. Agent E — 国际化(中英)

### 涉及文件
**新建**:
- `ai-frontend/src/locales/zh-CN.ts`
- `ai-frontend/src/locales/en-US.ts`
- `ai-frontend/src/locales/index.ts`(i18n 配置)
- `ai-frontend/src/composables/useLocale.ts`(切换 composable)

**修改**:
- `ai-frontend/src/main.ts`(注册 i18n)
- 所有 .vue 模板中的硬编码中文 → `$t('key')`
- `ai-frontend/src/router/index.ts` 的 `meta.title` → i18n key
- `ai-frontend/src/stores/user.ts` 加 `locale` 字段,持久化到 localStorage

### 行为契约
- 默认 `zh-CN`(向后兼容,所有现有 UI 不变)
- 用户可在右上角下拉切换 `zh-CN` ↔ `en-US`,刷新后保留
- Element Plus 国际化跟随(`el-config-provider`)
- i18n key 命名:`模块.页面.元素`,例 `project.apikeys.title`
- 第一次扫描所有 .vue,提取硬编码中文到 `zh-CN.ts`;`en-US.ts` 暂用机器翻译(后续人工精修)
- 不强求 100% 覆盖;不替换的保留原文(降级显示)

### 不修改
- 后端代码
- 后端错误消息(后续 Sprint 再做)

## 6. Agent F — 暗色主题

### 涉及文件
**新建**:
- `ai-frontend/src/styles/variables.scss`(CSS 变量:浅色/暗色两套)
- `ai-frontend/src/styles/dark.scss`(暗色覆盖)
- `ai-frontend/src/composables/useTheme.ts`

**修改**:
- `ai-frontend/src/main.ts`(注入主题 class 到 `<html>`)
- `ai-frontend/src/App.vue`(加主题切换按钮 + 持久化)
- 主要页面 `.vue` 的硬编码颜色 → CSS 变量

### 行为契约
- 默认 `light`(向后兼容)
- 用户可在右上角切换 `light` ↔ `dark`,刷新后保留(localStorage)
- 使用 CSS 变量 + `:root.dark { ... }` 选择器
- Element Plus 主题跟随(`el-config-provider` 的 theme 属性)
- 覆盖 5 个核心组件:Header / Sider / Card / Table / Button

### 不修改
- 任何业务逻辑
- 任何后端

## 7. 公共边界

| 检查项 | A | B | E | F |
|---|---|---|---|---|
| `ai-start/pom.xml` | 不动 | 加 micrometer | 不动 | 不动 |
| `application.yml` | 不动 | 加 prometheus 端点 | 不动 | 不动 |
| `main.ts` | 不动 | 不动 | 加 i18n | 加 theme |
| `App.vue` | 不动 | 不动 | 不动 | 加切换按钮 |
| `router/index.ts` | 加 system/logs | 不动 | 加 i18n key | 不动 |
| 任何 Controller | 不动 | 不动 | 不动 | 不动 |
| 任何 Service | 不动 | 不动 | 不动 | 不动 |

## 8. 主线程联调
1. 各 Agent 在 `feature/sprint5-{a/b/e/f}` 分支开发
2. 主线程 merge → `mvn -o -pl ai-start -am compile` 必须 BUILD SUCCESS
3. 跑 `tests/e2e_sprint3.ps1` 28 测试 + 新加 4 个 Sprint 5 验证
4. 检查 `/actuator/prometheus` 有指标输出
5. 切换 i18n + 主题,刷新保留
6. 打 tag `v0.7.0`
