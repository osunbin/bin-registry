package com.bin.registry.server.core;


import com.bin.registry.server.common.utils.IpUtils;
import com.bin.registry.server.common.utils.JsonUtils;
import com.bin.registry.server.http.NettyHttpRequest;
import com.bin.registry.server.model.CallerInstance;
import com.bin.registry.server.model.JsonResult;
import com.bin.registry.server.model.Node;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class RegistryCenterApis {

    private static Logger logger = LoggerFactory.getLogger(RegistryCenterApis.class);




    public RegistryCenterApis() {

    }


    public JsonResult<List<Node>> discovery(Channel channel, NettyHttpRequest nettyHttpRequest) {
        CallerInstance caller = null;
        if (nettyHttpRequest.content().isReadable()) {
            caller = JsonUtils.readCaller(nettyHttpRequest.content());
        }
       return poll(channel,caller);
    }


    public JsonResult<List<Node>> poll(Channel channel,CallerInstance caller) {
        if (caller != null) {
            String callerName = caller.getCallerName();
            Set<String> serverInfo = caller.getServerInfo();
            NodeManager.registryCaller(callerName,serverInfo);
            Map<String,List<Node>> nodes = NodeManager.callerFetchNode(callerName, serverInfo);
           return JsonResult.<List<Node>>ok("success",nodes);
        } else {
            return JsonResult.<List<Node>>failed("caller info is null",null);
        }
    }




    public JsonResult<Boolean> registry(Channel channel, NettyHttpRequest nettyHttpRequest) {
        Node node = null;
        if (nettyHttpRequest.content().isReadable()) {
            node = JsonUtils.readNode(nettyHttpRequest.content());
        }
       return push(channel,node);
    }

    public JsonResult<Boolean> push(Channel channel,Node node) {
        if (node != null) {
            String host = IpUtils.resoleHost(channel.remoteAddress());
            node.setIp(host);

            registerNode(node);
            return JsonResult.<Boolean>ok("success",true);
        } else {
           return JsonResult.<Boolean>failed("node info is null",false);
        }
    }
    private void registerNode(Node node) {

        Node oldNode = NodeManager.isRegistry(node);
        if (oldNode != null) {
            if (!oldNode.isActive() && node.isActive()) {
                oldNode.setOnLineTime(new Date());
            }
            oldNode.setHeartbeatTime(System.currentTimeMillis());
            oldNode.setRunning(node.getRunning());
            oldNode.setTags(node.getTags());
            oldNode.setWeight(node.getWeight());
            oldNode.setMetadata(node.getMetadata());
            oldNode.setServiceId(node.getServiceId());
            oldNode.setPid(node.getPid());
            oldNode.setSystemEnv(node.getSystemEnv());
            oldNode.setZoneName(node.getZoneName());
            oldNode.setGroupArray(node.getGroupArray());
            oldNode.setContainer(node.getContainer());
            HeartbeatManager.againHeartbeat(oldNode);
            NodeManager.agentRegistryNode(oldNode);
        } else {
            // 第一次  nodeOnline  存储,
            Date date = new Date();
            node.setCreateTime(date);
            node.setOnLineTime(date);
            List<String> callers = NodeManager.registryNode(node);

            HeartbeatManager.heartbeat(node);
        }
    }






    private ExecutorService initPool(int coreSize, int maximumSize, String poolName) {
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        ThreadFactory threadFactory = genThreadFactory(poolName);

        return new ThreadPoolExecutor(coreSize,
                maximumSize, 0L, TimeUnit.MILLISECONDS,
                workQueue, threadFactory);
    }

    private  ThreadFactory genThreadFactory(String poolName) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);

                thread.setName(poolName + "-" + thread.getName());
                thread.setDaemon(true);
                return thread;
            }
        };
    }



}
