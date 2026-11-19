# 19 Kasım 2026 · Android günlük karne kaydı

Öğretmen ve yönetim formları seçili öğrencinin gerçek sınıfıyla `/api/daily-reports` API’sine gider. Öğrenci ve gözlemler boş başlar. İşlem sırasında form kilitlenir. Kayıt doğrulandıktan sonra liste seçili şube için yenilenir. Karne kaydı telefonun yerel listesine yazılmaz ve bildirim teslimi duyurulmaz.

Okuma izni olan hesaplarda istemci kayıt kimliğini, öğrenciyi, sınıfı, tarihi, kaydeden hesabı ve gözlemleri sunucudan doğrular. Kayıp yanıtta aynı öğrenci ve tarihe ait kayıt aranır. Aynı içerik doğrulanırsa ikinci kayıt açılmaz. Farklı içerikle karşılaşılırsa form açık kalır. Yalnız yazma yetkisi olan hesap, sunucu kayıt yanıtı ile doğrulanır.

Son kaynakla Gradle derlemesi ve iki hedef Android testi geçti. Yerel HTTP testi kayıp yanıtta tek kayıt ve yalnız API alanlarının gönderilmesini doğruladı. İlk büyük yazı testinde pencere yerleşimi tamamlanamadı. Sınırlandırılmış ve kaydırılabilir pencereyle 360 dp ve 2× yazıda boş form ve kayıt kilidi doğrulandı. Fiziksel A26 kabulü açık.

Sırada webdeki günlük karne ayrıntısı, düzenleme ve silme işlemlerini Android’de yetkili API uçlarına bağlamak var.
