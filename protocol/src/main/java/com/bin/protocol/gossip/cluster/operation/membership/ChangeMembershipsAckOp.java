package com.bin.protocol.gossip.cluster.operation.membership;

import com.bin.protocol.gossip.cluster.membership.MembershipRecord;

import java.util.List;

public class ChangeMembershipsAckOp extends ChangeMembershipsOp {


    public ChangeMembershipsAckOp() {}

    public ChangeMembershipsAckOp(List<MembershipRecord> membership) {
       super(membership);
    }

    @Override
    public void doRun() {
        getService().onChangeMembershipAck(this);
    }
}