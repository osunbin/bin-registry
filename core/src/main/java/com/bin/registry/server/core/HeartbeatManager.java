package com.bin.registry.server.core;

import com.bin.registry.server.core.task.NodeHeartbeat;
import com.bin.registry.server.core.timer.DelayedOperationPurgatory;
import com.bin.registry.server.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HeartbeatManager {

    private static Logger logger = LoggerFactory.getLogger(HeartbeatManager.class);

    public static final DelayedOperationPurgatory<NodeHeartbeat> heartbeatTimer =
            new DelayedOperationPurgatory("heartbeatTimer");



    /**
     *  第一次来加入时间论
     *  之后每次来把之前的任务取消,重置时间
     *
     *  5秒上报
     *  15秒标记不健康
     *  30秒剔除
     */
    public static void heartbeat(Node node) {
        // extends WatchKey
        NodeHeartbeat operation = new NodeHeartbeat(15 * 1000,node);
        heartbeatTimer.tryCompleteElseWatch(operation, List.of(node));

    }


    public static void againHeartbeat(Node node) {
        List<NodeHeartbeat> connHeartbeats = heartbeatTimer.cancelForKey(node);
        if (connHeartbeats.size() > 0) {
            NodeHeartbeat connHeartbeat = connHeartbeats.get(0);
            heartbeatTimer.tryCompleteElseWatch(connHeartbeat, List.of(node));
        } else {
            heartbeat(node);
        }
    }
}
