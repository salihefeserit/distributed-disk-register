package com.example.family;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import family.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

public class MemberNode {

    private static final String myIp = System.getProperty("myHost", "127.0.0.1");
    private static final String LEADER_HOST = System.getProperty("leaderHost", "127.0.0.1");

    private static final int START_PORT = 5556;

    private static final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
    private static final int PRINT_INTERVAL_SECONDS = 10;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");

        // Java'nın sistem proxy'lerini kullanmasını engelle
        System.setProperty("java.net.useSystemProxies", "false");
    
        // Spesifik olarak http/https proxy'lerini temizle
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");

        int port = findFreePort(START_PORT, myIp);

        NodeInfo self = NodeInfo.newBuilder()
                .setHost(myIp)
                .setPort(port)
                .setMessageCount(0)
                .build();

        NodeRegistry registry = new NodeRegistry();
        FamilyServiceImpl service = new FamilyServiceImpl(registry, self);

        // (PASIF)
        // StorageServiceImpl service_storage = new StorageServiceImpl(port);

        // (AKTIF)
        ZeroCopyServiceImpl service_zerocopy = new ZeroCopyServiceImpl(port);

        Server server = NettyServerBuilder.forAddress(new InetSocketAddress(myIp, port))
                .addService(service)
                .addService(service_zerocopy) // zerocopy yada buffered seçeneği burada
                // .addService(service_storage)
                .build()
                .start();

        System.out.printf("Node started on %s:%d%n", myIp, port);

        // Eğer bu ilk node ise (port 5555), TCP 6666'da text dinlesin

        joinToFamily(LEADER_HOST, port, registry, self);

        startFamilyPrinter(self, LEADER_HOST);

        server.awaitTermination();

    }

        // Join to family.
    private static void joinToFamily(String host,
            int selfPort,
            NodeRegistry registry,
            NodeInfo self) {

            try {
                ManagedChannel channel = getChannel(host, 5555);

                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                FamilyView view = stub.join(self);
                registry.addAll(view.getMembersList());

                System.out.printf("Joined through %s:%d, family size now: %d%n",
                        host, 5555, registry.snapshot().size());

            } catch (Exception ignored) {
            }
    }

    private static int findFreePort(int startPort, String host) {
        try {
            int port = startPort;
            InetAddress addr = InetAddress.getByName(host);
            while (true) {
                try (ServerSocket ignored = new ServerSocket(port, 50, addr)) {
                    return port;
                } catch (IOException e) {
                    port++;
                }
            }
        } catch (UnknownHostException e) {
            System.err.printf("Sisteme ait olmayan bir host adresi girildi.");
            return 0;
        }
    }

    private static ManagedChannel getChannel(String host, int port) {
        String key = host + ":" + port;
        return channelCache.computeIfAbsent(key, k -> {
            return ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .build();
        });
    }

    private static void startFamilyPrinter(NodeInfo self, String host) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                ManagedChannel channel = getChannel(host, 5555);
                try {
                    FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);
                    List<NodeInfo> members = stub
                            .withDeadlineAfter(3, TimeUnit.SECONDS)
                            .getFamily(Empty.newBuilder().build()).getMembersList();
                            
                    System.out.println("======================================");
                    System.out.printf("Family at %s:%d (me)%n", self.getHost(), self.getPort());
                    System.out.println("Time: " + LocalDateTime.now());
                    System.out.println("Members:");

                    for (NodeInfo n : members) {
                        boolean isMe = n.getHost().equals(self.getHost()) && n.getPort() == self.getPort();
                        System.out.printf(" - %s:%d - %d msgs, %d bytes%s%n",
                                n.getHost(),
                                n.getPort(),
                                n.getMessageCount(),
                                n.getTotalBytes(),
                                isMe ? " (me)" : "");
                    }
                    System.out.println("======================================");
                } catch (StatusRuntimeException e) {
                    System.err.println("Lidere (" + host + ") ulaşılamadı, tekrar deneniyor...");
                }
            } catch (Exception e) {
                System.err.println("Beklenmedik bir hata oluştu: " + e.getMessage());
                e.printStackTrace();
            }
        }, 3, PRINT_INTERVAL_SECONDS, TimeUnit.SECONDS);

    }
}
