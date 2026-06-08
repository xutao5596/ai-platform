# AI-Platform

> Enterprise AI Workflow Automation Platform

## 🌟 What is AI-Platform?

AI-Platform is a project-centric AI workflow automation platform with:

- 🏢 **Workspace Projects** - Multi-member collaboration
- 🔄 **Visual Flow Editor** - Drag-and-drop AI workflow
- 🤖 **AI Assistants** - Reuse Flow mechanism, full capabilities
- 📚 **RAG Knowledge Base** - Document upload + vector search
- 🔌 **Multi-Model Support** - OpenAI, DeepSeek, Claude, etc.
- ⚡ **5 Trigger Types** - Manual / Cron / Webhook / Event / Chained
- 🎨 **Multi-Theme** - 3 colors + light/dark mode
- 🚀 **Minimal Deployment** - JDK + MariaDB + Nginx only

## 🚀 Quick Start

### Prerequisites

- JDK 21 LTS
- Maven 3.9+
- Node.js 24.x
- MariaDB 12.2
- Nginx 1.24+ (production)

### Local Development

```bash
# Backend
cd ai-backend
mvn clean install -DskipTests
java -jar ai-start/target/ai-start-1.0.0.jar

# Frontend (in another terminal)
cd ai-frontend
npm install --legacy-peer-deps
npm run dev
```

Visit:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

### Production Deployment

See [docs/DEPLOY.md](docs/DEPLOY.md) for full deployment guide.

Quick deploy:
```bash
# 1. Build
mvn -B clean package -DskipTests   # backend
npm run build                       # frontend

# 2. Package
tar -czf ai-platform-v1.0.0.tar.gz backend/ frontend/

# 3. Deploy to server
scp ai-platform-v1.0.0.tar.gz user@server:/tmp/
ssh user@server "cd /tmp && tar -xzf ai-platform-v1.0.0.tar.gz && sudo /opt/ai-platform/scripts/deploy.sh production"
```

## 📚 Documentation

- [📋 SOW - Statement of Work](docs/SOW.md)
- [📅 Development Plan](docs/PLAN.md)
- [🏗️ Architecture](docs/ARCHITECTURE.md)
- [📐 Decisions (ADR)](docs/DECISIONS.md)
- [🎨 UI Design](docs/UI-DESIGN.md)
- [🌿 Git Workflow](docs/GIT-WORKFLOW.md)

## 🛠️ Tech Stack

**Backend**: Spring Boot 3.5 · Java 21 · MyBatis Plus · Shiro + JWT · LangChain4j · LiteFlow · Caffeine · Hnswlib · Apache Tika

**Frontend**: Vue 3 · Vite · TypeScript · Element Plus · Pinia · LogicFlow · ECharts

**Infrastructure**: MariaDB 12.2 · Nginx (no Redis, no PostgreSQL)

## 🗂️ Project Structure

```
AI-Platform/
├── ai-backend/          # Spring Boot backend
├── ai-frontend/         # Vue 3 frontend
├── ai-deploy/           # Deployment scripts & config
├── docs/                # Project documentation
├── .github/             # GitHub Actions
├── .gitlab-ci.yml       # GitLab CI alternative
├── .memory/             # Project memory for AI assistant
└── README.md
```

## 📊 Project Status

| Item | Status |
|---|---|
| **Version** | 1.0.0 |
| **Phase** | 🟡 Pre-Sprint 0 |
| **Started** | - |
| **ETA** | TBD |

## 🔄 CI/CD

- **GitHub Actions**: `.github/workflows/`
  - `ci.yml` - Lint, test, build on every push
  - `cd.yml` - Deploy to staging/production
  - `codeql.yml` - Security scanning
  - `pr-check.yml` - PR validation
- **GitLab CI**: `.gitlab-ci.yml` (alternative)

## 📜 License

Internal project

## 👥 Team

| Role | Member |
|---|---|
| Tech Lead | TBD |
| Backend | TBD |
| Frontend | TBD |
| QA/DevOps | TBD |

## 🤝 Contributing

1. Create a feature branch: `git checkout -b feature/AI-XXX-description`
2. Commit using Conventional Commits: `feat(scope): description`
3. Push and create a PR to `develop`
4. Wait for code review
5. Merge after approval

See [GIT-WORKFLOW.md](docs/GIT-WORKFLOW.md) for details.
