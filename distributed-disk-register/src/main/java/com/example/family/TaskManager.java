package com.example.family;

import family.Empty;
import family.FamilyServiceGrpc;
import family.NodeInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskManager extends JFrame {

    private final DefaultTableModel tableModel;
    private final String targetHost = LeaderNode.LEADER_HOST;
    private final int targetPort = 5555;

    public TaskManager() {
        setTitle("Görev Yöneticisi");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        String[] columns = { "Host", "Port", "Mesaj Sayısı", "Data Size (Bytes / MB)" };
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        startUpdateLoop();
    }

    private void startUpdateLoop() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshData();
            } catch (Throwable t) {
                System.err.println("Task Manager Update Error: " + t.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void refreshData() {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(targetHost, targetPort)
                    .usePlaintext()
                    .build();

            FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

            List<NodeInfo> members = stub.withDeadlineAfter(1, TimeUnit.SECONDS)
                    .getFamily(Empty.newBuilder().build())
                    .getMembersList();

            SwingUtilities.invokeLater(() -> updateTable(members));

        } catch (Exception e) {
            System.err.println("Task Manager: Lidere bağlanılamadı (" + e.getMessage() + ")");
        } finally {
            if (channel != null) {
                try {
                    channel.shutdownNow();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void updateTable(List<NodeInfo> members) {
        java.util.Map<String, NodeInfo> uniqueMap = new java.util.HashMap<>();

        for (NodeInfo n : members) {
            String key = n.getHost() + ":" + n.getPort();
            uniqueMap.merge(key, n, (existing,
                    candidate) -> (candidate.getMessageCount() > existing.getMessageCount()) ? candidate : existing);
        }

        tableModel.setRowCount(0);

        java.util.List<NodeInfo> sortedMembers = new java.util.ArrayList<>(uniqueMap.values());
        sortedMembers.sort(java.util.Comparator.comparingLong(NodeInfo::getTotalBytes).reversed());

        for (NodeInfo n : sortedMembers) {
            double mb = n.getTotalBytes() / (1024.0 * 1024.0);
            String sizeStr = String.format("%d B (%.2f MB)", n.getTotalBytes(), mb);
            String host = n.getHost();

            tableModel.addRow(new Object[] {
                    host,
                    n.getPort(),
                    n.getMessageCount(),
                    sizeStr
            });
        }
    }
}
