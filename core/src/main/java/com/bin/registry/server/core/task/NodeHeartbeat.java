package com.bin.registry.server.core.task;

import com.bin.registry.server.core.HeartbeatManager;
import com.bin.registry.server.core.NodeManager;
import com.bin.registry.server.core.timer.DelayedOperation;
import com.bin.registry.server.model.Node;

import java.util.List;


/**
 *   到时间
 */
public class NodeHeartbeat extends DelayedOperation {

    private Node node;


    public NodeHeartbeat(long delayMs, Node node) {
        super(delayMs);
        this.node = node;
    }


    /**
     * 过期了,剔除节点,通知客户端
     */
    @Override
    public void onExpiration() {
        long curr = System.currentTimeMillis() ;
        if(curr - node.getHeartbeatTime() < 16 * 1000) {
            // 亚健康
            HeartbeatManager.heartbeatTimer.
                    tryCompleteElseWatch(this, List.of(node));
        } else {

            List<String> callers = NodeManager.cancelNode(node);

        }
    }


    @Override
    public void onComplete() {

    }

    @Override
    public boolean tryComplete() {
        return false;
    }
}
