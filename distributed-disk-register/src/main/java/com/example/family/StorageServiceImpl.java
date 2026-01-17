package com.example.family;

import family.ChatMessage;
import family.MessageId;
import family.StorageServiceGrpc;
import family.StoreResult;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase {
    private final String RUN_ID;

    // Uyedeki StorageService ne zaman olusturulduysa
    // o zamanin damgasini depolama klasoru yap
    // degistirilebilir.
    public StorageServiceImpl(int port) {
        this.RUN_ID = port + "_" + UUID.randomUUID().toString();
    }

    @Override
    public void store(ChatMessage msg, StreamObserver<StoreResult> responseObserver) {
        String id = String.valueOf(msg.getId());
        String text = msg.getText();
        String resultMsg = "FAIL";

        try {
            Path dosyaYolu = Path.of("messages/" + RUN_ID, id + ".msg");
            Files.createDirectories(dosyaYolu.getParent());
            boolean isNewFile = !Files.exists(dosyaYolu);

            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
                    dosyaYolu,
                    StandardOpenOption.CREATE)) {
                bufferedWriter.write(text);
                resultMsg = isNewFile ? "STORED" : "UPDATED";
            }

            StoreResult status = StoreResult.newBuilder()
                    .setResult(resultMsg)
                    .build();

            responseObserver.onNext(status);
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void retrieve(MessageId id, StreamObserver<ChatMessage> responseObserver) {
        try (BufferedReader reader = new BufferedReader(
                new FileReader("messages/" + RUN_ID + "/" + id.getId() + ".msg"))) {
            String line = reader.readLine();

            ChatMessage msg = ChatMessage.newBuilder()
                    .setId(id.getId())
                    .setText("OK " + line)
                    .build();

            responseObserver.onNext(msg);
            responseObserver.onCompleted();
        } catch (FileNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.asRuntimeException());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(MessageId id, StreamObserver<StoreResult> responseObserver) {
        Path dosyaYolu = Path.of("messages/" + RUN_ID, id.getId() + ".msg");

        try {
            boolean deleted = Files.deleteIfExists(dosyaYolu);
            responseObserver.onNext(StoreResult.newBuilder()
                    .setResult(deleted ? "DELETED" : "NOT_FOUND")
                    .build());
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
