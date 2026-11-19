# 19 Kasım 2026 · Android karne formunda öğrenci seçimi

Öğretmen ve yönetim formları, web ile aynı karne öğrenci seçimi API’sini kullanıyor. Öğrenci adı ve numarası aranabiliyor, seçenekler sayfalanıyor. Aktif şube ve varsa öğretmenin seçtiği sınıf isteğe ekleniyor. Kayda seçilen öğrencinin sunucudan gelen öğrenci ve sınıf kimlikleri gönderiliyor.

Karne yazma yetkisi olan kullanıcı öğrenci dizini izni olmasa da formu açabiliyor. Karne okuma izni yoksa liste istenmiyor ve ekranda erişim durumu gösteriliyor.

Üç Android HTTP istemci testi ve bir Robolectric Compose testi geçti. Compose testi 360 dp genişlik ve 2× yazı ölçeğinde öğrenci seçimini, boş formun kayıt kilidini ve bekleyen kayıt sırasında düğmelerin kapalı olmasını doğruladı. API kapsamı ve gerçek DTO ile kayıt geçici SQLite üzerinde doğrulandı. Fiziksel A26 ve PostgreSQL kabulü bekliyor.
