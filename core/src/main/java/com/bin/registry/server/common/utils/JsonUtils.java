package com.bin.registry.server.common.utils;

import com.bin.registry.server.model.CallerInstance;
import com.bin.registry.server.model.Node;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

public class JsonUtils {

    static Gson gson = new Gson();

    public static String toJson(Object obj) {
        if (Objects.isNull(obj)) return "";
        try {

            return gson.toJson(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static CallerInstance readCaller(ByteBuf byteBuf) {
        Type type = new TypeToken<CallerInstance>(){}.getType();
        CallerInstance caller = null;
        try(Reader reader =new InputStreamReader(new ByteBufInputStream(byteBuf))){
            caller = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return caller;
    }


    public static Node  readNode(ByteBuf byteBuf) {
        Type type = new TypeToken<Node>(){}.getType();
        Node node = null;
        try(Reader reader =new InputStreamReader(new ByteBufInputStream(byteBuf))){
            node = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return node;
    }

    public static Map<String,Object> jsonToMap(String json) {
        if (Objects.isNull(json)) return null;
        try {
            Type type = new TypeToken<Map<String,Object>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
