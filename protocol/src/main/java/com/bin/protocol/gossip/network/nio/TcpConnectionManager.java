package com.bin.protocol.gossip.network.nio;

import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.codec.ProtostuffSerializer;
import com.bin.protocol.gossip.common.Address;
import com.bin.protocol.gossip.network.Connection;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TcpConnectionManager implements Connection {
    private final Logger logger = LoggerFactory.getLogger(TcpConnectionManager.class);


    private ConcurrentMap<Address, NioTcpConnection> connections = new ConcurrentHashMap<>();



    public void send(Address address, Operation op) throws IOException {

        NioTcpConnection tcpConnection = connections.computeIfAbsent(address, this::connect);
        if (tcpConnection == null) {
            throw new IOException("new Connection connect address[ " + address + "] fail");
        }
        ByteBuf byteBuf = ProtostuffSerializer.writeOperation(1024 * 2, op);
        tcpConnection.send(byteBuf.nioBuffer());
    }


    public void close() {
        Collection<NioTcpConnection> values = connections.values();
        for (NioTcpConnection tcpConnection : values) {
            try {
                tcpConnection.close();
            } catch (IOException e) {
                logger.error("Connection close fail", e);
            }
        }
    }


    public NioTcpConnection connect(Address address) {
        InetSocketAddress target = new InetSocketAddress(address.host(), address.port());
        try {
            return new NioTcpConnection(target);
        } catch (IOException e) {
            logger.error("new Connection connect address[{}] fail", address, e);
        }
        return null;
    }

}
