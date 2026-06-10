#!/usr/bin/env bash
# ============================================================
# AI-Platform 单机部署脚本
# 目标: Linux(CentOS / Ubuntu / Debian)+ JDK 21 + MariaDB 12.x
# 用法: sudo ./deploy.sh
# 行为: 检查环境 → 初始化 DB → 构建 jar → 安装 systemd → 启动
# ============================================================
set -euo pipefail

# ---------- 基础配置 ----------
APP_NAME="ai-platform"
APP_USER="aiplatform"
APP_HOME="/opt/ai-platform"
APP_JAR="${APP_HOME}/ai-platform.jar"
APP_PORT="${SERVER_PORT:-8080}"
APP_REPO_DIR_DEFAULT="$(cd "$(dirname "$0")/.." >/dev/null 2>&1 && pwd)"
APP_REPO_DIR="${APP_REPO_DIR:-$APP_REPO_DIR_DEFAULT}"

DATA_DIR="/data"
LOG_DIR="${DATA_DIR}/logs"
HNSW_DIR="${DATA_DIR}/hnsw"
UPLOAD_DIR="${DATA_DIR}/upload"
BACKUP_DIR="${DATA_DIR}/backup"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SYSTEMD_SRC="${SCRIPT_DIR}/../systemd/${APP_NAME}.service"
ENV_FILE_SRC="${SCRIPT_DIR}/../env.example"
ENV_FILE_DST="${APP_HOME}/.env"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ai_platform}"
DB_USER="${DB_USER:-aiplatform}"
DB_PWD="${DB_PWD:-ChangeMeStrong!2026}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-}"

# ---------- 颜色 ----------
RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; NC=$'\033[0m'
log()   { echo "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
warn()  { echo "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"; }
err()   { echo "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*" >&2; }

# ---------- 前置检查 ----------
require_root() {
    if [ "${EUID:-$(id -u)}" -ne 0 ]; then
        err "请使用 root 账号执行: sudo $0"
        exit 1
    fi
}

check_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        err "缺少依赖命令: $1"
        return 1
    fi
}

check_java() {
    log "检查 Java 21..."
    if ! command -v java >/dev/null 2>&1; then
        err "未安装 Java,请先安装 OpenJDK 21"
        err "  Ubuntu/Debian: apt install -y openjdk-21-jdk"
        err "  CentOS/RHEL : yum install -y java-21-openjdk-devel"
        return 1
    fi
    local ver
    ver="$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')"
    log "Java 版本: $ver"
    if [[ "$ver" != 21* ]]; then
        warn "检测到 Java $ver,Sprint 4 要求 21.x;继续但可能编译失败"
    fi
}

check_mariadb() {
    log "检查 MariaDB..."
    if ! command -v mysql >/dev/null 2>&1; then
        err "未安装 MariaDB / MySQL 客户端"
        err "  Ubuntu/Debian: apt install -y mariadb-client"
        err "  CentOS/RHEL : yum install -y mariadb"
        return 1
    fi
    if ! command -v mysqldump >/dev/null 2>&1; then
        err "未安装 mysqldump,无法后续做备份"
        return 1
    fi
    log "尝试连接 MariaDB ${DB_HOST}:${DB_PORT}..."
    if ! mysql -h "$DB_HOST" -P "$DB_PORT" -u root \
            --password="${DB_ROOT_PASSWORD}" \
            -e "SELECT VERSION();" >/dev/null 2>&1; then
        err "无法连接 MariaDB,请检查服务状态和 DB_ROOT_PASSWORD"
        return 1
    fi
    log "MariaDB 连接正常"
}

check_nginx() {
    log "检查 nginx(可选,未安装仅警告)..."
    if ! command -v nginx >/dev/null 2>&1; then
        warn "未安装 nginx,如需反向代理请安装: apt install -y nginx"
    else
        log "nginx 已安装: $(nginx -v 2>&1)"
    fi
}

check_maven() {
    log "检查 Maven(构建后端需要)..."
    if ! command -v mvn >/dev/null 2>&1; then
        err "未安装 Maven,无法构建后端"
        err "  Ubuntu/Debian: apt install -y maven"
        err "  CentOS/RHEL : yum install -y maven"
        return 1
    fi
    log "Maven 版本: $(mvn -v | head -n 1)"
}

# ---------- 准备目录与用户 ----------
prepare_dirs_and_user() {
    log "创建运行用户和目录..."
    if ! id "$APP_USER" >/dev/null 2>&1; then
        useradd --system --shell /usr/sbin/nologin --home-dir "$APP_HOME" "$APP_USER"
        log "创建用户: $APP_USER"
    fi
    install -d -m 0755 -o "$APP_USER" -g "$APP_USER" \
        "$APP_HOME" "$LOG_DIR" "$HNSW_DIR" "$UPLOAD_DIR" "$BACKUP_DIR"
    install -d -m 0755 "$APP_HOME/conf"
    log "目录: $APP_HOME, $DATA_DIR 已就绪"
}

# ---------- 数据库初始化 ----------
init_database() {
    log "准备数据库 $DB_NAME..."
    local sql_user_sql
    sql_user_sql=$(cat <<EOF
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PWD}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PWD}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF
)
    mysql -h "$DB_HOST" -P "$DB_PORT" -u root --password="${DB_ROOT_PASSWORD}" \
        -e "$sql_user_sql"
    log "数据库与账号已就绪 (Flyway 会在首次启动时自动建表)"
}

# ---------- 构建后端 ----------
build_backend() {
    log "构建后端 (Maven)..."
    if [ ! -d "$APP_REPO_DIR" ]; then
        err "未找到源码目录: $APP_REPO_DIR"
        exit 1
    fi
    cd "$APP_REPO_DIR"
    # -pl ai-start -am 只构建启动模块及其依赖
    mvn -o -pl ai-start -am package -DskipTests
    local built_jar
    built_jar=$(ls -1 ai-start/target/ai-start-*.jar 2>/dev/null | grep -v '\.original$' | head -n 1 || true)
    if [ -z "$built_jar" ] || [ ! -f "$built_jar" ]; then
        err "未找到构建产物 ai-start/target/ai-start-*.jar"
        exit 1
    fi
    log "构建产物: $built_jar"
    install -m 0644 -o "$APP_USER" -g "$APP_USER" "$built_jar" "$APP_JAR"
    log "已安装 jar → $APP_JAR"
}

# ---------- 配置文件 ----------
deploy_env_file() {
    if [ -f "$ENV_FILE_SRC" ]; then
        if [ ! -f "$ENV_FILE_DST" ]; then
            install -m 0600 -o "$APP_USER" -g "$APP_USER" "$ENV_FILE_SRC" "$ENV_FILE_DST"
            log "已生成 .env(env.example 复制),请编辑后重新部署"
        else
            log ".env 已存在,保留用户配置"
        fi
    fi
}

# ---------- systemd ----------
install_systemd() {
    if [ ! -f "$SYSTEMD_SRC" ]; then
        err "缺少 systemd 单元: $SYSTEMD_SRC"
        return 1
    fi
    install -m 0644 "$SYSTEMD_SRC" "/etc/systemd/system/${APP_NAME}.service"
    # 如果 .env 存在,加载到 EnvironmentFile
    if [ -f "$ENV_FILE_DST" ]; then
        # 在 [Service] 段后追加 EnvironmentFile
        if ! grep -q '^EnvironmentFile=' "/etc/systemd/system/${APP_NAME}.service"; then
            sed -i "/^\[Service\]/a EnvironmentFile=${ENV_FILE_DST}" \
                "/etc/systemd/system/${APP_NAME}.service"
        fi
    fi
    systemctl daemon-reload
    systemctl enable "${APP_NAME}.service"
    log "已安装并启用 systemd: ${APP_NAME}.service"
}

# ---------- 启动 ----------
start_service() {
    log "启动服务..."
    systemctl restart "${APP_NAME}.service"
    sleep 5
    if systemctl is-active --quiet "${APP_NAME}.service"; then
        log "服务运行中"
    else
        err "服务启动失败,查看日志: journalctl -u ${APP_NAME} -n 80"
        return 1
    fi
}

# ---------- 健康检查 ----------
health_check() {
    local url="http://127.0.0.1:${APP_PORT}/actuator/health"
    log "健康检查: $url"
    if curl -fsS --max-time 5 "$url" >/dev/null 2>&1; then
        log "健康检查通过"
    else
        warn "健康检查未通过(可忽略:可能 actuator 未启用)"
    fi
}

# ---------- main ----------
main() {
    require_root
    log "==== AI-Platform 部署开始 ===="
    log "APP_HOME=$APP_HOME  PORT=$APP_PORT  REPO=$APP_REPO_DIR"

    check_command curl || exit 1
    check_java
    check_mariadb
    check_nginx
    check_maven

    prepare_dirs_and_user
    init_database
    deploy_env_file
    build_backend
    install_systemd
    start_service
    health_check

    log "==== 部署完成 ===="
    log "查看状态: sudo systemctl status ${APP_NAME}"
    log "查看日志: sudo journalctl -u ${APP_NAME} -f"
    log "健康检查: curl http://127.0.0.1:${APP_PORT}/actuator/health"
}

main "$@"