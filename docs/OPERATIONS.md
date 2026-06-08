# Git 仓库操作手册 (OPERATIONS)

> **目的**:记录 AI-Platform 项目的 Git 仓库初始化、推送、协作全流程。
> **更新日期**: 2026-06-08
> **适用**: 团队成员、CI/CD 维护者

---

## 目录

1. [初次部署(已完成)](#1-初次部署已完成)
2. [网络环境检查](#2-网络环境检查)
3. [SSH 密钥配置](#3-ssh-密钥配置)
4. [Git 配置](#4-git-配置)
5. [仓库初始化与推送](#5-仓库初始化与推送)
6. [日常提交流程](#6-日常提交流程)
7. [分支管理](#7-分支管理)
8. [故障排查](#8-故障排查)
9. [CI/CD 触发规则](#9-cicd-触发规则)
10. [附录](#10-附录)

---

## 1. 初次部署(已完成)

> 本节记录项目首次推送到 GitHub 的全过程,作为后续操作的参考案例。

### 1.1 时间线

| 时间 | 事件 | 结果 |
|---|---|---|
| 2026-06-08 08:53 | 在 GitHub 创建空仓库 `xutao5596/ai-platform` | ✅ |
| 2026-06-08 ~09:00 | 本地初始化 Git 仓库 | ✅ |
| 2026-06-08 ~09:30 | 添加 GitHub remote + 生成 SSH 密钥 | ✅ |
| 2026-06-08 ~10:00 | 推送 main + develop + v0.1.0 | ✅ |

### 1.2 推送内容

```
2 commits:
  510296e  chore(ci): add PR and issue templates
  0de80c4  chore: initial project setup

2 branches:
  main      (生产)
  develop   (开发)

1 tag:
  v0.1.0

35 files
~7,140 lines
```

### 1.3 关键发现

> ⚠️ 本机网络环境特殊:HTTPS 443 端口被阻止,但 SSH 22 端口可用。
> **结论**:本项目远程仓库 URL 统一使用 SSH 协议(`git@github.com:...`)。

---

## 2. 网络环境检查

### 2.1 检测 GitHub 连通性

```powershell
# 检查 HTTPS (443) - 可能被防火墙阻止
Test-NetConnection -ComputerName github.com -Port 443

# 检查 SSH (22) - 通常可用
Test-NetConnection -ComputerName github.com -Port 22

# 检查 HTTP (80) - 通常可用
Test-NetConnection -ComputerName github.com -Port 80

# 检查 DNS
nslookup github.com
```

### 2.2 端口可用性矩阵

| 端口 | 协议 | 本机 | 公网通用 |
|---|---|---|---|
| 22 | SSH | ✅ | ✅ |
| 80 | HTTP | ✅ | ✅ |
| 443 | HTTPS | ❌(本机被阻止) | ✅ |

### 2.3 URL 协议选择

| 协议 | URL 格式 | 适用 |
|---|---|---|
| SSH | `git@github.com:user/repo.git` | **本项目使用** ✅ |
| HTTPS | `https://github.com/user/repo.git` | 443 端口可用时 |

---

## 3. SSH 密钥配置

### 3.1 检查现有密钥

```powershell
$sshDir = "$HOME\.ssh"
if (Test-Path $sshDir) {
    Get-ChildItem $sshDir | Where-Object { $_.Name -like "id_*" -and -not $_.Name.EndsWith(".pub") }
}
```

### 3.2 生成新密钥

```powershell
$sshDir = "$HOME\.ssh"
if (-not (Test-Path $sshDir)) {
    New-Item -ItemType Directory -Path $sshDir -Force | Out-Null
}

ssh-keygen -t ed25519 `
    -C "ai-platform@xutao5596.local" `
    -f "$sshDir\id_ed25519" `
    -N '""'
```

**生成结果**:
- 私钥:`C:\Users\10099\.ssh\id_ed25519`
- 公钥:`C:\Users\10099\.ssh\id_ed25519.pub`

### 3.3 添加公钥到 GitHub

**步骤**:
1. 打开 https://github.com/settings/keys
2. 点击 **"New SSH key"**
3. 填写:
   - Title: `AI-Platform Dev Machine` (或自定义)
   - Key type: `Authentication Key`
   - Key: 粘贴 `id_ed25519.pub` 内容
4. 点击 **"Add SSH key"**

**查看公钥**:
```powershell
Get-Content "$HOME\.ssh\id_ed25519.pub"
```

### 3.4 验证认证

```bash
ssh -i $HOME/.ssh/id_ed25519 -T git@github.com
# 成功输出: Hi xutao5596! You've successfully authenticated, but GitHub does not provide shell access.
```

---

## 4. Git 配置

### 4.1 用户信息(必须)

```bash
# 全局配置
git config --global user.name "AI Platform Team"
git config --global user.email "team@ai-platform.local"

# 或仓库级配置(推荐,项目特定身份)
git -C "D:\Projet\AI-Platform" config user.name "AI Platform Team"
git -C "D:\Projet\AI-Platform" config user.email "team@ai-platform.local"
```

### 4.2 SSH 命令配置(本机特殊环境)

由于本机 SSH agent 服务无法启动(权限受限),需配置 Git 直接使用密钥:

```bash
git -C "D:\Projet\AI-Platform" config core.sshCommand '"/c/Windows/System32/OpenSSH/ssh.exe" -i /c/Users/10099/.ssh/id_ed25519 -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new'
```

**注意**:
- 使用**正斜杠**(`/`)而非反斜杠(`\`)
- 路径用 `/c/Users/...` 格式(Git Bash 风格)
- 启用 `StrictHostKeyChecking=accept-new` 首次连接自动接受 GitHub 主机密钥

### 4.3 行尾配置(避免跨平台问题)

```bash
git config --global core.autocrlf false
```

### 4.4 查看配置

```bash
# 查看所有配置
git -C "D:\Projet\AI-Platform" config --list --local

# 查看特定配置
git -C "D:\Projet\AI-Platform" config --get core.sshCommand
```

---

## 5. 仓库初始化与推送

### 5.1 本地初始化

```bash
# 进入项目目录
cd D:\Projet\AI-Platform

# 初始化 Git 仓库(以 main 为默认分支)
git init -b main
```

### 5.2 添加远程仓库

```bash
# SSH 协议(本项目使用)
git remote add origin git@github.com:xutao5596/ai-platform.git

# 验证
git remote -v
```

### 5.3 首次推送

```bash
# 创建并切换到 develop
git branch develop

# 推送 main + develop + tag
git push -u origin main
git push -u origin develop
git push origin v0.1.0
```

### 5.4 推送结果验证

```bash
# 查看远程所有 ref
git ls-remote
# 应显示:
#   refs/heads/main
#   refs/heads/develop
#   refs/tags/v0.1.0
```

或访问 GitHub API:
```
https://api.github.com/repos/xutao5596/ai-platform/git/trees/main?recursive=1
```

---

## 6. 日常提交流程

### 6.1 标准流程(Conventional Commits)

```bash
# 1. 拉取最新
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

# 5. 在 GitHub 创建 PR(feature/AI-101-flow-editor → develop)

# 6. Code Review + Merge

# 7. 清理
git checkout develop
git pull
git branch -d feature/AI-101-flow-editor
git push origin --delete feature/AI-101-flow-editor
```

### 6.2 提交类型速查

| 类型 | 用途 | 示例 |
|---|---|---|
| `feat` | 新功能 | `feat(flow): add LLM node` |
| `fix` | 修复 | `fix(auth): JWT expiry bug` |
| `docs` | 文档 | `docs(readme): update install` |
| `style` | 代码格式 | `style(backend): fix indent` |
| `refactor` | 重构 | `refactor(node): extract base` |
| `perf` | 性能 | `perf(db): add index` |
| `test` | 测试 | `test(flow): add unit tests` |
| `chore` | 杂项 | `chore(deps): bump version` |
| `build` | 构建 | `build(docker): multi-stage` |
| `ci` | CI | `ci(github): add cache` |
| `revert` | 回滚 | `revert: feat(flow) xxx` |

### 6.3 提交命令简写

```bash
# 提交并推送(本项目推荐)
git add .
git commit -m "feat(scope): description"
git push
```

---

## 7. 分支管理

### 7.1 分支列表

| 分支 | 用途 | 受保护 |
|---|---|---|
| `main` | 生产代码 | ✅ |
| `develop` | 开发集成 | ✅ |
| `feature/*` | 功能开发 | ❌ |
| `fix/*` | Bug 修复 | ❌ |
| `hotfix/*` | 紧急修复 | ❌ |
| `release/*` | 发布准备 | ❌ |

### 7.2 分支命名规范

```
<type>/<ticket-id>-<short-description>

示例:
  feature/AI-101-flow-editor
  feature/AI-102-assistant-dialog
  fix/AI-203-sse-streaming
  hotfix/AI-301-critical-auth
```

### 7.3 创建/删除分支

```bash
# 创建
git checkout -b feature/AI-101-flow-editor

# 推送并跟踪
git push -u origin feature/AI-101-flow-editor

# 删除(已合并)
git branch -d feature/AI-101-flow-editor
git push origin --delete feature/AI-101-flow-editor
```

---

## 8. 故障排查

### 8.1 DNS 解析失败

**症状**:
```
fatal: unable to access '...': Could not connect to server
```

**排查**:
```bash
nslookup github.com
# 如果超时,本地 DNS 不可用
```

**解决**:
- 使用 SSH 协议(本项目做法)
- 或配置公共 DNS:`8.8.8.8`、`1.1.1.1`

### 8.2 端口 443 被阻止

**症状**:
```
Failed to connect to github.com port 443
```

**解决**:
- 改用 SSH 协议
- 或配置 HTTP 代理

### 8.3 SSH 认证失败

**症状**:
```
Permission denied (publickey)
```

**排查**:
```bash
# 1. 检查公钥是否已添加到 GitHub
ssh -T -i $HOME/.ssh/id_ed25519 git@github.com

# 2. 检查密钥权限(应该是 600)
icacls $HOME\.ssh\id_ed25519

# 3. 检查 known_hosts
Get-Content $HOME\.ssh\known_hosts
```

### 8.4 SSH agent 无法启动

**症状**:
```
Error connecting to agent: No such file or directory
```

**原因**:Windows OpenSSH agent 服务被禁用(权限限制)

**解决**:配置 Git 直接使用密钥(本项目做法):
```bash
git config core.sshCommand '"/c/Windows/System32/OpenSSH/ssh.exe" -i /c/Users/10099/.ssh/id_ed25519 -o IdentitiesOnly=yes'
```

### 8.5 Git 路径中的反斜杠问题

**症状**:
```
C:WindowsSystem32OpenSSHssh.exe: command not found
```

**原因**:PowerShell 中反斜杠被错误处理

**解决**:使用正斜杠:
```bash
# ❌ 错误
git config core.sshCommand "C:\Windows\System32\OpenSSH\ssh.exe"

# ✅ 正确
git config core.sshCommand '"/c/Windows/System32/OpenSSH/ssh.exe"'
```

### 8.6 推送冲突

**症状**:
```
! [rejected] develop -> develop (non-fast-forward)
```

**解决**:
```bash
# 拉取并 rebase
git pull --rebase origin develop

# 解决冲突后
git add .
git rebase --continue

# 强制推送(慎用)
git push --force-with-lease
```

---

## 9. CI/CD 触发规则

### 9.1 GitHub Actions

| 触发 | 工作流 | 行为 |
|---|---|---|
| push to main/develop | ci.yml | 完整 CI(lint+test+build) |
| push to main | ci.yml | 创建 Release |
| PR to main/develop | ci.yml + pr-check.yml | 验证 PR |
| tag v*.*.* | cd.yml | 部署到生产(需手动确认) |
| workflow_dispatch | cd.yml | 手动部署到 staging/production |
| 每周一 06:00 UTC | codeql.yml | 安全扫描 |

### 9.2 部署流程

```
代码合并到 develop
  ↓
CI 自动运行
  ↓
自动部署到 staging(暂未配置,需手动)

代码合并到 main
  ↓
CI 自动运行
  ↓
创建 GitHub Release
  ↓
手动触发 CD 部署到生产
```

---

## 10. 附录

### 10.1 完整推送命令(本项目)

```bash
# ============ 一次性设置 ============
# 1. 生成 SSH 密钥
ssh-keygen -t ed25519 -C "ai-platform@xutao5596.local" -f "$HOME\.ssh\id_ed25519" -N '""'

# 2. 添加公钥到 GitHub(手动操作)
#    复制 ~/.ssh/id_ed25519.pub 内容到 https://github.com/settings/keys

# 3. 配置 Git
git config --global user.name "AI Platform Team"
git config --global user.email "team@ai-platform.local"

# 4. 进入项目目录
cd D:\Projet\AI-Platform

# 5. 初始化仓库(首次)
git init -b main

# 6. 配置 SSH 命令
git config core.sshCommand '"/c/Windows/System32/OpenSSH/ssh.exe" -i /c/Users/10099/.ssh/id_ed25519 -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new'

# 7. 添加远程
git remote add origin git@github.com:xutao5596/ai-platform.git

# ============ 推送 ============
# 创建分支
git branch develop

# 推送
git push -u origin main
git push -u origin develop
git push origin v0.1.0
```

### 10.2 常用命令速查

```bash
# 状态
git status
git log --oneline -10

# 远程
git remote -v
git ls-remote

# 分支
git branch -a
git checkout <branch>
git checkout -b <new-branch>

# 提交
git add .
git commit -m "..."
git commit --amend

# 推送/拉取
git push
git push -u origin <branch>
git pull
git pull --rebase

# 撤销
git reset --soft HEAD~1     # 撤销 commit, 保留修改
git reset --hard HEAD~1     # 撤销 commit, 丢弃修改
git checkout -- <file>      # 撤销文件修改

# 标签
git tag                     # 列出
git tag v1.0.0              # 创建
git push origin v1.0.0      # 推送
git tag -d v1.0.0           # 删除本地
git push origin --delete v1.0.0  # 删除远程

# 储藏
git stash
git stash pop
git stash list

# 清理
git branch --merged | grep -v "main\|develop" | xargs git branch -d
```

### 10.3 推荐 Git 别名

```bash
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.lg "log --oneline --graph --decorate"
git config --global alias.last "log -1 HEAD --stat"
git config --global alias.amend "commit --amend --no-edit"
```

### 10.4 远程仓库信息

| 项目 | 值 |
|---|---|
| **GitHub 用户** | `xutao5596` |
| **仓库** | `ai-platform` |
| **HTTPS URL** | `https://github.com/xutao5596/ai-platform.git` |
| **SSH URL** | `git@github.com:xutao5596/ai-platform.git` |
| **Web 访问** | https://github.com/xutao5596/ai-platform |

### 10.5 关键时间点

| 日期 | 事件 |
|---|---|
| 2026-06-08 | 项目创建 + 首次推送 |
| v0.1.0 | 初始版本(35 文件,~7,140 行) |

---

## 快速参考卡

### 推送新功能(标准流程)

```bash
cd D:\Projet\AI-Platform
git checkout develop
git pull
git checkout -b feature/AI-XXX-description
# ... 开发 ...
git add .
git commit -m "feat(scope): description"
git push -u origin feature/AI-XXX-description
# 在 GitHub 创建 PR
```

### 修复 Bug

```bash
cd D:\Projet\AI-Platform
git checkout develop
git pull
git checkout -b fix/AI-XXX-description
# ... 修复 ...
git commit -m "fix(scope): description"
git push -u origin fix/AI-XXX-description
```

### 发布版本

```bash
cd D:\Projet\AI-Platform
git checkout main
git pull
git tag v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
# 在 GitHub 创建 Release
```

---

**OPERATIONS 终**
