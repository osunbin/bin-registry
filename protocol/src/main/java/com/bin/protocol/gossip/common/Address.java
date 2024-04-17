package com.bin.protocol.gossip.common;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Address  {

    private static final Pattern ADDRESS_FORMAT = Pattern.compile("(?<host>^.*):(?<port>\\d+$)");

    /**
     * ip地址
     */
    private final String host;

    /**
     * 端口
     */
    private final int port;

    private Address(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public static Address from(String hostAndPort) {

        if (hostAndPort == null && hostAndPort.trim().equals("")) {
            throw new NullPointerException("hostAndPort can't be blank !");
        }
        Matcher matcher = ADDRESS_FORMAT.matcher(hostAndPort);
        if (!matcher.find()) {
            throw new IllegalArgumentException("hostAndPort has wrong format");
        }
        String host = matcher.group(1);
        if (host == null && host.trim().equals("")) {
            throw new NullPointerException("host can't be empty！");
        }
        String host1 = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) ? Addressing.getLocalIpAddress().getHostAddress() : host;
        int port = Integer.parseInt(matcher.group(2));
        return new Address(host1, port);
    }

    public static Address create(String host, int port) {
        return new Address(host, port);
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }


    public static InetAddress getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        return port == address.port && Objects.equals(host, address.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }



}
