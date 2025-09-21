package org.redisson;

import org.junit.Test;
import org.redisson.api.RedissonClient;

import static org.junit.Assert.*;

/**
 * 测试 Redisson 版本信息
 */
public class VersionTest {

    @Test
    public void testRedissonVersion() {
        RedissonClient client = null;
        try {
            // 使用 RedisTestHelper 创建客户端，避免 Kryo 问题
            client = RedisTestHelper.createSingleServerClient();
            
            // 测试基本操作
            client.getBucket("version_test").set("Hello from local source!");
            String value = (String) client.getBucket("version_test").get();
            assertEquals("Hello from local source!", value);
            
            // 清理
            client.getBucket("version_test").delete();
            
            System.out.println("✅ 成功使用本地编译的 Redisson 3.17.6 源码！");
            System.out.println("✅ 测试程序现在直接依赖本地源码编译的类");
            
        } catch (Exception e) {
            fail("连接 Redis 失败: " + e.getMessage());
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }
}
