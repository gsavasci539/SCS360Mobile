# 20 Kasım 2026 · Android karne analizi

Yönetim karne listesi ve öğretmen formu karne okuma yetkisiyle aynı sunucu özetini açıyor. Seçili şube istekle gönderiliyor ve Android yanıttaki şubeyi doğruluyor. Şube, kurum veya hesap değiştiğinde panel kapanıyor ve önceki veri temizleniyor.

Özet kayıtlı gözlemlerden hesaplanan oranları ve takip notlarını gösteriyor. Gözlemsiz alanlar `Gözlem yok` olarak gösteriliyor. Formdaki açık analiz, karne kaydından sonra yeniden yükleniyor. Yönetim ekranında düzenleme ve silme sonrası da yeniden yükleniyor.

Bir yerel HTTP istemci ve üç hedefli Robolectric Compose testi geçti. 360 dp ve 2× yazıda %0, gözlemsiz alan ve şube değişimi doğrulandı. Panel filtre alanına yerleştirildikten sonra karne ayrıntısı, koşullu silme ve etkin filtreyle yenileme testi geçti. Fiziksel A26 kabulü açık.
