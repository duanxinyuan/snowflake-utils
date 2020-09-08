package com.dxy.library.snowflake;

import com.google.common.collect.Maps;
import com.dxy.library.cache.redis.RedisCache;
import com.dxy.library.util.config.ConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 * Twitter_Snowflake
 * SnowFlake的结构如下(每部分用-分开):
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 * 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，
 * 由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。
 * 41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69
 * 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号
 * 加起来刚好64位，为一个Long型。
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，
 * 经测试，SnowFlake每秒能够产生26万ID左右。
 * @author Twitter
 */
@Slf4j
final class SnowflakeIdWorker {

    /**
     * 开始时间截 (2015-01-01)
     */
    private final long twepoch = 1488297600000L;
    /**
     * 机器id所占的位数
     */
    private final long workerIdBits = 5L;
    /**
     * 数据标识id所占的位数
     */
    private final long datacenterIdBits = 5L;
    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);
    /**
     * 支持的最大数据标识id，结果是31
     */
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    /**
     * 序列在id中占的位数
     */
    private final long sequenceBits = 12L;
    /**
     * 机器ID向左移12位
     */
    private final long workerIdShift = sequenceBits;
    /**
     * 数据标识id向左移17位(12+5)
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    /**
     * 时间截向左移22位(5+5+12)
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private final long sequenceMask = ~(-1L << sequenceBits);
    /**
     * 工作机器ID(0~31)
     */
    private long workerId;
    /**
     * 数据中心ID(0~31)
     */
    private long datacenterId;
    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;
    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;
    /**
     * 可以接受的时间回拨最大毫秒数
     */
    private long maxBackwardMillis = 1000;

    /**
     * Redis 模块名
     */
    private final String REDIS_MODULE = "snowflake";

    /**
     * 缓存workerId_datacenterId的Redis Key
     */
    private final String DATA_CENTER_WORKER_ID_KEY;

    SnowflakeIdWorker() {
        String module = ConfigUtils.getAsString("snowflake.module");
        if (StringUtils.isEmpty(module)) {
            throw new SnowflakeException("缺少snowflake.module配置");
        }
        DATA_CENTER_WORKER_ID_KEY = "snowflake:workerId_datacenterId:" + module;
        String dataCenterWorkerId = getDataCenterWorkerId();
        String[] split = dataCenterWorkerId.split("_");
        long workerId = NumberUtils.toLong(split[0]);
        long datacenterId = NumberUtils.toLong(split[1]);
        checkAndSetWorkerIdAndDatacenterId(workerId, datacenterId);
    }

    /**
     * 获取全部的workerId和datacenterId集合
     */
    public Map<String, Map<String, Long>> getAllDataCenterWorkerId() {
        Map<String, String> map = RedisCache.name(REDIS_MODULE).hgetAll(DATA_CENTER_WORKER_ID_KEY);
        return map.keySet().stream().sorted(String::compareTo).collect(Collectors.toMap(map::get, key -> {
            Map<String, Long> linkedHashMap = Maps.newLinkedHashMap();
            String[] split = key.split("_");
            linkedHashMap.put("workerId", NumberUtils.toLong(split[0]));
            linkedHashMap.put("datacenterId", NumberUtils.toLong(split[1]));
            return linkedHashMap;
        }, (k1, k2) -> k1, LinkedHashMap::new));
    }

    /**
     * 使用Redis的hash的原子性方法获取workerId和datacenterId
     * @return workerId_datacenterId
     */
    private String getDataCenterWorkerId() {
        //根据机器IP分配 workerId 和 datacenterId
        String localIp = getLocalIp();
        log.info("snowflake localIp: {}", localIp);
        Map<String, String> map = RedisCache.name(REDIS_MODULE).hgetAll(DATA_CENTER_WORKER_ID_KEY);
        if (map.containsValue(localIp)) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getValue().equals(localIp)) {
                    return entry.getKey();
                }
            }
        }
        for (long i = 0; i <= maxWorkerId; i++) {
            for (long j = 0; j < maxDatacenterId; j++) {
                String key = i + "_" + j;
                Long hsetnx = RedisCache.name(REDIS_MODULE).hsetnx(DATA_CENTER_WORKER_ID_KEY, key, localIp);
                if (hsetnx == 1) {
                    return key;
                }
            }
        }
        throw new SnowflakeException("初始化workerId和datacenterId失败");
    }

    private void checkAndSetWorkerIdAndDatacenterId(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new SnowflakeException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new SnowflakeException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.info("snowflake workerId: {}", workerId);
        log.info("snowflake datacenterId: {}", datacenterId);
    }
    // ==============================Methods==========================================

    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return SnowflakeId
     */
    synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            //如果时钟回拨在可接受范围内, 直接等待
            if (timestamp - lastTimestamp <= maxBackwardMillis) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(maxBackwardMillis));
                timestamp = System.currentTimeMillis();
            }
            if (timestamp < lastTimestamp) {
                throw new SnowflakeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
            }
        }
        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒，直到获得新的时间戳
                long millis;
                while ((millis = System.currentTimeMillis()) <= lastTimestamp) {
                    timestamp = millis;
                }
            }
        } else {
            //时间戳改变，毫秒内序列重置
            sequence = 0L;
        }
        //上次生成ID的时间截
        lastTimestamp = timestamp;
        //移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift) | (workerId << workerIdShift) | sequence;
    }

    /**
     * 获取 maxWorkerId
     */
    protected static long getWorkerId(long dataCenterId, long maxWorkerId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dataCenterId);
        //用当前JVM进程的PID作为workerId
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (!name.isEmpty()) {
            //获取jvm pid
            stringBuilder.append(name.split("@")[0]);
        }
        //MAC + PID 的 hashcode 获取16个低位
        return (stringBuilder.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * 数据标识id部分
     */
    protected static long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    id = ((0x000000FF & (long) mac[mac.length - 1]) | (0x0000FF00 & (((long) mac[mac.length - 2]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            log.error(" getDatacenterId error", e);
        }
        return id;
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    /**
     * 获取本机IP
     */
    private static String getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces(); enumeration.hasMoreElements(); ) {
                //遍历所有的网卡
                NetworkInterface item = enumeration.nextElement();
                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    if (item.isLoopback() || !item.isUp()) {
                        // 如果是回环和虚拟网络地址的话继续
                        continue;
                    }
                    if (address.getAddress() instanceof Inet4Address) {
                        Inet4Address inet4Address = (Inet4Address) address.getAddress();
                        //只获取ipv4地址
                        return inet4Address.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new SnowflakeException(e);
        }
    }

}
