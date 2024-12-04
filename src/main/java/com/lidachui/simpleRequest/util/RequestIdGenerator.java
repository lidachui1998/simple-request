package com.lidachui.simpleRequest.util;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
/**
 * 高性能 RequestId 生成工具。
 * 基于时间戳、机器 ID 和序列号。
 * @author: lihuijie
 * @date: 2024/11/24 0:37
 * @version: 1.0
 */
public class RequestIdGenerator {

    // 常量定义
    private static final long START_EPOCH = 1700841600000L; // 起始时间戳
    private static final int MACHINE_ID = getMachineId();
    private static final int DATACENTER_ID = 0;
    private static final long MAX_SEQUENCE = 4095L;
    private static final long MACHINE_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = MAX_SEQUENCE;
    private static final int RANDOM_BITS = 32;
    private static final Random random = new Random();

    // 状态变量
    private static volatile long lastTimestamp = -1L;
    private static final AtomicLong sequence = new AtomicLong(0);

    /**
     * 生成 Snowflake 风格的唯一 ID。
     *
     * @return 唯一的 Request ID
     */
    public static synchronized String generate() {
        long currentTimestamp = getCurrentEpochMilli();

        if (currentTimestamp == lastTimestamp) {
            long nextSequence = sequence.incrementAndGet() & SEQUENCE_MASK;
            if (nextSequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence.set(0);
        }

        lastTimestamp = currentTimestamp;
        long id = ((currentTimestamp - START_EPOCH) << TIMESTAMP_SHIFT)
            | (DATACENTER_ID << DATACENTER_ID_SHIFT)
            | (MACHINE_ID << MACHINE_ID_SHIFT)
            | sequence.get();

        long randomPart = random.nextLong() & ((1L << RANDOM_BITS) - 1);

        String idString = Long.toString(id, 36).toUpperCase();
        String randomString = Long.toString(randomPart, 36).toUpperCase();

        return String.format("REQ-%s-%s", idString, randomString);
    }

    /**
     * 获取当前毫秒级时间戳。
     */
    private static long getCurrentEpochMilli() {
        return Instant.now().toEpochMilli();
    }

    /**
     * 获取机器 ID（0~31）。
     * 可通过环境变量或配置文件指定。
     */
    private static int getMachineId() {
        String machineId = System.getenv("MACHINE_ID");
        if (machineId != null) {
            try {
                int id = Integer.parseInt(machineId);
                if (id >= 0 && id <= 31) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 0; // 默认值
    }

    /**
     * 等待下一毫秒。
     */
    private static long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = getCurrentEpochMilli();
        }
        return currentTimestamp;
    }
}
