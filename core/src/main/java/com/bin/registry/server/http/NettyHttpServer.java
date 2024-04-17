package com.bin.registry.server.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NettyHttpServer {

    private static Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);

    private int port = 7090;

    public NettyHttpServer(int port) {
        if (port > 0) {
            this.port = port;
        }
    }

    public void start() {

        ServerBootstrap bootstrap = new ServerBootstrap();

        EventLoopGroup workerGroup = getEventLoopGroup(8);

        bootstrap.group(workerGroup);

        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.SO_RCVBUF, 4 * 1024);

        if (workerGroup instanceof EpollEventLoopGroup) {
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            bootstrap.channel(EpollServerSocketChannel.class);
        } else if (workerGroup instanceof NioEventLoopGroup) {
            bootstrap.channel(NioServerSocketChannel.class);
        }
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, false);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, 4 * 1024);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 4 * 1024);
        HttpServerHandler httpServerHandler = new HttpServerHandler();
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {

                ch.pipeline().addLast("codec", new HttpServerCodec());
                // post处理
                ch.pipeline().addLast("aggregator", new HttpObjectAggregator(1024 * 1024));
                ch.pipeline().addLast("bizHandler", httpServerHandler);
            }
        });
        ChannelFuture channelFuture = bootstrap.bind(port).syncUninterruptibly().addListener(future -> {

            logger.info("Netty Http Server started on port {}", port);
        });
        channelFuture.channel().closeFuture().addListener(future -> {
            logger.info("Netty Http Server Start Shutdown ............");
            workerGroup.shutdownGracefully().syncUninterruptibly();
        });
    }


    public EventLoopGroup getEventLoopGroup(int nThreads) {
        EventLoopGroup eventLoopGroup;
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup(nThreads);
            ((EpollEventLoopGroup) eventLoopGroup).setIoRatio(99);
        } else {
            eventLoopGroup = new NioEventLoopGroup(nThreads);
            ((NioEventLoopGroup) eventLoopGroup).setIoRatio(99);
        }
        return eventLoopGroup;
    }

}
