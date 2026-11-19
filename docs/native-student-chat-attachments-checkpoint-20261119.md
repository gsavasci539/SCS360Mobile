# 19 Kasım 2026 · Android veli sohbeti dosya ekleri

Veli, öğrencisinin öğretmeniyle yaptığı sohbette Android belge seçicisinden PDF, PNG veya JPEG ekleyebilir. Dosya tek başına ya da mesajla gönderilebilir. Gönderim ayrı bir istekle sunucudan okunur. Mesaj kimliği, katılımcılar, metin ve varsa dosya adı ile SHA-256 özeti eşleştiğinde taslak kapanır. Yanıt kesilirse aynı işlem kimliği sorgulanır ve kayıt bulunamazsa aynı kimlikle yeniden denenir.

İndirme seçili öğrenci ve öğretmenin oturumlu adresinden yapılır. Dosya ancak SHA-256 özeti eşleştiğinde sistemde konum seçmek için açılır. Dosya işlemleri oturum ve öğrenci kapsamına bağlıdır.

İki yerel HTTP testi, iki sentetik Compose testi ve geçici SQLite üzerinde gerçek FastAPI ile bir Kotlin kabul testi geçti. Gerçek API testi aynı ekin yinelenen gönderimini tek kayıtta gördü ve indirilen baytları karşılaştırdı. HTTP testleri kayıp yanıtı, değişmiş dosyanın reddini ve eski oturumun istek göndermemesini doğruladı. Compose testleri 360 dp ve yüzde 200 yazı ölçeğinde gönderim, geçmiş ve dosya düğmelerini kapsadı.

Fiziksel A26, sistem belge seçici ve iOS kabulü açık. Yeni APK üretilmedi. Sırada Android öğretmen ve yönetim sohbetinde seçili öğrenci ile veliye özel yanıt ve dosya işlemlerini web ile eşitlemek var.
