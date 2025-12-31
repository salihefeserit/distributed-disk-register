package com.example.family;

import family.ChatMessage;
import family.StorageServiceGrpc;
import family.StoreResult;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase{
    private final String RUN_ID;
    private int messageCount;

    // Uyedeki StorageService ne zaman olusturulduysa
    // o zamanin damgasini depolama klasoru yap
    // degistirilebilir.
    public StorageServiceImpl() {
        this.messageCount = 0;
        this.RUN_ID = UUID.randomUUID().toString();
    }

    @Override
    public void store(ChatMessage msg, StreamObserver<StoreResult> responseObserver) {
        String id = String.valueOf(msg.getId());
        String text = msg.getText();
        try {
            Path dosyaYolu = Path.of("messages/" + RUN_ID, id + ".msg");
            Files.createDirectories(dosyaYolu.getParent());
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
                    dosyaYolu,
                    StandardOpenOption.CREATE)) {
                bufferedWriter.write(text);
            }
            StoreResult status = StoreResult.newBuilder()
                    .setResult("OK")
                    .build();

            responseObserver.onNext(status);
            responseObserver.onCompleted();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void retrieve(Integer id, PrintWriter out) {
        try (BufferedReader reader = new BufferedReader(new FileReader("messages/" + id + ".msg"))) {
            String line = reader.readLine();

            out.println(line);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            out.println("NOT_FOUND");
        }

    }
}
