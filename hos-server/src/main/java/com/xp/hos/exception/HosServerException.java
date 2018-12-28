package com.xp.hos.exception;

/**
 * hosserver异常类
 */
public class HosServerException extends HosException {

    private int code;
    private String msg;

    public HosServerException(int code,String msg,Throwable cause){
        super(msg,cause);
        this.code=code;
        this.msg=msg;
    }

    public HosServerException(int code,String msg){
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
