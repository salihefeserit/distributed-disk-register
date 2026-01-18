package com.example.family;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.util.stream.Stream;

import family.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

public class LeaderNode {

    // Üyelerle olan gRPC kanallarının kaydedilmesi.
    private static final Map<Integer, List<NodeInfo>> storageNodes = new ConcurrentHashMap<>();

    public static final String LEADER_HOST = System.getProperty("myHost", "127.0.0.1");
    private static final int LEADER_PORT = 5555;

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

        NodeInfo self = NodeInfo.newBuilder()
                .setHost(LEADER_HOST)
                .setPort(LEADER_PORT)
                .setMessageCount(0)
                .build();

        NodeRegistry registry = new NodeRegistry();
        FamilyServiceImpl service = new FamilyServiceImpl(registry, self);

        // (PASIF)
        // StorageServiceImpl service_storage = new StorageServiceImpl(port);

        // (AKTIF)
        ZeroCopyServiceImpl service_zerocopy = new ZeroCopyServiceImpl(LEADER_PORT);

        Server server = NettyServerBuilder.forAddress(new InetSocketAddress(LEADER_HOST, LEADER_PORT))
                .addService(service)
                .addService(service_zerocopy) // zerocopy yada buffered seçeneği burada
                // .addService(service_storage)
                .build()
                .start();

        System.out.printf("Lider %s:%d adresinde başlatıldı.\n", LEADER_HOST, LEADER_PORT);

        startLeaderTextListener(registry, self);
        startTaskManager();

        startFamilyPrinter(self, registry, LEADER_HOST);
        startHealthChecker(registry, self);

        server.awaitTermination();

    }

    private static void broadcastToFamily(NodeRegistry registry,
            NodeInfo self,
            ChatMessage msg,
            PrintWriter outtelnet) {

        int tolerance = getTolerance();
        List<NodeInfo> toBeUpdatedExistingTargets = storageNodes.getOrDefault(msg.getId(), Collections.emptyList());
        List<NodeInfo> additionalTargets = new ArrayList<>();

        List<NodeInfo> allLiveMembers = registry.snapshot();
        allLiveMembers.remove(self);

        MessageId msgId = MessageId.newBuilder()
                    .setId(msg.getId())
                    .build();

        if (!toBeUpdatedExistingTargets.isEmpty()) {

            for (NodeInfo target : toBeUpdatedExistingTargets) {
                if (!isNodeAlive(target, registry)) {
                    toBeUpdatedExistingTargets.remove(target);
                }
            }

            int currentSize = toBeUpdatedExistingTargets.size();

            if (currentSize < tolerance) {
                int needed = tolerance - currentSize;
                // SORT BY SIZE (Total Bytes)
                allLiveMembers.sort(Comparator.comparingLong(NodeInfo::getTotalBytes));

                for (NodeInfo candidate : allLiveMembers) {
                    if (needed <= 0)
                        break;

                    boolean alreadyInList = toBeUpdatedExistingTargets.stream()
                            .anyMatch(
                                    t -> t.getHost().equals(candidate.getHost()) && t.getPort() == candidate.getPort());

                    if (!alreadyInList) {
                        if (isNodeAlive(candidate, registry)) {
                            additionalTargets.add(candidate);
                            needed--;
                        }
                    }
                }
            }
            storageNodes.put(msg.getId(), new CopyOnWriteArrayList<>(
                Stream.concat(toBeUpdatedExistingTargets.stream(), additionalTargets.stream())
                    .collect(Collectors.toList())
            ));

        } else {
            allLiveMembers = registry.snapshot();
            allLiveMembers.remove(self);
            // SORT BY SIZE (Total Bytes)
            allLiveMembers.sort(Comparator.comparingLong(NodeInfo::getTotalBytes));

            int needed = tolerance;
            for (NodeInfo candidate : allLiveMembers) {
                if (needed <= 0)
                    break;
                if (isNodeAlive(candidate, registry)) {
                    additionalTargets.add(candidate);
                    needed--;
                }
            }
            storageNodes.put(msg.getId(), new CopyOnWriteArrayList<>(
                Stream.concat(toBeUpdatedExistingTargets.stream(), additionalTargets.stream())
                    .collect(Collectors.toList())
            ));
        }

        List<NodeInfo> successfulNodes = new ArrayList<>();
        boolean failedExists = false;
        boolean failedNews = false;
        String finalError = "";

        ChatMessage oldMessage = ChatMessage.newBuilder().build();
        long oldSize = 0;

        for (NodeInfo n : toBeUpdatedExistingTargets) {
            try {
                ManagedChannel channel = getChannel(n.getHost(), n.getPort());

                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                stub.receiveChat(msg);

                // ZEROVOPY (AKTIF)
                ZeroCopyServiceGrpc.ZeroCopyServiceBlockingStub stub_ZeroCopy = ZeroCopyServiceGrpc
                        .newBlockingStub(channel);

                oldMessage = stub_ZeroCopy.retrieveZeroCopy(msgId);
                oldSize = oldMessage.getText().length();

                StoreResult storeRes = stub_ZeroCopy.storeZeroCopy(msg);

                // BUFFERED (PASIF)
                // StorageServiceGrpc.StorageServiceBlockingStub stub =
                // StorageServiceGrpc.newBlockingStub(channel);
                // StoreResult storeRes = stub.store(msg);

                String resStr = storeRes.getResult();

                if ("UPDATED".equals(resStr)) {
                    successfulNodes.add(n);
                    System.out.printf("Mesaj %s:%d adresinde güncellendi%n", n.getHost(), n.getPort());
                } else {
                    failedExists = true;
                    finalError = "Sonuç TAMAM degil: " + resStr;
                    break;
                }

            } catch (Exception e) {
                failedExists = true;
                finalError = e.getMessage();
                System.err.printf("%s:%d adresine gönderim başarısız (%s)%n",
                        n.getHost(), n.getPort(), e.getMessage());

                // Hata aldıysak bu kanalı temizle
                invalidateChannel(n.getHost(), n.getPort());
                
                break;
            }
        }

        if (failedExists) {
            System.err.println("Hata oluştu.Başarılı nodelar geri alınıyor... Hata: " + finalError);
            for (NodeInfo n : successfulNodes) {
                try {
                    ManagedChannel channel = getChannel(n.getHost(), n.getPort());
                    MessageId id = MessageId.newBuilder().setId(msg.getId()).build();

                    // ZEROCOPY ROLLBACK (AKTIF)
                    ZeroCopyServiceGrpc.ZeroCopyServiceBlockingStub stub = ZeroCopyServiceGrpc.newBlockingStub(channel);
                    stub.deleteZeroCopy(id);
                    stub.storeZeroCopy(oldMessage);

                    // BUFFERED (PASIF)
                    // StorageServiceGrpc.StorageServiceBlockingStub stub = StorageServiceGrpc.newBlockingStub(channel);
                    // stub.delete(id);
                    // stub.store(oldMessage);

                    System.out.printf("%s:%d adresindeki mesaj geri alındı (silindi)%n", n.getHost(), n.getPort());
                } catch (Exception e) {
                    System.err.printf("%s:%d adresinde geri alma başarısız: %s%n", n.getHost(), n.getPort(),
                            e.getMessage());
                }
            }
            outtelnet.println("ERROR");
        } else {
            // Hepsi başarılı ise load güncellemesi yap
            for (NodeInfo n : toBeUpdatedExistingTargets) {
                long size = msg.getText().length();
                registry.decreaseLoad(n, oldSize);
                registry.increaseLoad(n, size);
            }
        }

        if (failedExists != true) {
            for (NodeInfo n : additionalTargets) {
                try {
                    ManagedChannel channel = getChannel(n.getHost(), n.getPort());

                    FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                    stub.receiveChat(msg);

                    // ZEROVOPY (AKTIF)
                    ZeroCopyServiceGrpc.ZeroCopyServiceBlockingStub stub_ZeroCopy = ZeroCopyServiceGrpc
                            .newBlockingStub(channel);
                    StoreResult storeRes = stub_ZeroCopy.storeZeroCopy(msg);

                    // BUFFERED (PASIF)
                    // StorageServiceGrpc.StorageServiceBlockingStub stub = StorageServiceGrpc.newBlockingStub(channel);
                    // StoreResult storeRes = stub.store(msg);

                    String resStr = storeRes.getResult();

                    if ("STORED".equals(resStr)) {
                        successfulNodes.add(n);
                        System.out.printf("Mesaj %s:%d adresine yayınlandı%n", n.getHost(), n.getPort());
                    } else {
                        failedNews = true;
                        finalError = "Sonuç TAMAM degil: " + resStr;
                        break;
                    }

                } catch (Exception e) {
                    failedNews = true;
                    finalError = e.getMessage();
                    System.err.printf("%s:%d adresine gönderim başarısız (%s)%n",
                            n.getHost(), n.getPort(), e.getMessage());
                    break;
                }
            }
        }

        if (failedNews) {
            System.err.println("Hata oluştu.Başarılı nodelar geri alınıyor... Hata: " + finalError);
            for (NodeInfo n : successfulNodes) {
                try {
                    ManagedChannel channel = getChannel(n.getHost(), n.getPort());
                    MessageId id = MessageId.newBuilder().setId(msg.getId()).build();

                    // ZEROCOPY ROLLBACK (AKTIF)
                    ZeroCopyServiceGrpc.ZeroCopyServiceBlockingStub stub = ZeroCopyServiceGrpc.newBlockingStub(channel);
                    stub.deleteZeroCopy(id);

                    // BUFFERED (PASIF)
                    // StorageServiceGrpc.StorageServiceBlockingStub stub = StorageServiceGrpc.newBlockingStub(channel);
                    // stub.delete(id);

                    System.out.printf("%s:%d adresindeki mesaj geri alındı (silindi)%n", n.getHost(), n.getPort());
                } catch (Exception e) {
                    System.err.printf("%s:%d adresinde geri alma başarısız: %s%n", n.getHost(), n.getPort(),
                            e.getMessage());
                }
            }
            outtelnet.println("ERROR");
        } 
        
        if (failedNews != true && failedExists != true) {
            // Hepsi başarılı ise load güncellemesi yap
            for (NodeInfo n : additionalTargets) {
                long size = msg.getText().length();
                registry.increaseLoad(n, size);
            }
            outtelnet.println("OK");
        }
    }

    private static void handleClientTextConnection(Socket client,
            NodeRegistry registry,
            NodeInfo self) {
        System.out.println("New TCP client connected: " + client.getRemoteSocketAddress());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String text = line.trim();
                if (text.isEmpty())
                    continue;

                long ts = System.currentTimeMillis();

                PrintWriter outtelnet = new PrintWriter(client.getOutputStream(), true);

                String[] parts = text.split(" ");
                String command = parts[0].toUpperCase();

                if (command.equals("SET")) {
                    if (parts.length < 3) {
                        outtelnet.println("SET ID <MESAJ>");
                        continue;
                    }

                    int id = Integer.parseInt(parts[1]);
                    String mesaj = parts[2];

                    ChatMessage msg = ChatMessage.newBuilder()
                            .setId(id)
                            .setText(mesaj)
                            .setFromHost(self.getHost())
                            .setFromPort(self.getPort())
                            .setTimestamp(ts)
                            .build();

                    System.out.println("📝 Received from TCP: " + mesaj);

                    broadcastToFamily(registry, self, msg, outtelnet);

                } else if (command.equals("GET")) {
                    if (parts.length < 2) {
                        outtelnet.println("HATA: Eksik parametre (GET ID)");
                        continue;
                    }
                    MessageId id = MessageId.newBuilder()
                            .setId(Integer.parseInt(parts[1]))
                            .build();

                    outtelnet.println(takeFromNodeList(self, id, registry));
                }

            }

        } catch (IOException e) {
            System.err.println("TCP client handler error: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String takeFromNodeList(NodeInfo self,
            MessageId id, NodeRegistry registry) {

        List<NodeInfo> targetNodes = storageNodes.getOrDefault(id.getId(), Collections.emptyList());

        for (NodeInfo n : targetNodes) {
            if (n.getHost().equals(self.getHost()) && n.getPort() == self.getPort()) {
                continue;
            }

            if (!isNodeAlive(n, registry)) {
                continue;
            }

            System.out.printf("%s:%d adresinden mesaj alınıyor...%n", n.getHost(), n.getPort());

            try {
                ManagedChannel channel = getChannel(n.getHost(), n.getPort());

                // BUFFERED (PASIF)
                // StorageServiceGrpc.StorageServiceBlockingStub stub_storage_get =
                // StorageServiceGrpc.newBlockingStub(channel);

                // return stub_storage_get.retrieve(id).getText();

                // ZEROCOPY (AKTIF)
                ZeroCopyServiceGrpc.ZeroCopyServiceBlockingStub stub_storage_zerocopy_get = ZeroCopyServiceGrpc
                        .newBlockingStub(channel);

                return stub_storage_zerocopy_get.retrieveZeroCopy(id).getText();

            } catch (Exception ignored) {
            }
        }
        return "NOT_FOUND";
    }

    private static void startHealthChecker(NodeRegistry registry, NodeInfo self) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            List<NodeInfo> members = registry.snapshot();

            for (NodeInfo n : members) {
                // Kendimizi kontrol etmeyelim
                if (n.getHost().equals(self.getHost()) && n.getPort() == self.getPort()) {
                    continue;
                }

                try {
                    ManagedChannel channel = getChannel(n.getHost(), n.getPort());

                    FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                    // Ping gibi kullanıyoruz: cevap bizi ilgilendirmiyor,
                    // sadece RPC'nin hata fırlatmaması önemli.
                    stub.getFamily(Empty.newBuilder().build());

                } catch (Exception e) {
                    // Bağlantı yok / node ölmüş → listeden çıkar
                    System.out.printf("Node %s:%d unreachable, removing from family%n",
                            n.getHost(), n.getPort());
                    registry.remove(n);
                    invalidateChannel(n.getHost(), n.getPort());
                }
            }

        }, 5, 10, TimeUnit.SECONDS); // 5 sn sonra başla, 10 sn'de bir kontrol et
    }

    private static void startTaskManager() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            TaskManager tm = new TaskManager();
            tm.setVisible(true);
        });
    }

    private static void startLeaderTextListener(NodeRegistry registry, NodeInfo self) {
        new Thread(() -> {
            ExecutorService executor = Executors.newCachedThreadPool();
            try (ServerSocket serverSocket = new ServerSocket(6666)) {
                System.out.printf("Leader listening for text on TCP %s:%d%n",
                        self.getHost(), 6666);

                while (true) {
                    Socket client = serverSocket.accept();
                    executor.submit(() -> handleClientTextConnection(client, registry, self));
                }

            } catch (IOException e) {
                System.err.println("Error in leader text listener: " + e.getMessage());
            } finally {
                // Sunucu soketi kapanırsa havuzuda kapat
                executor.shutdown();
            }
        }, "LeaderTextListener").start();
    }

    private static int getTolerance() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("TOLERANCE.conf")) {
            props.load(fis);
            String val = props.getProperty("TOLERANCE");
            if (val != null) {
                return Integer.parseInt(val.trim());
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Uyarı! Yanlış veya eksik format!: " + 1);
        }
        return 1;
    }

    private static boolean isNodeAlive(NodeInfo node, NodeRegistry registry) {
        try {
            ManagedChannel channel = getChannel(node.getHost(), node.getPort());
            FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);
            // bağlantı kontrolü
            stub.getFamily(Empty.newBuilder().build());
            return true;
        } catch (Exception e) {
            registry.remove(node);
            invalidateChannel(node.getHost(), node.getPort());
            return false;
        }
    }

    public static ManagedChannel getChannel(String host, int port) {
        String key = host + ":" + port;
        return channelCache.computeIfAbsent(key, k -> {
            return ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .build();
        });
    }

    public static void invalidateChannel(String host, int port) {
        String key = host + ":" + port;
    
        // 1. Kanalı map'ten çıkar ve referansını al
        ManagedChannel channel = channelCache.remove(key);
    
        // 2. Eğer map'te böyle bir kanal varsa, onu güvenli bir şekilde kapat
        if (channel != null) {
            // Kanal zaten kapanmadıysa kapatmaya zorla
            if (!channel.isShutdown()) {
                channel.shutdownNow(); 
            }
            System.out.println("Kanal temizlendi ve kapatıldı: " + key);
        }
    }

    public static void startFamilyPrinter(NodeInfo self, NodeRegistry registry, String host) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<NodeInfo> members = registry.snapshot();
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
                            isMe ? " (LEADER)" : "");
                }
                    System.out.println("======================================");
            } catch (Exception e) {
                System.err.println("Beklenmedik bir hata oluştu: " + e.getMessage());
                e.printStackTrace();
            }
        }, 3, PRINT_INTERVAL_SECONDS, TimeUnit.SECONDS);

    }
}
