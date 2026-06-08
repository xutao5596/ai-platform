#!/bin/bash
# ====================================================================
# AI-Platform Deployment Script
# Usage: sudo ./deploy.sh [staging|production]
# ====================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ENVIRONMENT=${1:-staging}
APP_NAME="ai-platform"
APP_USER="aiplatform"
APP_HOME="/opt/ai-platform"
APP_PORT=8080
BACKUP_DIR="/data/backup"
DATA_DIR="/data"

log() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
warn() { echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
error() { echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }

# Check root
if [ "$EUID" -ne 0 ]; then
    error "Please run as root (use sudo)"
    exit 1
fi

log "=========================================="
log "AI-Platform Deployment - $ENVIRONMENT"
log "=========================================="

# Check deployment package
if [ ! -d "backend" ] || [ ! -d "frontend" ] || [ ! -d "scripts" ]; then
    error "Deployment package not found. Run from the extracted package directory."
    exit 1
fi

# Create user if not exists
if ! id "$APP_USER" &>/dev/null; then
    log "Creating user $APP_USER..."
    useradd -r -s /bin/false $APP_USER
fi

# Create directories
log "Creating directories..."
mkdir -p $APP_HOME/{bin,conf,logs}
mkdir -p $DATA_DIR/{hnsw,upload,logs,backup}
chown -R $APP_USER:$APP_USER $APP_HOME $DATA_DIR

# Stop existing service
if systemctl is-active --quiet $APP_NAME; then
    log "Stopping $APP_NAME service..."
    systemctl stop $APP_NAME
fi

# Backup current version
if [ -d "$APP_HOME/bin" ] && [ "$(ls -A $APP_HOME/bin/*.jar 2>/dev/null)" ]; then
    log "Backing up current version..."
    BACKUP_NAME="backup_$(date +%Y%m%d_%H%M%S).tar.gz"
    tar -czf $BACKUP_DIR/$BACKUP_NAME -C $APP_HOME bin conf
    log "Backup saved to $BACKUP_DIR/$BACKUP_NAME"
fi

# Deploy new version
log "Deploying new version..."
rm -rf $APP_HOME/bin/*
cp backend/*.jar $APP_HOME/bin/
chown $APP_USER:$APP_USER $APP_HOME/bin/*.jar

# Deploy frontend
log "Deploying frontend..."
mkdir -p $APP_HOME/frontend
rm -rf $APP_HOME/frontend/*
cp -r frontend/* $APP_HOME/frontend/
chown -R $APP_USER:$APP_USER $APP_HOME/frontend

# Deploy nginx config
if [ -d "nginx" ]; then
    log "Deploying nginx configuration..."
    cp nginx/conf.d/ai-platform.conf /etc/nginx/conf.d/ai-platform.conf
    nginx -t
    systemctl reload nginx
fi

# Deploy scripts
log "Updating scripts..."
cp -r scripts/* $APP_HOME/scripts/
chmod +x $APP_HOME/scripts/*.sh

# Update systemd service
if [ ! -f /etc/systemd/system/$APP_NAME.service ]; then
    log "Installing systemd service..."
    cp systemd/$APP_NAME.service /etc/systemd/system/
    systemctl daemon-reload
    systemctl enable $APP_NAME
fi

# Start service
log "Starting $APP_NAME service..."
systemctl start $APP_NAME

# Wait and check
sleep 10
if systemctl is-active --quiet $APP_NAME; then
    log "✅ Service is running"
else
    error "❌ Service failed to start"
    log "Check logs: journalctl -u $APP_NAME -n 50"
    exit 1
fi

# Smoke test
log "Running smoke test..."
if curl -f http://localhost:$APP_PORT/actuator/health &>/dev/null; then
    log "✅ Health check passed"
else
    warn "Health check endpoint not available (may be normal if disabled)"
fi

# Clean up old backups (keep last 5)
log "Cleaning up old backups..."
ls -t $BACKUP_DIR/backup_*.tar.gz 2>/dev/null | tail -n +6 | xargs -r rm

log "=========================================="
log "✅ Deployment complete!"
log "=========================================="
log "Service: systemctl status $APP_NAME"
log "Logs: journalctl -u $APP_NAME -f"
log "URL: http://localhost:$APP_PORT"
log "=========================================="
