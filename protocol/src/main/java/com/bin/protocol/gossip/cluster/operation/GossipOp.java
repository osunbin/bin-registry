package com.bin.protocol.gossip.cluster.operation;

import com.bin.protocol.gossip.cluster.gossip.Gossip;

import java.util.Collections;
import java.util.List;

public class GossipOp extends Operation {
    private List<Gossip> gossips;
    private String from;

    public GossipOp(Gossip gossip, String from) {
        this(Collections.singletonList(gossip),from);
    }

    public GossipOp(List<Gossip> gossips, String from) {
        this.gossips = gossips;
        this.from = from;
    }


    public List<Gossip> getGossips() {
        return gossips;
    }

    public String getFrom() {
        return from;
    }


    @Override
    public void doRun() {
        getService().onGossip(this);
    }
}
