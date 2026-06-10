#!/usr/bin/env bash
# 查看 AI-Platform 服务状态、端口、健康检查
set -euo pipefail

APP_NAME="ai-platform"
APP_PORT="${SERVER_PORT:-8080}"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'
log() { echo "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
warn() { echo "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }

echo "==== systemd 状态 ===="
systemctl status "${APP_NAME}.service" --no-pager || true

echo ""
echo "==== 进程 ===="
pgrep -af "java.*ai-platform.jar" || warn "未找到 ai-platform.jar 进程"

echo ""
echo "==== 端口 ${APP_PORT} ===="
if command -v ss >/dev/null 2>&1; then
    ss -tlnp 2>/dev/null | grep ":${APP_PORT}" || warn "端口 ${APP_PORT} 未监听"
elif command -v netstat >/dev/null 2>&1; then
    netstat -tlnp 2>/dev/null | grep ":${APP_PORT}" || warn "端口 ${APP_PORT} 未监听"
fi

echo ""
echo "==== 健康检查 ===="
if curl -fsS --max-time 5 "http://127.0.0.1:${APP_PORT}/actuator/health" 2>/dev/null; then
    log "健康检查通过"
else
    warn "健康检查失败(可忽略:actuator 可能未启用)"
fi

echo ""
echo "==== 磁盘使用(数据目录) ===="
du -sh /opt/ai-platform /data 2>/dev/null || true