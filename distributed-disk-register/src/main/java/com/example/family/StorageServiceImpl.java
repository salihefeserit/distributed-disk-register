package com.example.family;

import family.ChatMessage;
import family.StorageServiceGrpc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class StorageServiceImpl extends StorageServiceGrpc.StorageServiceImplBase{

    public void store(ChatMessage msg, PrintWriter outtelnet) {
        String id = String.valueOf(msg.getId());
        String text = msg.getText();
        try {
            Path dosyaYolu = Path.of("messages/", id + ".msg"); //uuid mantığı eklenecek
            Files.createDirectories(dosyaYolu.getParent());
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
                    dosyaYolu,
                    StandardOpenOption.CREATE)) {
                bufferedWriter.write(text);
                outtelnet.println("OK");
            }
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
