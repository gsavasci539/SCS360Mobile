# 19 Kasım 2026 Kotlin genel sohbet dosyaları

Kotlin sohbet ekranı sistem belge seçicisinden PDF, PNG ve JPEG alır. Android istemcisinin dosya sınırı 10 MB'dir ve sunucunun yükleme sınırı ayrıca uygulanır. Metin olmadan da dosya gönderilebilir. Dosya adı ve SHA256 özeti, aynı mesaj kimliğinin sunucudaki sonucu ile eşleşmeden taslak kapanmaz. Kayıp yanıt sonucu ayrı GET ile okunur.

İletinin gerçek ek adı gösterilir. Dosya yalnızca aynı API'deki oturumlu indirme adresinden alınır ve SHA256 eşleşirse sistemde konum seçme açılır. Eski dış bağlantılar açılmaz.

Dört hedefli Android testi geçti. Yerel HTTP testleri multipart alanlarını, tek yazmadan sonra doğrulamayı ve değiştirilmiş indirmenin reddini kapsadı. Compose testleri dosya taslağı ve kaydetme denetimlerini kapsadı. Gerçek A26 ve sistem dosya seçici kabulü açık. Yeni APK üretilmedi.

Sırada ortak web öğrenci sohbetine, ilgili veli/öğretmen/personel yetkileriyle ek göndermek ve indirmek var. Aynı öğrenciye özel Kotlin veli ekranı bu adımda metin sohbetini sürdürür.
