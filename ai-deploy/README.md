# AI-Platform Deploy

> 部署包与运维配置

## 部署架构

```
Nginx (80/443)
├── /api/*  → ai-platform-backend.jar (8080)
└── /*      → ai-frontend dist (静态资源)
```

## 部署方式

### 方式一:传统部署(推荐)

适用:Linux 虚拟机/物理机

详见 [部署手册](../docs/deploy.md)

### 方式二:Docker Compose(可选)

```bash
cd docker
docker-compose up -d
```

## 目录结构

```
ai-deploy/
├── nginx/
│   ├── nginx.conf
│   └── conf.d/
│       └── ai-platform.conf
├── scripts/
│   ├── start.sh           # 启动后端
│   ├── stop.sh            # 停止后端
│   ├── restart.sh         # 重启后端
│   ├── status.sh          # 状态检查
│   └── deploy.sh          # 一键部署
├── systemd/
│   └── ai-platform.service  # systemd 服务
├── docker/
│   ├── Dockerfile.backend
│   ├── Dockerfile.frontend
│   └── docker-compose.yml
└── README.md
```

## 快速部署

### 1. 上传部署包

```bash
scp ai-platform-deploy-1.0.0.tar.gz user@server:/tmp/
```

### 2. 解压

```bash
ssh user@server
cd /opt
tar -xzf /tmp/ai-platform-deploy-1.0.0.tar.gz
cd ai-platform
```

### 3. 配置数据库

```bash
mysql -u root -p < sql/init.sql
```

### 4. 配置 Nginx

```bash
sudo cp nginx/conf.d/ai-platform.conf /etc/nginx/conf.d/
sudo nginx -t
sudo systemctl reload nginx
```

### 5. 启动后端

```bash
sudo systemctl enable ai-platform
sudo systemctl start ai-platform
```

### 6. 部署前端

```bash
sudo cp -r frontend/* /var/www/ai-platform/
```

## 数据目录

```
/data/
├── hnsw/         # Hnswlib 向量索引
├── upload/       # 上传文件
└── logs/         # 应用日志
```

## 监控

- 应用日志:`/data/logs/ai-platform.log`
- Nginx 日志:`/var/log/nginx/`
- MariaDB 日志:`/var/log/mariadb/`

## 备份与恢复

### 备份

```bash
# 数据库
mysqldump -u root -p ai_platform > backup_$(date +%Y%m%d).sql

# 向量索引
tar -czf hnsw_backup_$(date +%Y%m%d).tar.gz /data/hnsw/

# 上传文件
tar -czf upload_backup_$(date +%Y%m%d).tar.gz /data/upload/
```

### 恢复

```bash
mysql -u root -p ai_platform < backup_xxx.sql
tar -xzf hnsw_backup_xxx.tar.gz -C /
tar -xzf upload_backup_xxx.tar.gz -C /
```

## 状态

🟡 **待开发** - Sprint 4 期间输出
