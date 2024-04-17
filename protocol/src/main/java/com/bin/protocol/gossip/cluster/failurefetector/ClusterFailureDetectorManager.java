package com.bin.protocol.gossip.cluster.failurefetector;

import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.ClusterService;
import com.bin.protocol.gossip.cluster.membership.MembershipListener;
import com.bin.protocol.gossip.cluster.membership.MembershipEvent;
import com.bin.protocol.gossip.cluster.operation.pingfailuredetector.AbstractPingAck;
import com.bin.protocol.gossip.cluster.operation.pingfailuredetector.NPingAckOp;
import com.bin.protocol.gossip.cluster.operation.pingfailuredetector.NPingOp;
import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.cluster.operation.pingfailuredetector.PingAckOp;
import com.bin.protocol.gossip.cluster.operation.pingfailuredetector.PingOp;
import com.bin.protocol.gossip.cluster.operation.pingfailuredetector.PingReqOp;
import com.bin.protocol.gossip.cluster.operation.Promise;
import com.bin.protocol.gossip.common.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import static com.bin.protocol.gossip.cluster.failurefetector.MemberStatus.ALIVE;
import static com.bin.protocol.gossip.cluster.failurefetector.MemberStatus.DEAD;
import static com.bin.protocol.gossip.cluster.failurefetector.MemberStatus.SUSPECT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClusterFailureDetectorManager implements MembershipListener {


    private final Logger logger = LoggerFactory.getLogger(ClusterFailureDetectorManager.class);




    private final List<Member> pingMembers = new ArrayList<>();
    private long currentPeriod = 0;
    private int pingMemberIndex = 0; // index for sequential ping member selection

    private final FailureDetectorConfig config;

    private final ClusterService clusterService;

    public ClusterFailureDetectorManager(ClusterService clusterService) {
        this.clusterService = clusterService;
        config = clusterService.getConfig().failureDetectorConfig();
    }


    public Member getLocalMember() {
        return clusterService.getLocalMember();
    }

    public void start() {
        clusterService.scheduleWithFixedDelay(() -> doPing(),
                config.pingInterval(), config.pingInterval(), MILLISECONDS);
    }


    @Override
    public void onMembershipEvent(MembershipEvent event) {
        Member member = event.member();
        if (event.isRemoved()) {
            boolean removed = pingMembers.remove(member);
            if (removed) {
                logger.debug(
                        "[{}][{}] Removed {} from pingMembers list (size={})",
                        getLocalMember(),
                        currentPeriod,
                        member,
                        pingMembers.size());
            }
        }
        if (event.isAdded()) {
            // insert member into random positions
            int size = pingMembers.size();
            int index = size > 0 ? ThreadLocalRandom.current().nextInt(size) : 0;
            pingMembers.add(index, member);
            logger.debug(
                    "[{}][{}] Added {} to pingMembers list (size={})",
                    getLocalMember(),
                    currentPeriod,
                    member,
                    pingMembers.size());
        }
    }



    public void onPing(Operation op) {
        long period = this.currentPeriod;
        Address sender = op.sender();
        logger.debug("[{}][{}] Received Ping from {}", getLocalMember(), period, sender);
        PingOp pingOp = (PingOp) op;
        boolean ack = true;
        if (!pingOp.getTo().id().equals(getLocalMember().id())) {
            logger.debug(
                    "[{}][{}] Received Ping from {} to {}, but local member is {}",
                    getLocalMember(),
                    period,
                    sender,
                    pingOp.getTo(),
                    getLocalMember());
            ack = false;
        }

        String cid = pingOp.getCorrelationId();
        PingAckOp pingAckOp = new PingAckOp(pingOp.getFrom(),pingOp.getTo(),ack);
        pingAckOp.setCorrelationId(cid);

        Address address = pingOp.getFrom().address();
        logger.debug("[{}][{}] Send PingAck to {}", getLocalMember(), period, address);
        try {
            if (op.isTcp()) {
                clusterService.tcpSend(address, pingAckOp);
            } else {
                clusterService.udpSend(address, pingAckOp);
            }
        }catch (IOException e) {
            logger.warn(
                    "[{}][{}] Failed to send PingAck to {}, cause: {}",
                    getLocalMember(), period, address, e.toString());
        }
    }



    public void onPingReq(Operation op) {
        Address sender = op.sender();
        long period = this.currentPeriod;
        logger.debug("[{}][{}] Received PingReq from {}", getLocalMember(), period, sender);
        PingReqOp pingReqOp = (PingReqOp) op;
        Member target = pingReqOp.getTo();
        Member originalIssuer = pingReqOp.getFrom();
        String cid = op.getCorrelationId();
        NPingOp nPingOp = new NPingOp(getLocalMember(),target,originalIssuer);
        nPingOp.setCorrelationId(cid);
        Address address = target.address();
        logger.debug("[{}][{}] Send nPing to {}", getLocalMember(), period, address);
        try {
            clusterService.udpSend(address,nPingOp);
        } catch (IOException e) {
            logger.warn(
                    "[{}][{}] Failed to send  nPing to {}, cause: {}",
                    getLocalMember(), period, address, e.toString());
        }
    }

    public void onNPing(Operation op) {
        Address sender = op.sender();
        long period = this.currentPeriod;
        logger.debug(
                "[{}][{}] Received nPing from {}", getLocalMember(), period,sender);

        NPingOp nPingOp = (NPingOp) op;
        boolean ack = true;
        if (!nPingOp.getTo().id().equals(getLocalMember().id())) {
            logger.debug(
                    "[{}][{}] Received nPing from {} to {}, but local member is {}",
                    getLocalMember(), period, sender, nPingOp.getTo(), getLocalMember());
            ack = false;
        }
        Member target = nPingOp.getOriginalIssuer();
        String cid = nPingOp.getCorrelationId();
        NPingAckOp pingAckOp = new NPingAckOp(target,nPingOp.getTo(),ack);
        pingAckOp.setCorrelationId(cid);
        Address address = nPingOp.getFrom().address();
        logger.debug("[{}][{}] Send nPingAck to {}", getLocalMember(), period, address);
        try {
            clusterService.udpSend(address,pingAckOp);
        } catch (IOException e) {
            logger.warn(
                    "[{}][{}] Failed to resend nPingAck to {}, cause: {}",
                    getLocalMember(), period, address, e.toString());
        }
    }


    public void onNPingAck(Operation op) {
        Address sender = op.sender();
        long period = this.currentPeriod;
        logger.debug(
                "[{}][{}] Received nPingAck from {}", getLocalMember(), period, sender);
        NPingAckOp npingAckOp = (NPingAckOp) op;
        Member target = npingAckOp.getFrom();
        String cid = npingAckOp.getCorrelationId();

        PingAckOp pingAckOp = new PingAckOp(target,npingAckOp.getTo(),npingAckOp.isAck());
        pingAckOp.setCorrelationId(cid);

        Address address = target.address();
        logger.debug("[{}][{}] Resend nPingAck to {}", getLocalMember(), period, address);
        try {
            clusterService.udpSend(address,pingAckOp);
        } catch (IOException e) {
            logger.debug(
                    "[{}][{}] Failed to resend  nPingAck to {}, cause: {}",
                    getLocalMember(), period, address, e.toString());
        }
    }

    private void doPing() {
        // Increment period counter
        long period = currentPeriod++;

        // Select ping member
        Member pingMember = selectPingMember();
        if (pingMember == null) {
            return;
        }

        String cid = UUID.randomUUID().toString();

        Operation pingOp = new PingOp(getLocalMember(), pingMember);
        pingOp.setCorrelationId(cid);
        logger.debug("[{}][{}] Send Ping to {}", getLocalMember(), period, pingMember);
        Address address = pingMember.address();


        try {
            Promise<Operation> operationPromise = clusterService.udpSendResponse(address, pingOp,
                    config.pingTimeout());
            clusterService.schedule(() -> processPingAck(
                            operationPromise.getResult(), period, pingMember, cid),
                    config.pingTimeout(), MILLISECONDS);
        } catch (IOException e) {
            logger.debug(
                    "[{}][{}] Failed to get PingAck from {} within {} ms",
                    getLocalMember(), period, pingMember, config.pingTimeout());
             processPingAck(null, period, pingMember, cid);
        }


    }


    private void doPingReq(
            long period, final Member pingMember, final List<Member> pingReqMembers) {


        logger.debug(
                "[{}][{}] Send nPing to {} for {}", getLocalMember(), period, pingReqMembers, pingMember);

        long timeout = config.pingInterval() - config.pingTimeout();

        BlockingQueue<ScheduledFuture<MemberStatus>> result = new ArrayBlockingQueue<>(pingReqMembers.size() + 1);

        Operation pingOp = new PingOp(getLocalMember(), pingMember);
        logger.debug(
                "[{}][{}] Send  Ping to {} agent one tcp", getLocalMember(), period, pingMember);

        try {
            Promise<Operation> promise = clusterService.tcpSendResponse(pingMember.address(), pingOp, timeout);
            ScheduledFuture<MemberStatus> schedule = clusterService.schedule(() -> {
                return processNPingAck(promise.getResult(), period, pingMember, pingMember, timeout);
            }, timeout, MILLISECONDS);
            result.add(schedule);
        } catch (IOException e) {
            logger.warn(
                    "[{}][{}] Timeout getting tcp nPingAck from {} to {} within {} ms",
                    getLocalMember(), period, pingOp, pingMember, timeout);

        }


        PingReqOp pingReqOp = new PingReqOp(getLocalMember(), pingMember);

        for (Member other : pingReqMembers) {
            try {
                logger.debug(
                        "[{}][{}] Send nPing to {} for {}", getLocalMember(), period, pingReqMembers, other);

                Promise<Operation> internalPromise = clusterService.udpSendResponse(other.address(), pingReqOp, timeout);
                ScheduledFuture<MemberStatus> internalSchedule = clusterService.schedule(() -> {
                    return processNPingAck(internalPromise.getResult(), period, pingMember, other, timeout);
                }, timeout, MILLISECONDS);
                result.add(internalSchedule);
            } catch (IOException e) {
                logger.warn(
                        "[{}][{}] Timeout getting udp nPingAck from {} to {} within {} ms",
                        getLocalMember(), period, pingReqOp, pingMember, timeout);
            }

        }

        MemberStatus memberStatus = SUSPECT;
        for (ScheduledFuture<MemberStatus> statusScheduledFuture : result) {
            MemberStatus status = null;
            try {
                status = statusScheduledFuture.get();
            } catch (Exception e) {}
            if (status != null) {
                if (status == ALIVE && memberStatus != DEAD) {
                    memberStatus = ALIVE;
                }
                if (status == DEAD) {
                    memberStatus = DEAD;
                }
            }
        }
        publishPingResult(period, pingMember, memberStatus);
    }


    private MemberStatus processNPingAck(Operation op, long period, Member pingMember, Member other, long timeout) {
        if (op == null) {
            logger.warn(
                    "[{}][{}] Timeout getting nPingAck from {} to {} within {} ms",
                    getLocalMember(), period, other, pingMember, timeout);
            return SUSPECT;
        } else {
            Address sender = op.sender();
            logger.debug(
                    "[{}][{}] Received nPingAck from {} to {}", getLocalMember(), period, sender, pingMember);
            return computeMemberStatus(op);
        }
    }

    private void processPingAck(Operation op, long period, Member pingMember, String cid) {
        if (op == null) {
            logger.debug(
                    "[{}][{}] Failed to get PingAck from {} within {} ms",
                    getLocalMember(),
                    period,
                    pingMember,
                    config.pingTimeout());

            // 10000    500
            final int timeLeft = config.pingInterval() - config.pingTimeout();
            // 剔除 pingMember   选取其他节点
            final List<Member> pingReqMembers = selectPingReqMembers(pingMember);
            if (timeLeft <= 0 || pingReqMembers.isEmpty()) {
                // 节点位置 状态
                logger.debug("[{}][{}] No PingReq occurred", getLocalMember(), period);
                publishPingResult(period, pingMember, SUSPECT);
            } else {
                doPingReq(currentPeriod, pingMember, pingReqMembers);
            }
            return;
        }

        Address sender = op.sender();
        logger.debug(
                "[{}][{}] Received PingAck from {}", getLocalMember(), period, sender);
        publishPingResult(period, pingMember, computeMemberStatus(op));
    }

    private void publishPingResult(long period, Member member, MemberStatus status) {
        logger.debug("[{}][{}] Member {} detected as {}", getLocalMember(), period, member, status);
        clusterService.execute(() ->
                clusterService.onFailureDetectorEvent(new FailureDetectorEvent(member, status)));

    }

    private Member selectPingMember() {
        if (pingMembers.isEmpty()) {
            return null;
        }
        if (pingMemberIndex >= pingMembers.size()) {
            pingMemberIndex = 0;
            Collections.shuffle(pingMembers);
        }
        return pingMembers.get(pingMemberIndex++);
    }

    private List<Member> selectPingReqMembers(Member pingMember) {
        if (config.pingReqMembers() <= 0) {
            return Collections.emptyList();
        }
        List<Member> candidates = new ArrayList<>(pingMembers);
        candidates.remove(pingMember);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.shuffle(candidates);
        boolean selectAll = candidates.size() < config.pingReqMembers();
        return selectAll ? candidates : candidates.subList(0, config.pingReqMembers());
    }


    private MemberStatus computeMemberStatus(Operation op) {

        AbstractPingAck pingAckOp = (AbstractPingAck) op;
        boolean ack = pingAckOp.isAck();
        if (ack) {
            return ALIVE;
        } else {
            return DEAD;
        }
    }


}
