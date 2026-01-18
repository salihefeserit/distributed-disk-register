Distributed-Disk-Registery (gRPC + TCP)
=======================================

---


# gRPC + Protobuf + TCP Hybrid Distributed Server

Bu proje, birden fazla sunucunun dağıtık bir küme (“family”) oluşturduğu, **gRPC + Protobuf** ile kendi aralarında haberleştiği ve aynı zamanda **lider üye (cluster gateway)** üzerinden dış dünyadan gelen **TCP text mesajlarını** seçilen üyelere broadcast ettiği hibrit bir mimari örneğidir.



---

## 👨🏻‍💻 Özellikler
- Dinamik Node Keşfi: Yeni başlayan node'lar, lider olarak kendilerine işaret edilen host adresinde bulunan lider'in ailesine katılırlar.

- Lider Node Mekanizması: LeaderNode.java dosyasının başlatıldığı sistemde eğer 5555 portu kullanılmıyorsa (başka bir lider yoksa) bu port lider'e tahsis edilir ve gelebilecek bağlantılar dinlenmeye başlanır.

- TCP Komut Arayüzü: Lider node, 6666 portu üzerinden TCP bağlantılarını Telnet üzerinden dinler ve SET/GET komutlarını istenilene göre çalıştırır.

- Görsel İzleme Paneli (Task Manager): Lider node üzerinde çalışan arayüz ile ailedeki node'ların ve mesaj yüklerinin durumu izlenebilir.

- Health Checking: Lider, düşen node'ları tespit eder ve ağ listesinden çıkarır (Health Checking). Lider düşer ise ağ kapanır.

- Veri Replikasyonu: Mesajlar, tolerans dosyasında belirtilen seviyeye göre birden fazla üye'ye üyelerin bulundurdukları toplam mesaj boyutuna göre dengeli bir şekilde kopyalanır.

- Lider Seçimi: Üyeler, bağlanacakları liderlerin ip adreslerini parametre alarak hangi lider'e bağlanacaklarını belirleyebilirler. (Parametre verilmezse localhost'ta lider aranır.)

- Ağ Seçimi: Node'lar kendilerine verilen CLI parametrelerini alarak bulundukları sistemin üyesi olduğu hangi ağ üzerinde bulunabileceklerini seçebilirler. Örn: sistemin fiziksel IPv4 adresi olan 192.168.1.44 parametre verilmesi halinde o node'un fiziksel ağ içinde erişebilir olmasını sağlar (Parametre verilmemesi halinde localhost).

## 💬 Gereksinimler
- Java 17 veya üzeri
- Maven

## 📁 Proje Yapısı
```
distributed-disk-register/
│
├── messages/
├── pom.xml
├── README.md
├── TOLERANCE.conf
├── src
│   └── main
│       ├── java/com/example/family/
│       │       ├── LeaderNode.java
│       │       ├── MemberNode.java
│       │       ├── NodeRegistry.java
│       │       └── FamilyServiceImpl.java
│       │       └── StorageServiceImpl.java
│       │       └── ZeroCopyServiceImpl.java
│       │
│       └── proto/
│               └── family.proto
```

- LeaderNode.java: Liderin başlatılma noktası.

- MemberNode.java: Üyelerin başlangıç noktası. Seçilen lider'e kendisini tanıtır.

- TaskManager.java: Swing tabanlı görsel arayüz. Ağ durumunu tablo halinde gösterir.

- FamilyServiceImpl.java: Node'lar arası iletişim (gRPC) mantığını içerir.

- StorageServiceImpl.java: Mesajların diske yazılmasını ve okunmasını Buffered IO sayesinde sağlar (messages/ klasörü altında).

- ZeroCopyServiceImpl.java: Mesajların diske yazılmasını ve okunmasını Zero-Copy ilkesi sağlar (messages/ klasörü altında).

- NodeRegistry.java: Aktif node'ların listesini tutan thread-safe yapı.

- TOLERANCE.conf: Verinin kaç farklı node'da yedekleneceğini belirler.

## 🔧 Kurulum ve Derleme
Projeyi kendi sisteminizde çalıştırmak için aşağıdaki komutu çalıştırın:
```
git clone https://github.com/salihefeserit/distributed-disk-register.git
```
Projeyi derlemek için proje dizininde aşağıdaki komutu çalıştırın:
```
mvn clean install
```
## ▶️ Çalıştırma
Sistemi başlatmak için NodeMain sınıfını çalıştırın. İdeal bir test ortamı için aşağıdaki adımları izleyebilirsiniz:

### 1. Lider Node'u Başlatma
İlk node her zaman 5555 portunda başlar ve Lider olur.
```
mvn exec:java -Dexec.mainClass=com.example.family.LeaderNode -DmyHost=192.168.1.150
```
Not: ``-DmyHost=192.168.1.150`` şeklinde parametre verilmemesi halinde lider localhost üzerinde başlatılacaktır.
Not: Lider başladığında Task Manager penceresi de otomatik olarak açılacaktır.

### 2. Üye Node'ları Başlatma
Aynı sistemdeki farklı terminallerde veya liderin kullandığı ağ ile aynı ağda bulunan başka bir sistem üzerinde MemberNode'u kullanacağı IPv4 adresi ve kullanacağı liderin IPv4 adresini vererek başlatılabilir.
Bulunduğu IP adresinde 5556 portundan başlayarak sırayla doluluk kontrolu yaparak uygun portlara yerleşir. (5556-5557-...)
```
mvn exec:java -Dexec.mainClass=com.example.family.MemberNode -DmyHost=192.168.1.44 -DleaderHost=192.168.1.150
```
Not: Üye, parametre alarak başlatılmazsa ``-DmyHost``ve ``-DleaderHost``parametreleri otomatik olarak localhost seçilir.


### 3. Komut Gönderme (Telnet)
Lider node 6666 portunda komut bekler.
Telnete bağlanılırken lidere ``-DmyHost`` parametresiyle verilen IP kullanılır.

Terminal Üzerinden Telnete Bağlanma:
```
telnet 192.168.1.150 6666
```
Diskten Veri Okuma (GET) ve Diske Veri Yazma (SET):
```
SET 1 Naber
OK
GET 1
OK Naber
```

## Eklenecekler
- [ ] Senkron olan bazı işlemleri asenkron yapma.
- [ ] Liderin düşmesi durumunda bulunan üyelerden birinin liderin yerini alması.
- [ ] Service Discovery mekanizması.
