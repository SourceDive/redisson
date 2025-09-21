package org.redisson;

import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zero
 * @description todo
 * @date 2025-07-02
 */
public class TestConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestConnection.class);

    @Test
    public void testConnection() {
        // 等待 Redis 服务启动
        RedisTestHelper.waitForRedisStartup(10, 2000);
        
        RedissonClient client = null;
        try {
            // 使用 Docker 中的 Redis 服务
            client = RedisTestHelper.createSingleServerClient();
            LOGGER.info("客户端创建成功");
            
            // 测试连接
            if (RedisTestHelper.testConnection(client)) {
                LOGGER.info("Redis 连接测试成功");
                
                // 测试基本操作
                client.getBucket("test:connection").set("Hello Docker Redis");
                String value = (String) client.getBucket("test:connection").get();
                LOGGER.info("获取value: {}", value);
                
                // 清理测试数据
                client.getBucket("test:connection").delete();
                LOGGER.info("测试数据已清理");
            } else {
                LOGGER.error("Redis 连接测试失败");
            }
        } catch (Exception e) {
            LOGGER.error("连接测试失败: {}", e.getMessage());
            throw e;
        } finally {
            RedisTestHelper.shutdownClient(client);
            LOGGER.info("连接已关闭");
        }
    }
}
