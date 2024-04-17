package com.bin.protocol.gossip.network;

import com.bin.protocol.gossip.common.Exceptions;
import com.bin.protocol.gossip.common.Address;

import java.util.StringJoiner;
import java.util.function.Function;

public class NetWorkConfig {

    // LAN cluster
    public static final int DEFAULT_CONNECT_TIMEOUT = 3_000;

    // WAN cluster (overrides default/LAN settings)
    public static final int DEFAULT_WAN_CONNECT_TIMEOUT = 10_000;

    // Local cluster working via loopback interface (overrides default/LAN settings)
    public static final int DEFAULT_LOCAL_CONNECT_TIMEOUT = 1_000;

    private int port;

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;


    private Function<Address, Address> addressMapper = Function.identity();

    public NetWorkConfig() {}

    public static NetWorkConfig defaultConfig() {
        return new NetWorkConfig();
    }


    public static NetWorkConfig defaultWanConfig() {
        return defaultConfig().connectTimeout(DEFAULT_WAN_CONNECT_TIMEOUT);
    }


    public static NetWorkConfig defaultLocalConfig() {
        return defaultConfig().connectTimeout(DEFAULT_LOCAL_CONNECT_TIMEOUT);
    }

    public int port() {
        return port;
    }

    public NetWorkConfig port(int port) {
        this.port = port;
        return this;
    }


    public int connectTimeout() {
        return connectTimeout;
    }


    public NetWorkConfig connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }



    public NetWorkConfig addressMapper(Function<Address, Address> addressMapper) {
        this.addressMapper = addressMapper;
        return this;
    }

    public Function<Address, Address> addressMapper() {
        return addressMapper;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", NetWorkConfig.class.getSimpleName() + "[", "]")
                .add("port=" + port)
                .add("connectTimeout=" + connectTimeout)
                .add("addressMapper=" + addressMapper)
                .toString();
    }


}
