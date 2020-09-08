package com.dxy.library.snowflake;

import java.util.Map;

/**
 * Twitter_Snowflake
 * 雪花Id生成器
 * 增加了使用Redis分配workerId和datacenterId的逻辑，确保不会重复
 * 最多可以分配32*32=1024个workerId+datacenterId的组合
 * @author Twitter
 */
public class SnowflakeIdUtils {
    private static final SnowflakeIdWorker ID_WORKER = new SnowflakeIdWorker();

    /**
     * 生成雪花Id（18位）
     */
    public static long getAsLong() {
        return ID_WORKER.nextId();
    }

    /**
     * 生成雪花Id（18位）
     */
    public static String getAsString() {
        return String.valueOf(ID_WORKER.nextId());
    }

    /**
     * 获取全部的workerId和datacenterId集合
     * Map<localIp, Map<String, Long>>
     */
    public static Map<String, Map<String, Long>> getAllDataCenterWorkerId() {
        return ID_WORKER.getAllDataCenterWorkerId();
    }

    public static long getWorkerId() {
        return ID_WORKER.getWorkerId();
    }

    public static long getDatacenterId() {
        return ID_WORKER.getDatacenterId();
    }

}
