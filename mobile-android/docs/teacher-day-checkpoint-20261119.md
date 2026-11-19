# 19 Kasım 2026 · Android öğretmen programı

Mevcut Kotlin öğretmen portalının Program sekmesi atanmış sınıfları, günlük programı ve bekleyen işleri öğretmen API’sinden alıyor. Haftalık görünüm, öğretmenin seçtiği sınıfın kayıtlı çizelgesini kullanıyor. Kullanıcının izinleri, görünen bölümleri ve yapılan istekleri belirliyor. Her yükleme aynı oturum ve kurum bağlamında tamamlanıyor. Boş sonuç ve bağlantı hatası ayrı gösteriliyor.

Örnek günlük program ve sınıf kaldırıldı. Günlük karne ekranının öğrenci seçimi yoklamada seçili gerçek sınıfı kullanıyor. Karne kaydının sunucuda doğrulanması ve yerel bildirim iddiasının kaldırılması sıradaki iş.

Son kaynakla Gradle çevrimdışı derleme ve üç hedef test başarılı. İki yerel HTTP testi API yolunu, çalışma alanı başlığını, DTO ayrıştırmasını ve izin hatasını kapsadı. Compose testi 320 dp genişlik ve 2× yazı ölçeğinde kayıtlı dersin görünmesini ve yalnız program isteği yapılmasını doğruladı. Fiziksel A26 kabulü açık.
