package com.dxy.apache.dbutils.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.sql.SQLException;
import java.util.HashMap;

/**
 * 异常处理工具类
 * @author dxy
 * 2018/4/24 20:15
 */
public class ExceptionUtil {

    /**
     * 处理异常
     */
    public static void disposeError(Throwable throwable) {
        disposeError(throwable, null);
    }

    /**
     * 处理异常
     */
    public static void disposeError(Throwable throwable, String paramsJson) {
        disposeError(throwable, paramsJson, null, null);
    }

    /**
     * 处理异常
     */
    public static void disposeError(Throwable throwable, String paramsJson, String method, String url) {
        throwable.printStackTrace();
        SLog.info(ExceptionUtils.getFullStackTrace(throwable));
        SLog.info(processError(throwable, paramsJson, method, url));
    }


    /***
     *  异常处理   返回 json 格式
     * @param throwable  异常
     * @param paramsJson   参数   json 格式
     * @return string 格式的json
     */
    public static String processError(Throwable throwable, String paramsJson) {
        return processError(throwable, paramsJson, null, null);
    }

    /***
     *  异常处理   返回 json 格式
     * @param throwable  异常
     * @param paramsJson   参数   json 格式
     * @param httpMethod   http 请求方式
     * @param url   http地址
     * @return string 格式的json
     */
    public static String processError(Throwable throwable, String paramsJson, String httpMethod, String url) {
        HashMap<String, String> hashMap = Maps.newHashMap();
        hashMap.put("fullLog", StringUtils.substring(ExceptionUtils.getStackTrace(throwable), 4000));
        hashMap.put("cause", String.valueOf(ExceptionUtils.getCause(throwable)));
        if (throwable instanceof SQLException) {
            //错误Sql
            hashMap.put("errorSql", getSQLByCause(String.valueOf(throwable.getMessage())));
        } else {
            if (StringUtils.isNotEmpty(url)) {
                hashMap.put("url", url);
            }
            if (StringUtils.isNotEmpty(httpMethod)) {
                hashMap.put("httpMethod", httpMethod);
            }
        }
        hashMap.put("message", String.valueOf(throwable.getMessage()));
        hashMap.put("params", paramsJson);
        StackTraceElement[] stackTraces = throwable.getStackTrace();
        for (StackTraceElement s : stackTraces) {
            //只捕获类包名为com 开始的
            if (StringUtils.isNotEmpty(s.getClassName()) && s.getClassName().contains("com.")) {
                if (s.getClassName().contains("Service")) {
                    //捕获service
                    hashMap.put("service", s.getClassName() + ":" + s.getLineNumber());
                    hashMap.put("serviceMethod", s.getMethodName());
                } else if (s.getClassName().contains("Rest")) {
                    //捕获 rest
                    hashMap.put("rest", s.getClassName() + " : " + s.getLineNumber());
                    hashMap.put("restMethod", s.getMethodName());
                } else if (s.getClassName().contains("Dao")) {
                    //捕获 dao
                    hashMap.put("dao", s.getClassName() + " : " + s.getLineNumber());
                    hashMap.put("daoMethod", s.getMethodName());
                }
            }
        }
        return Joiner.on(" , ").withKeyValueSeparator(" = ").join(hashMap);
    }

    /**
     * 截取SQl
     * @param cause 错误原因，截取sql在 exception cause中的第一行
     */
    private static String getSQLByCause(String cause) {
        if (cause.contains("Update:")) {
            return cause.substring(cause.indexOf("Update:"));
        } else if (cause.contains("Insert:")) {
            return cause.substring(cause.indexOf("Insert:"));
        } else if (cause.contains("Query:")) {
            return cause.substring(cause.indexOf("Query:"));
        } else if (cause.contains("Delete:")) {
            return cause.substring(cause.indexOf("Delete:"));
        }
        return "";
    }

}