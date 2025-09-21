#!/bin/bash

# Redis Docker 启动脚本
# 用于启动 Redis 服务并运行测试

set -e

echo "=== Redis Docker 启动脚本 ==="

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查 docker-compose 是否可用
if ! command -v docker-compose > /dev/null 2>&1; then
    echo "错误: docker-compose 未安装，请先安装 docker-compose"
    exit 1
fi

# 停止并删除现有容器
echo "停止现有容器..."
docker-compose down -v

# 启动 Redis 服务
echo "启动 Redis 服务..."
docker-compose up -d redis

# 等待 Redis 服务启动
echo "等待 Redis 服务启动..."
sleep 5

# 检查 Redis 服务状态
echo "检查 Redis 服务状态..."
if docker-compose ps redis | grep -q "Up"; then
    echo "✅ Redis 服务启动成功"
else
    echo "❌ Redis 服务启动失败"
    docker-compose logs redis
    exit 1
fi

# 测试 Redis 连接
echo "测试 Redis 连接..."
if docker-compose exec redis redis-cli ping | grep -q "PONG"; then
    echo "✅ Redis 连接测试成功"
else
    echo "❌ Redis 连接测试失败"
    exit 1
fi

echo "=== Redis 服务已就绪，可以运行测试 ==="
echo "运行测试命令: mvn test"
echo "停止服务命令: docker-compose down"
