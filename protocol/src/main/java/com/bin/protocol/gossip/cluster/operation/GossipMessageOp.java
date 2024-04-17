package com.bin.protocol.gossip.cluster.operation;

import com.bin.protocol.gossip.Message;

public class GossipMessageOp extends Operation{

    private Message message;


    public GossipMessageOp(Message message) {
        this.message = message;
    }


    public Message getMessage() {
        return message;
    }

    @Override
    public void doRun() {
        getService().onMessage(this);
    }
}
