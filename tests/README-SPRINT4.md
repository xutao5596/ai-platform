# Sprint 4 E2E 全量回归测试报告

> 测试日期: 2026-06-10
> 测试范围: Sprint 3 全部功能 + Sprint 4 新增(API Key + Webhook)
> 后端: localhost:8080
> 账号: admin / admin123

## 结果

**28/28 回归 PASS** + Sprint 4 新功能手动验证通过

## Sprint 3 回归(28 项)
全部通过(详见 e2e_run.log)

## Sprint 4 新功能手动测试

### API Key CRUD
- POST 创建 → 返回明文 secret ✓
- GET 列表 → 含 masked secret ✓
- PUT 更新 → scopes/rateLimit 可改 ✓
- POST reset → 新 secret 替换旧 ✓
- DELETE → 软删 ✓

### API Key 鉴权(`/api/v1/ext/flow/list`)
- 无 ApiKey → 401 ✓
- 有 ApiKey → 200,返回流程列表 ✓
- 限流触发 → 429 + Retry-After ✓

### Webhook CRUD
- POST 创建 → 返回明文 secret ✓
- GET 列表 ✓
- PUT 更新 ✓
- DELETE ✓
- POST reset-secret ✓
- POST test → 立即触发 ✓
- GET logs → 返回投递历史 ✓

### Webhook 接收
- 无签名 → 401 ✓
- 错误签名 → 401 ✓
- 时间戳过期 → 401 ✓
- 正确签名 → 202,异步派发 ✓
- WebhookDispatcher 重试:1s/5s/30s/5min ✓

## 联调合并冲突处理
- `ai-frontend/src/router/index.ts` — A + B 都加路由,合并保留两条
- `ai-frontend/src/views/project/detail.vue` — A + B 都加按钮,合并保留两个
- `JwtAuthenticationFilter` — B 没动(A 加了 `/api/v1/ext/**`,B 不需要加,因现有 `webhook/**` 规则已覆盖)
- 无后端代码冲突

## Sprint 3.1 编码规范执行
- 全部 3 个 Agent 严格遵守 `[System.IO.File]` API
- 0 个文件带 BOM
- 全部 `mvn -o compile` BUILD SUCCESS
