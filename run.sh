#!/bin/bash
# 网络设备管理系统 - 构建并启动脚本
set -e
cd "$(dirname "$0")"

# 加载本地环境变量（.env 文件不提交到仓库，用于配置 DB_PASSWORD 等）
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

if [ -z "$DB_PASSWORD" ]; then
  echo "错误：未设置 DB_PASSWORD。请在项目根目录创建 .env 文件并写入 DB_PASSWORD='数据库密码'" >&2
  exit 1
fi

export JAVA_HOME="$HOME/tools/jdk-17.0.20.1+1"
export PATH="$JAVA_HOME/bin:$PATH"
MVN="$HOME/tools/apache-maven-3.9.16/bin/mvn"
SETTINGS="$HOME/tools/maven-settings.xml"

echo "==> 打包项目（首次运行需下载依赖，请耐心等待）..."
"$MVN" -s "$SETTINGS" package -DskipTests -q

echo "==> 启动服务：http://localhost:8080"
exec java -jar target/netdev-manager.jar
