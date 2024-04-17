package com.bin.protocol.gossip.cluster.operation;

import com.bin.protocol.gossip.cluster.ClusterService;
import com.bin.protocol.gossip.common.Address;
import com.bin.protocol.gossip.common.StringUtils;

import static com.bin.protocol.gossip.cluster.operation.EndpointQualifier.TCP;

public abstract class Operation {

    private transient ClusterService service;


    private String endpointQualifier = "UDP";


    private String correlationId;


    private String sender;

    public ClusterService getService() {
        return service;
    }

    public void setService(ClusterService service) {
        this.service = service;
    }


    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }


    public void setSender(Address sender) {
        this.sender = sender.toString();
    }


    public boolean isSender() {
        if (StringUtils.isEmpty(sender)) {
            return false;
        }
        return true;
    }

    public Address sender() {
        return Address.from(sender);
    }

    public void setUdp() {
        endpointQualifier = "UDP";
    }
    public void setTCP() {
        endpointQualifier = "TCP";
    }


    public boolean isTcp() {
        if ("TCP".equals(endpointQualifier)) {
            return true;
        }
        return false;
    }

    public void execute() {
        getService().submit(() -> run());
    }

    public final void run() {

        boolean notify = getService().isNotify(this);
        if (notify) {
            getService().promise(this);
        } else {
            doRun();
        }
    }

    public void doRun() {
    }

}
