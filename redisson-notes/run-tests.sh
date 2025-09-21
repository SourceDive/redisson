#!/bin/bash

# Redisson 测试运行脚本
# 自动启动 Redis 服务并运行所有测试

set -e

echo "=== Redisson 测试运行脚本 ==="

# 检查 Maven 是否可用
MAVEN_CMD=""
if command -v mvn > /dev/null 2>&1; then
    MAVEN_CMD="mvn"
elif [ -f "/usr/local/apache-maven-3.8.4/bin/mvn" ]; then
    MAVEN_CMD="/usr/local/apache-maven-3.8.4/bin/mvn"
else
    echo "错误: Maven 未找到，请先安装 Maven"
    exit 1
fi

# 启动 Redis 服务
echo "启动 Redis 服务..."
./start-redis.sh

# 等待一下确保服务完全启动
sleep 3

# 运行测试
echo "运行 Redisson 测试..."
$MAVEN_CMD clean test

# 检查测试结果
if [ $? -eq 0 ]; then
    echo "✅ 所有测试通过"
else
    echo "❌ 测试失败"
    exit 1
fi

echo "=== 测试完成 ==="
