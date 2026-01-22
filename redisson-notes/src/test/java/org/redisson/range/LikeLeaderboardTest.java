package org.redisson.range;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redisson.RedisTestHelper;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 点赞排行榜实现
 * 
 * 核心思路：
 * 1. 使用 Redis 的 SortedSet（有序集合）数据结构
 * 2. 用户ID作为成员（member），点赞数作为分数（score）
 * 3. 利用 SortedSet 自动排序的特性实现排行榜
 * 
 * 数据结构：
 * - Key: "like:leaderboard:post:{postId}" 
 * - Value: SortedSet<userId, likeCount>
 * 
 * 优势：
 * - O(log N) 时间复杂度进行排名查询
 * - 自动按分数排序
 * - 支持范围查询（Top N、分页等）
 * - 原子操作保证数据一致性
 * 
 * @author zero
 * @date 面试准备
 */
public class LikeLeaderboardTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LikeLeaderboardTest.class);
    
    // Redis 客户端
    private RedissonClient redissonClient;
    
    // 排行榜的 Key 前缀
    private static final String LEADERBOARD_KEY_PREFIX = "like:leaderboard:post:";
    
    // 测试用的文章ID
    private static final String TEST_POST_ID = "post_001";

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
        
        LOGGER.info("=== 点赞排行榜测试准备完成 ===");
    }

    @After
    public void tearDown() {
        // 清理测试数据
        if (redissonClient != null) {
            RScoredSortedSet<String> leaderboard = getLeaderboard(TEST_POST_ID);
            leaderboard.clear();
            LOGGER.info("清理测试数据完成");
        }
        
        RedisTestHelper.shutdownClient(redissonClient);
        LOGGER.info("=== 点赞排行榜测试结束 ===");
    }

    /**
     * 获取排行榜的 SortedSet 对象
     */
    private RScoredSortedSet<String> getLeaderboard(String postId) {
        String key = LEADERBOARD_KEY_PREFIX + postId;
        return redissonClient.getScoredSortedSet(key);
    }

    /**
     * 点赞功能
     * 
     * 实现方式：
     * - 使用 addScore() 方法增加分数
     * - 如果用户不存在，会自动创建并设置分数
     * - 如果用户已存在，会在原有分数基础上增加
     * 
     * @param postId 文章ID
     * @param userId 用户ID
     * @return 点赞后的总点赞数
     */
    public Double like(String postId, String userId) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        // addScore 是原子操作，线程安全
        Double newScore = leaderboard.addScore(userId, 1.0);
        LOGGER.info("用户 {} 给文章 {} 点赞，当前点赞数: {}", userId, postId, newScore);
        return newScore;
    }

    /**
     * 取消点赞功能
     * 
     * @param postId 文章ID
     * @param userId 用户ID
     * @return 取消点赞后的总点赞数，如果用户不存在返回 null
     */
    public Double unlike(String postId, String userId) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        Double currentScore = leaderboard.getScore(userId);
        
        if (currentScore == null) {
            LOGGER.warn("用户 {} 未给文章 {} 点赞，无法取消", userId, postId);
            return null;
        }
        
        // 如果点赞数为1，直接删除；否则减少分数
        if (currentScore <= 1.0) {
            leaderboard.remove(userId);
            LOGGER.info("用户 {} 取消对文章 {} 的点赞，已从排行榜移除", userId, postId);
            return 0.0;
        } else {
            Double newScore = leaderboard.addScore(userId, -1.0);
            LOGGER.info("用户 {} 取消对文章 {} 的点赞，当前点赞数: {}", userId, postId, newScore);
            return newScore;
        }
    }

    /**
     * 获取用户的点赞数
     * 
     * @param postId 文章ID
     * @param userId 用户ID
     * @return 点赞数，如果用户不存在返回 null
     */
    public Double getUserLikeCount(String postId, String userId) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        Double score = leaderboard.getScore(userId);
        return score;
    }

    /**
     * 获取用户排名（从1开始，排名越小表示点赞数越多）
     * 
     * 注意：
     * - rank() 返回的是从0开始的索引（升序排名）
     * - revRank() 返回的是从0开始的索引（降序排名）
     * - 我们需要降序排名，所以使用 revRank()
     * 
     * @param postId 文章ID
     * @param userId 用户ID
     * @return 排名（从1开始），如果用户不存在返回 null
     */
    public Integer getUserRank(String postId, String userId) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        Integer rank = leaderboard.revRank(userId);
        if (rank == null) {
            return null;
        }
        // revRank 返回的是从0开始的索引，需要+1转换为从1开始的排名
        return rank + 1;
    }

    /**
     * 获取排行榜 Top N
     * 
     * @param postId 文章ID
     * @param topN 前N名
     * @return 用户ID列表（按点赞数从高到低）
     */
    public Collection<String> getTopN(String postId, int topN) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        // valueRangeReversed 获取降序排列的值（从高到低）
        // 参数：startIndex, endIndex（包含）
        return leaderboard.valueRangeReversed(0, topN - 1);
    }

    /**
     * 获取排行榜 Top N（带分数）
     * 
     * @param postId 文章ID
     * @param topN 前N名
     * @return 用户ID和点赞数的集合
     */
    public Collection<ScoredEntry<String>> getTopNWithScores(String postId, int topN) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        // entryRangeReversed 获取降序排列的条目（包含分数）
        return leaderboard.entryRangeReversed(0, topN - 1);
    }

    /**
     * 获取分页排行榜
     * 
     * @param postId 文章ID
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @return 用户ID列表
     */
    public Collection<String> getLeaderboardPage(String postId, int pageNum, int pageSize) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = startIndex + pageSize - 1;
        return leaderboard.valueRangeReversed(startIndex, endIndex);
    }

    /**
     * 获取排行榜总人数
     */
    public int getLeaderboardSize(String postId) {
        RScoredSortedSet<String> leaderboard = getLeaderboard(postId);
        return leaderboard.size();
    }

    // ==================== 测试用例 ====================

    /**
     * 测试基本点赞功能
     */
    @Test
    public void testBasicLike() {
        LOGGER.info("\n=== 测试基本点赞功能 ===");
        
        String postId = TEST_POST_ID;
        
        // 用户1点赞
        Double count1 = like(postId, "user_001");
        assert count1 == 1.0 : "用户1点赞后应该是1";
        
        // 用户2点赞
        Double count2 = like(postId, "user_002");
        assert count2 == 1.0 : "用户2点赞后应该是1";
        
        // 用户1再次点赞（连续点赞）
        Double count3 = like(postId, "user_001");
        assert count3 == 2.0 : "用户1再次点赞后应该是2";
        
        // 验证排行榜
        Collection<String> top3 = getTopN(postId, 3);
        LOGGER.info("Top 3: {}", top3);
        
        // 验证用户排名
        Integer rank1 = getUserRank(postId, "user_001");
        Integer rank2 = getUserRank(postId, "user_002");
        LOGGER.info("user_001 排名: {}, user_002 排名: {}", rank1, rank2);
        
        assert rank1 == 1 : "user_001 应该是第1名";
        assert rank2 == 2 : "user_002 应该是第2名";
    }

    /**
     * 测试取消点赞功能
     */
    @Test
    public void testUnlike() {
        LOGGER.info("\n=== 测试取消点赞功能 ===");
        
        String postId = TEST_POST_ID;
        
        // 先点赞
        like(postId, "user_001");
        like(postId, "user_001");
        like(postId, "user_002");
        
        // 获取初始状态
        Double count1 = getUserLikeCount(postId, "user_001");
        Double count2 = getUserLikeCount(postId, "user_002");
        LOGGER.info("取消前 - user_001: {}, user_002: {}", count1, count2);
        
        // 取消点赞
        Double newCount1 = unlike(postId, "user_001");
        Double newCount2 = unlike(postId, "user_002");
        
        LOGGER.info("取消后 - user_001: {}, user_002: {}", newCount1, newCount2);
        
        assert newCount1 == 1.0 : "user_001 取消后应该是1";
        assert newCount2 == 0.0 : "user_002 取消后应该是0（已移除）";
        
        // 验证 user_002 已从排行榜移除
        Double finalCount2 = getUserLikeCount(postId, "user_002");
        assert finalCount2 == null : "user_002 应该已从排行榜移除";
    }

    /**
     * 测试排行榜排序
     */
    @Test
    public void testLeaderboardRanking() {
        LOGGER.info("\n=== 测试排行榜排序 ===");
        
        String postId = TEST_POST_ID;
        
        // 创建多个用户并点赞
        like(postId, "user_001"); // 1个赞
        like(postId, "user_002"); // 1个赞
        like(postId, "user_003"); // 1个赞
        
        like(postId, "user_001"); // 变成2个赞
        like(postId, "user_001"); // 变成3个赞
        
        like(postId, "user_002"); // 变成2个赞
        
        // 获取 Top 3（带分数）
        Collection<ScoredEntry<String>> top3 = getTopNWithScores(postId, 3);
        
        LOGGER.info("排行榜 Top 3（带分数）:");
        int rank = 1;
        for (ScoredEntry<String> entry : top3) {
            LOGGER.info("  第{}名: {} - {} 个赞", rank++, entry.getValue(), entry.getScore());
        }
        
        // 验证排序正确性
        assert getUserRank(postId, "user_001") == 1 : "user_001 应该是第1名（3个赞）";
        assert getUserRank(postId, "user_002") == 2 : "user_002 应该是第2名（2个赞）";
        assert getUserRank(postId, "user_003") == 3 : "user_003 应该是第3名（1个赞）";
    }

    /**
     * 测试分页查询
     */
    @Test
    public void testPagination() {
        LOGGER.info("\n=== 测试分页查询 ===");
        
        String postId = TEST_POST_ID;
        
        // 创建10个用户
        for (int i = 1; i <= 10; i++) {
            String userId = "user_" + String.format("%03d", i);
            // 每个用户点赞数等于用户编号
            for (int j = 0; j < i; j++) {
                like(postId, userId);
            }
        }
        
        LOGGER.info("总人数: {}", getLeaderboardSize(postId));
        
        // 测试第1页（每页3条）
        Collection<String> page1 = getLeaderboardPage(postId, 1, 3);
        LOGGER.info("第1页（Top 3）: {}", page1);
        
        // 测试第2页
        Collection<String> page2 = getLeaderboardPage(postId, 2, 3);
        LOGGER.info("第2页: {}", page2);
        
        // 测试第3页
        Collection<String> page3 = getLeaderboardPage(postId, 3, 3);
        LOGGER.info("第3页: {}", page3);
        
        assert page1.size() == 3 : "第1页应该有3条数据";
        assert page2.size() == 3 : "第2页应该有3条数据";
        assert page3.size() == 3 : "第3页应该有3条数据";
    }

    /**
     * 测试并发点赞（模拟高并发场景）
     * 
     * 这个测试很重要，验证了：
     * 1. Redis 操作的原子性
     * 2. 数据一致性
     * 3. 并发安全性
     */
    @Test
    public void testConcurrentLikes() throws InterruptedException {
        LOGGER.info("\n=== 测试并发点赞 ===");
        
        String postId = TEST_POST_ID;
        String userId = "user_concurrent";
        
        int threadCount = 10;
        int likesPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 并发点赞
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < likesPerThread; j++) {
                        like(postId, userId);
                        successCount.incrementAndGet();
                    }
                    LOGGER.info("线程 {} 完成 {} 次点赞", threadId, likesPerThread);
                } catch (Exception e) {
                    LOGGER.error("线程 {} 执行失败", threadId, e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        // 验证最终结果
        Double finalCount = getUserLikeCount(postId, userId);
        int expectedCount = threadCount * likesPerThread;
        
        LOGGER.info("并发测试结果:");
        LOGGER.info("  成功点赞次数: {}", successCount.get());
        LOGGER.info("  最终点赞数: {}", finalCount);
        LOGGER.info("  期望点赞数: {}", expectedCount);
        
        assert finalCount == expectedCount : 
            String.format("最终点赞数应该是 %d，实际是 %s", expectedCount, finalCount);
        
        LOGGER.info("✓ 并发测试通过，数据一致性验证成功");
    }

    /**
     * 测试多个文章的排行榜（隔离性）
     */
    @Test
    public void testMultiplePosts() {
        LOGGER.info("\n=== 测试多个文章的排行榜 ===");
        
        String postId1 = "post_001";
        String postId2 = "post_002";
        
        // 给文章1点赞
        like(postId1, "user_001");
        like(postId1, "user_002");
        
        // 给文章2点赞
        like(postId2, "user_001");
        like(postId2, "user_003");
        
        // 验证两个文章的排行榜是独立的
        Collection<String> top1 = getTopN(postId1, 10);
        Collection<String> top2 = getTopN(postId2, 10);
        
        LOGGER.info("文章1的排行榜: {}", top1);
        LOGGER.info("文章2的排行榜: {}", top2);
        
        assert top1.size() == 2 : "文章1应该有2个用户";
        assert top2.size() == 2 : "文章2应该有2个用户";
        assert top1.contains("user_001") && top1.contains("user_002") : "文章1应该包含 user_001 和 user_002";
        assert top2.contains("user_001") && top2.contains("user_003") : "文章2应该包含 user_001 和 user_003";
        
        LOGGER.info("✓ 多文章排行榜隔离性验证成功");
    }

    /**
     * 综合测试：完整的点赞排行榜场景
     */
    @Test
    public void testCompleteScenario() {
        LOGGER.info("\n=== 综合测试：完整的点赞排行榜场景 ===");
        
        String postId = TEST_POST_ID;
        
        // 1. 初始状态：多个用户点赞
        LOGGER.info("步骤1: 多个用户点赞");
        like(postId, "user_001");
        like(postId, "user_002");
        like(postId, "user_003");
        like(postId, "user_001"); // user_001 再次点赞
        
        // 2. 查看排行榜
        LOGGER.info("步骤2: 查看排行榜");
        Collection<ScoredEntry<String>> top = getTopNWithScores(postId, 10);
        for (ScoredEntry<String> entry : top) {
            Integer rank = getUserRank(postId, entry.getValue());
            LOGGER.info("  排名 {}: {} - {} 个赞", rank, entry.getValue(), entry.getScore());
        }
        
        // 3. 查询特定用户信息
        LOGGER.info("步骤3: 查询特定用户信息");
        String userId = "user_001";
        Double likeCount = getUserLikeCount(postId, userId);
        Integer rank = getUserRank(postId, userId);
        LOGGER.info("  用户 {}: {} 个赞，排名第 {}", userId, likeCount, rank);
        
        // 4. 取消点赞
        LOGGER.info("步骤4: 取消点赞");
        unlike(postId, "user_001");
        
        // 5. 再次查看排行榜
        LOGGER.info("步骤5: 再次查看排行榜");
        top = getTopNWithScores(postId, 10);
        for (ScoredEntry<String> entry : top) {
            Integer rankAfter = getUserRank(postId, entry.getValue());
            LOGGER.info("  排名 {}: {} - {} 个赞", rankAfter, entry.getValue(), entry.getScore());
        }
        
        // 6. 验证最终状态
        Double finalCount = getUserLikeCount(postId, "user_001");
        Integer finalRank = getUserRank(postId, "user_001");
        LOGGER.info("最终状态 - user_001: {} 个赞，排名第 {}", finalCount, finalRank);
        
        assert finalCount == 1.0 : "user_001 最终应该有1个赞";
        LOGGER.info("✓ 综合测试通过");
    }
}
