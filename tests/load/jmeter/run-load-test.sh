#!/usr/bin/env bash
# JMeter 压测启动脚本
# 用法:
#   ./run-load-test.sh                    # 默认:50 线程 / 10s ramp / 100 循环
#   ./run-load-test.sh -t 200 -r 20 -l 50 # 自定义
#   ./run-load-test.sh -g                 # 启动 GUI 模式(本机调试)
#   BASE_URL=https://api.example.com ./run-load-test.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JMX_FILE="${SCRIPT_DIR}/test-plan.jmx"
RESULTS_DIR="${SCRIPT_DIR}/results"
JMETER_BIN="${JMETER_HOME:-/opt/apache-jmeter-5.6.3}/bin/jmeter"

THREADS=50
RAMP=10
LOOPS=100
GUI=0

# 解析参数
while [ $# -gt 0 ]; do
    case "$1" in
        -t) THREADS="$2"; shift 2;;
        -r) RAMP="$2";    shift 2;;
        -l) LOOPS="$2";   shift 2;;
        -g) GUI=1;        shift;;
        -h) echo "用法: $0 [-t 线程数] [-r ramp 秒] [-l 循环] [-g GUI 模式]"; exit 0;;
        *)  echo "未知参数: $1"; exit 1;;
    esac
done

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-admin123}"

# 前置检查
if [ ! -f "$JMX_FILE" ]; then
    echo "[ERROR] 未找到测试计划: $JMX_FILE" >&2
    exit 1
fi

# 查找 jmeter
if [ ! -x "$JMETER_BIN" ]; then
    JMETER_BIN="$(command -v jmeter || true)"
fi
if [ -z "$JMETER_BIN" ] || ! [ -x "$JMETER_BIN" ]; then
    echo "[ERROR] 未找到 jmeter。请设置 JMETER_HOME 或把 jmeter 加入 PATH" >&2
    echo "        下载: https://jmeter.apache.org/download_jmeter.cgi" >&2
    exit 1
fi

# 健康检查目标服务
echo "[INFO] 健康检查 ${BASE_URL}/actuator/health ..."
if ! curl -fsS --max-time 5 "${BASE_URL}/actuator/health" >/dev/null 2>&1; then
    echo "[WARN] 健康检查失败(可忽略),将按计划继续"
fi

mkdir -p "$RESULTS_DIR"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
LOG_FILE="${RESULTS_DIR}/run_${TIMESTAMP}.log"
JTL_FILE="${RESULTS_DIR}/result_${TIMESTAMP}.jtl"
REPORT_DIR="${RESULTS_DIR}/report_${TIMESTAMP}"

COMMON_ARGS=(
    -Jthreads="$THREADS"
    -Jramp="$RAMP"
    -Jloops="$LOOPS"
    -JbaseUrl="$BASE_URL"
    -Jusername="$USERNAME"
    -Jpassword="$PASSWORD"
    -j "$LOG_FILE"
    -l "$JTL_FILE"
)

if [ "$GUI" -eq 1 ]; then
    echo "[INFO] 启动 GUI 模式(本机调试用)"
    exec "$JMETER_BIN" -t "$JMX_FILE" "${COMMON_ARGS[@]}"
fi

echo "=========================================="
echo " AI-Platform 压测启动"
echo " 目标:    $BASE_URL"
echo " 线程:    $THREADS"
echo " Ramp:    ${RAMP}s"
echo " 循环:    $LOOPS"
echo " 结果:    $JTL_FILE"
echo " HTML:    $REPORT_DIR"
echo "=========================================="

# 非 GUI 模式 + 生成 HTML 报告
"$JMETER_BIN" -n \
    -t "$JMX_FILE" \
    "${COMMON_ARGS[@]}" \
    -e -o "$REPORT_DIR"

echo ""
echo "[DONE] 压测完成"
echo "  JTL:        $JTL_FILE"
echo "  HTML 报告:  $REPORT_DIR/index.html"
echo "  日志:       $LOG_FILE"