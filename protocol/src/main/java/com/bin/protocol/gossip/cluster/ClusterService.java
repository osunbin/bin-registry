package com.bin.protocol.gossip.cluster;


import com.bin.protocol.gossip.Cluster;
import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.failurefetector.ClusterFailureDetectorManager;
import com.bin.protocol.gossip.cluster.failurefetector.FailureDetectorEvent;
import com.bin.protocol.gossip.cluster.membership.MembershipListener;
import com.bin.protocol.gossip.cluster.membership.MembershipEvent;
import com.bin.protocol.gossip.cluster.operation.GossipMessageOp;
import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.cluster.operation.Promise;
import com.bin.protocol.gossip.Message;
import com.bin.protocol.gossip.common.Address;
import com.bin.protocol.gossip.common.Addressing;
import com.bin.protocol.gossip.common.StringUtils;
import com.bin.protocol.gossip.network.Connection;
import com.bin.protocol.gossip.network.nio.TcpConnectionManager;
import com.bin.protocol.gossip.network.nio.UdpConnection;
import com.bin.protocol.gossip.network.Server;
import com.bin.protocol.gossip.network.netty.TcpServer;
import com.bin.protocol.gossip.network.netty.UdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class ClusterService implements MembershipListener {
    public static Logger logger = LoggerFactory.getLogger(ClusterService.class);


    private final ScheduledExecutorService internalSchedule =
            new ScheduledThreadPoolExecutor(3, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = Executors.defaultThreadFactory().newThread(r);
                    thread.setName("gossip-schedule-" + thread.getName());
                    return thread;
                }
            });


    private final ExecutorService internalExecutor = new ThreadPoolExecutor(4, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);

            thread.setName("gossip-executor-" + thread.getName());
            return thread;
        }
    });


    private Connection tcpConnection;


    private Connection udpConnection;


    private final ClusterConfig config;

    private ClusterFailureDetectorManager clusterFailureDetectorManager;

    private MembershipManager membershipManager;

    private MembershipListener membershipListener;

    private GossipManager gossipManager;

    private final Map<String, Promise<Operation>> sessionMap = new ConcurrentHashMap<>();

    private Member localMember;

    private Address localAddress;

    private Server server;

    private Server udpServer;

    private ClusterMessageHandler handler = new ClusterMessageHandler() {
    };

    public ClusterService() {
        this(new ClusterConfig());
    }

    public ClusterService(ClusterConfig config) {
        this.config = config;

        server = new TcpServer(config.transportConfig(), this);
        udpServer = new UdpServer(config.transportConfig(), this);

    }

    public void start() {
        SocketAddress start = server.start();

        localAddress = Addressing.resoleAddress(start);
        localMember = createLocalMember(localAddress);
        udpServer.start();

        tcpConnection = new TcpConnectionManager();
        udpConnection = new UdpConnection();
        clusterFailureDetectorManager = new ClusterFailureDetectorManager(this);
        membershipManager = new MembershipManager(this);
        gossipManager = new GossipManager(this);

        List<MembershipListener> membershipListeners = new ArrayList<>();
        membershipListeners.add(clusterFailureDetectorManager);
        membershipListeners.add(gossipManager);
        membershipListeners.add(handler);
        membershipListener = new DefaultMembershipListener(membershipListeners);

        clusterFailureDetectorManager.start();
        membershipManager.start();
        gossipManager.start();
        logger.info("[{}][doStart] Starting, config: {}", localMember, config);
    }


    public void shutdown() {
        logger.info("[{}][doShutdown] Shutting down", localMember);
        membershipManager.stop();

        try {
            getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        try {
            getExecutor().awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        udpConnection.close();
        tcpConnection.close();
        server.close();
        udpServer.close();
        logger.info("[{}][doShutdown] Shutdown", localMember);
    }

    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit) {
        return getScheduledExecutor().schedule(command, delay, unit);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit) {
        return getScheduledExecutor().schedule(callable, delay, unit);
    }


    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit) {
        return getScheduledExecutor().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit) {
        return getScheduledExecutor().scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public void seedMembers(List<Address> seedMembers) {
        getConfig().membershipConfig().seedMembers(seedMembers);
    }


    public void handler(ClusterMessageHandler handler) {
        this.handler = handler;
    }

    public void port(int port) {
        getConfig().transportConfig().port(port);
    }

    public void onGossip(Message gossip) {
        handler.onGossip(gossip);
    }

    public void onGossip(Operation op) {
        getGossipManager().onGossip(op);
    }

    public void onMessage(Operation op) {
        GossipMessageOp gossipMessageOp = (GossipMessageOp) op;
        handler.onMessage(gossipMessageOp.getMessage());
    }

    public String spread(Message message) {
        GossipMessageOp gossipMessageOp = new GossipMessageOp(message);
        return getGossipManager().spread(gossipMessageOp);
    }

    public void spread(Operation op) {
        getGossipManager().spread(op);
    }

    public void udpSend(Address address, Operation op) throws IOException {
        if (!op.isSender()) {
            op.setSender(localMember.address());
        }
        op.setUdp();
        udpConnection.send(address, op);
    }

    public void tcpSend(Address address, Operation op) throws IOException {
        if (!op.isSender()) {
            op.setSender(localMember.address());
        }
        op.setTCP();
        tcpConnection.send(address, op);
    }


    public String sendBefore(Operation op) {
        if (!op.isSender()) {
            op.setSender(localMember.address());
        }
        String correlationId = op.getCorrelationId();
        if (StringUtils.isEmpty(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    public Promise<Operation> udpSendResponse(Address address, Operation op, long timeout) throws IOException {

        String correlationId = sendBefore(op);
        op.setUdp();
        udpConnection.send(address, op);

        Promise<Operation> promise = Promise.ofOperation(timeout);
        sessionMap.put(correlationId, promise);
        return promise;
    }

    public Promise<Operation> tcpSendResponse(Address address, Operation op, long timeout) throws IOException {
        String correlationId = sendBefore(op);
        op.setTCP();
        tcpConnection.send(address, op);

        Promise<Operation> promise = Promise.ofOperation(timeout);
        sessionMap.put(correlationId, promise);
        return promise;
    }

    public boolean isNotify(Operation op) {
        String cid = op.getCorrelationId();
        if (StringUtils.isEmpty(cid)) {
            return false;
        }
        Promise<Operation> promise = sessionMap.get(cid);
        if (promise == null) {
            return false;
        }
        return true;
    }

    public void promise(Operation op) {
        Promise<Operation> promise = sessionMap.get(op.getCorrelationId());
        promise.set(op);
    }


    public void onPing(Operation op) {
        getClusterHeartbeatManager().onPing(op);
    }

    public void onPingReq(Operation op) {
        getClusterHeartbeatManager().onPingReq(op);
    }

    public void onNPing(Operation op) {
        getClusterHeartbeatManager().onNPing(op);
    }

    public void onNPingAck(Operation op) {
        getClusterHeartbeatManager().onNPingAck(op);
    }


    public void onFailureDetectorEvent(FailureDetectorEvent fdEvent) {
        getMembershipManager().onFailureDetectorEvent(fdEvent);
    }


    public void onChangeMembership(Operation op) {
        getMembershipManager().onChangeMembership(op);
    }

    public void onChangeMembershipAck(Operation op) {
        getMembershipManager().onChangeMembershipAck(op);
    }

    public void onMembershipGossip(Operation op) {
        getMembershipManager().onMembershipGossip(op);
    }


    public Future<?> submit(Runnable task) {
        return getExecutor().submit(task);
    }

    public void execute(Runnable command) {
        getExecutor().execute(command);
    }

    private ExecutorService getExecutor() {
        return internalExecutor;
    }

    private ScheduledExecutorService getScheduledExecutor() {
        return internalSchedule;
    }

    public MembershipListener getMembershipListener() {
        return membershipListener;
    }


    public Address getLocalTransport() {
        return localAddress;
    }

    public Collection<Member> members() {
        return getMembershipManager().members();
    }

    public Collection<Member> otherMembers() {
        return getMembershipManager().otherMembers();
    }

    public Member member(String id) {
        return getMembershipManager().member(id);
    }

    public Member member(Address address) {
        return getMembershipManager().member(address);
    }

    public List<Address> getSeedMembers() {
        return config.membershipConfig().seedMembers();
    }

    private ClusterFailureDetectorManager getClusterHeartbeatManager() {
        return clusterFailureDetectorManager;
    }

    private MembershipManager getMembershipManager() {
        return membershipManager;
    }

    private GossipManager getGossipManager() {
        return gossipManager;
    }

    public ClusterConfig getConfig() {
        return config;
    }

    public Member getLocalMember() {
        return localMember;
    }


    private Member createLocalMember(Address address) {
        Integer port = config.externalPort();
        if (port == null || port == 0) {
            port = address.port();
        }

        String host = config.externalHost();
        if (StringUtils.isEmpty(host)) {
            host = address.host();
        }
        Address memberAddress = Address.create(host, port);

        String id = config.memberId();
        if (StringUtils.isEmpty(id)) {
            id = UUID.randomUUID().toString();
        }
        return new Member(id,
                memberAddress,
                config.membershipConfig().namespace());
    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        membershipListener.onMembershipEvent(event);
    }


    private class DefaultMembershipListener implements MembershipListener {

        private List<MembershipListener> membershipListeners;

        public DefaultMembershipListener(List<MembershipListener> membershipListeners) {
            this.membershipListeners = Collections.unmodifiableList(membershipListeners);
        }

        @Override
        public void onMembershipEvent(MembershipEvent event) {
            for (MembershipListener membershipListener : membershipListeners) {
                execute(() -> membershipListener.onMembershipEvent(event));
            }
        }
    }
}
