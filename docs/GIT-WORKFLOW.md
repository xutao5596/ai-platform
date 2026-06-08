# Git Workflow - AI-Platform

> **参考**: [Conventional Branch](https://conventional-branch.github.io/) + Git Flow

---

## 1. 分支策略

### 1.1 主分支

| 分支 | 用途 | 保护 | 部署 |
|---|---|---|---|
| `main` | 生产代码 | ✅ 受保护 | 自动部署到生产 |
| `develop` | 开发集成 | ✅ 受保护 | 自动部署到测试环境 |

### 1.2 辅助分支

| 分支 | 命名规范 | 来源 | 合并目标 |
|---|---|---|---|
| 功能分支 | `feature/*` 或 `feat/*` | develop | develop |
| 修复分支 | `fix/*` 或 `hotfix/*` | develop 或 main | develop 或 main |
| 发布分支 | `release/*` | develop | main + develop |
| 重构分支 | `refactor/*` | develop | develop |
| 文档分支 | `docs/*` | develop | develop |
| 实验分支 | `experiment/*` 或 `spike/*` | develop | develop (可丢弃) |

### 1.3 分支示例

```bash
# 命名格式
<type>/<ticket-id>-<short-description>

# 示例
feature/AI-101-flow-editor
feature/AI-102-assistant-dialog
fix/AI-203-sse-streaming-error
fix/AI-205-memory-leak
hotfix/AI-301-critical-auth-bug
refactor/AI-401-extract-flow-node-spi
docs/AI-501-update-readme
release/v1.0.0
```

---

## 2. 提交规范 (Conventional Commits)

### 2.1 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 2.2 Type 类型

| Type | 用途 | 示例 |
|---|---|---|
| `feat` | 新功能 | feat(flow): add 12th node type |
| `fix` | 修复 bug | fix(auth): JWT token expiration issue |
| `docs` | 仅文档 | docs(readme): update installation |
| `style` | 代码格式(不影响逻辑) | style(backend): fix indentation |
| `refactor` | 重构(非新功能/非 bug) | refactor(node): extract base class |
| `perf` | 性能优化 | perf(db): add index to ai_flow |
| `test` | 测试相关 | test(flow): add unit tests |
| `chore` | 构建/工具/依赖 | chore(deps): bump langchain4j to 1.9.2 |
| `build` | 构建系统 | build(docker): multi-stage build |
| `ci` | CI 配置 | ci(github): add cache for maven |
| `revert` | 回滚 | revert: feat(flow) 节点设计器 |

### 2.3 Scope 范围

| Scope | 说明 |
|---|---|
| `backend` | 后端通用 |
| `frontend` | 前端通用 |
| `flow` | 流程模块 |
| `assistant` | 助手模块 |
| `knowledge` | 知识库 |
| `model` | 大模型 |
| `mcp` | MCP |
| `prompt` | 提示词 |
| `auth` | 认证 |
| `ui` | UI 设计 |
| `docs` | 文档 |
| `ci` | CI/CD |
| `deps` | 依赖 |

### 2.4 提交示例

```bash
# 简单提交
git commit -m "feat(flow): add LLM node implementation"

# 带 scope 和 body
git commit -m "feat(auth): implement JWT refresh token

- Add refresh token endpoint
- Implement token rotation logic
- Add expiration handling

Closes #123"

# Breaking change
git commit -m "feat(api)!: change /api/v1/flow to /api/v2/flow

BREAKING CHANGE: All flow API endpoints moved from v1 to v2.
Migration guide: https://docs.ai-platform.com/migrate-v1-to-v2"
```

---

## 3. Git 工作流

### 3.1 标准开发流程

```bash
# 1. 拉取最新 develop
git checkout develop
git pull origin develop

# 2. 创建功能分支
git checkout -b feature/AI-101-flow-editor

# 3. 开发 + 提交
git add .
git commit -m "feat(flow): add LogicFlow canvas"

git add .
git commit -m "feat(flow): add 5 node types"

# 4. 推送分支
git push -u origin feature/AI-101-flow-editor

# 5. 创建 PR (target: develop)
# GitHub / GitLab 上操作

# 6. Code Review + 通过

# 7. Squash and merge
# 合并到 develop

# 8. 删除功能分支
git branch -d feature/AI-101-flow-editor
git push origin --delete feature/AI-101-flow-editor
```

### 3.2 Bug 修复流程

#### 开发环境 bug
```bash
git checkout develop
git pull
git checkout -b fix/AI-203-sse-error
# ... fix ...
git commit -m "fix(flow): SSE streaming interrupted after 60s"
git push -u origin fix/AI-203-sse-error
# PR to develop
```

#### 生产紧急 bug
```bash
git checkout main
git pull
git checkout -b hotfix/AI-301-critical-auth-bug
# ... fix ...
git commit -m "hotfix: auth token validation bypass"
git push -u origin hotfix/AI-301-critical-auth-bug
# PR to main (Bypass reviewers if critical)
# After merge: also cherry-pick to develop
```

### 3.3 发布流程

```bash
# 1. 从 develop 创建 release 分支
git checkout develop
git pull
git checkout -b release/v1.0.0

# 2. 修复 release 分支的 bug
# commit: "fix: ..."

# 3. 合并到 main
git checkout main
git merge --no-ff release/v1.0.0
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin main --tags

# 4. 同步到 develop
git checkout develop
git merge --no-ff release/v1.0.0
git push origin develop

# 5. 删除 release 分支
git branch -d release/v1.0.0
```

---

## 4. 提交检查清单 (PR)

每次 PR 必须满足:

- [ ] 提交信息遵循 Conventional Commits
- [ ] 通过 Code Review(至少 1 人)
- [ ] CI 全绿(lint, test, build)
- [ ] 单元测试覆盖率 ≥ 80%(新代码)
- [ ] 关键路径有测试
- [ ] 无新增 linter 警告
- [ ] 无敏感信息(密钥、密码)
- [ ] 文档已更新(若需要)
- [ ] 破坏性变更已记录

---

## 5. 常用 Git 别名

```bash
# 添加到 ~/.gitconfig
[alias]
  st = status
  co = checkout
  br = branch
  ci = commit
  lg = log --oneline --graph --decorate
  last = log -1 HEAD --stat
  amend = commit --amend --no-edit
  unwip = commit --amend -m
  wip = commit -m "wip: $(date +%Y-%m-%dT%H:%M:%S)"
  pof = push origin --force-with-lease
  pofd = push origin --force-with-lease --delete
  co-pr = "!f() { git fetch origin pull/$1/head:pr-$1 && git checkout pr-$1; }; f"
  contributors = shortlog -sn --all
  cleanup = "!git branch --merged | grep -v '\\*\\|main\\|develop' | xargs -n 1 git branch -d"
```

---

## 6. 大文件管理 (LFS)

对于大型二进制文件,使用 Git LFS:

```bash
# 安装 Git LFS
git lfs install

# 跟踪大文件类型
git lfs track "*.bin"
git lfs track "*.hnsw"
git lfs track "data/**/*.pdf"
```

**注意**: Hnswlib 索引文件(`.bin`)和上传文件不应提交到 Git!

---

## 7. Tag 规范

```bash
# 格式: v<MAJOR>.<MINOR>.<PATCH>
v1.0.0
v1.0.1
v1.1.0
v2.0.0

# 创建 tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

| 版本 | 含义 | 何时 |
|---|---|---|
| MAJOR | 重大变更(不兼容 API) | 架构调整 |
| MINOR | 新功能(向后兼容) | Sprint 完成 |
| PATCH | Bug 修复 | Hotfix |

---

## 8. 冲突解决

```bash
# 1. 更新 develop
git checkout develop
git pull

# 2. 切到功能分支,rebase
git checkout feature/AI-101
git rebase develop

# 3. 解决冲突...
git add .
git rebase --continue

# 4. 强制推送
git push --force-with-lease
```

---

## 9. 紧急回滚

```bash
# 1. 找到上一个稳定版本
git log --oneline

# 2. 创建回滚分支
git checkout main
git checkout -b hotfix/rollback-to-v1.0.0

# 3. 回滚到上一个 tag
git revert <bad-commit-sha>
# 或者
git reset --hard v1.0.0

# 4. 强制推送(慎用)
git push --force
```

---

## 10. 附录:Git Cheat Sheet

```bash
# 撤销工作区修改
git checkout -- <file>

# 撤销已 add
git reset HEAD <file>

# 撤销最近一次 commit(保留修改)
git reset --soft HEAD~1

# 修改最近一次 commit
git commit --amend

# 查看所有分支历史
git log --oneline --graph --all

# 暂存当前工作
git stash
git stash pop

# 删除已合并的本地分支
git branch --merged | grep -v 'main\|develop' | xargs git branch -d
```

---

**GIT-WORKFLOW 终**
