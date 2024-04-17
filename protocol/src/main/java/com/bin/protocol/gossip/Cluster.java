package com.bin.protocol.gossip;

import com.bin.protocol.gossip.cluster.ClusterConfig;
import com.bin.protocol.gossip.cluster.ClusterMessageHandler;
import com.bin.protocol.gossip.cluster.ClusterService;
import com.bin.protocol.gossip.cluster.operation.GossipMessageOp;
import com.bin.protocol.gossip.common.Address;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Cluster {


    private ClusterService clusterService;


    public Cluster() {
        clusterService = new ClusterService();
    }

    public Cluster(ClusterConfig config) {
        clusterService = new ClusterService(config);
    }

    public Cluster port(int port) {
        clusterService.port(port);
        return this;
    }

    public Cluster handler(ClusterMessageHandler handler) {
        clusterService.handler(handler);
        return this;
    }

    public Cluster membership(Address... seedMembers) {
        clusterService.seedMembers(Arrays.asList(seedMembers));
        return this;
    }

    public Cluster start() {
        clusterService.start();
        return this;
    }

    public Address address() {
        return clusterService.getLocalMember().address();
    }


    public void send(Member member, Message message) throws IOException {
        clusterService.tcpSend(member.address(),new GossipMessageOp(message));
    }

    public void send(Address address, Message message) throws IOException {
        clusterService.tcpSend(address,new GossipMessageOp(message));
    }



    public String spreadGossip(Message message){
       return clusterService.spread(message);
    }

    public Member member(){
      return   clusterService.getLocalMember();
    }


    public Member member(String id){
       return clusterService.member(id);
    }

    public Member member(Address address){
      return   clusterService.member(address);
    }

    public Collection<Member> members(){
      return   clusterService.members();
    }

    public Collection<Member>  otherMembers(){
       return clusterService.otherMembers();
    }

    void shutdown(){
        clusterService.shutdown();
    }
}
