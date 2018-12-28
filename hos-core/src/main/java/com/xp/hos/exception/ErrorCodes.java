package com.xp.hos.exception;

/**
 * 错误状态码
 */
public interface ErrorCodes {

    public static final int ERROR_PERMISSION_DENIED = 403;
    public static final int ERROR_FILE_NOT_FOUND = 404;
    public static final int ERROR_HBASE = 500;
    public static final int ERROR_HDFS = 501;
}
