# AI-Platform 项目记忆

> **目的**：本文件是 AI-Platform 项目的核心记忆库,用于在上下文压缩或新会话开始时恢复项目状态。
> **维护原则**：每次重大决策后必须更新本文件。
> **最后更新**: 2026-06-08

---

## 0. 快速恢复指引

如果上下文丢失,先读这 5 个文件恢复项目全貌:
1. `D:\Projet\AI-Platform\.memory\CLAUDE.md` (本文件) - 项目记忆
2. `D:\Projet\AI-Platform\README.md` - 项目概览
3. `D:\Projet\AI-Platform\docs\PLAN.md` - 开发计划
4. `D:\Projet\AI-Platform\docs\SOW.md` - 工作说明书
5. `D:\Projet\AI-Platform\docs\DECISIONS.md` - 决策记录

### 0.1 用户偏好

- **语言**:所有回复使用**中文**(用户明确指定,2026-06-08)
- **回复风格**:简洁、直接、避免冗长
- **模式**:当前为 build 模式(可写文件、跑命令)

---

## 1. 项目一句话定位

**AI-Platform** 是一个 **以"项目 (Project)"为顶层容器的 AI 流程自动化编排平台**,项目内可创建多个 AI 助手(复用 Flow 机制)和业务流程;通过拖拽式可视化编辑器构建 AI 流程;支持 5 种触发器;提供多成员协作 4 级权限;部署仅需 JDK + MariaDB + Nginx。

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

### 2.3 不引入的依赖

- ❌ Redis (用 Caffeine 替代)
- ❌ PostgreSQL (用 MariaDB + Hnswlib 替代)
- ❌ pgvector (用 Hnswlib 嵌入式替代)
- ❌ 微服务 (单体部署)
- ❌ 多租户

---

## 3. 技术栈(已确认)

### 3.1 后端

| 类别 | 选型 | 版本 |
|---|---|---|
| 语言 | Java | 21 LTS |
| 框架 | Spring Boot | 3.5.5 |
| 持久层 | MyBatis Plus | 3.5.9 |
| 数据库 | MariaDB | 12.2 |
| 缓存 | Caffeine | 3.1.8 |
| 向量库 | Hnswlib (jelmerk) | 0.7.1 |
| 权限 | Apache Shiro + JWT | 2.0.2 |
| AI | LangChain4j | 1.9.1 |
| 流程 | LiteFlow | 2.15.0 |
| 文档 | Apache Tika | 3.2.3 |
| 限流 | Bucket4j | 8.10.x |

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

---

## 11. 待办与开放问题

### 11.1 待办

- [ ] Hnswlib 集成 PoC (Sprint 0)
- [ ] LangChain4j 多厂商适配
- [ ] 12 个节点的具体配置表单设计
- [ ] 系统工具集(助手调用)的最终清单
- [ ] 部署环境检查(目标 Linux 服务器)

### 11.2 开放问题

- 暂无重大开放问题,主要决策已敲定

---

## 12. CI/CD 与版本控制(2026-06-08 已搭建)

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
