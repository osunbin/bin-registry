package com.bin.protocol.gossip.network.nio;

import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.codec.ProtostuffSerializer;
import com.bin.protocol.gossip.common.Address;
import com.bin.protocol.gossip.network.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.protostuff.ByteBufOutput;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UdpConnection implements Connection {


    private DatagramChannel channel;


    public UdpConnection()  {
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
        } catch (IOException e) {

        }
    }



    public void send(Address address, Operation op) throws IOException {
        if (channel == null)
            throw new IOException("udp not available ");
        InetSocketAddress target =  new InetSocketAddress(address.host(),address.port());

        ByteBuf byteBuf = ProtostuffSerializer.writeOperation(512, op);
        channel.send(byteBuf.nioBuffer(),target);
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
