package com.example.family;

import family.ChatMessage;
import family.MessageId;
import family.ZeroCopyServiceGrpc;
import family.StoreResult;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.*;
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
        String text = msg.getText();
        String resultMsg = "FAIL";

        try {
            Path dosyaYolu = Path.of("messages/" + RUN_ID, id + ".msg");
            Files.createDirectories(dosyaYolu.getParent());

            boolean isNewFile = !Files.exists(dosyaYolu);

            try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
                    dosyaYolu,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);

                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
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
    public void retrieveZeroCopy(MessageId id, StreamObserver<ChatMessage> responseObserver) {
        Path dosyaYolu = Path.of("messages/" + RUN_ID, id.getId() + ".msg");

        try (java.nio.channels.FileChannel channel = java.nio.channels.FileChannel.open(
                dosyaYolu,
                StandardOpenOption.READ)) {

            long fileSize = channel.size();
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate((int) fileSize);

            channel.read(buffer);
            buffer.flip();

            String line = new String(buffer.array(), java.nio.charset.StandardCharsets.UTF_8);

            ChatMessage msg = ChatMessage.newBuilder()
                    .setId(id.getId())
                    .setText("OK " + line)
                    .build();

            responseObserver.onNext(msg);
            responseObserver.onCompleted();
        } catch (FileNotFoundException | java.nio.file.NoSuchFileException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.asRuntimeException()
            );
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }
}
