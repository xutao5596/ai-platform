#!/usr/bin/env bash
# AI-Platform 数据恢复
# 用法: sudo ./restore.sh <db_YYYYmmdd_HHMMSS>   # 不带 .gz 后缀
# 例  : sudo ./restore.sh db_20260610_153000
# 也可仅恢复 hnsw / upload: ./restore.sh hnsw_20260610_153000
set -euo pipefail

DATA_DIR="/data"
BACKUP_DIR="${DATA_DIR}/backup"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ai_platform}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-}"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'
log()  { echo "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
warn() { echo "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
err()  { echo "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" >&2; }

if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    err "请使用 root 账号执行: sudo $0"
    exit 1
fi

if [ "${1:-}" = "" ]; then
    err "用法: $0 <备份前缀,如 db_20260610_153000>"
    err "可用备份:"
    ls -1 "${BACKUP_DIR}" 2>/dev/null | sed 's/^/  /' || true
    exit 1
fi

KEY="$1"

warn "本次操作将覆盖目标数据,确认继续? [yes/N]"
read -r CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    log "已取消"
    exit 0
fi

# --- 数据库恢复 ---
if [[ "$KEY" == db_* ]]; then
    DB_FILE="${BACKUP_DIR}/${KEY}.sql.gz"
    if [ ! -f "$DB_FILE" ]; then
        err "未找到: $DB_FILE"
        exit 1
    fi
    log "恢复数据库 ${DB_NAME} ← ${DB_FILE}"
    gunzip -c "$DB_FILE" | mysql \
        --host="$DB_HOST" --port="$DB_PORT" \
        --user=root --password="${DB_ROOT_PASSWORD}" \
        "$DB_NAME"
    log "数据库恢复完成"
fi

# --- Hnswlib ---
if [[ "$KEY" == hnsw_* ]]; then
    HNSW_FILE="${BACKUP_DIR}/${KEY}.tar.gz"
    [ -f "$HNSW_FILE" ] || { err "未找到: $HNSW_FILE"; exit 1; }
    log "恢复 Hnswlib ← ${HNSW_FILE}"
    rm -rf "${DATA_DIR}/hnsw"
    tar -xzf "$HNSW_FILE" -C "$DATA_DIR"
    log "Hnswlib 恢复完成"
fi

# --- Upload ---
if [[ "$KEY" == upload_* ]]; then
    UP_FILE="${BACKUP_DIR}/${KEY}.tar.gz"
    [ -f "$UP_FILE" ] || { err "未找到: $UP_FILE"; exit 1; }
    log "恢复上传文件 ← ${UP_FILE}"
    rm -rf "${DATA_DIR}/upload"
    tar -xzf "$UP_FILE" -C "$DATA_DIR"
    log "上传文件恢复完成"
fi

# 修正权限
chown -R aiplatform:aiplatform "$DATA_DIR" || true
log "==== 恢复完成 ===="