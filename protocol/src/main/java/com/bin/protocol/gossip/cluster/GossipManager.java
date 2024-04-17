package com.bin.protocol.gossip.cluster;

import com.bin.protocol.gossip.Member;
import com.bin.protocol.gossip.cluster.membership.MembershipListener;
import com.bin.protocol.gossip.cluster.gossip.Gossip;
import com.bin.protocol.gossip.cluster.gossip.GossipConfig;
import com.bin.protocol.gossip.cluster.gossip.GossipState;
import com.bin.protocol.gossip.cluster.gossip.SequenceIdCollector;
import com.bin.protocol.gossip.cluster.membership.MembershipEvent;
import com.bin.protocol.gossip.cluster.operation.GossipMessageOp;
import com.bin.protocol.gossip.cluster.operation.GossipOp;
import com.bin.protocol.gossip.cluster.operation.Operation;
import com.bin.protocol.gossip.cluster.operation.membership.ChangeMembershipOp;
import com.bin.protocol.gossip.common.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GossipManager implements MembershipListener {

    private static final Logger logger = LoggerFactory.getLogger(GossipManager.class);

    private ClusterService clusterService;

    private final GossipConfig config;

    private long currentPeriod = 0;
    private long gossipCounter = 0;
    private final Map<String, SequenceIdCollector> sequenceIdCollectors = new HashMap<>();
    private final Map<String, GossipState> gossips = new HashMap<>();

    private final List<Member> remoteMembers = new ArrayList<>();
    private int remoteMembersIndex = -1;


    public GossipManager(ClusterService clusterService) {
        this.clusterService = clusterService;
        config = clusterService.getConfig().gossipConfig();
    }


    public void start() {
        clusterService.scheduleWithFixedDelay(() -> doSpreadGossip(),
                config.gossipInterval(),
                config.gossipInterval(),
                TimeUnit.MILLISECONDS);

    }


    public String spread(Operation op) {
        return createAndPutGossip(op);
    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        Member member = event.member();
        if (event.isRemoved()) {
            boolean removed = remoteMembers.remove(member);
            sequenceIdCollectors.remove(member.id());
            if (removed) {
                logger.debug(
                        "[{}][{}] Removed {} from remoteMembers list (size={})",
                        getLocalMember(),
                        currentPeriod,
                        member,
                        remoteMembers.size());
            }
        }
        if (event.isAdded()) {
            remoteMembers.add(member);
            logger.debug(
                    "[{}][{}] Added {} to remoteMembers list (size={})",
                    getLocalMember(),
                    currentPeriod,
                    member,
                    remoteMembers.size());
        }
    }


    public void onGossip(Operation op) {
        final long period = this.currentPeriod;
        final GossipOp gossipOp = (GossipOp) op;
        for (Gossip gossip : gossipOp.getGossips()) {
            GossipState gossipState = gossips.get(gossip.gossipId());
            if (ensureSequence(gossip.gossiperId()).add(gossip.sequenceId())) {
                if (gossipState == null) { // new gossip
                    gossipState = new GossipState(gossip, period);
                    gossips.put(gossip.gossipId(), gossipState);

                    if (gossip.operation() instanceof ChangeMembershipOp)
                        clusterService.onMembershipGossip(gossip.operation());
                    else {
                        if (gossip.operation() instanceof GossipMessageOp) {
                            GossipMessageOp messageOp = (GossipMessageOp) gossip.operation();
                            clusterService.onGossip(messageOp.getMessage());
                        }
                    }

                }
            }
            if (gossipState != null) {
                gossipState.addToInfected(gossipOp.getFrom());
            }
        }
    }


    private void doSpreadGossip() {
        // Increment period
        long period = currentPeriod++;

        // Check segments
        checkGossipSegmentation();

        // Check any gossips exists
        if (gossips.isEmpty()) {
            return; // nothing to spread
        }

        try {
            // Spread gossips to randomly selected member(s)
            selectGossipMembers().forEach(member -> spreadGossipsTo(period, member));

            // Sweep gossips
            Set<String> gossipsToRemove = getGossipsToRemove(period);
            if (!gossipsToRemove.isEmpty()) {
                logger.debug("[{}][{}] Sweep gossips: {}", getLocalMember(), period, gossipsToRemove);
                for (String gossipId : gossipsToRemove) {
                    gossips.remove(gossipId);
                }
            }

            // Check spread gossips
            Set<String> gossipsThatSpread = getGossipsThatMostLikelyDisseminated(period);
            if (!gossipsThatSpread.isEmpty()) {
                logger.debug(
                        "[{}][{}] Most likely disseminated gossips: {}",
                        getLocalMember(),
                        period,
                        gossipsThatSpread);
            }
        } catch (Exception ex) {
            logger.warn("[{}][{}][doSpreadGossip] Exception occurred:", getLocalMember(), period, ex);
        }
    }


    private void spreadGossipsTo(long period, Member member) {
        // Select gossips to send
        List<Gossip> gossips = selectGossipsToSend(period, member);
        if (gossips.isEmpty()) {
            return; // nothing to spread
        }

        // Send gossip request
        Address address = member.address();

        for (Gossip gossip : gossips) {
            GossipOp gossipOp = new GossipOp(gossip, getLocalMember().id());
            try {
                clusterService.tcpSend(address, gossipOp);
            } catch (IOException e) {
                logger.debug(
                        "[{}][{}] Failed to send GossipReq({}) to {}, cause: {}",
                        getLocalMember(),
                        period,
                        gossipOp,
                        address,
                        e.toString());
            }
        }
    }


    private String createAndPutGossip(Operation op) {
        final long period = this.currentPeriod;
        final Gossip gossip = createGossip(op);
        final GossipState gossipState = new GossipState(gossip, period);

        gossips.put(gossip.gossipId(), gossipState);
        ensureSequence(getLocalMember().id()).add(gossip.sequenceId());

        return gossip.gossipId();
    }

    private void checkGossipSegmentation() {
        final int intervalsThreshold = config.gossipSegmentationThreshold();
        for (Map.Entry<String, SequenceIdCollector> entry : sequenceIdCollectors.entrySet()) {
            // Size of sequenceIdCollector could grow only if we never received some messages.
            // Which is possible only if current node wasn't available(suspected) for some time
            // or network issue
            final SequenceIdCollector sequenceIdCollector = entry.getValue();
            if (sequenceIdCollector.size() > intervalsThreshold) {
                logger.warn(
                        "[{}][{}] Too many missed gossip messages from original gossiper: '{}', "
                                + "current node({}) was SUSPECTED much for a long time or connection problem",
                        getLocalMember(),
                        currentPeriod,
                        entry.getKey(),
                        getLocalMember());

                sequenceIdCollector.clear();
            }
        }
    }

    private List<Member> selectGossipMembers() {
        int gossipFanout = config.gossipFanout();
        if (remoteMembers.size() < gossipFanout) {
            return remoteMembers;
        } else { // random members
            // 先打乱成员的顺序，一旦达到上限
            if (remoteMembersIndex < 0 || remoteMembersIndex + gossipFanout > remoteMembers.size()) {
                Collections.shuffle(remoteMembers);
                remoteMembersIndex = 0;
            }


            List<Member> selectedMembers =
                    gossipFanout == 1
                            ? Collections.singletonList(remoteMembers.get(remoteMembersIndex))
                            : remoteMembers.subList(remoteMembersIndex, remoteMembersIndex + gossipFanout);


            remoteMembersIndex += gossipFanout;
            return selectedMembers;
        }
    }


    private List<Gossip> selectGossipsToSend(long period, Member member) {
        int periodsToSpread =
                ClusterMath.gossipPeriodsToSpread(config.gossipRepeatMult(), remoteMembers.size() + 1);
        return gossips.values().stream()
                .filter(
                        gossipState -> gossipState.infectionPeriod() + periodsToSpread >= period) // max rounds
                .filter(gossipState -> !gossipState.isInfected(member.id())) // already infected
                .map(GossipState::gossip)
                .collect(Collectors.toList());
    }


    private Set<String> getGossipsToRemove(long period) {
        // Select gossips to sweep
        int periodsToSweep =
                ClusterMath.gossipPeriodsToSweep(config.gossipRepeatMult(), remoteMembers.size() + 1);
        return gossips.values().stream()
                .filter(gossipState -> period > gossipState.infectionPeriod() + periodsToSweep)
                .map(gossipState -> gossipState.gossip().gossipId())
                .collect(Collectors.toSet());
    }

    private Set<String> getGossipsThatMostLikelyDisseminated(long period) {
        // Select gossips to spread
        int periodsToSpread =
                ClusterMath.gossipPeriodsToSpread(config.gossipRepeatMult(), remoteMembers.size() + 1);
        return gossips.values().stream()
                .filter(gossipState -> period > gossipState.infectionPeriod() + periodsToSpread)
                .map(gossipState -> gossipState.gossip().gossipId())
                .collect(Collectors.toSet());
    }


    public Gossip createGossip(Operation op) {
        return new Gossip(getLocalMember().id(), op, gossipCounter++);
    }

    public SequenceIdCollector ensureSequence(String key) {
        return sequenceIdCollectors.computeIfAbsent(key, s -> new SequenceIdCollector());
    }

    public Member getLocalMember() {
        return clusterService.getLocalMember();
    }

}
