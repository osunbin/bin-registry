package com.bin.protocol.gossip.cluster.operation.pingfailuredetector;

import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.operation.Operation;

public class PingOp extends Operation {


    private Member from;

    private Member to;



    public PingOp(Member from, Member to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void doRun() {
        getService().onPing(this);
    }

    public Member getFrom() {
        return from;
    }

    public void setFrom(Member from) {
        this.from = from;
    }

    public Member getTo() {
        return to;
    }

    public void setTo(Member to) {
        this.to = to;
    }
}
