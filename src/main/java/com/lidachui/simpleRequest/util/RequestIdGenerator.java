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

    // 起始时间戳（自定义一个固定时间点）
    private static final long START_EPOCH = 1700841600L; // 示例：2024-11-24 00:00:00 UTC 的秒级时间戳

    // 机器 ID（用来标识当前节点，0~31）
    private static final int MACHINE_ID = getMachineId();

    // 最大序列号（每秒支持 4096 个请求）
    private static final long MAX_SEQUENCE = 4095L;

    // 最后生成的时间戳
    private static volatile long lastTimestamp = -1L;

    // 当前时间戳对应的序列号
    private static final AtomicLong sequence = new AtomicLong(0);

    /**
     * 生成 Request ID。
     *
     * @return 唯一的 Request ID
     */
    public static String generate() {
        long currentTimestamp = getCurrentEpochSecond();

        synchronized (RequestIdGenerator.class) {
            // 如果当前时间戳和最后生成的时间戳相同，递增序列号
            if (currentTimestamp == lastTimestamp) {
                long nextSequence = sequence.incrementAndGet();
                if (nextSequence > MAX_SEQUENCE) {
                    // 序列号超过最大值，等待下一个秒级时间戳
                    while (currentTimestamp <= lastTimestamp) {
                        currentTimestamp = getCurrentEpochSecond();
                    }
                    sequence.set(0);
                }
            } else {
                // 时间戳变动，重置序列号
                sequence.set(0);
            }

            lastTimestamp = currentTimestamp;

            // 组合生成 Request ID
            long id = ((currentTimestamp - START_EPOCH) << 17) // 时间戳部分（左移 17 位）
                    | (MACHINE_ID << 12)                       // 机器 ID 部分（左移 12 位）
                    | sequence.get();                          // 序列号部分
            return String.format("REQ-%s", Long.toString(id, 36).toUpperCase());
        }
    }

    /**
     * 获取当前秒级时间戳。
     */
    private static long getCurrentEpochSecond() {
        return Instant.now().getEpochSecond();
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
