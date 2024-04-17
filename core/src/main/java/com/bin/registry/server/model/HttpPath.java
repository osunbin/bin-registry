package com.bin.registry.server.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   服务关闭,
 *   服务启动,
 *   修改权重,
 *   服务删除,
 *   服务查询，
 *   调用方角度查询-有哪些下游服务
 */
public enum HttpPath {
    REGISTRY_PATH("post","/node/registry","服务注册"),
    DISCOVERY_PATH("post","/node/discovery","服务发现"),
    NODE_LISTS("get","/node/lists","页面展示"),
    NODES_SERVER("get","/node/server","服务查询"),
    NODE_CALLER("get","/node/caller","服务查询"),
    NODE_OPEN("get","/node/open","启动服务"),
    NODE_CLOSE("get","/node/close","服务关闭"),
    NODE_DELETE("get","/node/delete","服务删除");


    private String httpMethod;

    private String path;

    private String doc;

    static Map<String,HttpPath> pathMapping = new HashMap<>();

    static {
        HttpPath[] values = values();
        for (HttpPath httpPath : values) {
            pathMapping.put(httpPath.getPath(),httpPath);
        }
    }


    public static HttpPath getHttpPath(String path) {
        return pathMapping.get(path);
    }


    HttpPath(String httpMethod, String path, String doc) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.doc = doc;
    }

    public String getPath() {
        return path;
    }

    private static List<Map<String, String>> DOC_MAN;

    public static List<Map<String, String>> fetchAllDoc() {
        if (DOC_MAN == null) {
            synchronized (HttpPath.class) {
                if (DOC_MAN == null) {
                    List<Map<String, String>> docs = new ArrayList<>();
                    HttpPath[] values = values();
                    for (HttpPath path : values) {
                        Map<String, String> maps = new HashMap<>();
                        maps.put("path", path.path);
                        maps.put("httpMethod", path.httpMethod);
                        maps.put("doc", path.doc);
                        docs.add(maps);
                    }
                    DOC_MAN = docs;
                }
            }
        }
        return DOC_MAN;
    }



}
