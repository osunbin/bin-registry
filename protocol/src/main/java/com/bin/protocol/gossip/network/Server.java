package com.bin.protocol.gossip.network;


import com.bin.protocol.gossip.cluster.operation.Operation;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.net.SocketAddress;

public interface Server {

     Schema<Operation> operationSchema
            = RuntimeSchema.getSchema(Operation.class);


    SocketAddress start();

    void close();
}
