# Redisson 测试案例

这个项目包含了 Redisson 的常见操作测试案例，使用 Docker 容器化的 Redis 服务。

## 项目结构

```
redisson-notes/
├── src/
│   ├── main/java/org/redisson/
│   │   ├── App.java                    # 主应用类
│   │   ├── TestConnection.java         # 连接测试
│   │   └── RedisTestHelper.java        # Redis 连接辅助工具
│   └── test/java/org/redisson/
│       ├── AppTest.java               # 基础测试
│       ├── BasicOperationsTest.java   # 基础操作测试
│       └── DistributedLockTest.java   # 分布式锁测试
├── docker-compose.yml                 # Docker 服务配置
├── redis.conf                         # Redis 配置文件
├── sentinel.conf                      # Redis Sentinel 配置
├── start-redis.sh                     # Redis 启动脚本
├── run-tests.sh                       # 测试运行脚本
└── pom.xml                           # Maven 配置
```

## 快速开始

### 1. 启动 Redis 服务

```bash
# 启动 Redis 服务
./start-redis.sh

# 或者手动启动
docker-compose up -d redis
```

### 2. 运行测试

```bash
# 自动启动 Redis 并运行所有测试
./run-tests.sh

# 或者手动运行
mvn clean test
```

### 3. 停止服务

```bash
# 停止 Redis 服务
docker-compose down
```

## 测试案例说明

### 基础操作测试 (BasicOperationsTest)

包含 Redis 基本数据类型的操作：

- **String 操作**: 设置、获取、过期时间、存在性检查
- **Hash 操作**: 字段设置、获取、删除、大小统计
- **List 操作**: 添加、插入、获取、删除元素
- **Set 操作**: 添加、检查存在、获取所有元素
- **ZSet 操作**: 分数设置、排名查询、范围查询
- **批量操作**: 批量设置、获取、删除

### 分布式锁测试 (DistributedLockTest)

包含分布式同步工具：

- **基本分布式锁**: 获取、释放、超时处理
- **可重入锁**: 同一线程多次获取锁
- **公平锁**: 按请求顺序获取锁
- **读写锁**: 读锁和写锁的互斥控制
- **并发锁竞争**: 多线程同时竞争锁
- **锁自动续期**: 长时间持有锁的自动续期
- **信号量**: 控制并发访问数量

## Docker 服务配置

### 单机模式 (默认)

- **端口**: 6379
- **地址**: redis://localhost:6379
- **持久化**: AOF + RDB

### 集群模式 (可选)

- **端口**: 7000-7005
- **节点数**: 6个节点（3主3从）
- **启动**: `docker-compose up -d redis-cluster`

### Sentinel 模式 (可选)

- **主节点**: 6380
- **从节点**: 6381, 6382
- **Sentinel**: 26379, 26380, 26381
- **启动**: `docker-compose up -d redis-sentinel-master redis-sentinel-slave1 redis-sentinel-slave2 redis-sentinel1 redis-sentinel2 redis-sentinel3`

## 配置说明

### Redis 配置 (redis.conf)

- 启用 AOF 持久化
- 设置内存淘汰策略为 allkeys-lru
- 配置慢查询日志
- 启用延迟监控

### 连接配置

所有测试案例都使用 `RedisTestHelper` 类统一管理连接：

- 自动等待 Redis 服务启动
- 连接池配置优化
- 自动重试机制
- 安全关闭连接

## 运行特定测试

```bash
# 运行基础操作测试
mvn test -Dtest=BasicOperationsTest

# 运行分布式锁测试
mvn test -Dtest=DistributedLockTest

# 运行连接测试
mvn test -Dtest=TestConnection
```

## 注意事项

1. **Docker 要求**: 确保 Docker 和 docker-compose 已安装并运行
2. **端口冲突**: 确保 6379 端口未被占用
3. **内存要求**: Redis 默认配置需要足够的内存
4. **网络连接**: 确保 Docker 网络连接正常

## 故障排除

### Redis 连接失败

```bash
# 检查 Redis 服务状态
docker-compose ps redis

# 查看 Redis 日志
docker-compose logs redis

# 测试 Redis 连接
docker-compose exec redis redis-cli ping
```

### 测试失败

```bash
# 清理并重新构建
mvn clean compile

# 检查依赖
mvn dependency:tree

# 查看详细日志
mvn test -X
```

## 扩展测试

可以基于现有框架添加更多测试案例：

- 发布订阅测试
- 原子操作测试
- 布隆过滤器测试
- 限流器测试
- 分布式计数器测试
