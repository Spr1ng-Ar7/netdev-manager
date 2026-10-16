#!/bin/bash
# 网络设备管理系统 - 构建并启动脚本
set -e
cd "$(dirname "$0")"

export JAVA_HOME="$HOME/tools/jdk-17.0.20.1+1"
export PATH="$JAVA_HOME/bin:$PATH"
MVN="$HOME/tools/apache-maven-3.9.16/bin/mvn"
SETTINGS="$HOME/tools/maven-settings.xml"

echo "==> 打包项目（首次运行需下载依赖，请耐心等待）..."
"$MVN" -s "$SETTINGS" package -DskipTests -q

echo "==> 启动服务：http://localhost:8080"
exec java -jar target/netdev-manager.jar
