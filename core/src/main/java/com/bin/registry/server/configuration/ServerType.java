package com.bin.registry.server.configuration;

import java.util.HashMap;
import java.util.Map;

public enum ServerType {
    UNKNOWN(99,"unknown","未知"),
    VM(0,"VM","虚拟机"),
    SERVER(1,"server","物理机"),
    DOCKER(2,"docker","docker镜像");

    private int code;

    private String value;

    private String desc;

    ServerType(int code, String value, String desc) {
        this.code = code;
        this.value = value;
        this.desc = desc;
    }

    private static Map<Integer, ServerType> codeMapping = new HashMap<>();

    private static Map<String, ServerType> valueMapping = new HashMap<>();
    static {
        for (ServerType e : values()) {
            codeMapping.put(e.getCode(), e);
            valueMapping.put(e.getValue(), e);
        }
    }

    public static ServerType from(Integer code){
        return codeMapping.get(code);
    }

    public static ServerType fromValue(String value) {
        return valueMapping.get(value);
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }
    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ServerType{" + "value='" + value + '\'' + ", desc='" + desc + '\'' + '}';
    }
}