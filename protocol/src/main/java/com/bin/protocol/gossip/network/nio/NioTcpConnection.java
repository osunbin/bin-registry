package com.bin.protocol.gossip.network.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioTcpConnection {
    SocketChannel socketChannel;


    public NioTcpConnection(SocketAddress socketAddress) throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(socketAddress);
        socketChannel.configureBlocking(false);

    }


    public void send(ByteBuffer buffer) throws IOException {
        socketChannel.write(buffer);
    }


    public void close() throws IOException {
        socketChannel.close();
    }
}
