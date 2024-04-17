package com.bin.protocol.gossip.cluster.membership;

import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.failurefetector.MemberStatus;

import java.util.Objects;

public class MembershipRecord {



    private Member member;
    private MemberStatus status;
    /**
     *  每传播一次+1
     */
    private int incarnation;

    public MembershipRecord() {}

    /** Instantiates new instance of membership record with given member, status and incarnation. */
    public MembershipRecord(Member member, MemberStatus status, int incarnation) {
        this.member = Objects.requireNonNull(member);
        this.status = Objects.requireNonNull(status);
        this.incarnation = incarnation;
    }

    public Member member() {
        return member;
    }

    public MemberStatus status() {
        return status;
    }

    public boolean isAlive() {
        return status == MemberStatus.ALIVE;
    }

    public boolean isSuspect() {
        return status == MemberStatus.SUSPECT;
    }

    public boolean isLeaving() {
        return status == MemberStatus.LEAVING;
    }

    public boolean isDead() {
        return status == MemberStatus.DEAD;
    }

    public int incarnation() {
        return incarnation;
    }

    /**
     * Checks either this record overrides given record.
     *  检查此记录是否覆盖给定记录
     *
     * @param r0 existing record in membership table
     * @return true if this record overrides exiting; false otherwise
     */
    public boolean isOverrides(MembershipRecord r0) {
        if (r0 == null) { // 旧节点已经离开
            return isAlive() || isLeaving();
        }
        // 节点id 不一致,说明不是同一个节点了，正常不会出现这种情况
        if (!Objects.equals(member.id(), r0.member.id())) {
            throw new IllegalArgumentException("Can't compare records for different members");
        }
        //  同一个节点，没有改变说明不需要更新状态
        if (this.equals(r0)) {
            return false;
        }
        // 旧节点 离开集群 也不需要更新
        if (r0.isDead()) {
            return false;
        }
        // 新节点 离开集群 需要更新 信息
        if (isDead()) {
            return true;
        }
        if (incarnation == r0.incarnation) {
            // 更新的节点未知状态    旧节点正常 或者 要离开
            return isSuspect() && (r0.isAlive() || r0.isLeaving());
        } else {
            return incarnation > r0.incarnation;
        }
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        MembershipRecord record = (MembershipRecord) that;
        return incarnation == record.incarnation
                && Objects.equals(member, record.member)
                && status == record.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(member, status, incarnation);
    }



    @Override
    public String toString() {
        return "{m: " + member + ", s: " + status + ", inc: " + incarnation + '}';
    }


}
