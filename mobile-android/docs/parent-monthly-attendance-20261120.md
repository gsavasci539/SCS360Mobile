# 20 Kasım 2026 · Android veli aylık yoklaması

Günlük akıştaki yoklama kaydı, seçili çocuğun ilgili ayını ve günü native takvimde açıyor. Doğrudan Yoklama sekmesi İstanbul saatine göre güncel aydan başlıyor. Sekme ve geçiş için `parent.view_attendance` gerekiyor. Aylık API isteği öğrenci kimliğini, yılı ve ayı taşıyor. Android yanıttaki aynı alanları ve takvim günlerini doğruluyor.

Takvim durum, giriş, çıkış ve not alanlarını sunucu yanıtından alıyor. Boş ay ve kayıt bulunmayan gün açıkça belirtiliyor. Ay değiştiğinde önceki ayın ayrıntısı kapanıyor. Günlük akışa dönüşte seçilmiş tarih korunuyor.

Kotlin derlemesi ve iki yerel HTTP destekli Robolectric Compose testi geçti. 360 dp ve 2× yazıda yetki kapısı, çocuk ve ay parametreleri, seçili gün, boş ay ve tarihli akışa dönüş doğrulandı. Fiziksel A26 kabulü açık.
