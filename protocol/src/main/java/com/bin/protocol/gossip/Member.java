package com.bin.protocol.gossip;

import com.bin.protocol.gossip.common.Address;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

public final class Member implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Address address;
    private String namespace;

    public Member() {
    }


    public Member(String id, Address address, String namespace) {
        this.id = Objects.requireNonNull(id, "member id");
        this.address = Objects.requireNonNull(address, "member address");
        this.namespace = Objects.requireNonNull(namespace, "member namespace");
    }


    /**
     * @return 返回群集成员本地id
     */
    public String id() {
        return id;
    }


    public String namespace() {
        return namespace;
    }

    public Address address() {
        return address;
    }


    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        Member member = (Member) that;
        return Objects.equals(id, member.id)
                && Objects.equals(address, member.address)
                && Objects.equals(namespace, member.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, address, namespace);
    }


    private static String stringifyId(String id) {
        try {
            final UUID uuid = UUID.fromString(id);
            //  UUID 的 128 位值的最高有效 64 位   toHexString 16进制字符串
            return Long.toHexString(uuid.getMostSignificantBits() & Long.MAX_VALUE);
        } catch (Exception ex) {
            return id;
        }
    }


    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(":");

        return stringJoiner.add(namespace).add(stringifyId(id) + "@" + address).toString();

    }
}
