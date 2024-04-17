package com.bin.protocol.gossip.cluster.operation.pingfailuredetector;

import com.bin.protocol.gossip.Member;

public class PingAckOp extends AbstractPingAck{



    private Member from;

    private Member to;




    public PingAckOp(Member from, Member to, boolean ack) {
        super(ack);
        this.from = from;
        this.to = to;
    }

    @Override
    public void doRun() {

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
