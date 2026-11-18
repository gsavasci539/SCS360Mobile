# Android izin işlemlerinde sonuç doğrulama

Android, yeni izin talebinde web ile aynı API alanlarını ve benzersiz istek kimliğini gönderiyor. Onay, ret ve oluşturma işlemleri listede doğrulandıktan sonra form kapanıyor. Bağlantı kesilirse kullanıcının girdiği bilgiler korunuyor ve Sonucu kontrol et düğmesi yalnızca listeyi okuyor. İşlem kapsamı oturum ve çalışma alanıyla birlikte değişiyor.

7 istemci ve 3 arayüz testi geçti. Kayıp yanıttan sonra tek POST ve başarılı okuma, yanlış personel veya yorum eşleşmesinin reddi, salt okuma görünümü ve 1,5 yazı ölçeğinde yatay değerlendirme penceresi doğrulandı. XML kanıtı yerel `output/native-staff-leave-20261118-*.xml` dosyalarında.

Yeni izin kimliği ve şube izni için ayrı geçici SQLite sonuçları web kabul kaydında yer alıyor. Sıradaki iş Android randevu listesini ve işlemlerini gerçek API'ye bağlamak. Fiziksel cihaz kabulü açık.
