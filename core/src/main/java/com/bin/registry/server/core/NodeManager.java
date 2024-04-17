package com.bin.registry.server.core;

import com.bin.registry.server.common.collect.NonBlockingHashMap;
import com.bin.registry.server.common.utils.StringUtils;
import com.bin.registry.server.core.task.NodeHeartbeat;
import com.bin.registry.server.core.timer.DelayedOperationPurgatory;
import com.bin.registry.server.model.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class NodeManager {

    private static final ConcurrentMap<String, List<String>> discoveryMapping =
            new NonBlockingHashMap<>();


    private static final ConcurrentMap<String, List<String>> discoveryChangeMapping =
            new NonBlockingHashMap<>();


    /**
     *  注册的服务端的集群映射
     *   变更通知 -> 客户端带着MD5 和其他的node一一对比
     *
     */
    private static final ConcurrentMap<String, List<Node>> registryMapping =
            new NonBlockingHashMap<>();


    public static Map<String,List<Node>> callerFetchNode(String caller,Set<String> servers) {
        Map<String,List<Node>> callers = new HashMap<>();

        for (String serverName : servers) {

            List<Node> nodes = registryMapping.get(serverName);
            if (nodes == null){
                continue;
            }
            List<Node> result = new ArrayList<>();
           for (Node node : nodes) {
               if (node.isActive()) {
                   result.add(node);
               }
           }
            callers.put(serverName,nodes);
        }
        return callers;
    }


    public static void registryCaller(String caller,Set<String> servers) {
        List<String> strings = discoveryMapping.get(caller);
        if (strings == null) {
            synchronized (discoveryMapping) {
                if (strings == null) {
                    strings = new CopyOnWriteArrayList<>();
                }
            }
        }
        strings.clear();
        strings.addAll(servers);

        for (String server : servers) {
            List<String> callers = discoveryChangeMapping.get(server);
            if (callers == null) {
                synchronized (discoveryChangeMapping) {
                    if (callers == null) {
                        callers = new CopyOnWriteArrayList<>();
                    }
                }
            }
            synchronized (callers) {
                if (!callers.contains(caller))
                    callers.add(caller);
            }
        }

    }



    public static Node isRegistry(Node node) {
        List<Node> nodes = registryMapping.get(node.getServiceName());
        if (nodes == null) {
            synchronized (registryMapping) {
                if (nodes == null)
                     nodes = new CopyOnWriteArrayList<>();
            }
        }

        int index = nodes.lastIndexOf(node);
        if (index > 0) {
            return nodes.get(index);
        } else {
            return null;
        }
    }



    public static List<String> registryNode(Node node) {
        registryMapping.get(node.getServiceName()).add(node);
        return discoveryChangeMapping.get(node.getServiceName());
    }

    public static void agentRegistryNode(Node node) {
        List<Node> nodes = registryMapping.get(node.getServiceName());
        nodes.remove(node);
        nodes.add(node);
    }

    /**
     *  通知
     */
    public static List<String> cancelNode(Node node) {
        registryMapping.get(node.getServiceName()).remove(node);
        return discoveryChangeMapping.get(node.getServiceName());
    }


    public static List<Node> fetchAllNode() {
        Collection<List<Node>> values = registryMapping.values();
        return values.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static Map<String,List<Node>> fetchServerNode(String serverName,String ip) {
        if (StringUtils.isEmpty(serverName)) {
            return  Map.copyOf(registryMapping);
        }
        Map<String,List<Node>> serviceNode = new HashMap<>();
        List<Node> nodes = registryMapping.get(serverName);
        List<Node> servers = new ArrayList<>();
        if (StringUtils.isEmpty(ip)) {
            servers.addAll(nodes);
        } else {
            for (Node server : nodes){
                if (ip.equals(server.getIp())) {
                    nodes.add(server);
                    break;
                }
            }
        }
        serviceNode.put(serverName,servers);
        return serviceNode;
    }

    public static List<Node> fetchCallerNode(String callerName) {
        List<String> strings = discoveryMapping.get(callerName);
        List<Node> caller = new ArrayList<>();
        for (String server : strings) {
            caller.addAll(registryMapping.get(server));
        }
        return caller;
    }

    public static void openNode(String serviceName,String ip) {
        List<Node> nodes = registryMapping.get(serviceName);
        for (Node node : nodes) {
            if (StringUtils.isNotEmpty(ip)) {
                if (ip.equals(node.getIp())) {
                    node.setRunning(true);
                }
            } else {
                node.setRunning(true);
            }
        }
    }


    public static void closeNode(String serviceName,String ip) {
        List<Node> nodes = registryMapping.get(serviceName);
        for (Node node : nodes) {
            if (StringUtils.isNotEmpty(ip)) {
                if (ip.equals(node.getIp())) {
                    node.setRunning(false);
                }
            } else {
                node.setRunning(false);
            }
        }
    }

    public static void deleteNode(String serviceName,String ip) {
        List<Node> nodes = registryMapping.get(serviceName);
        Iterator<Node> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            if (StringUtils.isNotEmpty(ip)) {
                Node node = iterator.next();
                if (ip.equals(node.getIp())) {
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
    }

}
