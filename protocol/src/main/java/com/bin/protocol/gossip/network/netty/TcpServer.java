package com.bin.protocol.gossip.network.netty;

import com.bin.protocol.gossip.cluster.ClusterService;
import com.bin.protocol.gossip.network.Server;
import com.bin.protocol.gossip.network.NetWorkConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteOrder;

public class TcpServer implements Server {
    private static Logger logger = LoggerFactory.getLogger(TcpServer.class);


    private final NetWorkConfig config;



    private ClusterService clusterService;

    private Channel parentChannel;

    private InetSocketAddress localAddress;

    public TcpServer(NetWorkConfig config, ClusterService clusterService) {
        this.config = config;
        this.clusterService = clusterService;
    }


    @Override
    public SocketAddress start() {
        SocketAddress socketAddress = newTcpServer(2);
        localAddress = (InetSocketAddress) socketAddress;
        return socketAddress;

    }




    private SocketAddress newTcpServer(int workThreads) {

        EventLoopGroup bossGroup = NettyUtils.newNioOrEpollEventLoopGroup(1,"gossip tcp server accept");

        EventLoopGroup workGroup = NettyUtils.newNioOrEpollEventLoopGroup(workThreads,"gossip tcp server process");

        ServerBootstrap serverBootstrap = NettyUtils.serverBootstrap(bossGroup,workGroup);
        serverBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,config.connectTimeout())
                .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 16 * 1024);
        NettyHandler nettyHandler = new NettyHandler(clusterService);
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                ch.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, Integer.MAX_VALUE, 0, 4, 0, 0, true));
                ch.pipeline().addLast("handler", nettyHandler);
            }
        });
        ChannelFuture channelFuture = serverBootstrap.bind(config.port()).syncUninterruptibly().addListener(future -> {

            logger.info("Netty tcp Server started on port {}", config.port());
        });
        channelFuture.channel().closeFuture().addListener(future -> {
            logger.info("Netty tcp  Server Start Shutdown ............");
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workGroup.shutdownGracefully().syncUninterruptibly();
        });
        parentChannel = channelFuture.channel();
        return channelFuture.channel().localAddress();

    }



    public void close() {

        logger.info("shutdown called {}", localAddress);
        if (parentChannel != null) {
            parentChannel.close();
        }
    }


}
