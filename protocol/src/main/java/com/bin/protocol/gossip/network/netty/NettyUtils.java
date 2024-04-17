package com.bin.protocol.gossip.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * Helper methods for netty code.
 */
public class NettyUtils {

    public static final String THREAD_POOL_NAME_PREFIX = "Netty-";
    private static final Logger logger = LoggerFactory.getLogger(NettyUtils.class);
    private static final int DEFAULT_INET_ADDRESS_COUNT = 1;


    private static ThreadFactory createThreadFactory(String clazz,String name) {
        if (name != null && !name.trim().isEmpty())
            clazz = name;
        final String poolName = THREAD_POOL_NAME_PREFIX + clazz;
        return new DefaultThreadFactory(poolName, true);
    }

    public static ServerBootstrap serverBootstrap(EventLoopGroup bossGroup, EventLoopGroup workGroup) {
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workGroup)
                .channel(nioOrEpollServerSocketChannel())
                /**
                 * TCP_NODELAY就是用于启用或关于Nagle算法。如果要求高实时性，有数据发送时就马上发送，
                 * 就将该选项设置为true关闭Nagle算法；如果要减少发送次数减少网络交互，就设置为false等累积一定大小后再发送。默认为false。
                 */
                .childOption(ChannelOption.TCP_NODELAY, true)
                /**
                 *  对象池，重用缓冲区
                 */
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                /**
                 *  心跳保活机制。在双方TCP套接字建立连接后（即都进入ESTABLISHED状态）并且在两个小时左右上层没有任何数据传输的情况下，这套机制才会被激活
                 */
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                /**
                 *  允许启动一个监听服务器并捆绑其众所周知端口，即使以前建立的将此端口用做他们的本地端口的连接仍存在。这通常是重启监听服务器时出现，若不设置此选项，则bind时将出错。
                 *  允许在同一端口上启动同一服务器的多个实例，只要每个实例捆绑一个不同的本地IP地址即可。对于TCP，我们根本不可能启动捆绑相同IP地址和相同端口号的多个服务器。
                 *  允许单个进程捆绑同一端口到多个套接口上，只要每个捆绑指定不同的本地IP地址即可。这一般不用于TCP服务器。
                 *  允许完全重复的捆绑：当一个IP地址和端口绑定到某个套接口上时，还允许此IP地址和端口捆绑到另一个套接口上。一般来说，这个特性仅在支持多播的系统上才有，而且只对UDP套接口而言（TCP不支持多播）
                 */
                .childOption(ChannelOption.SO_REUSEADDR, true);
        return bootstrap;
    }


    public static Bootstrap clientBootstrap(EventLoopGroup eventLoopGroup) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);

        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return bootstrap;
    }

    public static EventLoopGroup newNioOrEpollEventLoopGroup() {
        return newNioOrEpollEventLoopGroup(0);
    }


    public static EventLoopGroup newNioOrEpollEventLoopGroup(int nThreads){
        return newNioOrEpollEventLoopGroup(nThreads,null);
    }
    public static EventLoopGroup newNioOrEpollEventLoopGroup(int nThreads,String poolName) {
        if (Epoll.isAvailable()) {
            final String clazz = EpollEventLoopGroup.class.getSimpleName();
            final ThreadFactory factory = createThreadFactory(clazz,poolName);
            return new EpollEventLoopGroup(nThreads, factory);
        } else {
            final String clazz = NioEventLoopGroup.class.getSimpleName();
            final ThreadFactory factory = createThreadFactory(clazz,poolName);
            return new NioEventLoopGroup(nThreads, factory);
        }
    }


    public static Class<? extends DatagramChannel> nioOrEpollDatagramChannel() {
        if (Epoll.isAvailable()) {
            return EpollDatagramChannel.class;
        } else {
            return NioDatagramChannel.class;
        }
    }

    public static Class<? extends SocketChannel> nioOrEpollSocketChannel() {
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }



    public static Class<? extends ServerSocketChannel> nioOrEpollServerSocketChannel() {
        if (Epoll.isAvailable()) {
            return EpollServerSocketChannel.class;
        } else {
            return NioServerSocketChannel.class;
        }
    }

    /**
     *
     * 尝试检测并返回客户端可以用来访问此服务器的本地网络地址的数量,会排除几种类型
     * 对剩余的所有地址进行计数，并返回总计数。配置 boos 事件循环组的线程数，以确保
     * 在服务器配置为侦听所有可用地址的情况下，为每个地址提供足够的线程。如果列出网络接口失败，此方法将返回1
     */
    public static int getClientReachableLocalInetAddressCount() {
        try {
            Set<InetAddress> validInetAddresses = new HashSet<>();
            Enumeration<NetworkInterface> allNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(allNetworkInterfaces)) {
                for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                    // 路由器不会将发送到链接本地地址的流量转发，因此任何实际的服务器部署都不会让客户端使用这些
                    if (inetAddress.isLinkLocalAddress()) {
                        logger.debug("Ignoring link-local InetAddress {}", inetAddress);
                        continue;
                    }
                    // 服务器套接字使用TCP 因此无法绑定到多播地址
                    if (inetAddress.isMulticastAddress()) {
                        logger.debug("Ignoring multicast InetAddress {}", inetAddress);
                        continue;
                    }
                    // 环回地址。通常仅用于测试
                    if (inetAddress.isLoopbackAddress()) {
                        logger.debug("Ignoring loopback InetAddress {}", inetAddress);
                        continue;
                    }
                    validInetAddresses.add(inetAddress);
                }
            }
            logger.debug("Detected {} local network addresses: {}", validInetAddresses.size(), validInetAddresses);
            return !validInetAddresses.isEmpty() ? validInetAddresses.size() : DEFAULT_INET_ADDRESS_COUNT;
        } catch (SocketException ex) {
            logger.warn("Failed to list all network interfaces, assuming 1", ex);
            return DEFAULT_INET_ADDRESS_COUNT;
        }
    }

}