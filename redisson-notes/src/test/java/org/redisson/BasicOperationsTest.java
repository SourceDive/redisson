package org.redisson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 基础操作测试案例
 * 包含 String、Hash、List、Set、ZSet 等基本数据类型的操作
 *
 * @author zero
 * @date 25.09.21 Sun
 */
public class BasicOperationsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicOperationsTest.class);
    private RedissonClient redissonClient;

    @Before
    public void setUp() {
        // 等待 Redis 服务启动
        RedisTestHelper.waitForRedisStartup(10, 2000);

        // 使用 Docker 中的 Redis 服务
        redissonClient = RedisTestHelper.createSingleServerClient();

        // 测试连接
        if (!RedisTestHelper.testConnection(redissonClient)) {
            throw new RuntimeException("无法连接到 Docker 中的 Redis 服务");
        }
    }

    @After
    public void tearDown() {
        RedisTestHelper.shutdownClient(redissonClient);
    }

    /**
     * 测试 String 类型操作
     */
    @Test
    public void testStringOperations() {
        LOGGER.info("=== 开始测试 String 操作 ===");

        RBucket<String> bucket = redissonClient.getBucket("test:string:key");

        // 设置值
        bucket.set("Hello Redisson");
        LOGGER.info("设置 String 值: Hello Redisson");

        // 获取值
        String value = bucket.get();
        LOGGER.info("获取 String 值: {}", value);

        // 设置过期时间
        bucket.set("Hello Redisson with TTL", 10L, TimeUnit.SECONDS);
        LOGGER.info("设置带过期时间的 String 值");

        // 检查是否存在
        boolean exists = bucket.isExists();
        LOGGER.info("键是否存在: {}", exists);

        // 删除
        bucket.delete();
        LOGGER.info("删除 String 键");

        LOGGER.info("=== String 操作测试完成 ===");
    }

    /**
     * 测试 Hash 类型操作
     */
    @Test
    public void testHashOperations() {
        LOGGER.info("=== 开始测试 Hash 操作 ===");

        RMap<String, String> map = redissonClient.getMap("test:hash:user");

        // 设置多个字段
        map.put("name", "张三");
        map.put("age", "25");
        map.put("email", "zhangsan@example.com");
        LOGGER.info("设置 Hash 字段");

        // 获取单个字段
        String name = map.get("name");
        LOGGER.info("获取 name 字段: {}", name);

        // 获取所有字段
        Set<String> keys = map.keySet();
        LOGGER.info("所有字段: {}", keys);

        // 检查字段是否存在
        boolean hasAge = map.containsKey("age");
        LOGGER.info("是否包含 age 字段: {}", hasAge);

        // 删除字段
        map.remove("email");
        LOGGER.info("删除 email 字段");

        // 获取字段数量
        int size = map.size();
        LOGGER.info("Hash 字段数量: {}", size);

        // 清空
        map.clear();
        LOGGER.info("清空 Hash");

        LOGGER.info("=== Hash 操作测试完成 ===");
    }

    /**
     * 测试 List 类型操作
     */
    @Test
    public void testListOperations() {
        LOGGER.info("=== 开始测试 List 操作 ===");

        RList<String> list = redissonClient.getList("test:list:items");

        // 添加元素
        list.add("item1");
        list.add("item2");
        list.add("item3");
        LOGGER.info("添加 List 元素");

        // 在指定位置插入
        list.add(1, "item1.5");
        LOGGER.info("在位置1插入元素");

        // 获取元素
        String firstItem = list.get(0);
        LOGGER.info("第一个元素: {}", firstItem);

        // 获取所有元素
        List<String> allItems = list.readAll();
        LOGGER.info("所有元素: {}", allItems);

        // 获取列表大小
        int size = list.size();
        LOGGER.info("List 大小: {}", size);

        // 删除元素
        list.remove("item2");
        LOGGER.info("删除 item2");

        // 清空
        list.clear();
        LOGGER.info("清空 List");

        LOGGER.info("=== List 操作测试完成 ===");
    }

    /**
     * 测试 Set 类型操作
     */
    @Test
    public void testSetOperations() {
        LOGGER.info("=== 开始测试 Set 操作 ===");

        RSet<String> set = redissonClient.getSet("test:set:items");

        // 添加元素
        set.add("apple");
        set.add("banana");
        set.add("orange");
        set.add("apple"); // 重复元素，不会添加
        LOGGER.info("添加 Set 元素");

        // 获取所有元素
        Set<String> allItems = set.readAll();
        LOGGER.info("所有元素: {}", allItems);

        // 检查元素是否存在
        boolean hasApple = set.contains("apple");
        LOGGER.info("是否包含 apple: {}", hasApple);

        // 获取集合大小
        int size = set.size();
        LOGGER.info("Set 大小: {}", size);

        // 删除元素
        set.remove("banana");
        LOGGER.info("删除 banana");

        // 清空
        set.clear();
        LOGGER.info("清空 Set");

        LOGGER.info("=== Set 操作测试完成 ===");
    }

    /**
     * 测试 ZSet (有序集合) 类型操作
     */
    @Test
    public void testZSetOperations() {
        LOGGER.info("=== 开始测试 ZSet 操作 ===");

        RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet("test:zset:ranking");

        // 添加带分数的元素
        zset.add(100.0, "player1");
        zset.add(85.5, "player2");
        zset.add(92.0, "player3");
        zset.add(78.0, "player4");
        LOGGER.info("添加 ZSet 元素");

        // 获取排名（按分数从高到低）
        int rank = zset.rank("player1");
        LOGGER.info("player1 的排名: {}", rank);

        // 获取分数
        Double score = zset.getScore("player1");
        LOGGER.info("player1 的分数: {}", score);

        // 获取前3名
        Collection<String> top3 = zset.valueRangeReversed(0, 2);
        LOGGER.info("前3名: {}", top3);

        // 获取分数范围
        Collection<String> players80to90 = zset.valueRange(80.0, true, 90.0, true);
        LOGGER.info("分数在80-90之间的玩家: {}", players80to90);

        // 获取集合大小
        int size = zset.size();
        LOGGER.info("ZSet 大小: {}", size);

        // 删除元素
        zset.remove("player4");
        LOGGER.info("删除 player4");

        // 清空
        zset.clear();
        LOGGER.info("清空 ZSet");

        LOGGER.info("=== ZSet 操作测试完成 ===");
    }

    /**
     * 测试批量操作
     */
    @Test
    public void testBatchOperations() {
        LOGGER.info("=== 开始测试批量操作 ===");

        // 批量设置
        RBucket<String> bucket1 = redissonClient.getBucket("test:batch:key1");
        RBucket<String> bucket2 = redissonClient.getBucket("test:batch:key2");
        RBucket<String> bucket3 = redissonClient.getBucket("test:batch:key3");

        bucket1.set("value1");
        bucket2.set("value2");
        bucket3.set("value3");
        LOGGER.info("批量设置完成");

        // 批量获取
        String value1 = bucket1.get();
        String value2 = bucket2.get();
        String value3 = bucket3.get();
        LOGGER.info("批量获取: {}, {}, {}", value1, value2, value3);

        // 批量删除
        bucket1.delete();
        bucket2.delete();
        bucket3.delete();
        LOGGER.info("批量删除完成");

        LOGGER.info("=== 批量操作测试完成 ===");
    }
}
