# AI-Platform 部署包

> 单机部署脚本与运维配置,目标平台:Linux(CentOS / Ubuntu / Debian)
> 依赖:JDK 21、MariaDB 12.x、Maven 3.9+(仅构建时)、Nginx(可选,反向代理)

## 目录结构

```
ai-deploy/
├── deploy.sh                # 一键部署:检查环境 → 建库 → 构建 → 装 systemd → 启动
├── stop.sh                  # 停止服务
├── restart.sh               # 重启服务
├── status.sh                # 状态 + 端口 + 健康检查
├── upgrade.sh               # 升级:git pull + 重构 + 替换 jar + 重启
├── backup.sh                # 数据库 + hnsw + upload 备份
├── restore.sh               # 按备份前缀恢复
├── env.example              # 环境变量样例(可复制为 /opt/ai-platform/.env)
├── nginx/
│   └── ai-platform.conf     # 反向代理 + 静态资源配置
├── systemd/
│   └── ai-platform.service  # systemd 单元
└── README.md
```

## 快速部署(单台 Linux)

```bash
# 1. 上传代码到目标机(目录 /opt/ai-platform-source)
# 2. 准备 .env
sudo cp ai-deploy/env.example /opt/ai-platform/.env
sudo vi /opt/ai-platform/.env       # 改 DB_PWD / JWT_SECRET

# 3. 设置环境变量(供 deploy.sh 读取)
export APP_REPO_DIR=/opt/ai-platform-source
export DB_ROOT_PASSWORD=<mariadb root 密码>

# 4. 赋予脚本执行权限
sudo chmod +x ai-deploy/*.sh

# 5. 一键部署
sudo ./ai-deploy/deploy.sh
```

`deploy.sh` 会做:
1. 校验 root 权限 + Java 21 + MariaDB 连接
2. 创建 `aiplatform` 系统用户
3. 创建 `/opt/ai-platform` 与 `/data/{hnsw,upload,logs,backup}`
4. 初始化 `ai_platform` 数据库与专用账号
5. `mvn -o -pl ai-start -am package -DskipTests` 构建
6. 复制 jar 到 `/opt/ai-platform/ai-platform.jar`
7. 复制 systemd 单元、reload、enable、start
8. 触发健康检查

## 日常运维

```bash
sudo ./ai-deploy/status.sh       # 状态 + 端口 + 健康检查
sudo ./ai-deploy/stop.sh
sudo ./ai-deploy/start.sh        # 同 restart
sudo ./ai-deploy/restart.sh
sudo journalctl -u ai-platform -f # 实时日志
```

## 升级

```bash
sudo ./ai-deploy/upgrade.sh               # 拉取当前分支最新
sudo ./ai-deploy/upgrade.sh v0.6.0         # 切到指定 tag
```

升级脚本会:
- 自动备份旧 jar 到 `/data/backup/jar_YYYYmmdd_HHMMSS.jar`
- 停服 → 替换 jar → 启服
- 失败时给出回滚提示(`/data/backup` 内的旧 jar 可手动还原)

## 备份与恢复

```bash
# 每日定时执行(放进 crontab)
sudo ./ai-deploy/backup.sh

# 手动恢复
sudo ./ai-deploy/restore.sh db_20260610_153000       # 仅恢复数据库
sudo ./ai-deploy/restore.sh hnsw_20260610_153000     # 仅恢复向量索引
sudo ./ai-deploy/restore.sh upload_20260610_153000   # 仅恢复上传文件
```

## 反向代理(nginx)

```bash
sudo cp ai-deploy/nginx/ai-platform.conf /etc/nginx/conf.d/
sudo nginx -t
sudo systemctl reload nginx
```

默认监听 80,转发规则:
- `/api/*` → `http://127.0.0.1:8080`(后端,关闭缓冲兼容 SSE)
- `/` → `/opt/ai-platform/frontend`(Vite 产物,7 天强缓存)
- `/actuator/` → 仅内网允许
- `/swagger-ui/`、`/v3/api-docs`、`/doc.html` → Knife4j

需要 HTTPS 时,在 80 server 之前加 443 server(自行准备证书)。

## 数据目录

```
/opt/ai-platform/
├── ai-platform.jar         # 后端可执行 jar
└── conf/                   # 外部配置(可选)

/data/
├── hnsw/                   # Hnswlib 向量索引
├── upload/                 # 用户上传文件
├── logs/                   # 应用日志(若使用文件 appender)
└── backup/                 # 备份产物
```

## 故障排查

| 现象 | 排查 |
| --- | --- |
| 启动报 `Permission denied` | `chown -R aiplatform:aiplatform /opt/ai-platform /data` |
| 数据库连不上 | `mysql -h 127.0.0.1 -uroot -p` 用 deploy.sh 里的密码测 |
| 端口 8080 占用 | `ss -tlnp | grep 8080` 找到占用进程,kill 掉 |
| 健康检查 404 | actuator 需 `management.endpoints.web.exposure.include` 含 `health` |
| 升级后白屏 | 浏览器禁用缓存后重试;前端 Vite 产物需重新 build 并复制 |

## 注意事项

- **不要**在生产环境的 `application.yml` 内写明文 `JWT_SECRET`,使用 `/opt/ai-platform/.env` 注入
- **不要**把 `/data` 放在系统盘(OS 重装会丢数据)
- **建议** 部署前先在 staging 跑一次 `tests/load/jmeter/README.md` 描述的压测