package com.bin.protocol.gossip.cluster.gossip;

import com.bin.protocol.gossip.cluster.operation.Operation;


import java.util.Objects;
import java.util.StringJoiner;

public final class Gossip  {


  private String gossiperId;
  private Operation op;
  // incremented counter
  private long sequenceId;

  public Gossip() {}

  public Gossip(String gossiperId, Operation op, long sequenceId) {
    this.gossiperId = Objects.requireNonNull(gossiperId);
    this.op = Objects.requireNonNull(op);
    this.sequenceId = sequenceId;
  }

  public String gossipId() {
    return gossiperId + "-" + sequenceId;
  }

  public String gossiperId() {
    return gossiperId;
  }

  public Operation operation() {
    return op;
  }

  public long sequenceId() {
    return sequenceId;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }
    if (that == null || getClass() != that.getClass()) {
      return false;
    }
    Gossip gossip = (Gossip) that;
    return sequenceId == gossip.sequenceId
        && Objects.equals(gossiperId, gossip.gossiperId)
        && Objects.equals(op, gossip.operation());
  }

  @Override
  public int hashCode() {
    return Objects.hash(gossiperId, op, sequenceId);
  }


  @Override
  public String toString() {
    return new StringJoiner(", ", Gossip.class.getSimpleName() + "[", "]")
        .add("gossiperId='" + gossiperId + "'")
        .add("operation=" + op)
        .add("sequenceId=" + sequenceId)
        .toString();
  }
}
