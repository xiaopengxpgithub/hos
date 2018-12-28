package com.xp.hos.security;

import com.xp.hos.pojo.UserInfo;

public class ContextUtil {
    public final static String SESSION_KEY="USER_TOKEN";

    private static ThreadLocal<UserInfo> threadLocal=new ThreadLocal<UserInfo>();

    public static UserInfo getCurrentUser(){
        return  threadLocal.get();
    }

    public static void setCurrentUser(UserInfo userInfo){
        threadLocal.set(userInfo);
    }

    public static void clear(){
        threadLocal.remove();
    }
}
