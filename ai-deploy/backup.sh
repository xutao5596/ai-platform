#!/usr/bin/env bash
# AI-Platform 数据库 + 数据目录备份
# 用法: sudo ./backup.sh
# 产物: /data/backup/db_YYYYmmdd_HHMMSS.sql.gz
#       /data/backup/hnsw_YYYYmmdd_HHMMSS.tar.gz
#       /data/backup/upload_YYYYmmdd_HHMMSS.tar.gz
set -euo pipefail

APP_NAME="ai-platform"
DATA_DIR="/data"
BACKUP_DIR="${DATA_DIR}/backup"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ai_platform}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-}"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'
log()  { echo "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
warn() { echo "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
err()  { echo "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" >&2; }

if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    err "请使用 root 账号执行: sudo $0"
    exit 1
fi

mkdir -p "$BACKUP_DIR"
log "==== 备份开始 $TIMESTAMP ===="

# --- 数据库 ---
DB_FILE="${BACKUP_DIR}/db_${TIMESTAMP}.sql"
log "导出数据库 ${DB_NAME} → ${DB_FILE}.gz"
mysqldump \
    --host="$DB_HOST" --port="$DB_PORT" \
    --user=root --password="${DB_ROOT_PASSWORD}" \
    --single-transaction --routines --triggers --events \
    "$DB_NAME" > "$DB_FILE"
gzip -f "$DB_FILE"
log "数据库备份完成: ${DB_FILE}.gz"

# --- 向量索引 ---
HNSW_FILE="${BACKUP_DIR}/hnsw_${TIMESTAMP}.tar.gz"
if [ -d "${DATA_DIR}/hnsw" ]; then
    log "备份 Hnswlib 索引..."
    tar -czf "$HNSW_FILE" -C "$DATA_DIR" hnsw
    log "Hnswlib: $HNSW_FILE"
else
    warn "未发现 ${DATA_DIR}/hnsw,跳过"
fi

# --- 上传文件 ---
UPLOAD_FILE="${BACKUP_DIR}/upload_${TIMESTAMP}.tar.gz"
if [ -d "${DATA_DIR}/upload" ]; then
    log "备份上传文件..."
    tar -czf "$UPLOAD_FILE" -C "$DATA_DIR" upload
    log "上传文件: $UPLOAD_FILE"
else
    warn "未发现 ${DATA_DIR}/upload,跳过"
fi

# --- 清理 30 天前 ---
log "清理 30 天前的旧备份..."
find "$BACKUP_DIR" \( -name "db_*.sql.gz" -o -name "hnsw_*.tar.gz" -o -name "upload_*.tar.gz" \) \
    -mtime +30 -delete -print

log "==== 备份完成 ===="
ls -lh "$BACKUP_DIR" | tail -n 10