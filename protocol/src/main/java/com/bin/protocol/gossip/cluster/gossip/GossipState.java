package com.bin.protocol.gossip.cluster.gossip;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public final class GossipState {

  /** Target gossip. */
  private final Gossip gossip;

  /** Local gossip period when gossip was received for the first time. */
  private final long infectionPeriod;

  /** Set of member IDs this gossip was received from. */
  private final Set<String> infected = new HashSet<>();

  public GossipState(Gossip gossip, long infectionPeriod) {
    this.gossip = Objects.requireNonNull(gossip);
    this.infectionPeriod = infectionPeriod;
  }

  public Gossip gossip() {
    return gossip;
  }

  public long infectionPeriod() {
    return infectionPeriod;
  }

  public void addToInfected(String memberId) {
    infected.add(memberId);
  }

  public boolean isInfected(String memberId) {
    return infected.contains(memberId);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GossipState.class.getSimpleName() + "[", "]")
        .add("gossip=" + gossip)
        .add("infectionPeriod=" + infectionPeriod)
        .add("infected=" + infected)
        .toString();
  }
}
