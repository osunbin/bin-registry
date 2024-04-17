package com.bin.protocol.gossip.examples;


import com.bin.protocol.gossip.Cluster;
import com.bin.protocol.gossip.Message;
import com.bin.protocol.gossip.cluster.ClusterMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GossipTest {

    private static final Logger log = LoggerFactory.getLogger(GossipTest.class);


    public static void main(String[] args) throws Exception {
        log.info("=========start time");
        Cluster alice =
                new Cluster()
                        .port(7958)
                        .handler(new ClusterMessageHandler() {
                            @Override
                            public void onGossip(Message gossip) {
                                System.out.println("Alice heard: " + gossip.data());
                            }
                        }).start();

        alice.spreadGossip(Message.fromData("Gossip from alice"));
    }


}

