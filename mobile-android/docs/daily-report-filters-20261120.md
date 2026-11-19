# 20 Kasım 2026 · Android karne listesi filtreleri

Kotlin karne listesi web ile aynı uçtan sayfa başına 20 kayıt istiyor. Arama, isteğe bağlı tarih ve sınıf filtresi seçili şubeyle birlikte sunucuya gönderiliyor. Liste alanları, toplam ve sayfa sayısı sunucu yanıtından geliyor.

Şube, kurum veya hesap değiştiğinde önceki istek iptal ediliyor ve filtreler sıfırlanıyor. Kayıt veya silme sonrası etkin sorgu yenileniyor. Son sayfa silmeyle boşalırsa liste mevcut son sayfaya dönüyor. Yerel tahmin kartı kaldırıldı ve özetin gerçek analiz API’sine bağlanması devam kaydına alındı.

Kotlin derlemesi, dört yerel HTTP istemci testi ve üç Robolectric Compose testi geçti. 360 dp / 2× yazıyla karne filtreleme, kayıt sonrası etkin sorgu, sunucuya koşullu silme, silme sonrası yenileme ve geciken şube yanıtının elenmesi doğrulandı. Fiziksel A26 kabulü açık.
