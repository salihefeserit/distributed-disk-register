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

    private static final int START_PORT = 5560;
    private static final int PRINT_INTERVAL_SECONDS = 10;

    //private static Map<Integer, String> database = new ConcurrentHashMap<>();    //aşama 1-2 haritalama
    private static final Map<Integer, List<NodeInfo>> nodes = new ConcurrentHashMap<>();

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
        StorageServiceImpl service_storage = new StorageServiceImpl();

        Server server = ServerBuilder
                .forPort(port)
                .addService(service)
                .addService(service_storage)
                .build()
                .start();

                System.out.printf("Node started on %s:%d%n", host, port);

                // Eğer bu ilk node ise (port 5555), TCP 6666'da text dinlesin
                if (port == START_PORT) {
                    startLeaderTextListener(registry, self);

                }

                discoverExistingNodes(host, port, registry, self);
                startFamilyPrinter(self);
                startHealthChecker(registry, self);

                server.awaitTermination();

    }

    private static void startLeaderTextListener(NodeRegistry registry, NodeInfo self) {
    // Sadece lider (5555 portlu node) bu methodu çağırmalı
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
            if (text.isEmpty()) continue;

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

                broadcastToFamily(registry, self, msg, outtelnet); //sonrasında güncellenecek

            }else if (command.equals("GET")) {
                if (parts.length < 2) {
                    outtelnet.println("HATA: Eksik parametre (GET ID)");
                    continue;
                }
                MessageId id = MessageId.newBuilder()
                        .setId(Integer.parseInt(parts[1]))
                        .build();

                outtelnet.println(takeFromNodeList(self, id));
            }

        }

    } catch (IOException e) {
        System.err.println("TCP client handler error: " + e.getMessage());
    } finally {
        try { client.close(); } catch (IOException ignored) {}
    }
}


    private static void broadcastToFamily(NodeRegistry registry,
                                          NodeInfo self,
                                          ChatMessage msg,
                                          PrintWriter outtelnet) {

    List<NodeInfo> members = registry.snapshot();
    members.remove(self);
    int tolerance = getTolerance();
    String result = "";
    members.sort(Comparator.comparingInt(NodeInfo::getMessageCount));
    int targetCount = Math.min(tolerance, members.size());
    List<NodeInfo> targets = members.subList(0, targetCount);

    nodes.put(msg.getId(), new CopyOnWriteArrayList<>(targets));

    for (NodeInfo n : targets) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder
                    .forAddress(n.getHost(), n.getPort())
                    .usePlaintext()
                    .build();

            FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                    FamilyServiceGrpc.newBlockingStub(channel);

            StorageServiceGrpc.StorageServiceBlockingStub stub_storage =
                    StorageServiceGrpc.newBlockingStub(channel);

            registry.increaseCount(n);

            stub.receiveChat(msg);
            result = stub_storage.store(msg).getResult();

            System.out.printf("Broadcasted message to %s:%d%n", n.getHost(), n.getPort());

        } catch (Exception e) {
            System.err.printf("Failed to send to %s:%d (%s)%n",
                    n.getHost(), n.getPort(), e.getMessage());
        } finally {
            if (channel != null) channel.shutdownNow();
        }
    }
    outtelnet.println(result);
}

    //üyelerden mesajı çekmek için kullanılan fonk
    private static String takeFromNodeList(NodeInfo self,
                                           MessageId id) {

        List<NodeInfo> targetNodes = nodes.getOrDefault(id, Collections.emptyList());

        for (NodeInfo n : targetNodes) {
            if (n.getHost().equals(self.getHost()) && n.getPort() == self.getPort()) {
                continue;
            }

            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress(n.getHost(), n.getPort())
                        .usePlaintext()
                        .build();

                StorageServiceGrpc.StorageServiceBlockingStub stub2 =
                        StorageServiceGrpc.newBlockingStub(channel);

                return stub2.retrieve(id).getText();

            } catch (Exception ignored) {
            } finally {
                if (channel != null) channel.shutdownNow();
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

                FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                        FamilyServiceGrpc.newBlockingStub(channel);

                FamilyView view = stub.join(self);
                registry.addAll(view.getMembersList());

                System.out.printf("Joined through %s:%d, family size now: %d%n",
                        host, port, registry.snapshot().size());

            } catch (Exception ignored) {
            } finally {
                if (channel != null) channel.shutdownNow();
            }
        }
    }

    private static void startFamilyPrinter(NodeInfo self) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress("127.0.0.1", START_PORT)
                        .usePlaintext()
                        .build();
                try {
                    FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                            FamilyServiceGrpc.newBlockingStub(channel);
                    List<NodeInfo> members = stub.getFamily(Empty.newBuilder().build()).getMembersList();

                    // List<NodeInfo> members = registry.snapshot();
                    System.out.println("======================================");
                    System.out.printf("Family at %s:%d (me)%n", self.getHost(), self.getPort());
                    System.out.println("Time: " + LocalDateTime.now());
                    System.out.println("Members:");

                    for (NodeInfo n : members) {
                        boolean isMe = n.getHost().equals(self.getHost()) && n.getPort() == self.getPort();
                        boolean isLeader = n.getHost().equals(self.getHost()) && n.getPort() == START_PORT;
                        System.out.printf(" - %s:%d - %s%s%n",
                                n.getHost(),
                                n.getPort(),
                                isLeader ? "Lider" : String.valueOf(n.getMessageCount()),
                                isMe ? " (me)" : "");
                    }
                    System.out.println("======================================");
                }
                catch (StatusRuntimeException e) {
                    System.out.println("Lider düştü! Kapatılıyor...");
                    System.exit(0);
                }
            }
            catch (Exception e) {
                System.err.println("Beklenmedik bir hata oluştu: " + e.getMessage());
                e.printStackTrace();
            }
            finally {
                if (channel != null) channel.shutdownNow();
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

            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress(n.getHost(), n.getPort())
                        .usePlaintext()
                        .build();

                FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                        FamilyServiceGrpc.newBlockingStub(channel);

                // Ping gibi kullanıyoruz: cevap bizi ilgilendirmiyor,
                // sadece RPC'nin hata fırlatmaması önemli.
                stub.getFamily(Empty.newBuilder().build());

            } catch (Exception e) {
                // Bağlantı yok / node ölmüş → listeden çıkar
                System.out.printf("Node %s:%d unreachable, removing from family%n",
                        n.getHost(), n.getPort());
                registry.remove(n);
            } finally {
                if (channel != null) {
                    channel.shutdownNow();
                }
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

}
