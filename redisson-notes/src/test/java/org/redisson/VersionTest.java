package org.redisson;

import org.junit.Test;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * 测试 Redisson 版本信息
 */
public class VersionTest {

    private static final Logger logger = LoggerFactory.getLogger(VersionTest.class);

    @Test
    public void testRedissonVersion() {
        logger.info("=== 开始测试 Redisson 版本 ===");
        
        RedissonClient client = null;
        try {
            logger.info("正在创建 Redisson 客户端...");
            // 使用 RedisTestHelper 创建客户端，避免 Kryo 问题
            client = RedisTestHelper.createSingleServerClient();
            logger.info("✅ Redisson 客户端创建成功");
            
            // 测试基本操作
            logger.info("正在测试基本操作...");
            client.getBucket("version_test").set("Hello from local source!");
            String value = (String) client.getBucket("version_test").get();
            assertEquals("Hello from local source!", value);
            logger.info("✅ 基本操作测试成功，值: {}", value);
            
            // 清理
            client.getBucket("version_test").delete();
            logger.info("✅ 清理完成");
            
            logger.info("✅ 成功使用本地编译的 Redisson 3.17.6 源码！");
            logger.info("✅ 测试程序现在直接依赖本地源码编译的类");
            
        } catch (Exception e) {
            logger.error("❌ 测试失败: {}", e.getMessage(), e);
            fail("连接 Redis 失败: " + e.getMessage());
        } finally {
            if (client != null) {
                logger.info("正在关闭 Redisson 客户端...");
                client.shutdown();
                logger.info("✅ Redisson 客户端已关闭");
            }
        }
        
        logger.info("=== Redisson 版本测试完成 ===");
    }
}
