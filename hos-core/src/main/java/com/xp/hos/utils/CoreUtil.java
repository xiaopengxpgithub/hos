package com.xp.hos.utils;

import java.security.MessageDigest;
import java.util.UUID;

public class CoreUtil {

    //系统管理员用户
    public final static String SYSTEM_USER = "SuperAdmin";

    //password md5加密
    public static String getPasswordMD5(String str) {
        String restr = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(str.getBytes());
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bytes) {
                int bt = b & 0xff;

                if (bt<16){
                    stringBuffer.append(0);
                }
                stringBuffer.append(Integer.toHexString(bt));
            }
            restr=stringBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return restr;
    }

    //获取uuid字符串
    public static String getUUIDStr() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}

