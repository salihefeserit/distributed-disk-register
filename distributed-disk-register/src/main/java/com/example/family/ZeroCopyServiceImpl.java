package com.example.family;

import family.ChatMessage;
import family.MessageId;
import family.ZeroCopyServiceGrpc;
import family.StoreResult;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class ZeroCopyServiceImpl extends ZeroCopyServiceGrpc.ZeroCopyServiceImplBase {
    private final String RUN_ID;

    // Uyedeki StorageService ne zaman olusturulduysa
    // o zamanin damgasini depolama klasoru yap
    // degistirilebilir.
    public ZeroCopyServiceImpl(int port) {
        this.RUN_ID = port + "_" + UUID.randomUUID().toString();
    }

    @Override
    public void storeZeroCopy(ChatMessage msg, StreamObserver<StoreResult> responseObserver) {
        String id = String.valueOf(msg.getId());
        byte[] data = msg.getText().getBytes(StandardCharsets.UTF_8);

        try {
            Path dosyaYolu = Path.of("messages/" + RUN_ID, id + ".msg");
            Files.createDirectories(dosyaYolu.getParent());

            boolean isNewFile = !Files.exists(dosyaYolu);

            // FileChannel ile doğrudan yazma
            try (FileChannel channel = FileChannel.open(dosyaYolu,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                ByteBuffer buffer = ByteBuffer.wrap(data);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }

                channel.force(true);
            }

            responseObserver.onNext(StoreResult.newBuilder()
                    .setResult(isNewFile ? "STORED" : "UPDATED")
                    .build());
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void retrieveZeroCopy(MessageId id, StreamObserver<ChatMessage> responseObserver) {
        Path dosyaYolu = Path.of("messages/" + RUN_ID, id.getId() + ".msg");

        if (!Files.exists(dosyaYolu)) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Dosya bulunamadı").asRuntimeException());
            return;
        }

        try (FileChannel channel = FileChannel.open(dosyaYolu, StandardOpenOption.READ)) {
            long fileSize = channel.size();

            MappedByteBuffer mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

            byte[] bytes = new byte[(int) fileSize];
            mappedBuffer.get(bytes);

            ChatMessage msg = ChatMessage.newBuilder()
                    .setId(id.getId())
                    .setText("OK " + new String(bytes, StandardCharsets.UTF_8))
                    .build();

            responseObserver.onNext(msg);
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deleteZeroCopy(MessageId id, StreamObserver<StoreResult> responseObserver) {
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