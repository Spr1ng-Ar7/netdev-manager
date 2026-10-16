#!/bin/bash
# 网络设备管理系统 - 一键停止脚本
# 用法：./stop.sh
PORT=8080

echo "==> 正在停止网络设备管理系统..."

# 1. 按进程名优雅停止（SIGTERM）
PIDS=$(pgrep -f "netdev-manager.jar")
if [ -n "$PIDS" ]; then
    echo "    发现进程: $PIDS"
    kill $PIDS 2>/dev/null
else
    echo "    未发现 netdev-manager 进程"
fi

# 2. 等待端口释放，最多 10 秒；超时则按端口找进程强制结束
for i in $(seq 1 10); do
    if ! ss -tln 2>/dev/null | grep -q ":$PORT "; then
        break
    fi
    if [ "$i" = "6" ]; then
        PID=$(ss -tlnp 2>/dev/null | grep ":$PORT " | grep -oP 'pid=\K[0-9]+' | head -1)
        if [ -n "$PID" ]; then
            echo "    端口仍被占用，强制结束进程 $PID"
            kill -9 "$PID" 2>/dev/null
        fi
    fi
    sleep 1
done

# 3. 确认结果
if ss -tln 2>/dev/null | grep -q ":$PORT "; then
    echo "==> 失败：端口 $PORT 仍被占用，请手动检查（ss -tlnp | grep $PORT）"
    exit 1
fi
echo "==> 已停止，端口 $PORT 已释放"
echo "    提示：数据库 MariaDB 仍在运行；如需一并停止请执行 sudo service mariadb stop"
