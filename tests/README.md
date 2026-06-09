# Sprint 3 E2E 全量回归测试报告

**测试日期**: 2026-06-09
**测试范围**: Sprint 3 全量功能(ai-flow + ai-assistant)
**测试脚本**: `e2e_sprint3.ps1`
**后端**: localhost:8080 (MariaDB 12.2 + JDK 21 + Spring Boot 3.5.5)
**前端**: 未参与(纯后端 API 验证)
**测试账号**: admin / admin123

## 测试结果

```
PASS: 28
FAIL: 0
TOTAL: 28
```

**通过率: 100%**

## 测试明细

### Section 1: 8 节点执行 (10/10)
- 1.1 Start→End: PASS
- 1.2 SetVar(变量赋值): PASS
- 1.3 IfElse 条件为真: PASS
- 1.4 IfElse 条件为假: PASS
- 1.5 HTTP GET 节点: PASS
- 1.6 HTTP POST 节点: PASS
- 1.7 LLM 节点(无 API key 应失败): PASS
- 1.8 KB 检索(无 KB 应返回空): PASS
- 1.9 Prompt 节点(无 prompt code): PASS
- 1.10 失败传播: PASS

### Section 2: 5 触发器 (5/5)
- 2.1 Manual 手动触发: PASS
- 2.2 Webhook 触发(含 token 解析+端到端调用): PASS
- 2.3 Cron 定时触发(配置入库): PASS
- 2.4 Event 事件触发(订阅注册): PASS
- 2.5 Chained 链式触发: PASS

### Section 3: Flow Versions (1/1)
- 3.1 版本 CRUD: PASS

### Section 4: Custom Nodes (1/1)
- 4.1 自定义节点 CRUD: PASS

### Section 5: Assistant + Tools (11/11)
- 5.1 助手 CRUD: PASS
- 5.2 工具列表(8 个): PASS
- 5.3a Calculator 工具(100/4=25): PASS
- 5.3b Current time 工具: PASS
- 5.3c List projects 工具: PASS
- 5.3d HTTP tool: PASS
- 5.3e Subflow 工具(无 flow 应失败): PASS
- 5.3f KB search 工具(无 KB 应失败): PASS
- 5.3g Project members 工具: PASS
- 5.3h Code run 工具(SpEL 求值): PASS
- 5.4 EventSub 事件订阅: PASS

## 本次回归修复的 6 个后端 Bug

1. **PreProjectRoleAspect 重复方法** — 之前编辑残留了 `resolveProjectId` 两个版本,编译期未发现但 aspect 行为异常。删除重复块。
2. **PreProjectRoleAspect 无法解析 entity 路径的 projectId** — 当 URI 是 `/flow/{id}/run` 时,`{id}` 是 flow id 而非 project id。改为按 URI 前缀查表反查(`ai_flow`、`ai_assistant`、`ai_flow_trigger`),fallback 到 body.projectId。
3. **ChainBuilder edge 字段不匹配** — LogicFlow 标准格式用 `sourceNodeId/targetNodeId`,而测试用 `source/target`。ChainBuilder 只认前者,导致所有 edge 被忽略、只跑 start 节点。增加 `firstNonIdentifiers(...)` 兼容两种格式。
4. **ChainBuilder config 字段不匹配** — LogicFlow 标准用 `properties.config`,测试用 `data`。增加兼容。
5. **IfElseNode 不支持裸变量名** — SpEL 严格区分 `#var`(变量)与 `obj.prop`(属性),`x > 10` 报"Property 'x' not found on null"。增加 `addHashToIdentifiers` 预处理,自动把裸标识符转为 `#ident`,保留关键字与属性访问语义。
6. **WebhookTrigger token 机制错误** — 原本用 triggerId 作为 token,但生成端生成了 `wh_<uuid>`,controller 用 `Long.parseLong(token)` 解析失败。改为 token 存进 `config.token` 字段,`resolveToken` 用 LIKE 查询反查。

## 附带修复

- **FlowTriggerController** webhook 创建时自动注入 token(`wh_<uuid>`)
- **KnowledgeSearchTool** `kbIds` 明确指定但 KB 不存在时返回 fail(而不是空 success)
- **CodeInterpreterTool** 弃用 Nashorn,改用 SpEL 求值(Java 21 已移除 Nashorn)
- **E2E 脚本**:`$global:h` 统一管理 auth header;`Flow-Body` hashtable 构造 design 字段;trigger config 改用 `{}` 字符串;inline run body 改用 `{"input":{...}}` 包装

## 运行方式

```powershell
cd D:\Projet\AI-Platform
powershell -ExecutionPolicy Bypass -File tests\e2e_sprint3.ps1
```

## 输出

- `tests/e2e_run.log` — 完整运行日志
- 控制台 SUMMARY 输出
