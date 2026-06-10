# AI-Platform 项目记忆

> **目的**：本文件是 AI-Platform 项目的核心记忆库,用于在上下文压缩或新会话开始时恢复项目状态。
> **维护原则**：每次重大决策后必须更新本文件。
> **最后更新**: 2026-06-08(Sprint 1 启动首日)

---

## 0. 快速恢复指引

如果上下文丢失,先读这 5 个文件恢复项目全貌:
1. `D:\Projet\AI-Platform\.memory\CLAUDE.md` (本文件) - 项目记忆
2. `D:\Projet\AI-Platform\README.md` - 项目概览
3. `D:\Projet\AI-Platform\docs\PLAN.md` - 开发计划
4. `D:\Projet\AI-Platform\docs\SOW.md` - 工作说明书
5. `D:\Projet\AI-Platform\docs\DECISIONS.md` - 决策记录

---

## 1. 项目一句话定位

**AI-Platform** 是一个 **以"项目 (Project)"为顶层容器的 AI 流程自动化编排平台**,项目内可创建多个 AI 助手(复用 Flow 机制)和业务流程;通过拖拽式可视化编辑器构建 AI 流程;支持 5 种触发器;提供多成员协作 4 级权限;部署仅需 JDK + MariaDB + Nginx。

### 1.1 当前进度(2026-06-10)

- **Sprint 1 + Sprint 2 + Sprint 3 + Sprint 3.1 + Sprint 4 + Sprint 5 全部完成** ✅
- **Sprint 1**:后端 8 模块 + 16 张表 + JWT + 系统管理 + 项目域 + 14 前端页面 (v0.2.0)
- **Sprint 2**:11 张 AI 表 + LangChain4j + 模型/知识库/提示词/对话 (v0.3.0/v0.3.1)
- **Sprint 3**:3 Agent 并行,Flow 引擎 + Assistant + 前端 (v0.4.0)
- **Sprint 3.1**:技术债清理(tag v0.5.1)
  - **Agent A**:LangChain4j Function Calling + SSE 真流式 + 真实 Embedding API
  - **Agent B**:Hnswlib 1.2.1 完整集成 + 索引持久化(V7 migration)
  - **Agent C**:LiteFlow 2.15.0 集成 + `AiFlowRun.costMs` 修复
- **Sprint 4**(tag v0.6.0):
  - **Agent A**:API Key 池(CRUD + 鉴权 + 限流 + 外部 API 端点)
  - **Agent B**:Webhook 外部触发(CRUD + HMAC 验签 + 异步重试 + 投递日志)
  - **Agent C**:部署脚本(deploy/backup/restore/upgrade)+ JMeter 压测 + 用户手册
- **Sprint 5**(本轮,2026-06-10 完成,**28/28 回归 + 新功能验证**,tag v0.7.0):
  - **Agent A**:审计日志激活(AOP 切面 + 78 条 sys_log 记录 + 排除白名单 + 密码字段脱敏)
  - **Agent B**:可观测性三件套(Micrometer + Prometheus 端点 + 7 类业务指标 + traceId MDC + X-Trace-Id header)
  - **Agent E**:国际化(中英双语 vue-i18n + 31 个文件 78% 覆盖)
  - **Agent F**:暗色主题(CSS 变量 + Element Plus dark + 7+ 页面覆盖)
- **Sprint 6 计划**(待规划):鉴权增强(MFA/SSO)、CI/CD 流水线、单元测试覆盖率、暗色主题扩展到剩余页面

---

## 2. 核心架构

### 2.1 顶层模型

```
项目 Workspace (顶层)        全局资源
├── 流程 (含 AI 助手)        ├── 大模型 (全局共享)
├── 知识库 (项目隔离)        └── MCP 服务 (全局共享)
├── 提示词 (项目隔离)
├── 成员 (4 级角色)          ai_app 已移除
├── API Key
└── Webhook
```

### 2.2 关键决策

| 决策项 | 选择 | 理由 |
|---|---|---|
| 顶层实体 | **项目 (Project)** | 用户指定 |
| ai_app 表 | **已移除** | 项目直接包含流程 |
| 知识库/提示词 | **项目隔离** | 数据安全 |
| 大模型/MCP | **全局共享** | 配置类资源 |
| 助手 | **项目内多实例(复用 Flow)** | 用户指定 |
| 多租户 | **不实现** | 用户指定 |
| 团队规模 | **6 人** | 吸收增加的工作量 |
| 工期 | **8 周** | 不变 |
| UI 导航 | **左侧栏 200px** | 用户最新决定(2026-06-08) |
| **鉴权方案**(2026-06-08 新增) | **自研 JWT + AOP,弃用 Shiro** | Shiro 2.0.2 仍用 javax.servlet,与 Spring Boot 3 / Jakarta 不兼容 |

### 2.3 不引入的依赖

- ❌ Redis (用 Caffeine 替代)
- ❌ PostgreSQL (用 MariaDB + Hnswlib 替代)
- ❌ pgvector (用 Hnswlib 嵌入式替代)
- ❌ 微服务 (单体部署)
- ❌ 多租户

---

## 3. 技术栈(已确认)

### 3.1 后端

| 类别 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 语言 | Java | 21 LTS | ✅ |
| 框架 | Spring Boot | 3.5.5 | ✅ |
| 持久层 | MyBatis Plus | 3.5.9 | ✅ |
| 数据库 | MariaDB | 12.2 | ✅ root/root,db=ai_platform |
| 缓存 | Caffeine | 3.1.8 | ✅ |
| 向量库 | Hnswlib (jelmerk) | **1.2.1** | 修正(原 0.7.1 不存在) |
| 权限 | **自研 JWT + AOP** | - | ❌ 弃用 Shiro 2.0.2(强依赖 javax.servlet,无法在 SB3 用) |
| AI | LangChain4j | 1.9.1 | ⏳ Sprint 2 引入 |
| 流程 | LiteFlow | 2.15.0 | ✅ starter 已配置 |
| 文档 | Apache Tika | 3.2.3 | ⏳ Sprint 2 引入 |
| 限流 | Bucket4j | 8.10.1 | ❌ 暂时移除(Maven Central 无此 artifact,待 Sprint 4 再选型) |
| JWT | jjwt | 0.12.6 | ✅ |
| API 文档 | Knife4j 3 + springdoc | 4.5.0 / 2.6.0 | ✅ |

### 3.2 前端

| 类别 | 选型 | 版本 |
|---|---|---|
| 框架 | Vue | 3.5.x |
| 构建 | Vite | 6.x |
| 语言 | TypeScript | 5.x |
| UI 库 | **Element Plus** | 2.8.x |
| 状态 | Pinia | 2.1.x |
| 流程图 | LogicFlow | 2.0.x |
| 图表 | ECharts | 5.6.x |

### 3.3 LLM 支持

OpenAI 兼容(主) + DeepSeek + 智谱 + 通义千问 + Claude + Ollama

---

## 4. 数据库(37 张表)

### 4.1 系统管理 (10 张)
`sys_user` `sys_role` `sys_user_role` `sys_menu` `sys_role_menu` `sys_role_permission` `sys_dept` `sys_dict` `sys_dict_item` `sys_log`

### 4.2 项目域 (6 张)
`ai_project` `ai_project_member` `ai_project_api_key` `ai_project_webhook` `ai_project_webhook_log` `ai_file`

### 4.3 流程域 (6 张)
`ai_flow` `ai_flow_version` `ai_flow_trigger` `ai_flow_run` `ai_flow_run_step` `ai_custom_node`

### 4.4 AI 资源-项目隔离 (5 张)
`ai_knowledge` `ai_knowledge_doc` `ai_knowledge_chunk` `ai_prompt` `ai_prompt_version`

### 4.5 AI 资源-全局 (2 张)
`ai_model` `ai_mcp`

### 4.6 AI 助手 (4 张)
`ai_assistant_config` `ai_assistant_event_sub` `ai_assistant_session` `ai_assistant_message`

### 4.7 AI 对话 + 通用 (4 张)
`ai_chat_session` `ai_chat_message` `sys_quartz_job` `sys_notice`

---

## 5. UI 设计要点

### 5.1 导航 (用户最终决定 2026-06-08)

**侧边栏 200px 左侧**,6 个一级菜单(从原 Jeecg-AI 沿用结构):
- 工作台
- 我的项目
- AI 资源
- 监控
- 系统管理
- 助手(带数字角标)

> ⚠ 注意:用户中途想改为顶部导航(Notion 风格),后明确"还是之前侧边的设计",**保持侧边栏**。

### 5.2 工作台

**双栏式仪表盘**:
- 左 60%:数据可视化(运行趋势/流程排行/知识库使用/任务调度)
- 右 40%:助手推荐/项目卡片/通知

### 5.3 流程编辑器(核心)

**5 页签设计**:
- 设计(拖拽画布 + LogicFlow)
- 调试(单步执行 + 实时日志)
- 运行(运行历史列表)
- 监控(实时 QPS / P95 / 错误率)
- 设置(基础信息/触发器/超时/版本)

### 5.4 助手入口

- **全局右下角悬浮按钮**(带角标)
- 项目内助手列表
- 嵌入式浮窗(380x520)
- 全屏对话页
- iframe 嵌入第三方

### 5.5 主题

- 3 套主题色:商务蓝 #409EFF / 智能紫 #6366F1 / 科技绿 #10B981
- 明暗双模
- 项目级覆盖
- 顶栏 [🎨] 按钮切换

### 5.6 22 个核心界面

均已在对话中设计完毕,详见 `docs/UI-DESIGN.md`

---

## 6. 关键功能设计

### 6.1 12 个内置节点 (AI 为主)

| # | 节点 | 类型 Key | 分类 |
|---|---|---|---|
| 1 | 开始 | start | 基础 |
| 2 | 结束 | end | 基础 |
| 3 | LLM 调用 | llm | AI |
| 4 | 知识库检索 | knowledge_search | AI |
| 5 | 提示词模板 | prompt | AI |
| 6 | Agent 决策 | agent | AI |
| 7 | 条件分支 | if_else | 控制 |
| 8 | 子流程调用 | subflow | 控制 |
| 9 | HTTP 请求 | http | 工具 |
| 10 | MCP 工具 | mcp_tool | 工具 |
| 11 | 脚本执行 | script | 工具 |
| 12 | 变量赋值 | set_var | 数据 |

### 6.2 5 种触发器

| 触发器 | 实现 |
|---|---|
| Manual | 用户点击"运行" |
| Cron | Quartz 调度 |
| Webhook | Controller + 鉴权 + 投递日志 |
| Event | ApplicationEvent 内部 |
| Chained | FlowService 内部调用 |

### 6.3 项目 4 级角色

| 角色 | 权限范围 |
|---|---|
| Owner | 全部 + 删除项目 |
| Admin | 项目管理/成员/API/Webhook/流程 CRUD |
| Developer | 流程 CRUD/发布/运行 |
| Viewer | 查看/运行 |

### 6.4 助手能力

- 工具调用(MCP/HTTP)
- 流程调用(子流程)
- 数据操作(系统工具集,带权限校验)
- RAG 检索
- 事件订阅与响应

### 6.5 节点自定义(2 层)

- 开发者扩展:Java 类实现 FlowNode 接口
- 用户自定义:UI 配置脚本/HTTP/SQL 节点

---

## 7. 开发计划(8 周 4 Sprint)

### 7.1 时间表

| Sprint | 周次 | 主题 | 工时 |
|---|---|---|---|
| 0 | Week 0 | 启动准备 + 技术预研 | 5 人天 |
| 1 | Week 1-2 | 脚手架 + 系统管理 + 项目域 | 22 人天 |
| 2 | Week 3-4 | AI 模型 + 知识库 + AI 对话 | 25 人天 |
| 3 | Week 5-6 | 流程编辑器 + 助手 + 触发器 | 30 人天 |
| 4 | Week 7-8 | API Key + Webhook + 联调上线 | 22 人天 |
| **合计** | **8 周** | | **104 人天** |

### 7.2 里程碑

- **M0** Week 0 - 团队就绪
- **M1** Week 2 - 系统管理 + 项目可用
- **M2** Week 4 - AI 对话 + 知识库可用
- **M3** Week 6 - 流程编辑器 + 助手可用
- **M4** Week 8 - 完整系统 + 部署

### 7.3 团队分工

| 角色 | 人数 | 负责 |
|---|---|---|
| 后端架构师 | 1 | 基础框架 + AI 引擎 + 流程引擎 + 助手核心 |
| 后端开发 A | 1 | 系统管理 + 项目域 + 权限 |
| 后端开发 B | 1 | AI 业务 + 流程节点 + 助手工具 |
| 前端主程 | 1 | 脚手架 + AI 对话 + 流程编辑器 + 助手对话框 |
| 前端开发 | 1 | 系统管理 + AI 管理页面 + 助手配置 |
| 测试/DevOps | 0.5 | 联调 + 压测 + 部署 + 文档 |

---

## 8. 部署

### 8.1 依赖

- ✅ JDK 21 (已装 21.0.2)
- ✅ Maven 3.9.15 (已装)
- ✅ Node 24 (已装)
- ✅ MariaDB 12.2 (已装, root/root)
- ❌ Redis (不需要)
- ❌ PostgreSQL (不需要)
- ❌ Nginx (部署时安装)

### 8.2 部署架构

```
Nginx (80/443)
├── /api/* → ai-backend.jar (Spring Boot, 8080)
└── /* → ai-frontend dist (静态资源)
```

### 8.3 数据目录

```
/data/
├── hnsw/        # 向量索引文件
├── upload/      # 上传文件
└── logs/        # 日志
```

---

## 9. 项目参考(已存在)

### 9.1 参考项目位置

`D:\Projet\jeecg-ai-main` (Jeecg-AI v3.9.1)

### 9.2 可参考的代码

- LangChain4j 集成方式(airag/llm/handler/AIChatHandler.java)
- LiteFlow 流程设计(`ai_flow.design` + `ai_flow.chain` 双字段模式)
- 节点类型定义(参考 `airag/flow/component/`)
- Element Plus 集成(参考 jeecgboot-vue3,但要换 UI 库)
- LogicFlow 用法(`@logicflow/core` + `DndPanel`)

### 9.3 不要复制

- Online 表单、积木报表、大屏等(不在范围)
- 大量 demo 代码
- 微服务相关

---

## 10. 重要决策变更历史

| 日期 | 决策 | 变更原因 |
|---|---|---|
| 2026-06-08 | UI 导航 | 中途想改顶部,后**维持侧边栏** |
| 2026-06-08 | 移除 Redis/PostgreSQL | 简化部署 |
| 2026-06-08 | 项目作为顶层 | 移除 ai_app |
| 2026-06-08 | 流程节点可自定义 | 用户要求 |
| 2026-06-08 | 增加 AI 助手 | 核心能力 |
| 2026-06-08 | 项目为 Workspace | 用户指定 |
| **2026-06-08**(Sprint 1 Day 1) | **弃用 Shiro,改自研 JWT + AOP** | Shiro 2.0.2 仍依赖 javax.servlet,与 Spring Boot 3 不兼容 |
| 2026-06-08 | Hnswlib 版本 0.7.1 → 1.2.1 | 0.7.1 不存在 |
| 2026-06-08 | Bucket4j 暂移除 | Maven Central 无 bucket4j_jdk17-core 8.10.1,需 Sprint 4 重新选型 |

---

## 11. 待办与开放问题

### 11.1 Sprint 1 + Sprint 2 + Sprint 3 完成清单

#### ✅ Sprint 1(Day 1-2)
- [x] 后端 8 模块 + 16 张表(V1/V2) + JWT + 系统管理 + 项目域
- [x] 前端 Vue3 脚手架 + 14 页面
- [x] v0.2.0 tag

#### ✅ Sprint 2(Day 3 2026-06-09)
- [x] **后端**:11 张 AI 表(V3 7 张 + V4 4 张) + LangChain4j 1.9.1 + langchain4j-open-ai + Tika 3.2.3
- [x] **前端**:4 页面(模型/知识库/提示词/AI 对话) + 完整 CRUD + 会话管理
- [x] v0.3.0 / v0.3.1 tag

#### ✅ Sprint 3(Day 3-4 2026-06-09)— 三 Agent 并行
- [x] **Agent A(后端架构师)**:ai-flow — 6 张表 + FlowNode SPI + NodeRegistry(自动扫描) + 8 节点 + 5 触发器 + FlowRunner(BFS 执行)
- [x] **Agent B(后端开发)**:ai-assistant — 3 张表 + 8 工具(http/subflow/kb/list_projects/get_project_members/current_time/calculator/code_run) + 事件订阅 + prompt 引导式 tool calling + SSE 流
- [x] **Agent C(前端主程)**:LogicFlow 拖拽编辑器(3 栏) + 流程列表/详情/运行历史 + 助手页(SSE 工具调用展示)
- [x] **联调**:Flow Start→End 跑通 + 8 节点定义 + 8 工具 + Calculator `(1+2)*3^2=27` ✅
- [x] v0.4.0 tag

### 11.2 Sprint 4 待办(Week 7-8)

- [ ] API Key 鉴权拦截器(限流、Sprint 2.2 已移除的 Bucket4j 重新选型)
- [ ] Webhook 管理 + 投递 + 签名验证 + 重试
- [ ] 监控(QPS/P95/错误率)
- [ ] 部署脚本完善(systemd + nginx)
- [ ] 主题切换(3 色 + 明暗)
- [ ] 用户手册 + 压测

### 11.3 Sprint 3.1 待办(技术债,可在 Sprint 4 之前修)

- [ ] **tool calling 升级** LangChain4j Function Calling(替代 prompt 引导 JSON)
- [ ] **Hnswlib 1.2.1 完整集成** 替换 in-memory 向量
- [ ] **真实 embedding API** 替换 hash-based PoC
- [ ] **LiteFlow 完整集成** 替换 BFS 简化版 FlowRunner
- [ ] **SSE 真流式 token 输出** 替换分块重发
- [ ] **AiFlowRun.costMs bug** 字段类型错(显示成 epoch 毫秒)

### 11.4 开放问题

- 暂无重大开放问题

---

## 14. Sprint 1 关键技术细节(2026-06-08 实施笔记)

### 14.1 Maven 多模块
- 父 pom:`ai-platform` (packaging=pom),继承 spring-boot-starter-parent 3.5.5
- 子模块:ai-common / ai-framework / ai-system / ai-project / ai-flow / ai-ai / ai-assistant / ai-start
- ai-start 引入所有子模块,作为可执行 jar 入口
- Lombok 注解处理器在父 pom 的 `<build><plugins>` 显式声明(不放在 pluginManagement)
- mybatis-plus 分页拦截器在 ai-framework 集中配置,所有模块共用

### 14.2 鉴权方案(Shiro 弃用后的自研实现)
- **JWT 签发/解析**:`ai-framework/jwt/JwtTokenProvider`(基于 jjwt 0.12.6)
- **JWT 过滤器**:`ai-framework/security/JwtAuthenticationFilter`(extends Spring `OncePerRequestFilter`)
  - 跳过路径:login/refresh/captcha/webhook/public/druid/swagger/actuator/error/favicon/static
  - 失败时直接写 401 JSON,不进入 controller
- **授权 AOP**:`ai-framework/security/AuthorizationAspect`
  - 拦截 `@RequiresRoles` 和 `@RequiresPermissions` 注解
  - 从 `UserContext` 取当前用户;`admin=true` 直接放行
- **项目级 AOP**:`ai-project/security/PreProjectRoleAspect`
  - 拦截 `@PreProjectRole("admin")` 等注解
  - 自动从 `@PathVariable Long projectId` 解析项目 ID
  - 4 级:owner(4) > admin(3) > developer(2) > viewer(1)
- **当前用户上下文**:`com.aiplatform.common.context.UserContext`(基于 ThreadLocal)

### 14.3 Flyway
- 位置:`ai-start/src/main/resources/db/migration/`
- 文件:`V1__init_schema.sql`(16 张表)、`V2__seed_data.sql`(admin 用户、6 菜单、7 字典)
- 配置:`spring.flyway.locations=classpath:db/migration`、`baseline-on-migrate=true`、`create-schemas=true`
- 启动会自动执行

### 14.4 启动方式
```bash
# 本地启动
java -jar ai-backend/ai-start/target/ai-start-1.0.0.jar --spring.profiles.active=dev

# 验证登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 14.5 已知问题与对策
- `WSREP_ON` 警告:启动时 MariaDB 12.2 新增的系统变量 Flyway 不识别,不影响功能
- Hnswlib 1.2.1 是新版本,与原 0.7.1 API 有差异,Sprint 2 集成时需注意
- Flyway 推荐升级到支持 MariaDB 12.x 的版本(目前用 9.x,Sprint 4 前可升级)

### 14.6 前端架构(2026-06-09 完成)

```
ai-frontend/
├── package.json          # Vue 3.5 / Vite 6 / TS 5.7 / Element Plus 2.9 / Pinia 2.3
├── vite.config.ts        # 端口 5173,/api 代理 → localhost:8080
├── tsconfig.json         # paths @/* → ./src/*
├── index.html
├── .env.development      # VITE_API_BASE=http://localhost:8080
├── .env.production       # VITE_API_BASE= (用 nginx 代理)
└── src/
    ├── main.ts           # 入口:Vue + Pinia + persistedstate + ElementPlus(zh-cn) + 图标
    ├── App.vue
    ├── styles/index.scss # CSS 变量(主题色/侧边栏宽度/字体)
    ├── api/
    │   ├── auth.ts       # 登录/登出/refresh/profile/menus/permissions
    │   ├── system/{user,role,menu,dept,dict,log}.ts
    │   ├── project.ts    # 项目 + 成员
    │   └── ai/           # Sprint 2 新增
    │       ├── model.ts  # 模型 CRUD
    │       ├── knowledge.ts
    │       ├── prompt.ts # 提示词 + 版本
    │       └── chat.ts   # AI 对话
    ├── store/modules/
    │   ├── user.ts       # token/refreshToken/userInfo/roles/permissions,localStorage
    │   └── app.ts        # sidebarCollapsed/menus/currentProjectId
    ├── router/index.ts   # createWebHistory + 路由守卫(未登录 → /login) + NProgress
    ├── utils/http.ts     # axios + JWT 注入 + 401 自动登出 + 统一错误提示
    ├── layouts/index.vue # 200px 侧边栏(可折叠 64px)+ 顶栏(breadcrumb + 用户下拉)
    └── views/
        ├── login/index.vue           # 渐变背景 + admin/admin123 提示
        ├── dashboard/index.vue       # 4 统计卡 + 4 步快速开始 + 个人卡片
        ├── system/{user,role,menu,dept,dict,log}.vue  # 完整 CRUD
        ├── project/{list,detail,members}.vue
        ├── ai/
        │   ├── model.vue             # Sprint 2:模型管理
        │   ├── knowledge.vue         # Sprint 2:知识库
        │   ├── prompt.vue            # Sprint 2:提示词 + 版本
        │   ├── chat.vue              # Sprint 2:AI 对话
        │   └── mcp.vue               # Sprint 3
        ├── monitor/index.vue         # Sprint 4 占位
        └── assistant/index.vue       # Sprint 3 占位
```

**关键约定**:
- 权限控制:`userStore.hasPermission("system:user:add")`,无权限按钮自动隐藏
- HTTP:统一在 `utils/http.ts` 拦截,业务代码只关心 data
- 类型:每个 API 文件导出 VO/Save 类型,View 层强类型
- 主题色:3 套(蓝/紫/绿)走 CSS 变量 `--ai-primary`,Sprint 4 加切换器

### 14.7 Sprint 2 关键技术细节(2026-06-09)

#### 14.7.1 LangChain4j 集成
- **artifact**:`dev.langchain4j:langchain4j-core` + `langchain4j-open-ai`(1.9.1)
- **统一客户端**:`OpenAiChatModel` / `OpenAiStreamingChatModel`(OpenAI 兼容协议)
- **LlmProviderFactory**:每个 modelId 缓存 ChatModel + StreamingChatModel 实例
- **厂商适配**:`provider` 字段(openai/deepseek/qwen/glm/ollama) → 映射默认 baseUrl
  - openai: https://api.openai.com/v1
  - deepseek: https://api.deepseek.com/v1
  - qwen: https://dashscope.aliyuncs.com/compatible-mode/v1
  - glm: https://open.bigmodel.cn/api/paas/v4
  - ollama: http://localhost:11434/v1
- **配置项**:apiBase、apiKey、maxTokens、temperature、embeddingModel、dimension

#### 14.7.2 简化版向量库(PoC)
- **真实生产**:Hnswlib 1.2.1(API 改造复杂,Sprint 2.1 升级)
- **当前**:`HnswlibVectorStore` 内存 Map + 余弦相似度,支持序列化为 `.vec` 文件
- **Embedding**:`EmbeddingService` hash-based 确定性向量(Sprint 2.1 替换为真 API)

#### 14.7.3 AI 对话 SSE 流式(PoC)
- **路径**:`/api/v1/ai/chat/stream` (produces=text/event-stream)
- **PoC 版**:同步 chat 完后,按 8 字符分块 + 30ms 间隔发送,模拟流式
- **生产版**:改用 `OpenAiStreamingChatModel.tokenStream()` + SSE event

#### 14.7.4 关键 bug 修复
- **Bug 1**:`@PreProjectRole` aspect 抛 NPE
  - 原因:`@annotation(ppr) || @within(ppr)` 合并,`ppr` 在 `@within` 触发时为 null
  - 修复:拆分为两个 `@Around`,`doCheck` 内 null-safe
- **Bug 2**:`projectId` 解析失败
  - 原因:Java 21 + SB 3 默认不保留编译期参数名,`@PathVariable` 无显式 value 时取不到
  - 修复:aspect 优先从 `HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE` 取
- **Bug 3**:`separator` 字段 SQL syntax error
  - 原因:MariaDB 保留字
  - 修复:列名 `sep` + 移除 DEFAULT '\n\n'(Flyway 转义问题)
- **Bug 4**:`insert-strategy: not_null` 导致 unique 索引冲突
  - 原因:deleted 字段不写入 SQL,unique (provider, model_name, deleted) 区分不开
  - 修复:删除全局配置,使用 MyBatis Plus 默认(全部字段写入)

### 14.8 Sprint 3 关键技术细节(2026-06-09)

#### 14.8.1 流程引擎 (ai-flow)
- **节点 SPI**:`FlowNode` 接口 (getTypeKey/getDisplayName/getCategory/getIcon/getColor/getSchema/execute)
- **NodeRegistry**:Spring 启动时自动扫描所有 `@Component implements FlowNode`,按 typeKey 索引
- **节点设计数据**:LogicFlow JSON 格式 (`{nodes: [...], edges: [...]}`)
- **FlowRunner**:BFS 顺序执行器(Sprint 3.1 改 LiteFlow 正式版)
- **5 触发器实现**:
  - Manual: 同步调用
  - Cron: Spring TaskScheduler + cron 表达式(Sprint 3.1 换 Quartz)
  - Webhook: `/api/v1/webhook/flow/{token}` 接收,token → flowId 映射
  - Event: 监听 ApplicationEvent(flow.run.success / failed)
  - Chained: 内部服务调用入口
- **8 内置节点**:start / end / llm / knowledge_search / prompt / if_else / http / set_var
- **重要坑**:`@EventListener` 在 JDK 代理下失效(接口方法未被代理),必须用独立类(不实现接口,CGLIB 代理)

#### 14.8.2 AI 助手 (ai-assistant)
- **8 工具**:http_request / run_flow(子流程) / search_kb / list_projects / project_members / current_time / calculator / code_run
- **ToolRegistry**:自动扫描 `@Component AssistantTool`
- **tool calling 实现**:prompt 引导 LLM 输出 JSON `{"tool": "name", "args": {...}}` 解析 → 调工具 → 反馈 LLM(最多 5 轮防死循环)
- **SSE 格式**:content / tool_call / tool_result / done 4 种事件
- **Sprint 3.1**:换 LangChain4j Function Calling

#### 14.8.3 前端 LogicFlow 集成
- **依赖**:`@logicflow/core` + `@logicflow/extension` 2.2.3
- **CSS 路径**:`dist/index.css`(2.x 改了路径)
- **API**:`lf.register(nodeDef)` + `lf.render(empty)` + `lf.getGraphData()`
- **节点数据**:从 `/api/v1/flow/node-definitions` 动态加载
- **属性面板**:根据 `Property.type` 渲染不同控件(model/prompt/kb 下拉)

#### 14.8.4 多 Agent 并行实战教训
- **API 契约先行**:`docs/api/sprint3-contracts.md` 锁定了所有路径 + DTO 字段,3 个 Agent 互不阻塞
- **分支命名**:`feature/sprint3-{flow|assistant|frontend}` 清晰区分所有权
- **跨分支污染**:Agent C 的 working dir 被 Agent A/B 切换,需要 `git stash + checkout + stash pop` 恢复
- **共享配置**:application.yml / AiPlatformApplication 三个 Agent 都改,合并后由 `git merge -no-ff` 保留各自 commit
- **PowerShell JSON 测试**:用 `InFile` 传 JSON 文件避免中文字符串拼接乱码

### 12.1 远程仓库(2026-06-08 已推送)

- **GitHub**:https://github.com/xutao5596/ai-platform
- **默认分支**:main
- **协议**:SSH(本机 HTTPS 443 端口被阻止)
- **已推送**:2 commits + main + develop + v0.1.0 tag
- **35 个文件,~7,140 行**

### 12.2 仓库(本地)

- 已初始化 Git 仓库(本地)
- 默认分支:`main`(生产)+ `develop`(开发)
- 提交规范:Conventional Commits
- 分支策略:见 `docs/GIT-WORKFLOW.md`

### 12.3 本机网络环境(重要!)

```
端口 22 (SSH)  ✅ 可用 ← 使用
端口 80 (HTTP) ✅ 可用
端口 443 (HTTPS) ❌ 被阻止 → 不能用 HTTPS
DNS 解析      ⚠️ 本地 DNS 不可用,但 git 仍可解析(走系统)
```

**结论**:所有 git 操作必须使用 SSH 协议。

### 12.4 SSH 密钥配置(本机特殊)

由于 Windows OpenSSH agent 服务被禁用(权限受限),**不能**用 ssh-agent,改为:

```bash
git config core.sshCommand '"/c/Windows/System32/OpenSSH/ssh.exe" -i /c/Users/10099/.ssh/id_ed25519 -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new'
```

**注意**:使用正斜杠(`/`)和 Git Bash 风格路径(`/c/Users/...`)。

### 12.5 日常推送命令(必须遵守)

```bash
# 1. 进入项目
cd D:\Projet\AI-Platform

# 2. 拉取最新
git checkout develop
git pull origin develop

# 3. 创建功能分支
git checkout -b feature/AI-XXX-description

# 4. 开发 + 提交
git add .
git commit -m "feat(scope): description"

# 5. 推送
git push -u origin feature/AI-XXX-description

# 6. 在 GitHub 创建 PR(feature → develop)
```

### 12.6 完整推送工作流(详见 docs/OPERATIONS.md)

文档: `docs/OPERATIONS.md`(包含完整命令、故障排查、SSH 配置等)

### 12.7 CI/CD 文件

```
.github/
├── workflows/
│   ├── ci.yml          # CI:lint + test + build
│   ├── cd.yml          # CD:deploy to staging/production
│   ├── codeql.yml      # 安全扫描
│   └── pr-check.yml    # PR 验证
├── ISSUE_TEMPLATE/
└── PULL_REQUEST_TEMPLATE.md

.gitlab-ci.yml          # GitLab CI 替代方案
```

### 12.8 部署脚本

```
ai-deploy/
├── scripts/
│   ├── deploy.sh       # 部署
│   ├── backup.sh       # 备份
│   ├── restore.sh      # 恢复
│   └── service.sh      # 服务管理
├── systemd/
│   └── ai-platform.service
├── nginx/
│   ├── nginx.conf
│   └── conf.d/ai-platform.conf
└── docker/
    ├── Dockerfile.backend
    ├── Dockerfile.frontend
    └── docker-compose.yml
```

### 12.9 CI/CD 工作流

```
push to develop → CI (lint/test/build) → 自动部署到 staging
push to main   → CI → CD 部署到 production
tag v*         → CD 部署到 production (需手动确认)
PR to main     → CI + PR Check
```

---

## 13. 联系信息(占位)

| 角色 | 姓名 | 联系方式 |
|---|---|---|
| 项目经理 | TBD | - |
| 技术负责人 | TBD | - |
| 产品负责人 | TBD | - |
| 后端架构师 | TBD | - |

---

**END OF MEMORY**

---

## 15. 永久记忆:Sprint 3.1 踩过的坑(2026-06-09)

> **强制规范见 `docs/OPERATIONS-ENCODING.md`**,**每次启动 Agent 必读**。

### 15.1 编码灾难
- Windows + PowerShell 5.1 + Git 三方编码不一致 = 必踩坑
- Git 按 GBK 解码 UTF-8 文件 → 中文变 mojibake → Agent 写回污染整个分支
- PowerShell `Get-Content` / `Set-Content` 默认带 UTF-8 BOM → Java 编译报"unmappable character"
- **绝对禁止**:`Get-Content` / `Set-Content` / `Out-File` 处理中文文件
- **必须用**:`[System.IO.File]::ReadAllBytes` + `[System.IO.File]::WriteAllText(path, content, [System.Text.UTF8Encoding]::new($False))`

### 15.2 LiteFlow 2.15.0 集成要点
- **QLExpress 解析器无法处理 `flow.start` 这种带点的 ID**(会当成属性访问)
- 必须用纯单词 ID: `start` / `end` / `llm` / `set_var` / `if_else`
- **LiteFlow 必须配置 `rule-source`**,不能省略
- placeholder chain 示例(`src/main/resources/liteflow/empty.el.xml`):
  ```xml
  <flow>
      <chain name="placeholder">
          THEN(start, end);
      </chain>
      <nodes>
          <node id="start" class="com.aiplatform.flow.nodes.StartNode"/>
          <node id="end" class="com.aiplatform.flow.nodes.EndNode"/>
      </nodes>
  </flow>
  ```
- 启动时扫描 `@LiteflowComponent` 注解,把节点注册到 `FlowBus` 才会成功

### 15.3 MyBatis Plus 配置
- **删除全局 `insert-strategy: not_null`**(Sprint 2 修复)
- 默认所有字段都写入,避免 unique 索引冲突

### 15.4 Agent 并行工作流(2026-06-09 验证有效)
1. **API 契约先行**(`docs/api/sprint{N}-contracts.md`),锁定边界
2. **创建 N 个 feature 分支**:`git checkout -b feature/sprint{N}-{a/b/c}`
3. **3 Agent 并行**:每个 Agent 在自己分支上工作,严格遵守契约边界
4. **主线程联调**:`git merge --no-ff` 三个分支 → 编译 → 启动 → 跑 E2E
5. **回归测试通过**才打 tag

### 15.5 踩坑检查清单(每次 Agent 提交前必跑)
- [ ] 文件是否带 BOM?`Get-Content` 是否污染过?
- [ ] `mvn -o compile` 是否 BUILD SUCCESS?
- [ ] 启动后端是否正常?端口 8080 是否起来?
- [ ] E2E 脚本是否 100% 通过?
- [ ] 是否有未提交的临时文件?`git status --short` 看 untracked
