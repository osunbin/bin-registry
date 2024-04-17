package com.bin.protocol.gossip.cluster.operation;

public class EndpointQualifier {

    public static final EndpointQualifier TCP = new EndpointQualifier("tcp");
    public static final EndpointQualifier UDP = new EndpointQualifier("udp");


    private String protocol;


    private EndpointQualifier(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }



}
