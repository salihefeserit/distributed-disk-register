package com.example.family;

import family.*;

import io.grpc.*;

import java.io.*;
import java.net.Socket;

import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class NodeMain {

    private static final int START_PORT = 5555;
    private static final int PRINT_INTERVAL_SECONDS = 10;

    private static final Map<Integer, List<NodeInfo>> storageNodes = new ConcurrentHashMap<>();

    // gRPC kanallarının kaydedilmesi
    private static final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();

    private static ManagedChannel getChannel(String host, int port) {
        String key = host + ":" + port;
        return channelCache.computeIfAbsent(key, k -> {
            return ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .build();
        });
    }

    // ----- Channel caching
    // nodelara yapılan grpc bağlantılarını kaydediyor sürekli açılım olmamasını
    // sağlıyor
    // hash mapte varsa var olanı kullanıuor yok ise yeni bağlantı oluşturuyor
    // -----

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = findFreePort(START_PORT);

        NodeInfo self = NodeInfo.newBuilder()
                .setHost(host)
                .setPort(port)
                .setMessageCount(0)
                .build();

        NodeRegistry registry = new NodeRegistry();
        FamilyServiceImpl service = new FamilyServiceImpl(registry, self);

        // (PASIF)
        // StorageServiceImpl service_storage = new StorageServiceImpl(port);

        // (AKTIF)
        ZeroCopyServiceImpl service_zerocopy = new ZeroCopyServiceImpl(port);

        Server server = ServerBuilder
                .forPort(port)
                .addService(service)
                .addService(service_zerocopy) // zerocopy yada buffered seçeneği burada
                // .addService(service_storage)
                .build()
                .start();

        System.out.printf("Node started on %s:%d%n", host, port);

        // Eğer bu ilk node ise (port 5555), TCP 6666'da text dinlesin
        if (port == START_PORT) {
            startLeaderTextListener(registry, self);
            startTaskManager();
        }

        discoverExistingNodes(host, port, registry, self);
        startFamilyPrinter(self);
        startHealthChecker(registry, self);

        server.awaitTermination();

    }

    private static void startTaskManager() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            TaskManager tm = new TaskManager();
            tm.setVisible(true);
        });
    }

    private static void startLeaderTextListener(NodeRegistry registry, NodeInfo self) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(6666)) {
                System.out.printf("Leader listening for text on TCP %s:%d%n",
                        self.getHost(), 6666);

                while (true) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClientTextConnection(client, registry, self)).start();
                }

            } catch (IOException e) {
                System.err.println("Error in leader text listener: " + e.getMessage());
            }
        }, "LeaderTextListener").start();
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

    private static void broadcastToFamily(NodeRegistry registry,
            NodeInfo self,
            ChatMessage msg,
            PrintWriter outtelnet) {

        int tolerance = getTolerance();
        List<NodeInfo> existingTargets = storageNodes.getOrDefault(msg.getId(), Collections.emptyList());
        List<NodeInfo> finalTargets = new ArrayList<>();

        List<NodeInfo> allLiveMembers = registry.snapshot();
        allLiveMembers.remove(self);

        if (!existingTargets.isEmpty()) {
            for (NodeInfo target : existingTargets) {
                if (isNodeAlive(target, registry)) {
                    finalTargets.add(target);
                }
            }

            int currentSize = finalTargets.size();
            if (currentSize < tolerance) {
                int needed = tolerance - currentSize;
                // SORT BY SIZE (Total Bytes)
                allLiveMembers.sort(Comparator.comparingLong(NodeInfo::getTotalBytes));

                for (NodeInfo candidate : allLiveMembers) {
                    if (needed <= 0)
                        break;

                    boolean alreadyInList = finalTargets.stream()
                            .anyMatch(
                                    t -> t.getHost().equals(candidate.getHost()) && t.getPort() == candidate.getPort());

                    if (!alreadyInList) {
                        if (isNodeAlive(candidate, registry)) {
                            finalTargets.add(candidate);
                            needed--;
                        }
                    }
                }
            }
            storageNodes.put(msg.getId(), new CopyOnWriteArrayList<>(finalTargets));

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
                    finalTargets.add(candidate);
                    needed--;
                }
            }
            storageNodes.put(msg.getId(), new CopyOnWriteArrayList<>(finalTargets));
        }

        List<NodeInfo> successfulNodes = new ArrayList<>();
        boolean failed = false;
        String finalError = "";

        for (NodeInfo n : finalTargets) {
            try {
                ManagedChannel channel = getChannel(n.getHost(), n.getPort());

                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                stub.receiveChat(msg);

                // ZEROVOPY (AKTIF)
                ZeroCopyServiceGrpc.ZeroCopyServiceBlockingStub stub_ZeroCopy = ZeroCopyServiceGrpc
                        .newBlockingStub(channel);
                StoreResult storeRes = stub_ZeroCopy.storeZeroCopy(msg);

                // BUFFERED (PASIF)
                // StorageServiceGrpc.StorageServiceBlockingStub stub =
                // StorageServiceGrpc.newBlockingStub(channel);
                // StoreResult storeRes = stub.store(msg);

                String resStr = storeRes.getResult();

                if ("STORED".equals(resStr) || "UPDATED".equals(resStr)) {
                    successfulNodes.add(n);
                    System.out.printf("Mesaj %s:%d adresine yayınlandı%n", n.getHost(), n.getPort());
                } else {
                    failed = true;
                    finalError = "Sonuç TAMAM degil: " + resStr;
                    break;
                }

            } catch (Exception e) {
                failed = true;
                finalError = e.getMessage();
                System.err.printf("%s:%d adresine gönderim başarısız (%s)%n",
                        n.getHost(), n.getPort(), e.getMessage());
                break;
            }
        }

        if (failed) {
            System.err.println("Hata oluştu.Başarılı nodelar geri alınıyor... Hata: " + finalError);
            for (NodeInfo n : successfulNodes) {
                try {
                    ManagedChannel channel = getChannel(n.getHost(), n.getPort());
                    MessageId id = MessageId.newBuilder().setId(msg.getId()).build();

                    // ZEROCOPY ROLLBACK (AKTIF)
                    ZeroCopyServiceGrpc.ZeroCopyServiceBlockingStub stub = ZeroCopyServiceGrpc.newBlockingStub(channel);
                    stub.deleteZeroCopy(id);

                    // BUFFERED (PASIF)
                    // StorageServiceGrpc.StorageServiceBlockingStub stub =
                    // StorageServiceGrpc.newBlockingStub(channel);
                    // stub.delete(id);

                    System.out.printf("%s:%d adresindeki mesaj geri alındı (silindi)%n", n.getHost(), n.getPort());
                } catch (Exception e) {
                    System.err.printf("%s:%d adresinde geri alma başarısız: %s%n", n.getHost(), n.getPort(),
                            e.getMessage());
                }
            }
            outtelnet.println("ERROR");
        } else {
            // Hepsi başarılı ise load güncellemesi yap
            for (NodeInfo n : finalTargets) {
                long size = msg.getText().length();
                registry.increaseLoad(n, size);
            }
            outtelnet.println("OK");
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

    private static int findFreePort(int startPort) {
        int port = startPort;
        while (true) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                port++;
            }
        }
    }

    private static void discoverExistingNodes(String host,
            int selfPort,
            NodeRegistry registry,
            NodeInfo self) {

        for (int port = START_PORT; port < selfPort; port++) {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress(host, port)
                        .usePlaintext()
                        .build();

                FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

                FamilyView view = stub.join(self);
                registry.addAll(view.getMembersList());

                System.out.printf("Joined through %s:%d, family size now: %d%n",
                        host, port, registry.snapshot().size());

            } catch (Exception ignored) {
            } finally {
                if (channel != null)
                    channel.shutdownNow();
            }
        }
    }

    private static void startFamilyPrinter(NodeInfo self) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                ManagedChannel channel = getChannel("127.0.0.1", START_PORT);
                try {
                    FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);
                    List<NodeInfo> members = stub.getFamily(Empty.newBuilder().build()).getMembersList();

                    // List<NodeInfo> members = registry.snapshot();
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
                    System.out.println("Lider düştü! Kapatılıyor...");
                    System.exit(0);
                }
            } catch (Exception e) {
                System.err.println("Beklenmedik bir hata oluştu: " + e.getMessage());
                e.printStackTrace();
            }
        }, 3, PRINT_INTERVAL_SECONDS, TimeUnit.SECONDS);

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
                }
            }

        }, 5, 10, TimeUnit.SECONDS); // 5 sn sonra başla, 10 sn'de bir kontrol et
    }

    public static int getTolerance() {
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
            return false;
        }
    }

}