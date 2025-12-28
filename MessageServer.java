import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class MessageServer {

    private static final int PORT = 12345;
    private static final String STORAGE_DIR = "messages";


    private final ConcurrentHashMap<String, String> memoryMap =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Sunucu Başlatılıyor...");
        new MessageServer().start();
    }
    public void start(){
        //başlangıç kodu eklenecek
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
        } catch (IOException e) {
            System.err.println("Dizin oluşturulamadı");
            return;
        }

        System.out.println("Sunucu başlatıldı, bağlantılar bekleniyor...");

        try (ServerSocket serversocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), true)
                ) {
                    string line;
                    while ((line = inReadLine()) != null) {
                        Command cmd = Command.parse(line);
                        switch (cmd.getType()) {
                            case SET:
                                handleSet(cmd, out);
                                break;
                            case GET:
                                handleGet(cmd, out);
                                break;
                            default:
                                out.println("ERROR");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Client bağlantı hatası");
                }
            

    }

    private void handleSet(Command cmd, PrintWriter out) {
        memoryMap.put(cmd.getId(), cmd.getMessage());

        File file = new File(STORAGE_DIR, cmd.getId() + ".msg");

        try (BufferedWriter =
                     new BufferedWriter(new FileWriter(file))) {

            writer.write(cmd.getMessage());
            out.println("OK");            
        } catch (IOException e) {
            out.println("ERROR");
        }
    }

    private void handleGet(Command cmd, PrintWriter out) {

        if (memoryMap.containsKey(cmd.getId())) {
            out.println(memoryMap.get(cmd.getId()));
            return;
        }

        File file = new File(STORAGE_DIR, cmd.getId() + ".msg");

        if  (!file.exists()) {
            out.println("NOT_FOUND");
            return;
        }

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(file))){

            String content = reader.lines()
                                   .collect(Collectors.joining("\n"));
            memoryMap.put(cmd.getId(), content);
            out.println(content); 
        } catch (IOException e) {
            out.println("ERROR");
        }
    }
}