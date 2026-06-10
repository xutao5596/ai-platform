# Sprint 5 E2E 全量回归测试报告

> 测试日期: 2026-06-10
> 测试范围: Sprint 3 全部 28 个 + Sprint 4 + Sprint 5 新增
> 后端: localhost:8080 (MariaDB 12.2 + JDK 21 + Spring Boot 3.5.5)
> 账号: admin / admin123

## 结果

**Sprint 3 回归: 28/28 PASS**(零回归)
**Sprint 5 新功能验证**:

### A. 审计日志激活 ✅
- `sys_log` 表记录数: **78 条**(E2E 期间产生)
- 字段完整:module/action/method/user_id/username/status/cost_ms/ip/user_agent
- 状态码 1=成功 0=失败
- 排除路径生效:`/api/v1/auth/login` 也有记录(契约未排除登录,只排除 captcha/refresh/ext/webhook-receive)
- AOP 自动拦截所有 `@RestController` 方法

### B. 可观测性三件套 ✅
- `GET /actuator/prometheus` → 200, 返回 Prometheus 格式
- 自定义指标:
  - `login_count_total{application="ai-platform",status="success"} 1.0` ✓
  - 业务指标 7 类(login / flow_run / flow_node / api_key / webhook / assistant)就位
- 响应头 `X-Trace-Id: 48eeb032aae44f89b7a43c52a61aa78e` ✓
- 日志格式 `[traceId=...] INFO` ✓
- Micrometer + Prometheus 自动暴露 80+ JVM/系统指标

### E. 国际化(zh-CN + en-US) ✅
- vue-i18n@9.14.5 集成
- locale 文件:
  - `locales/zh-CN.ts` 25.3 KB / 801 行
  - `locales/en-US.ts` 25.1 KB / 801 行
- 31 个 .vue/.ts 文件应用 i18n,**78% 可见文案**被覆盖
- 切换 UI 在 layouts 顶部(el-dropdown)
- localStorage 持久化
- Element Plus 国际化跟随(`el-config-provider`)

### F. 暗色主题 ✅
- CSS 变量层(`variables.scss`):浅色/暗色两套
- 暗色覆盖(`dark.scss`):Element Plus 10+ 组件 + 自定义 chat/assistant
- 切换 UI 在 App.vue 右下角浮动按钮(月亮/太阳)
- localStorage 持久化
- 7+ 个核心页面用 CSS 变量(超过契约 5 个最低要求)

## 联调合并冲突

### 已解决
- `ai-frontend/src/views/system/log.vue` — A 加了过滤/状态字段,E 改了 i18n 文案,3 个 conflict block,取 E 方案(E 是更新)
- `ai-frontend/src/api/system/log.ts` — LogQuery 类型未加 `keyword`,后修
- `ai-frontend/src/router/index.ts` — Sprint 4 残留 merge 标记,E 已修

### 后端零冲突(A + B 不重叠)
- A 改 ai-framework + ai-system
- B 改 ai-framework + ai-flow + ai-assistant + ai-start + ai-project
- application.yml 由 B 单独改(无冲突)

## 编码规范执行
- 4 个 Agent 全部 BOM-free
- 全部 `mvn -o compile` BUILD SUCCESS
- 前端 `npm run build` BUILD SUCCESS

## tag
**v0.7.0** 已推送到 origin

## Git
- 分支:develop
- HEAD: `5707514`
- 4 个 merge commit + 1 个 hotfix commit
- 4 个 feature/sprint5-*/ 分支保留(已 merge)

## Sprint 5 总结
- **后端 9 模块 + 审计日志 + 可观测性**
- **前端 53 文件 + i18n + 暗色主题**
- **生产级可观测性就位**(指标 + Trace)
- **安全合规** — 所有用户操作可追溯
