package com.dxy.library.snowflake;

/**
 * Twitter_Snowflake
 * 雪花Id生成器
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

    public static long getWorkerId() {
        return ID_WORKER.getWorkerId();
    }

    public static long getDatacenterId() {
        return ID_WORKER.getDatacenterId();
    }

}
