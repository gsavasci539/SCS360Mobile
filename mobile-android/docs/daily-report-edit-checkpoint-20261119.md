# 19 Kasım 2026 · Android karne ayrıntısı, düzenleme ve silme

Karne kartı sunucudaki ayrıntıyı açıyor. Yazma yetkisi olan hesaplar gözlemleri düzenleyebiliyor ve açık onayla kayıt silebiliyor. Değişiklikten önce kayıt karşılaştırılıyor. Düzenleme sunucudaki içerikten, silme ise ayrıntının artık bulunmamasından doğrulanıyor. Yanıt kaybolduğunda istemci yeniden okuyarak sonucu kontrol ediyor. Doğrulanmış işlemden sonra liste yenileniyor.

Son kaynakla 3 Android testi geçti. Yerel HTTP testleri kayıp düzenleme ve silme yanıtlarını, yazma alanlarını ve önceki kaydın sunucuda değiştiği durumu kapsadı. 360 dp ve 2× yazıda salt okunur ayrıntı testi geçti. Geçici SQLite API testi, atanmış öğrenciye ait karne düzenleme ve silmeyi, izin reddini ve kapsam dışındaki kaydın korunmasını doğruladı. Fiziksel A26 ve PostgreSQL kabulü açık.

Karşılaştırma istemcideki ön okumaya dayanıyor. Sırada sunucuda koşullu güncelleme ve silme ekleyerek eşzamanlı web ve Android işlemlerini korumak var.
