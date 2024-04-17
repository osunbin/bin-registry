package com.bin.protocol.gossip.cluster.operation.pingfailuredetector;

import com.bin.protocol.gossip.cluster.operation.Operation;

public class AbstractPingAck extends Operation {

    private boolean ack;


    public AbstractPingAck(boolean ack) {
        this.ack = ack;
    }

    public boolean isAck() {
        return ack;
    }

    public void setAck(boolean ack) {
        this.ack = ack;
    }
}
