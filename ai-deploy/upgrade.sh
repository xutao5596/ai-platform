#!/usr/bin/env bash
# AI-Platform 升级脚本
# 流程: git pull → mvn package → 停服 → 替换 jar → 起服
# 用法: sudo ./upgrade.sh [git_ref]
#   git_ref 可选: 分支/tag/提交,默认当前分支最新
set -euo pipefail

APP_NAME="ai-platform"
APP_USER="aiplatform"
APP_HOME="/opt/ai-platform"
APP_JAR="${APP_HOME}/ai-platform.jar"
DATA_DIR="/data"
BACKUP_DIR="${DATA_DIR}/backup"
APP_REPO_DIR_DEFAULT="$(cd "$(dirname "$0")/.." >/dev/null 2>&1 && pwd)"
APP_REPO_DIR="${APP_REPO_DIR:-$APP_REPO_DIR_DEFAULT}"
GIT_REF="${1:-}"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'
log()  { echo "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
warn() { echo "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
err()  { echo "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" >&2; }

if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    err "请使用 root 账号执行: sudo $0"
    exit 1
fi

if [ ! -d "$APP_REPO_DIR/.git" ]; then
    err "未找到 Git 仓库: $APP_REPO_DIR"
    exit 1
fi

log "==== 升级开始 ===="
log "仓库: $APP_REPO_DIR  目标 ref: ${GIT_REF:-当前分支}"

# 1. 拉取最新代码
cd "$APP_REPO_DIR"
if [ -n "$GIT_REF" ]; then
    git fetch --all --prune
    git checkout "$GIT_REF"
    git pull --ff-only origin "$GIT_REF" 2>/dev/null || true
else
    # 当前分支先看是否有未提交
    if [ -n "$(git status --porcelain)" ]; then
        warn "工作区有未提交改动,继续升级可能导致覆盖"
        git status --short
    fi
    git pull --ff-only
fi
log "当前提交: $(git log -1 --pretty=format:'%h %s')"

# 2. 备份当前 jar
if [ -f "$APP_JAR" ]; then
    BK="${BACKUP_DIR}/jar_$(date +%Y%m%d_%H%M%S).jar"
    mkdir -p "$BACKUP_DIR"
    cp -a "$APP_JAR" "$BK"
    log "已备份旧 jar: $BK"
fi

# 3. 重新构建
log "重新构建后端..."
mvn -o -pl ai-start -am package -DskipTests
NEW_JAR="$(ls -1 ai-start/target/ai-start-*.jar 2>/dev/null | grep -v '\.original$' | head -n 1 || true)"
if [ -z "$NEW_JAR" ] || [ ! -f "$NEW_JAR" ]; then
    err "未找到构建产物 ai-start/target/ai-start-*.jar"
    exit 1
fi

# 4. 停服
log "停止服务..."
systemctl stop "${APP_NAME}.service" 2>/dev/null || warn "服务未运行"

# 5. 替换 jar
install -m 0644 -o "$APP_USER" -g "$APP_USER" "$NEW_JAR" "$APP_JAR"
log "已安装新 jar: $APP_JAR"

# 6. 启动
log "启动服务..."
systemctl start "${APP_NAME}.service"
sleep 6
if systemctl is-active --quiet "${APP_NAME}.service"; then
    log "服务运行中"
else
    err "启动失败,查看: journalctl -u ${APP_NAME} -n 80"
    exit 1
fi

# 7. 健康检查
if curl -fsS --max-time 5 "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health" >/dev/null 2>&1; then
    log "健康检查通过"
else
    warn "健康检查未通过(可忽略)"
fi

log "==== 升级完成 ===="
log "提交: $(cd "$APP_REPO_DIR" && git log -1 --pretty=format:'%h %s')"