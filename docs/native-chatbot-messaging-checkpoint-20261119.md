# 19 Kasım 2026 Android asistanı ve sohbet kısayolu

Android asistanı webdeki `/api/parent/chatbot` ve `/api/v1/chatbot/process-query` adreslerini kullanır. İstemci son altı uygun konuşma turunu, veli seçtiğinde bağlı öğrenci kimliğini gönderir. Okul asistanına erişilemiyorsa hata gösterilir. Örnek kişi listeleri, yerel yanıt üretimi ve doğrulanmamış sayaçlar kaldırıldı.

Veli kısayolu seçili öğrencinin mevcut Kotlin öğretmen sohbetini açar. Personel kısayolu mesaj okuma yetkisiyle gerçek sohbet listesine gider. Uygulamanın desteklediği sunucu kısayolları yetkili sayfalara yönlenir. Öğrenci, oturum, kurum, şube veya rol değişiminde asistan geçmişi temizlenir.

Üç istemci ve navigasyon testi ile bir sentetik Compose testi geçti. Veli ekranı testi seçili öğrencinin öğretmen isteğini ve mesaj göndermeden açılışı doğruladı. Gerçek sağlayıcı ve fiziksel cihaz kabulü açık. Önceki yerel debug APK bu kaynak güncellemesinden önce derlendi.

Sırada sohbet eklerini yetkili yükleme ve indirme akışına bağlamak var.
