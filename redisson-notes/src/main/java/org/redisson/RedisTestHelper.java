package org.redisson;

import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis 测试辅助工具类
 * 提供统一的 Redis 连接配置和管理
 *
 * @author zero
 * @date 25.09.21 Sun
 */
public class RedisTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTestHelper.class);

    // Docker Compose 中的 Redis 服务地址
    private static final String REDIS_HOST = "localhost";
    private static final String REDIS_PORT = "6379";
    private static final String REDIS_URL = "redis://" + REDIS_HOST + ":" + REDIS_PORT;

    // 集群模式地址（可选）
    private static final String[] CLUSTER_NODES = {
            "redis://localhost:7000",
            "redis://localhost:7001",
            "redis://localhost:7002",
            "redis://localhost:7003",
            "redis://localhost:7004",
            "redis://localhost:7005"
    };

    // Sentinel 模式地址（可选）
    private static final String[] SENTINEL_ADDRESSES = {
            "redis://localhost:26379",
            "redis://localhost:26380",
            "redis://localhost:26381"
    };

    /**
     * 创建单机模式 Redis 客户端
     */
    public static RedissonClient createSingleServerClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(REDIS_URL)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());

        try {
            RedissonClient client = org.redisson.Redisson.create(config);
            LOGGER.info("单机模式 Redis 客户端创建成功: {}", REDIS_URL);
            return client;
        } catch (RedisConnectionException e) {
            LOGGER.error("单机模式 Redis 客户端创建失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 创建集群模式 Redis 客户端
     */
    public static RedissonClient createClusterClient() {
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress(CLUSTER_NODES)
                .setMasterConnectionPoolSize(10)
                .setSlaveConnectionPoolSize(10)
                .setMasterConnectionMinimumIdleSize(5)
                .setSlaveConnectionMinimumIdleSize(5)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());

        try {
            RedissonClient client = org.redisson.Redisson.create(config);
            LOGGER.info("集群模式 Redis 客户端创建成功");
            return client;
        } catch (RedisConnectionException e) {
            LOGGER.error("集群模式 Redis 客户端创建失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 创建 Sentinel 模式 Redis 客户端
     */
    public static RedissonClient createSentinelClient() {
        Config config = new Config();
        config.useSentinelServers()
                .setMasterName("mymaster")
                .addSentinelAddress(SENTINEL_ADDRESSES)
                .setMasterConnectionPoolSize(10)
                .setSlaveConnectionPoolSize(10)
                .setMasterConnectionMinimumIdleSize(5)
                .setSlaveConnectionMinimumIdleSize(5)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        config.setCodec(new org.redisson.codec.JsonJacksonCodec());

        try {
            RedissonClient client = org.redisson.Redisson.create(config);
            LOGGER.info("Sentinel 模式 Redis 客户端创建成功");
            return client;
        } catch (RedisConnectionException e) {
            LOGGER.error("Sentinel 模式 Redis 客户端创建失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 测试 Redis 连接是否正常
     */
    public static boolean testConnection(RedissonClient client) {
        try {
            // 尝试执行一个简单的操作
            client.getBucket("test:connection").set("test");
            String value = (String) client.getBucket("test:connection").get();
            client.getBucket("test:connection").delete();
            return "test".equals((String) value);
        } catch (Exception e) {
            LOGGER.error("Redis 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 安全关闭 Redis 客户端
     */
    public static void shutdownClient(RedissonClient client) {
        if (client != null) {
            try {
                client.shutdown();
                LOGGER.info("Redis 客户端已安全关闭");
            } catch (Exception e) {
                LOGGER.error("关闭 Redis 客户端时发生错误: {}", e.getMessage());
            }
        }
    }

    /**
     * 等待 Redis 服务启动
     */
    public static void waitForRedisStartup(int maxRetries, long retryIntervalMs) {
        LOGGER.info("等待 Redis 服务启动...");

        for (int i = 0; i < maxRetries; i++) {
            try {
                RedissonClient testClient = createSingleServerClient();
                if (testConnection(testClient)) {
                    shutdownClient(testClient);
                    LOGGER.info("Redis 服务已启动并可用");
                    return;
                }
                shutdownClient(testClient);
            } catch (Exception e) {
                LOGGER.debug("Redis 服务尚未就绪，重试 {}/{}", i + 1, maxRetries);
            }

            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("等待 Redis 启动时被中断");
                return;
            }
        }

        LOGGER.warn("Redis 服务在 {} 次重试后仍未就绪", maxRetries);
    }
}
