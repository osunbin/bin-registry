package com.bin.protocol.gossip.network.netty;

import com.bin.protocol.gossip.cluster.ClusterService;
import com.bin.protocol.gossip.network.Server;
import com.bin.protocol.gossip.network.NetWorkConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class UdpServer implements Server {
    private static Logger logger = LoggerFactory.getLogger(UdpServer.class);


    private ClusterService clusterService;

    private final NetWorkConfig config;

    private Channel channel;

    public UdpServer(NetWorkConfig config, ClusterService clusterService) {
        this.config = config;
        this.clusterService = clusterService;
    }

    @Override
    public SocketAddress start() {
        channel = newUdpServer(1);
        return channel.localAddress();
    }

    @Override
    public void close() {
        channel.close();
    }

    private Channel newUdpServer(int workThreads) {

        EventLoopGroup group = NettyUtils.newNioOrEpollEventLoopGroup(workThreads, "gossip udp server");
        NettyHandler nettyHandler = new NettyHandler(clusterService);
        Bootstrap bootstrap = NettyUtils.clientBootstrap(group);
        bootstrap.channel(NettyUtils.nioOrEpollDatagramChannel())
                 .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,config.connectTimeout())
                .option(ChannelOption.SO_RCVBUF, 2 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024)
                .handler(nettyHandler);

        ChannelFuture channelFuture = bootstrap.bind(config.port()).syncUninterruptibly().addListener(future -> {

            logger.info("Netty udp Server started on port {}", config.port());
        });
        channelFuture.channel().closeFuture().addListener(future -> {
            logger.info("Netty udp  Server Start Shutdown ............");
            group.shutdownGracefully().syncUninterruptibly();
        });
        return channelFuture.channel();
    }
}
