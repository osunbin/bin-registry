package com.bin.registry.server.model;

import com.bin.registry.server.common.utils.JsonUtils;

import java.io.Serializable;


/**
 * 响应消息类
 */
public class JsonResult<T> implements Serializable {

    public static int SUCCESS_CODE = 1;

    public static int FAIL_CODE = 0;

    public static final String SUCCESS_MESSAGE = "SUCCESS";

    public static final String FAIL_MESSAGE = "FAILED";

    private int code = SUCCESS_CODE;//状态

    private String msg = SUCCESS_MESSAGE;//消息

    private T data;

    public JsonResult() {
    }

    public JsonResult(T data) {
        this.data = data;
    }

    public JsonResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static JsonResult ok(String msg, Object data) {
       return new JsonResult(SUCCESS_CODE,msg,data);
    }

    public static JsonResult ok() {
        return new JsonResult(SUCCESS_CODE,SUCCESS_MESSAGE,null);
    }


    public static JsonResult failed( String msg, Object data) {
        return new JsonResult(FAIL_CODE,msg,data);
    }


    public static JsonResult getFailedResult() {
        return new JsonResult(FAIL_CODE, FAIL_MESSAGE, null);
    }

    public int getCode() {
        return code;
    }

    public JsonResult setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public JsonResult setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Object getData() {
        return data;
    }

    public JsonResult setData(T data) {
        this.data = data;
        return this;
    }


    @Override
    public String toString() {
        return toJson();
    }

    public String toJson(){
        return JsonUtils.toJson(this);
    }

}
