package com.xp.hos.exception;

/**
 * 用户异常处理类
 */
public class UserMsgException extends HosException {

    private int code;
    private String msg;

    public UserMsgException(int code,String msg,Throwable cause){
        super(msg,cause);
        this.code=code;
        this.msg=msg;
    }

    public UserMsgException(int code,String msg){
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
