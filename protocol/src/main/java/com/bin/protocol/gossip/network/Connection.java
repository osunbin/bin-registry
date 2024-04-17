package com.bin.protocol.gossip.network;

import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.common.Address;


import java.io.IOException;

public interface Connection {


    void send(Address address, Operation op) throws IOException;


    void close();
}
