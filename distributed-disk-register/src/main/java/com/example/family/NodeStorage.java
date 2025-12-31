package com.example.family;

import family.ChatMessage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class NodeStorage {
    public void store(ChatMessage msg) {
        String id = String.valueOf(msg.getId());
        String text = msg.getText();
        try {
            Path dosyaYolu = Path.of("messages/", id + ".msg"); //uuid mantığı eklenecek
            Files.createDirectories(dosyaYolu.getParent());
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
                    dosyaYolu,
                    StandardOpenOption.CREATE)) {
                bufferedWriter.write(text);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
