package com.dxy.library.snowflake;

import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

/**
 * Twitter_Snowflake
 * 雪花Id生成器
 * @author Twitter
 */
public class SnowflakeIdUtils {
    private static final long DEF_WORKER_ID;
    private static final long DEF_DATA_CENTER_ID;
    private static List<String> hardwareAddresses;
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final SnowflakeIdWorker ID_WORKER;

    static {
        int macPiece = RANDOM.nextInt();
        int processPiece = RANDOM.nextInt();
        try {
            List<String> addresses = getHardwareAddresses();
            StringBuilder buffer = new StringBuilder();
            for (String addr : addresses) {
                buffer.append(addr);
                buffer.append("&");
            }
            macPiece = System.identityHashCode(buffer.toString());
        } catch (Exception ignore) {
        }
        try {
            String process = ManagementFactory.getRuntimeMXBean().getName();
            processPiece = System.identityHashCode(process);
        } catch (Throwable ignore) {
        }
        DEF_WORKER_ID = (long) (processPiece & 31);
        DEF_DATA_CENTER_ID = (long) (macPiece & 31);
        ID_WORKER = new SnowflakeIdWorker(DEF_WORKER_ID, DEF_DATA_CENTER_ID);
    }

    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    /**
     * 转16进制
     */
    private static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }

    private static List<String> getHardwareAddresses() {
        if (hardwareAddresses != null) {
            return hardwareAddresses;
        }
        hardwareAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            if (ifs != null) {
                while (ifs.hasMoreElements()) {
                    NetworkInterface networkInterface = ifs.nextElement();
                    byte[] hardware = networkInterface.getHardwareAddress();
                    if ((hardware != null) && (hardware.length == 6) && (hardware[1] != -1)) {
                        String hardwareAddr = printHexBinary(hardware);
                        hardwareAddresses.add(hardwareAddr);
                    }
                }
            }
        } catch (SocketException ignore) {
        }
        return hardwareAddresses;
    }

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

}
