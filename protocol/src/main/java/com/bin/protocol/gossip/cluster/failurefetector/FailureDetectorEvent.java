package com.bin.protocol.gossip.cluster.failurefetector;

import com.bin.protocol.gossip.Member;

import java.util.StringJoiner;

public final class FailureDetectorEvent {

  private final Member member;
  private final MemberStatus status;

 public FailureDetectorEvent(Member member, MemberStatus status) {
    this.member = member;
    this.status = status;
  }

  public Member member() {
    return member;
  }

  public MemberStatus status() {
    return status;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", FailureDetectorEvent.class.getSimpleName() + "[", "]")
        .add("member=" + member)
        .add("status=" + status)
        .toString();
  }
}
