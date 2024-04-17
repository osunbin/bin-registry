package com.bin.registry.server.common.utils;


import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

    public static String md5(String value) {

        MessageDigest md = null;// 生成一个MD5加密计算摘要
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        md.update(value.getBytes(StandardCharsets.UTF_8));// 计算md5函数
        /**
         * digest()最后确定返回md5 hash值，返回值为8位字符串。
         * 因为md5 hash值是16位的hex值，实际上就是8位的字符
         * BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
         * 一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
         *  e10adc3949ba59abbe56e057f20f883e
         */
        return new BigInteger(1, md.digest()).toString(16);// 16是表示转换为16进制数
    }
}
