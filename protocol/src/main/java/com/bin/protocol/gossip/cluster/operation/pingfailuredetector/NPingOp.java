package com.bin.protocol.gossip.cluster.operation.pingfailuredetector;

import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.operation.Operation;

public class NPingOp extends Operation {


    private Member from;

    private Member to;

    private Member originalIssuer;

    public NPingOp(Member from, Member to,Member originalIssuer) {
        this.from = from;
        this.to = to;
        this.originalIssuer = originalIssuer;
    }

    @Override
    public void doRun() {
        getService().onNPing(this);
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

    public Member getOriginalIssuer() {
        return originalIssuer;
    }

    public void setOriginalIssuer(Member originalIssuer) {
        this.originalIssuer = originalIssuer;
    }
}

