#!/bin/bash
# ====================================================================
# AI-Platform Restore Script
# Usage: sudo ./restore.sh <backup_name>
# Example: sudo ./restore.sh backup_20260608_120000
# ====================================================================

set -e

APP_NAME="ai-platform"
APP_HOME="/opt/ai-platform"
BACKUP_DIR="/data/backup"
DATA_DIR="/data"

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_name>"
    echo "Available backups:"
    ls -la $BACKUP_DIR/backup_*.tar.gz 2>/dev/null | awk '{print $9}'
    exit 1
fi

BACKUP_NAME="$1"
DB_NAME="ai_platform"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
warn() { echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
error() { echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }

# Check root
if [ "$EUID" -ne 0 ]; then
    error "Please run as root (use sudo)"
    exit 1
fi

# Check backup exists
if [ ! -f "$BACKUP_DIR/$BACKUP_NAME" ]; then
    error "Backup not found: $BACKUP_DIR/$BACKUP_NAME"
    exit 1
fi

# Confirm
warn "This will REPLACE the current installation!"
read -p "Are you sure? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    log "Aborted"
    exit 0
fi

log "=========================================="
log "AI-Platform Restore - $BACKUP_NAME"
log "=========================================="

# Stop service
if systemctl is-active --quiet $APP_NAME; then
    log "Stopping $APP_NAME service..."
    systemctl stop $APP_NAME
fi

# Restore application files
log "Restoring application files..."
tar -xzf $BACKUP_DIR/$BACKUP_NAME -C $APP_HOME

# Restore database
DB_BACKUP="$BACKUP_DIR/db_${BACKUP_NAME#backup_}.sql.gz"
if [ -f "$DB_BACKUP" ]; then
    log "Restoring database..."
    gunzip -c $DB_BACKUP | mysql -u root ai_platform
else
    warn "Database backup not found, skipping"
fi

# Restore Hnswlib indexes
HNSW_BACKUP="$BACKUP_DIR/hnsw_${BACKUP_NAME#backup_}.tar.gz"
if [ -f "$HNSW_BACKUP" ]; then
    log "Restoring Hnswlib indexes..."
    tar -xzf $HNSW_BACKUP -C $DATA_DIR
fi

# Restore uploads
UPLOAD_BACKUP="$BACKUP_DIR/upload_${BACKUP_NAME#backup_}.tar.gz"
if [ -f "$UPLOAD_BACKUP" ]; then
    log "Restoring uploaded files..."
    tar -xzf $UPLOAD_BACKUP -C $DATA_DIR
fi

# Fix permissions
chown -R aiplatform:aiplatform $APP_HOME /data

# Start service
log "Starting $APP_NAME service..."
systemctl start $APP_NAME

sleep 10
if systemctl is-active --quiet $APP_NAME; then
    log "✅ Service is running"
else
    error "❌ Service failed to start"
    log "Check logs: journalctl -u $APP_NAME -n 50"
    exit 1
fi

log "=========================================="
log "✅ Restore complete!"
log "=========================================="
