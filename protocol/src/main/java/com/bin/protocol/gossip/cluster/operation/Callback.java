package com.bin.protocol.gossip.cluster.operation;

public interface Callback<T> {

    void execute(T result);
}
