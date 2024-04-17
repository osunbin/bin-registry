package com.bin.protocol.gossip.cluster.membership;

import com.bin.protocol.gossip.Member;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

public class MembershipEvent {
    public enum Type {
        ADDED,
        REMOVED,
        LEAVING,
        UPDATED
    }

    private final Type type;
    private final Member member;
    private final long timestamp;

    private MembershipEvent(
            Type type, Member member, long timestamp) {
        this.type = type;
        this.member = member;
        this.timestamp = timestamp;
    }

    public static MembershipEvent createRemoved(Member member, long timestamp) {
        Objects.requireNonNull(member, "member must be not null");
        return new MembershipEvent(Type.REMOVED, member, timestamp);
    }

    public static MembershipEvent createAdded(Member member, long timestamp) {
        Objects.requireNonNull(member, "member must be not null");
        return new MembershipEvent(Type.ADDED, member, timestamp);
    }

    public static MembershipEvent createLeaving(Member member, long timestamp) {
        Objects.requireNonNull(member, "member must be not null");
        return new MembershipEvent(Type.LEAVING, member, timestamp);
    }


    public static MembershipEvent createUpdated(
            Member member, long timestamp) {
        Objects.requireNonNull(member, "member must be not null");
        return new MembershipEvent(Type.UPDATED, member, timestamp);
    }

    public Type type() {
        return type;
    }

    public boolean isAdded() {
        return type == Type.ADDED;
    }

    public boolean isRemoved() {
        return type == Type.REMOVED;
    }

    public boolean isLeaving() {
        return type == Type.LEAVING;
    }

    public boolean isUpdated() {
        return type == Type.UPDATED;
    }

    public Member member() {
        return member;
    }


    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MembershipEvent.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("member=" + member)
                .add("timestamp=" + timestampAsString(timestamp))
                .toString();
    }

    private String timestampAsString(long timestamp) {
        return Instant.ofEpochMilli(timestamp).toString();
    }


}
