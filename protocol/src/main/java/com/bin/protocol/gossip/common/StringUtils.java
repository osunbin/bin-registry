package com.bin.protocol.gossip.common;

public class StringUtils {

    public static boolean isEmpty(String value) {
        if (value == null || value.trim().equals("")) {
            return true;
        }
        return false;
    }
}
