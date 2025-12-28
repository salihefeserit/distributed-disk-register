public class MessageServer {

    private static final int PORT = 12345;
    private static final String STORAGE_DIR = "messages";

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
            return
        }

        System.out.println("Sunucu başlatıldı, bağlantılar bekleniyor...");
    }
}