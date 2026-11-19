# 19 Kasım 2026 Kotlin genel mesajlaşma

Mevcut Kotlin sohbet ekranı, öğretmen portalı ve genel navigasyonda gerçek sohbet listesi, geçmiş ve alıcı dizinini açıyor. Şube seçimi tüm isteklerde doğrulanıyor. Önceki mesajlar sunucudan sayfalanıyor. Okundu bilgisi ve sohbet sayıları sunucu yanıtından geliyor. Veliye özel öğrenci sohbeti mevcut API'sini kullanmaya devam ediyor.

Gönderim, oturum ve kapsam içinde tutulan taslak kimliğiyle kaydediliyor. Gönderme yanıtı kaybolursa sonuç düğmesi yeni yazma isteği üretmeden sunucu kaydını okur. Sunucu henüz kayıt göstermiyorsa kullanıcı aynı kimlikle yeniden deneyebilir. Doğrulanmış mesaj gösterilir. Kurum, şube, rol veya oturum değişince ekrandaki taslak ve liste yenilenir.

Android'de 11 hedefli test geçti. Yerel HTTP testi kayıp yanıt, şube, sayfalama ve tek istek davranışını doğruladı. Compose testi 640×360 dp yatay ekranda yüzde 150 yazı boyutuyla taslak ve doğrulama düğmesini kapsadı. Dokuz mobil senaryo testi de geçti. Fiziksel A26 kabulü açık.

Sohbet dosyası yükleme ve indirme açık. Kapsamdaki mevcut dosyanın varlığı gösteriliyor. Chatbot'taki eski isim içeren kısayolların gerçek alıcı seçimine ve varsayılan veli portalındaki öğrenci sohbetine bağlanması sıradaki iç iş.
