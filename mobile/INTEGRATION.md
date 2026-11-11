# Mobil ödeme bağlantısı

Kaynak: kullanıcının `remix-okul360.zip` arşivindeki Expo istemcisi. Eski backend/web kopyaları güncel projeye alınmadı. Arşivdeki dağıtım ve anahtar yükleme yönergeleri çalıştırılmadı.

- `EXPO_PUBLIC_API_BASE_URL`: sahibi tarafından sağlanan backend HTTPS adresi, `/api` dahil. İstemciye merchant secret konulmaz. Yerel geliştirmede HTTP yalnız development modunda kullanılabilir.
- Finans ekranı yetkili borçları gerçek API'den alır; kurumun backend'de seçili iyzico/PayTR/Craftgate/Sipay hesabına yönlenir. Banka POS'ları Craftgate'teki kurum bağlantıları üzerinden desteklenir; doğrudan her bankanın API adaptörü hazır değildir.
- Hosted sayfa dış tarayıcıda açılır. Uygulamaya dönünce veya “Sunucudan ödeme sonucunu kontrol et” ile authenticated refresh yapılır. URL parametresi başarı sayılmaz. Sunucu `PAID` ve `payment_id` doğrulamadan tahsilat gösterilmez.
- İşlem kimliği kullanıcı/kurum/borca bağlı kalıcı metadata'dır. Zaman aşımında aynı kimlik ve bellek içindeki aynı istek yeniden kullanılır. Kimlik/fatura bilgileri, hosted URL ve sağlayıcı anahtarları checkout depolamasına yazılmaz. Sonucu belirsiz ödeme silinip yenisi oluşturulmaz.
- Superadmin varsayılan SaaS kurum listesini görür; kurum seçimi açıkça yapılır. “SaaS yönetimine dön” seçimi kaldırır. Sunucunun kurum/yetki kontrolü her istekte geçerlidir.
- SaaS üyeliği kurum tahsilatından ayrıdır. Web/PWA mevcut üyelik checkout'unu kullanır. Native IOS/ANDROID üyelik satın alımı henüz mağaza ödeme yapılandırması olmadığı için backend tarafından engellenir. Mevcut üyeliğe erişim ayrı kalır; mağaza satın alma kabulü yapılmış sayılmaz.

Doğrulama: `npm test`; `npx expo export --platform all --max-workers 2`. Export, Android/iOS cihaz derlemesi veya mağaza/provider kabulü değildir. iOS native derlemesi macOS/Xcode ve kullanıcıya ait imzalama bilgileri gerektirir. EAS proje kimliği ve Android applicationId kaynak arşivinden gelir; buluta build/publish yapılmadı. Gerçek sağlayıcı sandbox anahtarları, erişilebilir HTTPS callback ve ilgili merchant sözleşmeleri hâlâ gereklidir.

Resmi referanslar: [Expo57](https://docs.expo.dev/versions/v57.0.0/), [iyzico CheckoutForm](https://docs.iyzico.com/on-hazirliklar/api-reference-beta/odeme-metotlari/iyzico-odeme-formu-cf), [Apple ödemeler](https://developer.apple.com/app-store/review/guidelines/#payments), [Google Play ödeme politikası](https://support.google.com/googleplay/android-developer/answer/9858738?hl=en).

11 Kasım 2026 kullanıcı yanıtı: App Store/Google Play abonelik ürünleri henüz tanımlı değil. Native üyelik satın alımı kapalı kalır; bu durum mevcut üyelik erişimi ve kurumun ayrı okul/kurs tahsilatını mağaza aboneliğine dönüştürmez. Ürün kimliği veya mağaza kabulü varmış gibi gösterilmez.
