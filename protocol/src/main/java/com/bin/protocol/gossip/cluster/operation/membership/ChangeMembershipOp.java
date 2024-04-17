package com.bin.protocol.gossip.cluster.operation.membership;

import com.bin.protocol.gossip.cluster.membership.MembershipRecord;
import com.bin.protocol.gossip.cluster.operation.Operation;

public class ChangeMembershipOp extends Operation {

   private MembershipRecord membershipRecord;

    public ChangeMembershipOp(MembershipRecord membershipRecord) {
        this.membershipRecord = membershipRecord;
    }

    public MembershipRecord getMembershipRecord() {
        return membershipRecord;
    }

    @Override
    public void doRun() {
        super.doRun();
    }
}
