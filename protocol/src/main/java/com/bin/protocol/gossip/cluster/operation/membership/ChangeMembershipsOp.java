package com.bin.protocol.gossip.cluster.operation.membership;

import com.bin.protocol.gossip.cluster.membership.MembershipRecord;
import com.bin.protocol.gossip.cluster.operation.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChangeMembershipsOp extends Operation {

    private List<MembershipRecord> membership;

    public ChangeMembershipsOp() {}

    public ChangeMembershipsOp(List<MembershipRecord> membership) {
        Objects.requireNonNull(membership);
        this.membership = Collections.unmodifiableList(new ArrayList<>(membership));
    }

    @Override
    public void doRun() {
        getService().onChangeMembership(this);
    }

    public List<MembershipRecord> getMembership() {
        return membership;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ChangeMembershipOp{");
        sb.append("membership=").append(membership);
        sb.append('}');
        return sb.toString();
    }
}
