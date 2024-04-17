package com.bin.protocol.gossip.cluster;

import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.failurefetector.FailureDetectorEvent;
import com.bin.protocol.gossip.cluster.failurefetector.MemberStatus;
import com.bin.protocol.gossip.cluster.membership.MembershipConfig;
import com.bin.protocol.gossip.cluster.membership.MembershipEvent;
import com.bin.protocol.gossip.cluster.membership.MembershipRecord;

import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.cluster.operation.Promise;
import com.bin.protocol.gossip.cluster.operation.membership.ChangeMembershipOp;
import com.bin.protocol.gossip.cluster.operation.membership.ChangeMembershipsAckOp;
import com.bin.protocol.gossip.cluster.operation.membership.ChangeMembershipsOp;
import com.bin.protocol.gossip.common.Address;
import com.bin.protocol.gossip.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MembershipManager {

    private static final Logger logger = LoggerFactory.getLogger(MembershipManager.class);


    private enum MembershipUpdateReason {
        FAILURE_DETECTOR_EVENT,
        MEMBERSHIP_GOSSIP, // membership
        SYNC,
        INITIAL_SYNC,
        SUSPICION_TIMEOUT
    }


    private ClusterService clusterService;


    private final List<Address> seedMembers;


    private final Map<String, MembershipRecord> membershipTable = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();

    private final MembershipConfig membershipConfig;
    private final Set<String> aliveEmittedSet = new HashSet<>();

    private final Map<String, ScheduledFuture<?>> suspicionTimeoutTasks = new HashMap<>();


    public MembershipManager(ClusterService clusterService) {
        this.clusterService = clusterService;
        this.membershipConfig = clusterService.getConfig().membershipConfig();
        seedMembers = cleanUpSeedMembers(clusterService.getSeedMembers());
        membershipTable.put(getLocalMember().id(), new MembershipRecord(getLocalMember(), MemberStatus.ALIVE, 0));
        members.put(getLocalMember().id(), getLocalMember());
    }

    public void start() {
        if (seedMembers.isEmpty()) {
            schedulePeriodicChange(0);
            return;
        }
        logger.info("[{}] Making initial Sync to all seed members: {}", getLocalMember(), seedMembers);
        List<Address> seedMemberList = new ArrayList<>(seedMembers);
        List<MembershipRecord> membershipRecords = new ArrayList<>(membershipTable.values());
        int timeout = membershipConfig.syncTimeout();
        for (Address address : seedMemberList) {
            String cid = UUID.randomUUID().toString();
            ChangeMembershipsOp changeMembershipsOp = new ChangeMembershipsOp(membershipRecords);
            changeMembershipsOp.setCorrelationId(cid);
            Promise<Operation> promise = null;
            try {
                promise = clusterService.tcpSendResponse(address, changeMembershipsOp, timeout);
            } catch (IOException e) {
                logger.warn(
                        "[{}] Exception on initial changeMembership, cause: {}",
                        getLocalMember(),
                        e.toString());
            }
            promise.subscribe(op -> onChangeMembershipsAck(op, true));
        }

        schedulePeriodicChange(timeout + 500);
    }


    public void stop() {
        leaveCluster();
        for (String memberId : suspicionTimeoutTasks.keySet()) {
            ScheduledFuture<?> future = suspicionTimeoutTasks.get(memberId);
            if (future != null && !future.isCancelled()) {
                future.isCancelled();
            }
        }

        suspicionTimeoutTasks.clear();
    }



    public void onChangeMembership(Operation op) {
        final Address sender = op.sender();
        logger.debug("[{}] Received onChangeMembership from {}", getLocalMember(), sender);
        changeMembership((ChangeMembershipsOp) op,false);
        String cid = op.getCorrelationId();
        List<MembershipRecord> membershipRecords = new ArrayList<>(membershipTable.values());
        ChangeMembershipsAckOp changeMembershipsAckOp = new ChangeMembershipsAckOp(membershipRecords);
        changeMembershipsAckOp.setCorrelationId(cid);
        logger.info("[{}] onChangeMembership [{}] value[{}]",getLocalMember(),sender,(ChangeMembershipsOp) op);
        try {
            clusterService.tcpSend(sender,changeMembershipsAckOp);
        } catch (IOException e) {
            logger.warn(
                    "[{}] Failed to send ChangeMembershipAck to {}, cause: {}",
                    getLocalMember(), sender, e.toString());
        }
    }


    public void onChangeMembershipAck(Operation op) {
        if (StringUtils.isEmpty(op.getCorrelationId())) {
            final Address sender = op.sender();
            logger.debug("[{}] Received onChangeMembershipAck from {}", getLocalMember(), sender);
            changeMembership((ChangeMembershipsOp) op, false);
        }
    }




    public void onChangeMembershipsAck(Operation op, boolean onStart) {
        logger.debug("[{}] Received onChangeMembershipAck from {}", getLocalMember(), op.sender());
        changeMembership((ChangeMembershipsOp) op, onStart);
    }


    private void changeMembership(ChangeMembershipsOp membershipOp, boolean onStart) {
        MembershipUpdateReason reason =
                onStart ? MembershipUpdateReason.INITIAL_SYNC : MembershipUpdateReason.SYNC;
        for (MembershipRecord membershipRecord : membershipOp.getMembership()) {
            try {
                changeMembership(membershipRecord, reason);
            } catch (Exception ex) {
                logger.warn(
                        "[{}][changeMembership][{}][error] cause: {}",
                        getLocalMember(), reason, ex.toString());
            }
        }
    }

    private void changeMembership(MembershipRecord remoteRecord, MembershipUpdateReason reason) {
        Objects.requireNonNull(remoteRecord, "Membership record can't be null");
        String localNamespace = membershipConfig.namespace();
        String namespace = remoteRecord.member().namespace();
        if (!areNamespacesRelated(localNamespace, namespace)) {
            logger.debug(
                    "[{}][changeMembership][{}] Skipping update, "
                            + "namespace not matched, local: {}, inbound: {}",
                    getLocalMember(),
                    reason,
                    localNamespace,
                    namespace);
            return;
        }
        MembershipRecord localRecord = membershipTable.get(remoteRecord.member().id());
        if ((localRecord == null || !localRecord.isLeaving())
                && !remoteRecord.isOverrides(localRecord)) {
            logger.debug(
                    "[{}][changeMembership][{}] Skipping update, "
                            + "can't override localRecord: {} with received remoteRecord: {}",
                    getLocalMember(),
                    reason,
                    localRecord,
                    remoteRecord);
            return;
        }

        if (remoteRecord.member().address().equals(getLocalMember().address())) {
            if (remoteRecord.member().id().equals(getLocalMember().id())) {
                onSelfMemberDetected(localRecord, remoteRecord, reason);  // 新旧节点 都离开集群 r0 有可能null
            }
            return;
        }

        if (remoteRecord.isLeaving()) {
            onLeavingDetected(localRecord, remoteRecord);
            return;
        }

        if (remoteRecord.isDead()) {
            onDeadMemberDetected(remoteRecord);
            return;
        }

        if (remoteRecord.isSuspect()) {
            // Update membership and schedule/cancel suspicion timeout task
            if (localRecord == null || !localRecord.isLeaving()) {
                membershipTable.put(remoteRecord.member().id(), remoteRecord);
            }
            // 定时任务 转成dead状态 发起新的通知
            scheduleSuspicionTimeoutTask(remoteRecord);
            spreadMembershipGossipUnlessGossiped(remoteRecord, reason);
        }

        if (remoteRecord.isAlive()) {
            onAliveMemberDetected(localRecord, remoteRecord, reason);
        }

    }


    private void onSelfMemberDetected(MembershipRecord localRecord,
                                      MembershipRecord remoteRecord,
                                      MembershipUpdateReason reason) {
        int currentIncarnation = Math.max(localRecord.incarnation(), remoteRecord.incarnation());
        MembershipRecord newRecord =
                new MembershipRecord(getLocalMember(), localRecord.status(), currentIncarnation + 1);
        membershipTable.put(getLocalMember().id(), newRecord);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "[{}][updateMembership][{}] Updating incarnation, "
                            + "local record r0: {} to received r1: {}, "
                            + "spreading with increased incarnation r2: {}",
                    getLocalMember(), reason, localRecord, remoteRecord, newRecord);
        }
        spreadMembershipGossip(newRecord);
    }


    private void onLeavingDetected(MembershipRecord localRecord,
                                   MembershipRecord remoteRecord) {
        final Member member = remoteRecord.member();
        final String memberId = member.id();
        membershipTable.put(memberId, remoteRecord);

        if (localRecord != null
                // 本地 副本 活跃  未知状态        同时 在活跃的集合中
                && (localRecord.isAlive() ||
                (localRecord.isSuspect() &&
                        aliveEmittedSet.contains(memberId)))) {

            final long timestamp = System.currentTimeMillis();
            publishEvent(MembershipEvent.createLeaving(member,  timestamp));
        }


        // 本地副本节点不是优雅离开 异常推出
        if (localRecord == null || !localRecord.isLeaving()) {
            scheduleSuspicionTimeoutTask(remoteRecord);
            spreadMembershipGossip(remoteRecord); // 广播新的节点信息,因为旧的节点非正常结束
        }

    }


    private void onDeadMemberDetected(MembershipRecord remoteRecord) {
        final Member member = remoteRecord.member();

        cancelSuspicionTimeoutTask(member.id());

        if (!members.containsKey(member.id())) {
            return;
        }

        // Removed membership
        members.remove(member.id());
        final MembershipRecord localRecord = membershipTable.remove(member.id());
        aliveEmittedSet.remove(member.id());

        // Log that member left gracefully or without notification
        if (localRecord.isLeaving()) {
            logger.info("[{}] Member left gracefully: {}", getLocalMember(), member);
        } else {
            logger.info("[{}] Member left without notification: {}", getLocalMember(), member);
        }

        final long timestamp = System.currentTimeMillis();
        publishEvent(MembershipEvent.createRemoved(member, timestamp));
    }


    private void onAliveMemberDetected(MembershipRecord localRecord,
                                       MembershipRecord remoteRecord,
                                       MembershipUpdateReason reason) {
        if (localRecord != null && localRecord.isLeaving()) {
            onAliveAfterLeaving(remoteRecord);
            return;
        }

        // New alive or updated alive
        if (localRecord == null || localRecord.incarnation() < remoteRecord.incarnation()) {
            try {
                cancelSuspicionTimeoutTask(remoteRecord.member().id());
                spreadMembershipGossipUnlessGossiped(remoteRecord, reason);

                onAliveMemberDetected(remoteRecord);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.warn(
                        "[{}][updateMembership][{}] Skipping to add/update member: {}, "
                                + "due to failed fetchMetadata call (cause: {})",
                        getLocalMember(),
                        reason,
                        remoteRecord,
                        ex.toString());
            }
        }
    }

    private void onAliveMemberDetected(MembershipRecord remoteRecord) {
        final Member member = remoteRecord.member();

        final boolean memberExists = members.containsKey(member.id());

        final long timestamp = System.currentTimeMillis();
        MembershipEvent event = null;
        if (!memberExists) {
            event = MembershipEvent.createAdded(member, timestamp);
        }

        members.put(member.id(), member);
        membershipTable.put(member.id(), remoteRecord);

        if (event != null) {

            publishEvent(event);

            if (event.isAdded()) {
                aliveEmittedSet.add(member.id());
            }
        }
    }


    private void onAliveAfterLeaving(MembershipRecord remoteRecord) {
        final Member member = remoteRecord.member();
        final String memberId = member.id();

        members.put(memberId, member);

        // Emit events if needed and ignore alive
        if (aliveEmittedSet.add(memberId)) {
            final long timestamp = System.currentTimeMillis();

            // There is no metadata in this case
            // We could'n fetch metadata because node already wanted to leave
            publishEvent(MembershipEvent.createAdded(member,  timestamp));
            publishEvent(MembershipEvent.createLeaving(member,  timestamp));
        }
    }

    private void spreadMembershipGossipUnlessGossiped(
            MembershipRecord r, MembershipUpdateReason reason) {
        // Spread gossip (unless already gossiped)
        if (reason != MembershipUpdateReason.MEMBERSHIP_GOSSIP
                && reason != MembershipUpdateReason.INITIAL_SYNC) {
            spreadMembershipGossip(r);
        }
    }


    public void publishEvent(MembershipEvent event) {
        logger.info("[{}][publish MembershipEvent] {}", getLocalMember(), event);
        clusterService.onMembershipEvent(event);
    }

    //  listen
    public void onFailureDetectorEvent(FailureDetectorEvent fdEvent) {
        MembershipRecord r0 = membershipTable.get(fdEvent.member().id());
        if (r0 == null) { // member already removed
            return;
        }
        if (r0.status() == fdEvent.status()) { // status not changed
            return;
        }
        logger.debug("[{}][FailureDetectorEvent] Received status change: {}", getLocalMember(), fdEvent);
        if (fdEvent.status() == MemberStatus.ALIVE) {
            // TODO: Consider to make more elegant solution
            // Alive won't override SUSPECT so issue instead extra sync with member to force it spread
            // alive with inc + 1
            // Alive不会覆盖SUSPECT，因此请与成员进行额外同步，以强制其使用inc+1进行活动传播
            List<MembershipRecord> membershipRecords = new ArrayList<>(membershipTable.values());
            ChangeMembershipsOp changeMembershipsOp = new ChangeMembershipsOp(membershipRecords);

            Address address = fdEvent.member().address();

            try {
                clusterService.tcpSend(address, changeMembershipsOp);
            } catch (IOException e) {
                logger.warn(
                        "[{}][FailureDetectorEvent] Failed to send Sync to {}, cause: {}",
                        getLocalMember(), address, e.toString());
            }

        } else {
            MembershipRecord record =
                    new MembershipRecord(r0.member(), fdEvent.status(), r0.incarnation());
            try {
                changeMembership(record, MembershipUpdateReason.FAILURE_DETECTOR_EVENT);
            } catch (Exception ex) {
                logger.error(
                        "[{}][onFailureDetectorEvent][updateMembership][error] cause:",
                        getLocalMember(),
                        ex);
            }

        }
    }


    /**
     * 加入集群、离开
     *   gossipProtocol
     *    todo
     */
    public void onMembershipGossip(Operation op) {
        ChangeMembershipOp changeMembershipOp = (ChangeMembershipOp) op;
        MembershipRecord record = changeMembershipOp.getMembershipRecord();
        logger.debug("[{}] Received membership gossip: {}", getLocalMember(), record);
        try {
            changeMembership(record, MembershipUpdateReason.MEMBERSHIP_GOSSIP);
        } catch (Exception ex) {
            logger.error(
                    "[{}][onMembershipGossip][updateMembership][error] cause:", getLocalMember(), ex);
        }

    }

    // listen end ---------------------------

    private void spreadMembershipGossip(MembershipRecord r) {
        ChangeMembershipOp changeMembershipOp = new ChangeMembershipOp(r);
        logger.debug("[{}] Send membership with gossip", getLocalMember());
        clusterService.spread(changeMembershipOp);
    }

    private void scheduleSuspicionTimeoutTask(MembershipRecord r) {
        long suspicionTimeout =
                ClusterMath.suspicionTimeout(
                        membershipConfig.suspicionMult(),
                        membershipTable.size(),
                        clusterService.getConfig().failureDetectorConfig().pingInterval());
        suspicionTimeoutTasks.computeIfAbsent(
                r.member().id(),
                id -> {
                    logger.debug(
                            "[{}] Scheduled SuspicionTimeoutTask for {}, suspicionTimeout: {}",
                            getLocalMember(),
                            id,
                            suspicionTimeout);
                    return clusterService.schedule(
                            () -> onSuspicionTimeout(id), suspicionTimeout, TimeUnit.MILLISECONDS);
                });
    }


    private void onSuspicionTimeout(String memberId) {
        suspicionTimeoutTasks.remove(memberId);
        MembershipRecord r = membershipTable.get(memberId);
        if (r != null) {
            logger.debug("[{}] Declare SUSPECTED member {} as DEAD by timeout", getLocalMember(), r);
            MembershipRecord deadRecord = new MembershipRecord(r.member(), MemberStatus.DEAD, r.incarnation());
            try {
                changeMembership(deadRecord, MembershipUpdateReason.SUSPICION_TIMEOUT);
            } catch (Exception e) {
                logger.error("[{}][onSuspicionTimeout][updateMembership][error] cause:", getLocalMember(), e);
            }
        }
    }


    private void schedulePeriodicChange(int base) {
        // 30_000
        int syncInterval = membershipConfig.syncInterval();
        clusterService.scheduleWithFixedDelay(this::doChangeMembership, base + syncInterval, syncInterval, TimeUnit.MILLISECONDS);
    }

    private void doChangeMembership() {
        Address address = selectSyncAddress();
        if (address == null) {
            return;
        }
        List<MembershipRecord> membershipRecords = new ArrayList<>(membershipTable.values());
        ChangeMembershipsOp changeMembershipsOp = new ChangeMembershipsOp(membershipRecords);
        logger.debug("[{}][changeMembership] Send Sync to {}", getLocalMember(), address);
        try {
            clusterService.tcpSend(address, changeMembershipsOp);
        } catch (IOException e) {
            logger.warn(
                    "[{}][doSync] Failed to send Sync to {}, cause: {}",
                    getLocalMember(), address, e.toString());
        }
    }

    private void cancelSuspicionTimeoutTask(String memberId) {
        ScheduledFuture<?> future = suspicionTimeoutTasks.remove(memberId);
        if (future != null && !future.isCancelled()) {
            logger.debug("[{}] Cancelled SuspicionTimeoutTask for {}", getLocalMember(), memberId);
            future.cancel(false);
        }
    }

    private void leaveCluster() {
        MembershipRecord curRecord = membershipTable.get(getLocalMember().id());
        MembershipRecord newRecord =
                new MembershipRecord(getLocalMember(), MemberStatus.LEAVING, curRecord.incarnation() + 1);
        membershipTable.put(getLocalMember().id(), newRecord);
        spreadMembershipGossip(newRecord);
    }



    public Collection<Member> members() {
        return new ArrayList<>(members.values());
    }



    public Member member(String id) {
        return members.get(id);
    }

    public Member member(Address address) {
        Collection<Member> values = members.values();
        for (Member member : members.values()) {
            if (member.address().equals(address))
                return member;
        }
        return null;
    }

    //  help method ---------------------------------------------
    public Member getLocalMember() {
        return clusterService.getLocalMember();
    }


    private Address selectSyncAddress() {
        List<Address> addresses =
                Stream.concat(seedMembers.stream(), otherMembers().stream().map(Member::address))
                        .collect(Collectors.collectingAndThen(Collectors.toSet(), ArrayList::new));
        Collections.shuffle(addresses);
        if (addresses.isEmpty()) {
            return null;
        } else {
            int i = ThreadLocalRandom.current().nextInt(addresses.size());
            return addresses.get(i);
        }
    }

    public Collection<Member> otherMembers() {
        return new ArrayList<>(members.values())
                .stream().filter(member -> !member.equals(getLocalMember())).collect(Collectors.toList());
    }

    private List<Address> cleanUpSeedMembers(Collection<Address> seedMembers) {
        InetAddress localIpAddress = Address.getLocalIpAddress();

        String hostAddress = localIpAddress.getHostAddress();
        String hostName = localIpAddress.getHostName();

        Address memberAddr = getLocalMember().address();
        Address transportAddr = clusterService.getLocalTransport();
        Address memberAddrByHostAddress = Address.create(hostAddress, memberAddr.port());
        Address transportAddrByHostAddress = Address.create(hostAddress, transportAddr.port());
        Address memberAddByHostName = Address.create(hostName, memberAddr.port());
        Address transportAddrByHostName = Address.create(hostName, transportAddr.port());

        return new LinkedHashSet<>(seedMembers)
                .stream()
                .filter(addr -> checkAddressesNotEqual(addr, memberAddr))
                .filter(addr -> checkAddressesNotEqual(addr, transportAddr))
                .filter(addr -> checkAddressesNotEqual(addr, memberAddrByHostAddress))
                .filter(addr -> checkAddressesNotEqual(addr, transportAddrByHostAddress))
                .filter(addr -> checkAddressesNotEqual(addr, memberAddByHostName))
                .filter(addr -> checkAddressesNotEqual(addr, transportAddrByHostName))
                .collect(Collectors.toList());
    }

    private boolean checkAddressesNotEqual(Address address0, Address address1) {
        if (!address0.equals(address1)) {
            return true;
        } else {
            logger.warn("[{}] Filtering out seed address: {}", getLocalMember(), address0);
            return false;
        }
    }

    private static boolean areNamespacesRelated(String namespace1, String namespace2) {
        Path ns1 = Paths.get(namespace1);
        Path ns2 = Paths.get(namespace2);

        if (ns1.compareTo(ns2) == 0) {
            return true;
        }

        int n1 = ns1.getNameCount();
        int n2 = ns2.getNameCount();
        if (n1 == n2) {
            return false;
        }

        Path shorter = n1 < n2 ? ns1 : ns2;
        Path longer = n1 < n2 ? ns2 : ns1;

        boolean areNamespacesRelated = true;
        for (int i = 0; i < shorter.getNameCount(); i++) {
            if (!shorter.getName(i).equals(longer.getName(i))) {
                areNamespacesRelated = false;
                break;
            }
        }
        return areNamespacesRelated;
    }
}
