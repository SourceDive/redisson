package org.redisson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redisson 分布式锁测试案例
 * 包含可重入锁、公平锁、读写锁、信号量等分布式同步工具
 *
 * @author zero
 * @date 25.09.21 Sun
 */
public class DistributedLockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockTest.class);
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
     * 测试基本分布式锁
     */
    @Test
    public void testBasicLock() {
        LOGGER.info("=== 开始测试基本分布式锁 ===");

        RLock lock = redissonClient.getLock("test:lock:basic");

        try {
            // 尝试获取锁，最多等待3秒，锁定10秒
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (acquired) {
                LOGGER.info("成功获取锁");

                // 模拟业务处理
                Thread.sleep(2000);
                LOGGER.info("业务处理完成");
            } else {
                LOGGER.warn("获取锁失败");
            }
        } catch (InterruptedException e) {
            LOGGER.error("锁操作被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                LOGGER.info("锁已释放");
            }
        }

        LOGGER.info("=== 基本分布式锁测试完成 ===");
    }

    /**
     * 测试可重入锁
     */
    @Test
    public void testReentrantLock() {
        LOGGER.info("=== 开始测试可重入锁 ===");

        RLock lock = redissonClient.getLock("test:lock:reentrant");

        try {
            // 第一次获取锁
            boolean acquired1 = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (acquired1) {
                LOGGER.info("第一次获取锁成功");

                // 第二次获取锁（可重入）
                boolean acquired2 = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (acquired2) {
                    LOGGER.info("第二次获取锁成功（可重入）");

                    // 模拟业务处理
                    Thread.sleep(1000);
                    LOGGER.info("业务处理完成");

                    // 释放第二次获取的锁
                    lock.unlock();
                    LOGGER.info("释放第二次获取的锁");
                }

                // 释放第一次获取的锁
                lock.unlock();
                LOGGER.info("释放第一次获取的锁");
            }
        } catch (InterruptedException e) {
            LOGGER.error("锁操作被中断", e);
            Thread.currentThread().interrupt();
        }

        LOGGER.info("=== 可重入锁测试完成 ===");
    }

    /**
     * 测试公平锁
     */
    @Test
    public void testFairLock() {
        LOGGER.info("=== 开始测试公平锁 ===");

        RLock fairLock = redissonClient.getFairLock("test:lock:fair");

        try {
            boolean acquired = fairLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (acquired) {
                LOGGER.info("成功获取公平锁");

                // 模拟业务处理
                Thread.sleep(2000);
                LOGGER.info("业务处理完成");
            } else {
                LOGGER.warn("获取公平锁失败");
            }
        } catch (InterruptedException e) {
            LOGGER.error("锁操作被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
                LOGGER.info("公平锁已释放");
            }
        }

        LOGGER.info("=== 公平锁测试完成 ===");
    }

    /**
     * 测试读写锁
     */
    @Test
    public void testReadWriteLock() {
        LOGGER.info("=== 开始测试读写锁 ===");

        RLock readLock = redissonClient.getReadWriteLock("test:lock:rw").readLock();
        RLock writeLock = redissonClient.getReadWriteLock("test:lock:rw").writeLock();

        try {
            // 获取读锁
            boolean readAcquired = readLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (readAcquired) {
                LOGGER.info("成功获取读锁");

                // 模拟读操作
                Thread.sleep(1000);
                LOGGER.info("读操作完成");

                readLock.unlock();
                LOGGER.info("读锁已释放");
            }

            // 获取写锁
            boolean writeAcquired = writeLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (writeAcquired) {
                LOGGER.info("成功获取写锁");

                // 模拟写操作
                Thread.sleep(1000);
                LOGGER.info("写操作完成");

                writeLock.unlock();
                LOGGER.info("写锁已释放");
            }
        } catch (InterruptedException e) {
            LOGGER.error("锁操作被中断", e);
            Thread.currentThread().interrupt();
        }

        LOGGER.info("=== 读写锁测试完成 ===");
    }

    /**
     * 测试多线程并发锁竞争
     */
    @Test
    public void testConcurrentLockCompetition() throws InterruptedException {
        LOGGER.info("=== 开始测试多线程并发锁竞争 ===");

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                RLock lock = redissonClient.getLock("test:lock:concurrent");
                try {
                    boolean acquired = lock.tryLock(1, 5, TimeUnit.SECONDS);
                    if (acquired) {
                        LOGGER.info("线程 {} 成功获取锁", threadId);
                        successCount.incrementAndGet();

                        // 模拟业务处理
                        Thread.sleep(2000);
                        LOGGER.info("线程 {} 业务处理完成", threadId);

                        lock.unlock();
                        LOGGER.info("线程 {} 释放锁", threadId);
                    } else {
                        LOGGER.warn("线程 {} 获取锁失败", threadId);
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("线程 {} 被中断", threadId, e);
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        LOGGER.info("并发测试结果 - 成功: {}, 失败: {}", successCount.get(), failCount.get());
        LOGGER.info("=== 多线程并发锁竞争测试完成 ===");
    }

    /**
     * 测试锁自动续期
     */
    @Test
    public void testLockAutoRenewal() {
        LOGGER.info("=== 开始测试锁自动续期 ===");

        RLock lock = redissonClient.getLock("test:lock:renewal");

        try {
            // 获取锁，设置较短的锁定时间
            boolean acquired = lock.tryLock(3, 3, TimeUnit.SECONDS);
            if (acquired) {
                LOGGER.info("成功获取锁，锁定时间3秒");

                // 模拟长时间业务处理（超过锁定时间）
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(1000);
                    LOGGER.info("业务处理中... {} 秒", i + 1);

                    // 检查锁是否仍然持有
                    if (lock.isHeldByCurrentThread()) {
                        LOGGER.info("锁仍然有效");
                    } else {
                        LOGGER.warn("锁已失效");
                        break;
                    }
                }
            } else {
                LOGGER.warn("获取锁失败");
            }
        } catch (InterruptedException e) {
            LOGGER.error("锁操作被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                LOGGER.info("锁已释放");
            }
        }

        LOGGER.info("=== 锁自动续期测试完成 ===");
    }

    /**
     * 测试信号量
     */
    @Test
    public void testSemaphore() throws InterruptedException {
        LOGGER.info("=== 开始测试信号量 ===");

        // 创建信号量，允许3个并发
        org.redisson.api.RSemaphore semaphore = redissonClient.getSemaphore("test:semaphore:resource");
        semaphore.trySetPermits(3);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger acquiredCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // 尝试获取信号量
                    boolean acquired = semaphore.tryAcquire(2L, TimeUnit.SECONDS);
                    if (acquired) {
                        LOGGER.info("线程 {} 成功获取信号量", threadId);
                        acquiredCount.incrementAndGet();

                        // 模拟资源使用
                        Thread.sleep(3000);
                        LOGGER.info("线程 {} 资源使用完成", threadId);

                        // 释放信号量
                        semaphore.release();
                        LOGGER.info("线程 {} 释放信号量", threadId);
                    } else {
                        LOGGER.warn("线程 {} 获取信号量失败", threadId);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("线程 {} 被中断", threadId, e);
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        LOGGER.info("信号量测试结果 - 成功获取: {}", acquiredCount.get());
        LOGGER.info("=== 信号量测试完成 ===");
    }
}
