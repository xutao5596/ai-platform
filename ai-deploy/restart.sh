#!/usr/bin/env bash
# 重启 AI-Platform 后端服务
set -euo pipefail

APP_NAME="ai-platform"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; NC=$'\033[0m'
log() { echo "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
err() { echo "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" >&2; }

if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    err "请使用 root 账号执行: sudo $0"
    exit 1
fi

log "重启 ${APP_NAME}..."
systemctl restart "${APP_NAME}.service"
sleep 5
if systemctl is-active --quiet "${APP_NAME}.service"; then
    log "${APP_NAME} 已启动"
    systemctl status "${APP_NAME}.service" --no-pager
else
    err "启动失败,查看日志: journalctl -u ${APP_NAME} -n 80"
    exit 1
fi