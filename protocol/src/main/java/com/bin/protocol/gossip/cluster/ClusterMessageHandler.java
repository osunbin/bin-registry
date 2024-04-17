package com.bin.protocol.gossip.cluster;

import com.bin.protocol.gossip.cluster.membership.MembershipListener;
import com.bin.protocol.gossip.cluster.membership.MembershipEvent;
import com.bin.protocol.gossip.Message;

public interface ClusterMessageHandler extends MembershipListener {

  default void onMessage(Message message) {
    // no-op
  }

  default void onGossip(Message gossip) {
    // no-op
  }

  default void onMembershipEvent(MembershipEvent event) {
    // no-op
  }
}