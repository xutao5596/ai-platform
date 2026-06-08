#!/bin/bash
# ====================================================================
# AI-Platform Backup Script
# Usage: sudo ./backup.sh
# ====================================================================

set -e

APP_NAME="ai-platform"
APP_HOME="/opt/ai-platform"
BACKUP_DIR="/data/backup"
DATA_DIR="/data"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="backup_${TIMESTAMP}.tar.gz"

GREEN='\033[0;32m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }

# Check root
if [ "$EUID" -ne 0 ]; then
    echo "Please run as root (use sudo)"
    exit 1
fi

log "=========================================="
log "AI-Platform Backup - $TIMESTAMP"
log "=========================================="

mkdir -p $BACKUP_DIR

# Stop service for consistent backup
SERVICE_WAS_ACTIVE=0
if systemctl is-active --quiet $APP_NAME; then
    SERVICE_WAS_ACTIVE=1
    log "Stopping $APP_NAME service..."
    systemctl stop $APP_NAME
    sleep 5
fi

# Backup application files
log "Backing up application files..."
tar -czf $BACKUP_DIR/$BACKUP_NAME \
    -C $APP_HOME bin conf frontend 2>/dev/null || true

# Backup MariaDB
log "Backing up MariaDB database..."
DB_BACKUP="$BACKUP_DIR/db_${TIMESTAMP}.sql"
mysqldump -u root -p"$DB_ROOT_PASSWORD" \
    --single-transaction --routines --triggers \
    ai_platform > $DB_BACKUP 2>/dev/null || \
mysqldump -u root \
    --single-transaction --routines --triggers \
    ai_platform > $DB_BACKUP

# Compress DB backup
gzip $DB_BACKUP

# Backup Hnswlib indexes
log "Backing up Hnswlib indexes..."
HNSW_BACKUP="$BACKUP_DIR/hnsw_${TIMESTAMP}.tar.gz"
tar -czf $HNSW_BACKUP -C $DATA_DIR hnsw/ 2>/dev/null || true

# Backup uploads
log "Backing up uploaded files..."
UPLOAD_BACKUP="$BACKUP_DIR/upload_${TIMESTAMP}.tar.gz"
tar -czf $UPLOAD_BACKUP -C $DATA_DIR upload/ 2>/dev/null || true

# Restart service
if [ $SERVICE_WAS_ACTIVE -eq 1 ]; then
    log "Starting $APP_NAME service..."
    systemctl start $APP_NAME
fi

# Clean up old backups (keep last 7 days)
log "Cleaning up old backups..."
find $BACKUP_DIR -name "backup_*.tar.gz" -mtime +7 -delete
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +7 -delete
find $BACKUP_DIR -name "hnsw_*.tar.gz" -mtime +7 -delete
find $BACKUP_DIR -name "upload_*.tar.gz" -mtime +7 -delete

log "=========================================="
log "✅ Backup complete!"
log "=========================================="
log "Application: $BACKUP_DIR/$BACKUP_NAME"
log "Database:    $BACKUP_DIR/db_${TIMESTAMP}.sql.gz"
log "Hnswlib:     $HNSW_BACKUP"
log "Uploads:     $UPLOAD_BACKUP"
log "=========================================="
