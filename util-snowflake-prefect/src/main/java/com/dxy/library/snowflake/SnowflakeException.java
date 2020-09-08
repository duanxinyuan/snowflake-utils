package com.dxy.library.snowflake;

/**
 * @author duanxinyuan
 * 2020/5/22 19:19
 */
public class SnowflakeException extends RuntimeException{

    public SnowflakeException() {
        super();
    }

    public SnowflakeException(String message) {
        super(message);
    }

    public SnowflakeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnowflakeException(Throwable cause) {
        super(cause);
    }

    protected SnowflakeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
