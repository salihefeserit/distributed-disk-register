package com.example.family;

import family.NodeInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {

    // "host:port" -> "info"
    private final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();

    private String getKey(NodeInfo node) {
        return node.getHost() + ":" + node.getPort();
    }

    public void add(NodeInfo node) {
        nodes.put(getKey(node), node);
    }

    public void addAll(Collection<NodeInfo> others) {
        for (NodeInfo n : others) {
            add(n);
        }
    }

    public List<NodeInfo> snapshot() {
        return new ArrayList<>(nodes.values());
    }

    public void remove(NodeInfo node) {
        nodes.remove(getKey(node));
    }

    public void increaseLoad(NodeInfo node, long messageSize) {
        String key = getKey(node);
        nodes.compute(key, (k, current) -> {
            int currentCount = (current != null) ? current.getMessageCount() : node.getMessageCount();
            long currentBytes = (current != null) ? current.getTotalBytes() : node.getTotalBytes();

            return NodeInfo.newBuilder()
                    .setHost(node.getHost())
                    .setPort(node.getPort())
                    .setMessageCount(currentCount + 1)
                    .setTotalBytes(currentBytes + messageSize)
                    .build();
        });
    }
}