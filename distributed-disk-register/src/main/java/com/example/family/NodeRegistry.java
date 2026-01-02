package com.example.family;

import family.NodeInfo;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {

    private final Set<NodeInfo> nodes = ConcurrentHashMap.newKeySet();

    public void add(NodeInfo node) {
        nodes.add(node);
    }

    public void addAll(Collection<NodeInfo> others) {
        nodes.addAll(others);
    }

    public List<NodeInfo> snapshot() {
        return new ArrayList<>(nodes);
    }

    public void remove(NodeInfo node) {
        nodes.remove(node);
    }

    public void increaseCount(NodeInfo tobeRemoved) {
        String host = tobeRemoved.getHost();
        int port = tobeRemoved.getPort();
        int messageCount = tobeRemoved.getMessageCount();

        nodes.remove(tobeRemoved);

        NodeInfo newNodeInfo = NodeInfo.newBuilder()
                .setHost(host)
                .setPort(port)
                .setMessageCount(messageCount + 1)
                .build();

        nodes.add(newNodeInfo);
    }
}
