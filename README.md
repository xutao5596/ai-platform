# AI-Platform

> **AI 流程自动化编排平台** - 以"项目"为顶层容器,可视化拖拽构建 AI 流程,支持多厂商大模型、RAG 知识库、AI 助手、5 种触发器。

[![Status](https://img.shields.io/badge/status-development-yellow.svg)]()
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)]()

## 项目概览

AI-Platform 是一个企业级 AI 流程自动化平台,核心能力包括:

- 🏢 **项目管理**:Workspace 模式,多成员协作,4 级权限
- 🔄 **流程编排**:拖拽式可视化构建,12 个内置 AI 节点
- 🤖 **AI 助手**:项目内多实例,全能力(对话+工具+流程+数据)
- 📚 **知识库 RAG**:Tika 解析 + Hnswlib 向量检索
- 🔌 **多模型支持**:OpenAI/DeepSeek/Claude/通义/智谱/Ollama
- ⚡ **5 种触发器**:Manual/Cron/Webhook/Event/Chained
- 🎨 **多主题**:3 套主题色 + 明暗双模
- 🚀 **极简部署**:仅需 JDK + MariaDB + Nginx

## 技术栈

**后端**:Spring Boot 3.5 + Java 21 + MyBatis Plus + Shiro + JWT + LangChain4j + LiteFlow + Caffeine + Hnswlib

**前端**:Vue 3 + Vite + TypeScript + Element Plus + Pinia + LogicFlow + ECharts

**数据库**:MariaDB 12.2(无 Redis/无 PostgreSQL)

## 目录结构

```
AI-Platform/
├── .memory/                    # 项目记忆(防止上下文丢失)
│   └── CLAUDE.md               # AI 助手的项目记忆库
│
├── .github/                    # GitHub Actions CI/CD
│   └── workflows/
│       ├── ci.yml              # CI: lint + test + build
│       ├── cd.yml              # CD: deploy to staging/production
│       ├── codeql.yml          # 安全扫描
│       └── pr-check.yml        # PR 验证
│
├── .gitlab-ci.yml              # GitLab CI 替代方案
│
├── docs/                       # 项目文档
│   ├── PLAN.md                 # 开发计划与周期
│   ├── SOW.md                  # 工作说明书 v5
│   ├── ARCHITECTURE.md         # 架构设计
│   ├── DECISIONS.md            # 决策记录 (ADR)
│   ├── UI-DESIGN.md            # UI 设计规范
│   └── GIT-WORKFLOW.md         # Git 分支与提交规范
│
├── ai-backend/                 # 后端 Spring Boot 工程
│   ├── ai-common/              # 公共模块
│   ├── ai-framework/           # 基础框架
│   ├── ai-system/              # 系统管理
│   ├── ai-project/             # 项目域
│   ├── ai-flow/                # 流程引擎 + 节点 + 助手
│   ├── ai-ai/                  # AI 业务(知识库/模型/MCP/提示词)
│   ├── ai-start/               # 启动模块
│   ├── sql/                    # Flyway 迁移脚本
│   └── pom.xml
│
├── ai-frontend/                # 前端 Vue3 工程
│   ├── src/
│   │   ├── api/                # API 定义
│   │   ├── components/         # 公共组件
│   │   ├── layouts/            # 布局
│   │   ├── views/              # 页面
│   │   ├── router/             # 路由
│   │   ├── store/              # Pinia
│   │   └── main.ts
│   ├── package.json
│   └── vite.config.ts
│
├── ai-deploy/                  # 部署包
│   ├── scripts/                # 部署/备份/恢复脚本
│   ├── systemd/                # systemd 服务
│   ├── nginx/                  # Nginx 配置
│   ├── docker/                 # Docker 相关
│   └── README.md
│
├── .gitignore                  # 根级忽略
├── README.md                   # 本文件
└── STATUS.md                   # 项目状态
```

## 快速开始

> 完整文档见 [docs/PLAN.md](docs/PLAN.md)

### 环境要求

| 组件 | 版本 | 状态 |
|---|---|---|
| JDK | 21 LTS | ✅ 已装 |
| Maven | 3.9+ | ✅ 已装 |
| Node.js | 24.x | ✅ 已装 |
| MariaDB | 12.2 | ✅ 已装 |
| Nginx | 1.24+ | 待部署时安装 |

### 启动后端

```bash
cd ai-backend
mvn clean install -DskipTests
java -jar ai-start/target/ai-start-1.0.0.jar
```

### 启动前端

```bash
cd ai-frontend
npm install --legacy-peer-deps
npm run dev
```

### 访问

- 前端:http://localhost:5173
- 后端:http://localhost:8080

## 开发周期

| 阶段 | 周期 | 里程碑 |
|---|---|---|
| Sprint 0 | Week 0 | 团队就绪 |
| Sprint 1 | Week 1-2 | 系统管理 + 项目域 |
| Sprint 2 | Week 3-4 | AI 模型 + 知识库 + 对话 |
| Sprint 3 | Week 5-6 | 流程编辑器 + 助手 + 触发器 |
| Sprint 4 | Week 7-8 | API Key + Webhook + 联调上线 |

**总工期**:8 周  
**团队规模**:6 人  
**总工时**:~104 人天

## 项目状态

🟡 **开发准备中** - Sprint 0 启动前

## 文档导航

- [项目记忆 (AI 助手使用)](.memory/CLAUDE.md)
- [开发计划](docs/PLAN.md)
- [工作说明书 SOW v5](docs/SOW.md)
- [架构设计](docs/ARCHITECTURE.md)
- [决策记录](docs/DECISIONS.md)
- [UI 设计](docs/UI-DESIGN.md)

## 许可证

内部项目
