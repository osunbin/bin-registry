package com.bin.protocol.gossip.cluster.membership;

import com.bin.protocol.gossip.cluster.membership.MembershipEvent;

import java.util.EventListener;

public interface MembershipListener extends EventListener {


    void onMembershipEvent(MembershipEvent event);



}
