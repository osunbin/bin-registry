package com.bin.registry.server.model;


import java.util.Set;

public class CallerInstance {
    private String callerName;
    private  Set<String> serverInfo;

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public Set<String> getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(Set<String> serverInfo) {
        this.serverInfo = serverInfo;
    }
}
