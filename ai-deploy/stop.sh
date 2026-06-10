#!/usr/bin/env bash
# 停止 AI-Platform 后端服务
set -euo pipefail

APP_NAME="ai-platform"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; NC=$'\033[0m'
log() { echo "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
err() { echo "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" >&2; }

if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    err "请使用 root 账号执行: sudo $0"
    exit 1
fi

if ! systemctl is-active --quiet "${APP_NAME}.service"; then
    log "${APP_NAME} 未运行,无需停止"
    exit 0
fi

log "停止 ${APP_NAME}..."
systemctl stop "${APP_NAME}.service"
sleep 2
if systemctl is-active --quiet "${APP_NAME}.service"; then
    err "停止失败,请检查: systemctl status ${APP_NAME}"
    exit 1
fi
log "已停止"