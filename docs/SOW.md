# 工作说明书 (SOW) v5

> **文档版本**: v5.0 正式版
> **编制日期**: 2026-06-08
> **项目代号**: AI-Platform
> **文档状态**: 待评审定稿

---

## 目录

1. [项目背景与目标](#1-项目背景与目标)
2. [范围定义](#2-范围定义)
3. [技术选型](#3-技术选型)
4. [功能详细说明](#4-功能详细说明)
5. [数据架构](#5-数据架构)
6. [UI/UX 设计](#6-uiux-设计)
7. [项目组织](#7-项目组织)
8. [Sprint 计划](#8-sprint-计划)
9. [里程碑与验收](#9-里程碑与验收)
10. [风险与缓解](#10-风险与缓解)
11. [交付物清单](#11-交付物清单)
12. [变更管理](#12-变更管理)
13. [决策记录 (ADR)](#13-决策记录-adr)
14. [签字确认](#14-签字确认)

---

## 1. 项目背景与目标

### 1.1 背景

现有 Jeecg-AI v3.9.1 综合性平台包含 AI 应用、零代码、积木报表、大屏、Online 表单等众多模块,后端约 70 万行代码,前端约 27 万行代码,技术栈和功能范围超出业务方当前需求。

业务方提出**精简需求**:仅保留 AI 应用平台 + 系统管理两个核心域,并要求:

- 重新开发一套更轻量、更易维护、可独立演进的 AI 流程自动化编排产品
- **以"项目 (Project)"为顶层容器**,所有 AI 流程归属于某个项目
- 流程编排需支持**可视化拖拽 + 节点可自定义**
- 提供**项目级 AI 助手**,可调用项目内的工具、流程和资源
- 部署仅依赖 JDK + MariaDB + Nginx,**无 Redis / 无 PostgreSQL** 外部依赖

### 1.2 项目目标

#### 1.2.1 功能目标

- 完整的 AI 流程自动化编排能力:可视化编辑器 + 5 类触发器 + 子流程 + 自定义节点
- 完整的 AI 助手能力:项目内多实例 + 全能力(对话/工具/流程/数据操作) + 事件驱动
- 完整的企业级系统管理:用户/角色/部门/字典/权限/审计
- 多租户友好的资源隔离(项目级隔离 + 全局共享)

#### 1.2.2 非功能目标

| 指标 | 目标值 |
|---|---|
| 部署依赖 | 仅 JDK + MariaDB + Nginx |
| 单机支持日活 | 1000+ 用户 |
| QPS | 100+ AI 对话 |
| 知识库容量 | 100 万级向量 |
| 单文档支持 | ≤ 200MB |
| 启动时间 | < 30 秒 |
| 包大小 | < 200MB |
| 单元测试覆盖率 | ≥ 50% |

### 1.3 范围外 (Out of Scope)

以下功能**不在本期项目范围**:

- 积木报表 / 大屏设计 / Online 表单(零代码能力)
- 微服务 / 分布式部署
- 多租户 SaaS(数据隔离)
- 移动 App / 移动端 H5 适配
- 第三方系统对接(SAP / 飞书 / 企微,仅支持 Webhook 事件外发)
- 国际化 i18n(仅简体中文 + 英文双语)

---

## 2. 范围定义

### 2.1 业务范围 (In Scope)

#### 2.1.1 P0 - 核心(必须 100% 实现)

| 域 | 模块 | 关键功能 |
|---|---|---|
| **系统管理** | 用户管理 | CRUD、密码重置、角色分配、批量操作、导入导出 |
| | 角色管理 | CRUD、菜单/数据权限分配、成员管理 |
| | 部门管理 | 树形 CRUD、负责人、成员管理、批量导入 |
| | 菜单管理 | 树形 CRUD、按钮权限、图标选择 |
| | 字典管理 | 字典类型 + 字典项 CRUD、缓存 |
| | 操作日志 | 自动记录、按用户/模块/时间筛选、详情查看 |
| | 登录认证 | 账号密码、JWT 颁发/刷新、登出、失败锁定 |
| | 个人中心 | 修改资料、修改密码、头像 |
| **项目域** | 项目管理 | 创建/编辑/归档、编码、图标、描述 |
| | 成员管理 | 邀请/移除/角色变更、4 级权限 |
| | API Key | 创建/轮换/吊销、scope、rate limit |
| | Webhook | 创建/编辑/测试、事件订阅、签名验证、投递日志 |
| **AI 资源** | 大模型管理 | 多厂商 CRUD、连接测试、参数配置 |
| | MCP 服务 | stdio/HTTP-SSE 传输、工具列表、连接测试 |
| | 提示词管理 | CRUD、变量定义、模板、版本管理 |
| **AI 业务** | AI 对话 | 流式输出、Markdown/代码、会话历史、引用 |
| | 知识库 | 创建/上传/解析/分段/检索测试 |
| | 流程编辑器 | 拖拽画布、12 节点、调试、运行、监控、设置 |
| | AI 助手 | 项目内多实例、人设/工具/事件订阅 |
| **触发器** | Manual / Cron / Webhook / Event / Chained | 5 种触发器 |

#### 2.1.2 P1 - 重要(≥ 80% 实现)

| 模块 | 关键功能 |
|---|---|
| 自定义节点 | 用户通过 UI 配置脚本/HTTP/SQL 类型节点 |
| 单步调试 | 节点级输入/输出/耗时查看、错误高亮 |
| 主题切换 | 3 套主题色 + 明暗模式 + 项目级覆盖 |
| 嵌入式助手 | 浮窗 + 全屏 + iframe 嵌入第三方 |
| 运行监控 | 实时 QPS / P95 / 错误率 / 资源使用 |

#### 2.1.3 P2 - 增强(二期)

| 模块 | 关键功能 |
|---|---|
| 图片生成 | 文生图(多厂商) |
| 多模态对话 | 图片/文件输入 |
| 流程模板市场 | 模板分享与复用 |
| 移动端 H5 适配 | 响应式优化 |

---

## 3. 技术选型

### 3.1 后端技术栈

| 类别 | 选型 | 版本 | 说明 |
|---|---|---|---|
| 语言 | Java | 21 (LTS) | 已装 21.0.2 |
| 框架 | Spring Boot | 3.5.5 | 沿用 |
| 持久层 | MyBatis Plus | 3.5.9 | 沿用 |
| 连接池 | Druid | 1.2.24 | 沿用 |
| 数据库 | MariaDB | 12.2 | 已装 |
| 权限 | Apache Shiro | 2.0.2 + JWT | 沿用 |
| 缓存 | Caffeine | 3.1.8 | **替代 Redis** |
| 向量库 | Hnswlib (jelmerk) | 0.7.1 | **替代 pgvector** |
| AI 框架 | LangChain4j | 1.9.1 | 沿用 |
| 流程引擎 | LiteFlow | 2.15.0 | 沿用 |
| 流程图 | LogicFlow | 2.0.x | 前端 |
| 文档解析 | Apache Tika | 3.2.3 | 沿用 |
| 脚本引擎 | Groovy + Aviator + GraalJS | - | 沿用 |
| MCP 协议 | langchain4j-mcp | 1.9.1 | 沿用 |
| API 文档 | springdoc-openapi | 2.6.0 | 沿用 |
| JSON | Jackson | 2.17.x | 沿用 |
| 工具 | Lombok + MapStruct + Hutool | - | 沿用 |
| 限流 | Bucket4j | 8.10.x | **替代 Redis 限流** |
| 邮件 | Spring Mail | - | 通知 |
| 定时任务 | Quartz | 2.3.x | 沿用 |

### 3.2 前端技术栈

| 类别 | 选型 | 版本 | 说明 |
|---|---|---|---|
| 框架 | Vue | 3.5.x | 沿用 |
| 构建 | Vite | 6.x | 沿用 |
| 语言 | TypeScript | 5.x | 沿用 |
| UI 库 | **Element Plus** | 2.8.x | **替换 Ant Design Vue** |
| 状态 | Pinia | 2.1.x | 沿用 |
| 路由 | Vue Router | 4.5.x | 沿用 |
| HTTP | Axios | 1.x | 沿用 |
| 流程图 | @logicflow/core + extension | 2.x | 沿用 |
| Markdown | markdown-it + highlight.js | - | 沿用 |
| 图表 | ECharts | 5.6.x | 沿用 |
| 图标 | Iconify + unplugin-icons | - | 沿用 |
| 代码编辑 | CodeMirror | 5.x | 沿用 |
| SSE | event-source-polyfill | 1.x | 沿用 |

### 3.3 部署架构

```
┌─────────────────────────────────────────────────────────┐
│                  Nginx (80/443)                          │
│          反向代理 + 静态资源 + SSL                        │
└────────────┬────────────────────────────┬───────────────┘
             │                            │
   ┌─────────▼────────┐         ┌─────────▼────────┐
   │  /ai-backend     │         │  /ai-frontend    │
   │  Spring Boot     │         │  静态 dist        │
   │  Port: 8080      │         │                  │
   └─────────┬────────┘         └──────────────────┘
             │
   ┌─────────┼─────────────────────┐
   │         │                     │
   ▼         ▼                     ▼
┌────────┐ ┌──────────────┐ ┌─────────────────┐
│MariaDB │ │Hnswlib 索引  │ │ 文件存储         │
│12.2    │ │/data/hnsw/  │ │/data/upload/    │
│        │ │*.bin         │ │/data/logs/      │
└────────┘ └──────────────┘ └─────────────────┘
```

### 3.4 不引入的外部依赖

- ❌ Redis(用 Caffeine 替代)
- ❌ PostgreSQL(用 MariaDB + Hnswlib 替代)
- ❌ pgvector(用 Hnswlib 嵌入式替代)
- ❌ Elasticsearch(如需全文检索用 MySQL LIKE + Hnswlib 混合)
- ❌ Kafka(事件用 ApplicationEvent 同步分发)
- ❌ 微服务相关组件(Nacos / Sentinel / Seata)

---

## 4. 功能详细说明

### 4.1 项目 (Project) - 顶层容器

**核心定位**:Workspace(协作空间),代表一个业务场景或客户场景

**关键属性**:

- 名称、编码(URL 标识,唯一)、描述、图标
- 状态:active(活跃)/ archived(归档)
- 所有者(Owner,可转移)
- 主题外观(可覆盖全局)

**项目内子资源(项目隔离)**:

- 流程(ai_flow,含助手)
- 知识库(ai_knowledge + ai_knowledge_doc + ai_knowledge_chunk)
- 提示词(ai_prompt)

**全局资源(跨项目共享)**:

- 大模型(ai_model)
- MCP 服务(ai_mcp)

**项目级集成**:

- 成员(4 级角色)
- API Key(含 scope、rate limit)
- Webhook(事件订阅 + 投递)

### 4.2 流程 (Flow) - 项目内的 AI 自动化单元

**两种身份**:

1. **普通流程**:后台运行的 AI 自动化(如:工单分类、报表生成)
2. **AI 助手**(`is_assistant = 1`):可与用户对话的智能体

**流程状态**:

- draft(草稿) - 可编辑
- published(已发布) - 可运行
- disabled(已禁用) - 不接收触发

**核心字段**:

- `design` LONGTEXT - LogicFlow 画布 JSON(节点 + 边)
- `chain` LONGTEXT - LiteFlow 执行链
- `current_version_id` - 当前发布版本

**版本管理**:

- `ai_flow_version` 表存储历史版本
- 每次发布创建新版本(自增 version)
- 支持版本回滚

**5 种触发器**:

| 触发器 | 配置 | 实现 |
|---|---|---|
| Manual | 无 | 用户点击"运行"按钮 |
| Cron | `{ "cron": "0 0 9 * * ?", "input": {...} }` | Quartz 调度 |
| Webhook | `{ "path": "/webhook/xxx", "authType": "signature" }` | Controller + 鉴权 + 投递日志 |
| Event | `{ "eventType": "order.created", "filter": "..." }` | ApplicationEvent |
| Chained | `{ "sourceFlowId": 123 }` | FlowService 内部调用 |

**12 个内置节点(AI 为主)**:

| # | 节点 | 类型 Key | 分类 | 实现 |
|---|---|---|---|---|
| 1 | 开始 | start | 基础 | 接收输入 |
| 2 | 结束 | end | 基础 | 格式化输出 |
| 3 | LLM 调用 | llm | AI | LangChain4j |
| 4 | 知识库检索 | knowledge_search | AI | 向量检索 |
| 5 | 提示词模板 | prompt | AI | 变量渲染 |
| 6 | Agent 决策 | agent | AI | langchain4j agent |
| 7 | 条件分支 | if_else | 控制 | LiteFlow SWITCH |
| 8 | 子流程调用 | subflow | 控制 | 引用其他 ai_flow |
| 9 | HTTP 请求 | http | 工具 | WebClient |
| 10 | MCP 工具 | mcp_tool | 工具 | langchain4j-mcp |
| 11 | 脚本执行 | script | 工具 | GraalJS / Groovy |
| 12 | 变量赋值 | set_var | 数据 | 上下文赋值 |

**节点自定义层级**:

- 第一层:**开发者扩展**(写 Java 类实现 FlowNode 接口 + 前端 Vue 组件)
- 第二层:**用户自定义**(通过 UI 配置脚本/HTTP/SQL 类型节点,无需编码)

### 4.3 AI 助手 (Assistant) - 复用 Flow 机制

**核心**:助手 = 项目内的一个特殊流程(`is_assistant = 1`)

**能力**:

- 对话能力:基于 LLM 多轮对话
- 工具调用:调用项目内的 MCP 工具 / HTTP 节点 / 数据库
- 流程调用:调用项目内的其他流程
- 数据操作:调用系统工具(list_flows、run_flow、search_knowledge 等)
- 知识库:RAG 检索
- 事件响应:订阅项目事件,被动响应

**助手配置**(ai_assistant_config):

- 人设、欢迎语、开场问题、头像
- 能力开关:工具/子流程/数据操作
- 限制:最大上下文轮数、温度、最大 tokens

**事件订阅**(ai_assistant_event_sub):

- 订阅项目事件
- 过滤条件
- 触发响应(提示词模板)
- 通知方式(站内信/邮件/不通知)
- 防循环触发

**系统工具集**(助手可调用):

- `list_flows(projectId)` - 列出流程
- `run_flow(flowId, input)` - 运行流程
- `list_flow_runs(flowId, status, limit)` - 查询运行
- `stop_run(runId)` - 停止运行
- `get_run_log(runId)` - 获取日志
- `list_knowledge_bases(projectId)` - 列出知识库
- `search_knowledge(kbId, query)` - 检索
- `get_flow_detail(flowId)` - 流程详情
- `update_flow_draft(flowId, design)` - 更新草稿(开发者及以上)

**安全**:

- 每个工具调用前校验用户在该项目中的角色
- 只读工具:viewer+
- 写操作:developer+
- 危险操作:owner only
- 用户可关闭 `enable_data_ops` 禁用数据操作

**入口形态**:

- 全局悬浮按钮(始终显示)
- 项目内助手列表
- 嵌入式对话框(380x520)
- 全屏对话页
- iframe 嵌入第三方

### 4.4 知识库 (Knowledge Base)

**核心能力**:

- 多知识库(每项目可建多个)
- 多文档类型支持:PDF / Word / Excel / PPT / Markdown / TXT / HTML
- 文档解析:Apache Tika 3.2.3
- 文本分块:LangChain4j Splitter(通用 / 段落 / 自定义)
- 向量化:可配置 Embedding 模型
- 检索:向量检索(Top-K + 相似度阈值)
- 元数据:源页、Hash、Token 数

**数据流**:

```
上传文件 → Tika 解析 → 文本分块 → Embedding 向量化
                                  ↓
                          Hnswlib 索引文件 (*.bin)
                                  ↓
                          MariaDB 存元数据 + 文本
```

### 4.5 系统管理

**用户管理**:

- 字段:账号、姓名、昵称、头像、性别、手机、邮箱、部门、职位、状态
- 批量操作:启用/禁用/删除/分配角色/重置密码
- 导入/导出
- 关联显示:项目角色

**角色管理**:

- 5 种内置角色:超级管理员 / 管理员 / 开发者 / 普通用户 / 只读用户
- 可视化菜单权限树(el-tree)
- 按钮级权限(user:view / user:edit / ...)
- 数据权限(全部/本部门/本部门及下级/仅本人/自定义)
- 成员管理(穿梭框)

**部门管理**:

- 树形结构(无限层级)
- 拖拽调整层级
- 负责人、子部门、成员管理

**菜单管理**:

- 树形菜单
- 按钮权限控制
- 路由配置
- 图标选择

**字典管理**:

- 字典类型 + 字典项
- 样式(Tag 颜色)
- 缓存(Caffeine)

**操作日志**:

- 全量记录(方法 + URL + 参数 + 响应 + 耗时 + IP + UA)
- 多维度筛选
- 详情展开

---

## 5. 数据架构

### 5.1 表数量:37 张

#### 5.1.1 系统管理(10 张)

```
sys_user              用户
sys_role              角色
sys_user_role         用户-角色
sys_menu              菜单
sys_role_menu         角色-菜单
sys_role_permission   角色-权限(按钮级)
sys_dept              部门
sys_dict              字典类型
sys_dict_item         字典项
sys_log               操作日志
```

#### 5.1.2 项目域(6 张)

```
ai_project            项目
ai_project_member     项目成员
ai_project_api_key    项目 API Key
ai_project_webhook    项目 Webhook
ai_project_webhook_log  Webhook 投递日志
ai_file               项目文件
```

#### 5.1.3 流程域(6 张)

```
ai_flow               流程(含助手)
ai_flow_version       流程版本
ai_flow_trigger       流程触发器
ai_flow_run           流程运行
ai_flow_run_step      运行步骤
ai_custom_node        自定义节点
```

#### 5.1.4 AI 资源-项目隔离(5 张)

```
ai_knowledge          知识库
ai_knowledge_doc      知识库文档
ai_knowledge_chunk    知识库分片
ai_prompt             提示词
ai_prompt_version     提示词版本
```

#### 5.1.5 AI 资源-全局共享(2 张)

```
ai_model              大模型
ai_mcp                MCP 服务
```

#### 5.1.6 AI 助手(4 张)

```
ai_assistant_config       助手配置
ai_assistant_event_sub    助手事件订阅
ai_assistant_session      助手会话
ai_assistant_message      助手消息
```

#### 5.1.7 AI 对话(2 张)

```
ai_chat_session       对话会话(与助手并行存在的通用对话)
ai_chat_message       对话消息
```

#### 5.1.8 通用(2 张)

```
sys_quartz_job        定时任务
sys_notice            系统通知
```

### 5.2 ER 核心关系

```
sys_user ─┬─< sys_user_role >─ sys_role ─┬─< sys_role_menu >─ sys_menu
          │                              │
          │                              └─< sys_role_permission
          │
          └─< ai_project_member >─ ai_project ─┬─< ai_flow (含助手)
                                               │     │
                                               │     ├─< ai_flow_version
                                               │     ├─< ai_flow_trigger
                                               │     └─< ai_flow_run >─ ai_flow_run_step
                                               │
                                               ├─< ai_knowledge ─< ai_knowledge_doc ─< ai_knowledge_chunk
                                               ├─< ai_prompt
                                               ├─< ai_project_member
                                               ├─< ai_project_api_key
                                               └─< ai_project_webhook >─ ai_project_webhook_log

ai_flow (is_assistant=1) ─┬─< ai_assistant_config
                          ├─< ai_assistant_event_sub
                          ├─< ai_assistant_session >─ ai_assistant_message
                          └─ 同 ai_flow 通用关系

ai_model (全局)
ai_mcp   (全局)
```

### 5.3 Hnswlib 存储

```
/data/hnsw/
├── knowledge_001.bin   # 知识库 1 的向量索引
├── knowledge_002.bin   # 知识库 2 的向量索引
└── ...

每个 *.bin 文件:
- 存储:向量 + ID
- 元数据存 MariaDB:ai_knowledge_chunk.vector_id → Hnswlib 内部 ID
```

---

## 6. UI/UX 设计

### 6.1 设计原则

| 原则 | 说明 |
|---|---|
| 专业感 | 参考企业级产品(飞书 / 钉钉 / Notion / Linear) |
| 简洁 | 中后台风格,大量留白,主次分明 |
| 高效 | 关键操作 ≤ 2 步触达 |
| 可定制 | 3 套主题色 + 明暗 + 项目级覆盖 |
| 响应式 | 适配 1280-1920 主流分辨率 |

### 6.2 主题方案

**3 套主题色**:

- 商务蓝:#409EFF(Element Plus 默认)
- 智能紫:#6366F1
- 科技绿:#10B981

**明暗双模**:

- 浅色:#FFFFFF 背景,#303133 文字
- 深色:#1F2937 背景,#F3F4F6 文字

**强调色**:8 色可选

**主题切换**:

- 顶栏 [🎨] 按钮一键切换
- 全局偏好 + 项目级覆盖(DB 存储 + localStorage 缓存)

### 6.3 核心界面(22 个已设计)

详见 [UI-DESIGN.md](UI-DESIGN.md)

| 域 | 界面 | 状态 |
|---|---|---|
| 公共 | 登录、工作台(双栏仪表盘)、主题切换 | ✅ |
| 项目 | 列表、详情、成员、API Key、Webhook、设置 | ✅ |
| 流程 | 列表、编辑器(5 页签:设计/调试/运行/监控/设置) | ✅ |
| 知识库 | 列表、文档列表、分段预览、检索测试 | ✅ |
| 助手 | 对话浮窗、全屏对话、编辑器、事件订阅 | ✅ |
| AI 资源 | 模型管理、MCP 服务、提示词详情/编辑器 | ✅ |
| 自定义节点 | 自定义节点设计器 | ✅ |
| 监控 | 全局监控、运行详情 | ✅ |
| 系统 | 用户、角色、部门、字典、菜单、操作日志 | ✅ |

### 6.4 导航 (用户最终决定 2026-06-08)

**侧边栏 200px 左侧**,6 个一级菜单:

1. 📊 工作台
2. 📁 我的项目
3. 🤖 AI 资源
4. 📊 监控
5. ⚙️ 系统管理
6. 💬 助手(带数字角标)

> 中途考虑过顶部导航 + 标签页(Notion 风格),后用户明确选择**维持原侧边栏**。

### 6.5 关键交互

| 交互 | 实现 |
|---|---|
| 全局搜索 | Cmd/Ctrl + K |
| 助手浮窗 | 右下角悬浮 + 角标 + 通知 |
| 流程拖拽 | LogicFlow DndPanel |
| 变量映射 | `{{nodeId.field}}` 语法 + 弹窗选择器 |
| 实时调试 | SSE 流式 + 节点高亮 |
| 危险确认 | 输入名称二次确认 |
| 空状态 | 友好插画 + 操作引导 |

---

## 7. 项目组织

### 7.1 团队配置(6 人)

| 角色 | 人数 | 主要职责 |
|---|---|---|
| 后端架构师 | 1 | 基础框架、AI 引擎、流程引擎、助手核心 |
| 后端开发 A | 1 | 系统管理、项目域、权限 |
| 后端开发 B | 1 | AI 业务(知识库/提示词)、流程节点、助手工具、事件订阅 |
| 前端主程 | 1 | 项目脚手架、核心组件、AI 对话、流程编辑器、助手对话框 |
| 前端开发 | 1 | 系统管理、AI 管理页面、助手配置、其他页面 |
| 测试 / DevOps | 0.5 | 联调、压测、部署、文档 |

### 7.2 沟通机制

- **每日站会**:9:30,15 分钟,同步进度 + 阻塞
- **周会**:周一上午 1 小时,review + 计划
- **Sprint 评审**:每 Sprint 结束,Demo + 回顾
- **即时沟通**:企业微信群 + 文档协作文档

### 7.3 代码管理

- **Git Flow**:`main` / `develop` / `feature/*` / `hotfix/*`
- **Commit 规范**:Conventional Commits(feat/fix/docs/style/refactor/test/chore)
- **Code Review**:MR 必须经 1+ 人 review
- **CI/CD**:GitLab CI(编译 + 单元测试 + 打包)

---

## 8. Sprint 计划

### 8.1 总览

| Sprint | 周次 | 主题 | 工时 |
|---|---|---|---|
| Sprint 0 | Week 0(启动前) | 培训 + 技术预研 | 5 人天 |
| Sprint 1 | Week 1-2 | 脚手架 + 系统管理 + 项目域 | 22 人天 |
| Sprint 2 | Week 3-4 | AI 模型 + 知识库 + AI 对话 | 25 人天 |
| Sprint 3 | Week 5-6 | 流程编辑器 + 助手 + 触发器 | 30 人天 |
| Sprint 4 | Week 7-8 | API Key + Webhook + 联调上线 | 22 人天 |
| **合计** | **8 周** | | **104 人天** |

### 8.2 Sprint 0(Week 0,5 人天)

| 任务 | 负责人 | 工时 |
|---|---|---|
| Hnswlib 集成 PoC | 后端架构师 | 2d |
| LangChain4j 培训 | 后端架构师 | 1d |
| Element Plus + LogicFlow 培训 | 前端主程 | 1d |
| 项目仓库初始化 + CI 配置 | DevOps | 0.5d |
| 数据库 ER 图定稿 | 后端架构师 | 0.5d |

### 8.3 Sprint 1(22 人天)

**目标**:可登录、可管理用户/角色/部门/字典,可创建项目和邀请成员

| 任务 | 负责人 | 工时 |
|---|---|---|
| 后端:Spring Boot 3.5 多模块工程 | 后端架构师 | 1.5d |
| 后端:MyBatis Plus + Druid + Caffeine + Shiro+JWT | 后端架构师 | 2d |
| 后端:sys_* 系统管理 10 张表 CRUD | 后端A | 5d |
| 后端:ai_project + member + 角色权限 | 后端A | 3d |
| 后端:项目拦截器 + 角色注解 | 后端A | 1d |
| 前端:Vue3 + Vite + Element Plus 脚手架 | 前端主程 | 1.5d |
| 前端:路由/Pinia/Axios + 登录/主布局 | 前端主程 | 2.5d |
| 前端:系统管理 10 个页面 | 前端开发 | 4d |
| 前端:项目列表 + 详情 + 创建/编辑 | 前端主程 | 2.5d |
| 前端:项目成员管理 | 前端开发 | 1.5d |
| 联调 | 全员 | 2d |

### 8.4 Sprint 2(25 人天)

**目标**:可与多厂商模型对话、上传文档、检索、引用知识库、管理提示词

| 任务 | 负责人 | 工时 |
|---|---|---|
| 后端:LangChain4j 集成 + 多厂商适配 | 后端架构师 | 4d |
| 后端:ai_model CRUD + 连接测试 | 后端B | 1.5d |
| 后端:Hnswlib 集成 + Embedding | 后端B | 2d |
| 后端:ai_knowledge + doc + chunk CRUD | 后端B | 3d |
| 后端:Apache Tika 文档解析 | 后端B | 1.5d |
| 后端:ai_prompt CRUD + 版本 | 后端B | 1d |
| 后端:AI 对话 SSE 流式 + 会话/消息 | 后端架构师 | 4d |
| 前端:模型管理(CRUD + 配置) | 前端开发 | 3d |
| 前端:知识库管理(CRUD + 文档 + 分段预览) | 前端开发 | 4d |
| 前端:提示词管理 | 前端开发 | 1.5d |
| 前端:AI 对话页面 | 前端主程 | 4d |
| 联调 | 全员 | 3d |

### 8.5 Sprint 3(30 人天)

**目标**:可视化拖拽流程编辑器、内置 12 节点、自定义节点、助手基础、5 触发器

| 任务 | 负责人 | 工时 |
|---|---|---|
| 后端:FlowNode SPI + NodeRegistry | 后端架构师 | 1.5d |
| 后端:design → chain 转换器 | 后端架构师 | 2d |
| 后端:FlowExecutor + 运行/步骤记录 | 后端B | 2d |
| 后端:12 个内置节点实现 | 后端B | 6d |
| 后端:自定义节点(脚本/HTTP/SQL) | 后端B | 2d |
| 后端:Assistant 配置 + 会话 + 消息 | 后端架构师 | 2d |
| 后端:Assistant Tool Registry | 后端B | 2.5d |
| 后端:事件总线 + 事件订阅处理 | 后端B | 2d |
| 后端:5 种触发器实现(Manual/Cron/Webhook/Event/Chained) | 后端B | 4d |
| 前端:FlowEditor 三栏布局 + LogicFlow | 前端主程 | 3d |
| 前端:节点面板 + 通用属性面板 | 前端主程 | 2d |
| 前端:12 个节点 ConfigForm + View | 前端开发 | 7d |
| 前端:自定义节点配置 UI | 前端开发 | 1.5d |
| 前端:单步调试 + 实时日志 | 前端主程 | 2d |
| 前端:嵌入式助手对话框 | 前端主程 | 3d |
| 前端:助手配置 UI(人设/工具/事件订阅) | 前端开发 | 2d |
| 前端:触发器配置 UI | 前端开发 | 1.5d |
| 联调 | 全员 | 3d |

### 8.6 Sprint 4(22 人天)

**目标**:项目 API Key、Webhook、运行监控、上线准备

| 任务 | 负责人 | 工时 |
|---|---|---|
| 后端:项目 API Key 生成/轮换/吊销 | 后端架构师 | 1.5d |
| 后端:API Key 鉴权拦截器 | 后端架构师 | 1d |
| 后端:项目 Webhook 管理 + 事件订阅 | 后端B | 1.5d |
| 后端:Webhook 投递 + 重试 + 签名 | 后端B | 1.5d |
| 后端:运行监控统计 + 审计 | 后端B | 1.5d |
| 前端:API Key 管理 UI | 前端开发 | 1d |
| 前端:Webhook 管理 UI + 投递日志 | 前端开发 | 1.5d |
| 前端:运行监控 + 审计日志 | 前端开发 | 1.5d |
| 前端:助手嵌入 iframe | 前端开发 | 1d |
| 前端:主题切换(3 色 + 明暗) | 前端主程 | 2d |
| 性能压测 + 修复 | 测试 | 1.5d |
| 部署文档 + 启动脚本 | DevOps | 0.5d |
| 用户手册 + API 文档 | 架构师 | 0.5d |
| 联调 | 全员 | 1.5d |

---

## 9. 里程碑与验收

### 9.1 关键里程碑

| 里程碑 | 时间 | 验收标准 |
|---|---|---|
| **M0** 项目启动 | Week 0 | 团队就绪、PoC 通过、ER 图定稿 |
| **M1** 基础就绪 | Week 2 | 可登录、用户/角色/部门/字典 CRUD 可用、项目可创建 |
| **M2** AI 对话可用 | Week 4 | 可与 5+ 厂商模型对话、可上传文档检索 |
| **M3** 流程+助手 | Week 6 | 拖拽编辑器、12 节点、助手可对话和调用工具 |
| **M4** 完整功能 | Week 8 | API Key + Webhook + 监控 + 部署就绪 |

### 9.2 验收标准

#### 9.2.1 功能验收

- ✅ P0 功能 100% 通过
- ✅ P1 功能 ≥ 80% 通过
- ✅ 所有 P0 路径有测试覆盖

#### 9.2.2 性能验收

- ✅ 单实例 100 QPS 持续 1 小时无错误
- ✅ AI 对话首字响应 ≤ 3 秒
- ✅ 知识库检索 P95 ≤ 500ms
- ✅ 启动时间 ≤ 30 秒
- ✅ 包大小 ≤ 200MB

#### 9.2.3 部署验收

- ✅ 可在空白 Linux 虚拟机按文档 30 分钟内完成部署
- ✅ 仅需 JDK + MariaDB + Nginx
- ✅ 提供 docker-compose 可选方案

#### 9.2.4 代码验收

- ✅ 单元测试覆盖率 ≥ 50%
- ✅ 关键路径(对话/检索/权限/流程执行)有测试
- ✅ 符合《阿里巴巴 Java 开发手册》
- ✅ 无明显代码异味(重复率 < 5%)

#### 9.2.5 文档验收

- ✅ README.md
- ✅ 架构设计文档
- ✅ 数据库设计文档
- ✅ API 接口文档(自动生成)
- ✅ 部署手册
- ✅ 用户手册

---

## 10. 风险与缓解

| # | 风险 | 等级 | 影响 | 缓解措施 |
|---|---|---|---|---|
| 1 | Hnswlib 集成复杂度 | 中 | 进度 | Sprint 0 PoC 验证,技术调研 |
| 2 | Element Plus 与 AI 场景适配 | 低 | 体验 | 二次封装,参考 Dify |
| 3 | LLM API 限流 | 中 | 稳定性 | Bucket4j 限流 + 多 Key 轮询 + 重试 |
| 4 | 大文档解析 OOM | 中 | 稳定性 | 流式解析 + 限制 200MB |
| 5 | 单机部署容量上限 | 中 | 扩展性 | 设计 Hnswlib → Milvus 升级路径 |
| 6 | 后端 B 对项目不熟 | 高 | 进度 | Sprint 1-2 跟岗 + pair programming |
| 7 | LogicFlow 复杂节点开发 | 中 | 进度 | 参考 jeecgboot-vue3 现有实现 |
| 8 | LiteFlow Chain 转换 bug | 中 | 进度 | Sprint 0 预研 + 单元测试 |
| 9 | 12 节点工作量超预期 | 中 | 范围 | 优先 P0 节点,次要可简化 |
| 10 | 节点 UI 组件工作量超预期 | 中 | 范围 | 优先保证 P0,次要可 MVP 简化 |
| 11 | 事件订阅风暴 | 高 | 稳定性 | 类型去重 + 链路追踪 + 死循环检测 |
| 12 | 工具权限控制疏漏 | 高 | 安全 | 统一权限注解 + 集成测试 |
| 13 | 助手 Function Calling 不稳定 | 中 | 体验 | 选择稳定的模型(GPT-4、DeepSeek-V3) |
| 14 | 嵌入式对话框兼容性 | 中 | 体验 | Teleport + Shadow DOM 隔离 |
| 15 | 暗色模式适配工作量大 | 中 | 体验 | 优先保证主流程页面,组件逐步覆盖 |

---

## 11. 交付物清单

### 11.1 代码仓库

```
ai-platform/
├── ai-backend/             后端 Spring Boot 工程
│   ├── ai-common/          公共模块
│   ├── ai-framework/       基础框架(Shiro/JWT/Caffeine/Hnswlib)
│   ├── ai-system/          系统管理
│   ├── ai-project/         项目域
│   ├── ai-flow/            流程引擎 + 节点 + 助手
│   ├── ai-ai/              AI 业务(知识库/模型/MCP/提示词)
│   ├── ai-start/           启动模块
│   ├── sql/                Flyway 迁移
│   └── pom.xml
│
├── ai-frontend/            前端 Vue3 工程
│   ├── src/
│   │   ├── api/            API 定义
│   │   ├── components/     公共组件
│   │   ├── views/          页面(22+ 界面)
│   │   ├── router/         路由
│   │   ├── store/          Pinia
│   │   ├── utils/          工具
│   │   ├── layouts/        布局
│   │   └── main.ts
│   ├── package.json
│   └── vite.config.ts
│
├── ai-deploy/              部署
│   ├── nginx/              Nginx 配置
│   ├── scripts/            启动/停止脚本
│   ├── docker/             Dockerfile + docker-compose
│   └── README.md
│
├── docs/                   文档
│   ├── architecture.md
│   ├── database.md
│   ├── api.md
│   ├── deploy.md
│   └── user-manual.md
│
└── README.md
```

### 11.2 部署包

- `ai-platform-backend-{version}.jar`(≤ 200MB)
- `ai-platform-frontend-{version}.tar.gz`(dist 静态资源)
- `ai-platform-deploy-{version}.tar.gz`(Nginx 配置 + 启动脚本 + SQL)

### 11.3 文档

- 架构设计文档
- 数据库设计文档(ER 图 + DDL)
- API 接口文档(OpenAPI 3.0 + Swagger UI)
- 部署手册(Linux / Windows / Docker)
- 用户手册(含截图)
- 开发文档(贡献指南)

### 11.4 测试用例

- 单元测试:关键 Service/Utils
- 集成测试:API 端到端
- E2E 测试(可选):关键用户路径

---

## 12. 变更管理

### 12.1 变更流程

1. 提交变更申请(含范围、工期、成本影响)
2. 项目负责人评审
3. 涉及 ≥ 3 人天或影响核心功能需经 3 人评审
4. 形成书面变更单 + 附录更新
5. 更新本 SOW

### 12.2 范围冻结

- Sprint 0 结束前:可调整技术选型
- Sprint 1 开始:功能范围冻结(仅 P0)
- Sprint 2 开始:P1 范围冻结
- Sprint 3 开始:所有范围冻结

---

## 13. 决策记录 (ADR)

详见 [DECISIONS.md](DECISIONS.md)

| 决策 | 备选 | 选择 | 理由 |
|---|---|---|---|
| 后端框架 | Spring Boot / Spring Cloud / Quarkus | Spring Boot 3.5 | 团队熟悉、AI 生态成熟 |
| 前端 UI 库 | Element Plus / Ant Design Vue / Naive UI | Element Plus | 用户指定,组件丰富 |
| AI 框架 | LangChain4j / Spring AI / 自研 | LangChain4j | 多厂商适配成熟、MCP 完整 |
| 向量库 | pgvector / Milvus / Hnswlib | Hnswlib | 零外部依赖、性能好 |
| 缓存 | Redis / Caffeine / Hazelcast | Caffeine | 零外部依赖、足够用 |
| 流程引擎 | LiteFlow / Flowable / 自研 | LiteFlow + LogicFlow | 沿用、轻量、JS 引擎丰富 |
| 数据库 | MySQL / MariaDB / PostgreSQL | MariaDB | 用户指定、已装 |
| 项目顶层 | ai_app / ai_project | ai_project | 用户指定 |
| 多租户 | 完整 / 简化 / 无 | 无 | 用户指定 |
| 助手定位 | 独立 / 项目内 / 平台级 | 项目内 | 用户指定 |
| UI 导航 | 侧边栏 / 顶部 | 侧边栏 | 用户最终决定(2026-06-08) |

---

## 14. 签字确认

| 角色 | 姓名 | 日期 | 签字 |
|---|---|---|---|
| 需求方 | | | |
| 技术负责人 | | | |
| 项目经理 | | | |
| 产品负责人 | | | |

---

## 附录 A:核心指标对比

| 指标 | 原 Jeecg-AI | 新 AI-Platform | 比例 |
|---|---|---|---|
| 后端代码 | 70 万+ 行 | ~4.5 万 行 | 6% |
| 前端代码 | 27 万 行 | ~3.5 万 行 | 13% |
| 数据库表 | 125 张 | 37 张 | 30% |
| 外部依赖 | Redis/PG/Mongo/ES | 无 | 0 |
| 启动包大小 | ~500MB | ≤ 200MB | 40% |
| 启动时间 | ~60s | < 30s | 50% |

## 附录 B:与原 Jeecg-AI 功能对比

| 原功能 | 保留情况 | 说明 |
|---|---|---|
| AI 应用平台 | ✅ 保留 + 增强 | 改为项目下流程 |
| 知识库 | ✅ 保留 | 项目隔离 |
| 流程编排 | ✅ 增强 | 节点可自定义 |
| 模型管理 | ✅ 保留 | 改为全局共享 |
| 提示词 | ✅ 保留 | 项目隔离 |
| MCP 插件 | ✅ 保留 | 改为全局共享 |
| AI 助手 | ✅ 新增 | 项目内多实例 |
| 系统管理 | ✅ 简化 | 保留核心 10 模块 |
| Online 表单 | ❌ 移除 | 不在范围 |
| 积木报表 | ❌ 移除 | 不在范围 |
| 大屏设计 | ❌ 移除 | 不在范围 |
| 示例代码 | ❌ 移除 | 不在范围 |
| 微服务 | ❌ 移除 | 单体部署 |
| 多租户 | ❌ 移除 | 用户指定 |
| CMS / Chat2BI | ❌ 移除 | 二期考虑 |

## 附录 C:Sprint 0 启动前检查清单

- [ ] 团队成员就位(6 人)
- [ ] JDK 21、Maven 3.9、Node 24、MariaDB 12.2 已装
- [ ] Hnswlib 集成 PoC 通过
- [ ] LangChain4j 培训完成
- [ ] Element Plus + LogicFlow 培训完成
- [ ] 项目仓库(Git)已创建
- [ ] CI/CD 基础配置就绪
- [ ] 数据库 ER 图定稿
- [ ] API 规范初稿完成
- [ ] UI 设计稿评审通过
- [ ] 部署目标环境已就绪(至少 1 台 Linux 虚拟机)

---

**SOW v5 终**
