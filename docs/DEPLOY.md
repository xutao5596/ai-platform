# AI-Platform 部署文档

> 适用版本:v0.6.0(Sprint 4)
> 目标读者:运维 / DevOps / 实施工程师
> 配套脚本:`ai-deploy/` 目录

## 1. 环境要求

### 1.1 硬件

| 项 | 最低 | 推荐 |
| --- | --- | --- |
| CPU | 4 核 | 8 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 50 GB | 100 GB(SSD) |
| 网络 | 100 Mbps | 1 Gbps |

### 1.2 软件

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| OS | CentOS 7+/Ubuntu 20.04+/Debian 11+ | 目标平台 |
| JDK | OpenJDK 21 LTS | 运行后端 |
| MariaDB | 10.6+(推荐 12.2) | 主数据库 |
| Nginx | 1.20+ | 反向代理(可选) |
| Maven | 3.9+ | **仅构建时需要**,运行时不需要 |

### 1.3 端口

| 端口 | 用途 | 暴露 |
| --- | --- | --- |
| 80 / 443 | HTTP/HTTPS(Nginx) | 对外 |
| 8080 | Spring Boot 后端 | 仅内网 |
| 3306 | MariaDB | 仅内网 |
| 22 | SSH 管理 | 仅运维 |

### 1.4 启动顺序

```
MariaDB → Java 21 (ai-platform.jar) → Nginx (可选)
```

## 2. 单机部署(用 deploy.sh)

### 2.1 上传代码

```bash
# 本地打包
cd D:\Projet\AI-Platform
# 把整个项目目录(包含 ai-backend / ai-frontend / ai-deploy)同步到服务器
rsync -avz --exclude 'node_modules' --exclude 'target' --exclude '.git/objects' \
    ./ user@server:/opt/ai-platform-source/
```

### 2.2 准备环境变量

```bash
ssh user@server
cd /opt/ai-platform-source
sudo cp ai-deploy/env.example /opt/ai-platform/.env
sudo vi /opt/ai-platform/.env   # 改 DB_PWD / JWT_SECRET
```

**关键变量**:
- `DB_PWD` / `DB_ROOT_PASSWORD`:数据库账号密码(必须)
- `JWT_SECRET`:建议用 `openssl rand -base64 48` 生成

### 2.3 执行部署

```bash
# 把仓库目录导出,deploy.sh 会 cd 进去 mvn build
export APP_REPO_DIR=/opt/ai-platform-source
export DB_ROOT_PASSWORD=YourRootPwd

sudo chmod +x ai-deploy/*.sh
sudo ./ai-deploy/deploy.sh
```

### 2.4 部署结果校验

```bash
# 1. 服务状态
sudo systemctl status ai-platform

# 2. 健康检查
curl http://127.0.0.1:8080/actuator/health

# 3. 登录验证
curl -X POST http://127.0.0.1:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}'

# 4. 端口监听
ss -tlnp | grep -E ':(80|3306|8080)'
```

### 2.5 常见部署问题

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| `mvn: command not found` | 未装 Maven | `apt install -y maven` |
| `JAVA_HOME not set` | 未装 Java 21 | `apt install -y openjdk-21-jdk` |
| `Access denied for user` | DB 密码错 | 检查 `.env` 和 `DB_ROOT_PASSWORD` |
| 端口 8080 占用 | 上次服务未停 | `sudo systemctl stop ai-platform` |
| Flyway 报 `WSREP_ON` | MariaDB 12.x 新增变量 | 忽略,不影响功能 |

## 3. 反向代理 + HTTPS

### 3.1 安装 Nginx

```bash
# Ubuntu / Debian
sudo apt update
sudo apt install -y nginx

# CentOS
sudo yum install -y nginx
```

### 3.2 部署配置

```bash
sudo cp /opt/ai-platform-source/ai-deploy/nginx/ai-platform.conf \
         /etc/nginx/conf.d/
sudo nginx -t
sudo systemctl reload nginx
```

默认配置监听 80,关键转发:
- `/api/*` → `http://127.0.0.1:8080`
- `/` → `/opt/ai-platform/frontend`
- `/actuator/*` → 仅内网可访问

### 3.3 启用 HTTPS(Let's Encrypt)

```bash
# 1. 安装 certbot
sudo apt install -y certbot python3-certbot-nginx

# 2. 申请证书(自动改 nginx)
sudo certbot --nginx -d ai-platform.example.com

# 3. 测试自动续期
sudo certbot renew --dry-run
```

### 3.4 防火墙放行

```bash
# Ubuntu (ufw)
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow OpenSSH

# CentOS (firewalld)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

## 4. 备份与恢复

### 4.1 自动备份(crontab)

```bash
sudo crontab -e
# 添加:每天凌晨 3 点备份
0 3 * * * /opt/ai-platform-source/ai-deploy/backup.sh >> /var/log/ai-platform-backup.log 2>&1
```

`backup.sh` 会生成:
- `/data/backup/db_YYYYmmdd_HHMMSS.sql.gz`(数据库)
- `/data/backup/hnsw_YYYYmmdd_HHMMSS.tar.gz`(向量索引)
- `/data/backup/upload_YYYYmmdd_HHMMSS.tar.gz`(上传文件)

保留 30 天,自动清理更早的备份。

### 4.2 手动恢复

```bash
# 1. 停服
sudo systemctl stop ai-platform

# 2. 恢复数据库
sudo /opt/ai-platform-source/ai-deploy/restore.sh db_20260610_153000

# 3. 恢复向量索引(如需要)
sudo /opt/ai-platform-source/ai-deploy/restore.sh hnsw_20260610_153000

# 4. 启服
sudo systemctl start ai-platform
```

### 4.3 异地备份(可选)

```bash
# 把本地备份同步到对象存储(S3 / OSS)
0 4 * * * /usr/local/bin/s3cmd sync /data/backup/ s3://my-bucket/ai-platform/
```

## 5. 升级

### 5.1 升级流程

```bash
# 1. 拉取最新代码到服务器
ssh user@server
cd /opt/ai-platform-source
git pull origin main

# 2. 执行升级脚本(自动:备份旧 jar → 重构 → 替换 → 重启)
sudo /opt/ai-platform-source/ai-deploy/upgrade.sh
```

### 5.2 回滚

升级脚本会把旧 jar 备份到 `/data/backup/jar_*.jar`,如需回滚:

```bash
sudo systemctl stop ai-platform
sudo cp /data/backup/jar_20260610_120000.jar /opt/ai-platform/ai-platform.jar
sudo systemctl start ai-platform
```

数据库方面,升级是向前兼容的(Flyway 增量脚本不会破坏旧 schema);如需回滚数据库,使用 `restore.sh`。

### 5.3 蓝绿部署(可选)

```bash
# 在 /opt 下维护 ai-platform-green,新版本先在 green 启动
# 确认 OK 后切 nginx upstream
# 失败时 1 秒切回 ai-platform-blue
```

## 6. 监控建议

### 6.1 应用监控

后端已暴露 actuator 端点,Prometheus 可拉取:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'ai-platform'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ai-platform.internal:8080']
```

**关键指标**:
- `jvm_memory_used_bytes`(堆内存使用)
- `hikaricp_connections_active`(数据库连接活跃数)
- `http_server_requests_seconds{quantile="0.99"}`(P99 响应时间)

### 6.2 系统监控

推荐用 `node_exporter` + Grafana 关注:

| 指标 | 告警阈值 |
| --- | --- |
| CPU 使用率 | > 80% 持续 5 分钟 |
| 内存使用率 | > 85% |
| 磁盘使用率 | > 85% |
| 磁盘 IO 等待 | > 20% |

### 6.3 日志聚合

```bash
# /etc/rsyslog.d/ai-platform.conf
:programname, isequal, "ai-platform" /var/log/ai-platform/journal.log
& stop
```

或用 Promtail + Loki / Filebeat + ELK 收集 `journalctl -u ai-platform` 输出。

## 7. 故障排查

### 7.1 服务无法启动

```bash
# 1. 看 systemd 状态
sudo systemctl status ai-platform

# 2. 看最近 100 行日志
sudo journalctl -u ai-platform -n 100 --no-pager

# 3. 手动启动看实时输出
sudo -u aiplatform /usr/bin/java $JAVA_OPTS -jar /opt/ai-platform/ai-platform.jar
```

### 7.2 数据库连接失败

```bash
# 1. MariaDB 状态
sudo systemctl status mariadb

# 2. 测连通
mysql -h 127.0.0.1 -u aiplatform -p ai_platform -e "SELECT 1"

# 3. 看 Flyway 错误
sudo journalctl -u ai-platform | grep -i flyway
```

### 7.3 接口 502 / 504

```bash
# 1. 后端是否还活着
curl -v http://127.0.0.1:8080/actuator/health

# 2. nginx 错误日志
sudo tail -f /var/log/nginx/ai-platform.error.log

# 3. OOM?
dmesg | grep -i killed
```

### 7.4 性能突然下降

```bash
# 1. 实时进程
top -u aiplatform

# 2. 数据库慢查询
mysql -uroot -p -e "SHOW PROCESSLIST" | head -30
mysql -uroot -p -e "SHOW ENGINE INNODB STATUS\G" | head -100

# 3. JVM 线程
sudo -u aiplatform jstack $(pgrep -f ai-platform.jar) | head -100
```

### 7.5 磁盘爆满

```bash
# 1. 找大文件
du -h /data/* | sort -hr | head -20

# 2. 清理 Hnswlib 旧索引(向量库会保留所有版本)
ls -lh /data/hnsw/

# 3. 清理 7 天前的日志
journalctl --vacuum-time=7d
```

### 7.6 重置管理员密码

```bash
mysql -uroot -p ai_platform
> UPDATE sys_user
>   SET password = SHA2('NewPassword123!', 256)
>   WHERE username = 'admin';
```

## 8. 安全清单

- [ ] 改默认 admin 密码
- [ ] 改 `JWT_SECRET`(`openssl rand -base64 48`)
- [ ] MariaDB 仅监听 127.0.0.1(`bind-address = 127.0.0.1`)
- [ ] 启用 HTTPS(Let's Encrypt)
- [ ] 配置 fail2ban 防 SSH 爆破
- [ ] 定期 `yum update` / `apt update`
- [ ] 备份定期演练(每季度)
- [ ] 关闭不必要的 actuator 端点
- [ ] nginx 加 rate limit(防 CC)

## 9. 附录:目录与文件

```
/opt/ai-platform/                   # 应用目录
├── ai-platform.jar                 # 后端可执行 jar
└── conf/                           # 外部配置(可选)

/data/                              # 数据目录(独立分区更佳)
├── hnsw/                           # Hnswlib 向量索引
├── upload/                         # 用户上传文件
├── logs/                           # 应用文件日志
└── backup/                         # 备份产物

/etc/systemd/system/ai-platform.service
/etc/nginx/conf.d/ai-platform.conf
/var/log/nginx/ai-platform.{access,error}.log
```

## 10. 参考

- 用户手册:[USER-MANUAL.md](./USER-MANUAL.md)
- API 契约:[api/sprint4-contracts.md](./api/sprint4-contracts.md)
- 编码规范:[OPERATIONS-ENCODING.md](./OPERATIONS-ENCODING.md)
- 压测指南:[../tests/load/jmeter/README.md](../tests/load/jmeter/README.md)
- 部署脚本:`../ai-deploy/README.md`
