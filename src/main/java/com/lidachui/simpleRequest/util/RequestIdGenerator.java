package com.lidachui.simpleRequest.util;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
/**
 * 高性能 RequestId 生成工具。
 * 基于时间戳、机器 ID 和序列号。
 * @author: lihuijie
 * @date: 2024/11/24 0:37
 * @version: 1.0
 */
public class RequestIdGenerator {

    // 起始时间戳（自定义一个固定时间点，例如：2024-11-24 00:00:00 UTC）
    private static final long START_EPOCH = 1700841600000L; // 毫秒级时间戳

    // 机器 ID（0~31，假设最大支持 32 个节点）
    private static final int MACHINE_ID = getMachineId();

    // 数据中心 ID（0~31，假设最大支持 32 个数据中心）
    private static final int DATACENTER_ID = 0;

    // 最大序列号（每毫秒支持 4096 个请求）
    private static final long MAX_SEQUENCE = 4095L;

    // 每部分的位数
    private static final long MACHINE_ID_BITS = 5L;    // 机器 ID 5 位
    private static final long DATACENTER_ID_BITS = 5L;  // 数据中心 ID 5 位
    private static final long SEQUENCE_BITS = 12L;      // 序列号 12 位

    // 每部分的偏移量
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;

    // 序列号掩码
    private static final long SEQUENCE_MASK = MAX_SEQUENCE;

    // 上次生成的时间戳
    private static volatile long lastTimestamp = -1L;

    // 当前毫秒内的序列号
    private static final AtomicLong sequence = new AtomicLong(0);

    /**
     * 生成 Snowflake 风格的唯一 ID。
     *
     * @return 唯一的 Request ID
     */
    public static String generate() {
        long currentTimestamp = getCurrentEpochMilli();

        // 同一毫秒内，序列号自增
        long nextSequence = sequence.get();
        if (currentTimestamp == lastTimestamp) {
            // 序列号递增
            nextSequence = sequence.incrementAndGet();
            if (nextSequence > MAX_SEQUENCE) {
                // 序列号溢出，等待下一毫秒
                while (currentTimestamp <= lastTimestamp) {
                    currentTimestamp = getCurrentEpochMilli();
                }
                sequence.set(0);
            }
        } else {
            // 时间戳变化，重置序列号
            sequence.set(0);
        }

        lastTimestamp = currentTimestamp;

        // 生成 Snowflake ID
        long id = ((currentTimestamp - START_EPOCH) << TIMESTAMP_SHIFT) // 时间戳部分
                | (DATACENTER_ID << DATACENTER_ID_SHIFT)                  // 数据中心 ID 部分
                | (MACHINE_ID << MACHINE_ID_SHIFT)                        // 机器 ID 部分
                | nextSequence;                                           // 序列号部分

        return String.format("REQ-%s", Long.toString(id, 36).toUpperCase());
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
}
