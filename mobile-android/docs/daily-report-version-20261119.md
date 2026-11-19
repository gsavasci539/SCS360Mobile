# 19 Kasım 2026 · Android karne sürümü

Karne ayrıntısı sunucunun kayıt sürümünü saklıyor. Android, düzenleme ve silmede açılan sürümü `If-Match` başlığıyla gönderiyor. Araya başka bir yazma girdiğinde API HTTP 412 dönüyor ve kullanıcı güncel kaydı açabiliyor. Kayıp yanıttan sonra doğrulama, sunucudaki son kayıt üzerinden yapılmaya devam ediyor.

Yazma için sürüm alanını döndüren günlük karne API’si gerekiyor. Fiziksel A26 ve PostgreSQL kabulü açık.

Son kaynakla Gradle derlemesi ve üç Android testi geçti. Yerel HTTP testi düzenleme ile silmenin ekrandaki doğru sürümü gönderdiğini ve kayıp yanıttan sonra doğrulamayı kapsadı. Salt okunur ayrıntı 360 dp ve 2× yazıyla yeniden geçti.
