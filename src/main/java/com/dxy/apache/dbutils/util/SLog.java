package com.dxy.apache.dbutils.util;

import java.util.logging.Logger;

/**
 * Log工具类
 * @author dxy
 * 2018/4/24 20:15
 */
public class SLog {
    private static String TAG = "17SNK-Logger";
    private static Logger logger;

    private static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(TAG);
        }
        return logger;
    }

    public static void info(String s) {
        getLogger().info(s);
    }


    public static void info(String sourceClass, String sourceMethod, String s) {
        getLogger().info(sourceClass + ":" + sourceMethod + "-" + s);
    }
}
