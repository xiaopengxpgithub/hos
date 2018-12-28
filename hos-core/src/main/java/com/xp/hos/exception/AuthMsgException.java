package com.xp.hos.exception;

/**
 * 权限异常类
 */
public class AuthMsgException extends HosException{

    private int code;
    private String msg;

    public AuthMsgException(int code,String msg,Throwable cause){
        super(msg,cause);
        this.code=code;
        this.msg=msg;
    }

    public AuthMsgException(int code,String msg){
        super(msg,null);
        this.code=code;
        this.msg=msg;
    }

    public int getCode(){
        return code;
    }

    public String getMsg(){
        return msg;
    }

    @Override
    public int errorCode() {
        return this.code;
    }
}
