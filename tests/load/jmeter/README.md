# JMeter 压测

> Sprint 4 性能基线压测,验证 AI-Platform 在单机部署下的吞吐与响应时间

## 目标

| 指标 | 目标值 |
| --- | --- |
| 单机 QPS(简单 API 如登录/查询) | ≥ 100 QPS |
| 流程执行 QPS(复杂路径) | ≥ 50 QPS |
| 响应时间 P99 | < 1s(LLM 助手聊天 < 3s) |
| 错误率 | < 1% |

## 测试场景

`test-plan.jmx` 模拟 50 个并发用户,Ramp-up 10s,每个用户循环 100 次,覆盖 4 类典型请求:

| # | 接口 | 路径 | 鉴权 | 期望 P99 |
| --- | --- | --- | --- | --- |
| 1 | 登录 | `POST /api/v1/auth/login` | 无 | < 500ms |
| 2 | 流程列表 | `GET /api/v1/flow?pageNum=1&pageSize=20` | JWT | < 1s |
| 3 | 触发流程 | `POST /api/v1/flow/1/run` | JWT | < 1s |
| 4 | 助手聊天 | `POST /api/v1/assistant/1/chat` | JWT | < 3s(LLM) |

## 环境准备

### 1. 安装 JMeter

```bash
# Ubuntu / Debian
sudo apt install -y default-jre          # 需要 JRE 8+
wget https://dlcdn.apache.org/jmeter/binaries/apache-jmeter-5.6.3.tgz
sudo tar -xzf apache-jmeter-5.6.3.tgz -C /opt/
export JMETER_HOME=/opt/apache-jmeter-5.6.3
echo 'export JMETER_HOME=/opt/apache-jmeter-5.6.3' | sudo tee /etc/profile.d/jmeter.sh
```

### 2. 启动 AI-Platform

```bash
sudo systemctl start ai-platform
curl http://127.0.0.1:8080/actuator/health
```

### 3. 准备压测账号

默认使用 `admin / admin123`(由 `V2__seed_data.sql` 注入)。如使用其他账号,通过环境变量传入:

```bash
export USERNAME=admin
export PASSWORD=admin123
```

## 运行压测

### 非 GUI 模式(推荐)

```bash
chmod +x run-load-test.sh
./run-load-test.sh                              # 默认 50/10/100
./run-load-test.sh -t 200 -r 20 -l 50           # 200 线程 / 20s ramp / 50 循环
BASE_URL=https://api.example.com ./run-load-test.sh
```

### GUI 模式(本地调试)

```bash
./run-load-test.sh -g
```

> GUI 模式仅用于脚本调试,不要用于正式压测(单机会成为瓶颈)。

### 直接用 jmeter 命令

```bash
$JMETER_HOME/bin/jmeter -n -t test-plan.jmx \
    -Jthreads=50 -Jramp=10 -Jloops=100 \
    -JbaseUrl=http://127.0.0.1:8080 \
    -Jusername=admin -Jpassword=admin123 \
    -l results/result.jtl \
    -e -o results/report
```

## 报告解读

### HTML 报告

`results/report_<时间戳>/index.html` 提供 Dashboard:

- **APDEX(Application Performance Index)**:0~1 区间,越接近 1 越好
- **Requests Summary**:总请求数、失败率、吞吐(throughput)
- **Response Times Overview**:平均/中位/P90/P95/P99 响应时间
- **Active Threads Over Time**:并发用户数变化曲线
- **Response Time vs Threads**:响应时间随并发的变化

### 关键指标

| 字段 | 含义 | 关注阈值 |
| --- | --- | --- |
| `Throughput` | 每秒请求数 | 登录 ≥ 100,流程 ≥ 50 |
| `Error %` | 失败率 | < 1% |
| `Average` | 平均响应时间 | 越低越好 |
| `90% Line` | P90 响应时间 | < 800ms |
| `95% Line` | P95 响应时间 | < 1s |
| `99% Line` | P99 响应时间 | < 1s(LLM < 3s) |

### 常见问题

| 现象 | 可能原因 | 排查 |
| --- | --- | --- |
| P99 > 1s | 数据库连接池耗尽 | 加大 `spring.datasource.hikari.maximum-pool-size` |
| 错误率 > 5% | 502/504 增多 | 看后端 `journalctl -u ai-platform` |
| 吞吐量上不去 | Tomcat 线程打满 | 调大 `server.tomcat.max-threads` |
| LLM 慢 | 外部 API 限流 | 切换到更稳的模型或本地 Ollama |
| 内存飙升 | Hnswlib 加载过多 | 调小 `ai.kb.chunk-batch` |

## 进阶:加压策略

```bash
# 阶梯式加压(手动多轮)
for t in 50 100 200 500; do
    echo "=== 压测线程: $t ==="
    ./run-load-test.sh -t $t -r 30 -l 30
    sleep 60
done
```

将多次报告结果横向对比,绘制响应时间 vs 并发用户曲线,找到拐点。

## 文件说明

```
tests/load/jmeter/
├── test-plan.jmx          # JMeter 测试计划
├── run-load-test.sh       # 启动脚本(参数化)
├── README.md              # 本文件
└── results/               # 产物目录(自动创建)
    ├── result_*.jtl       # 原始采样数据
    ├── report_*/          # HTML 报告
    └── run_*.log          # JMeter 日志
```

## 注意

- 压测期间不要同时跑大模型请求(会污染响应时间)
- `results/` 目录已 git 忽略,不会提交
- 不要在生产环境直接压测,务必使用 staging 复刻