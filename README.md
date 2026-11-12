<!-- native-finance-20261112:start -->
Native recorded finance summary checkpoint: Android 82/82 PASS, 0 failures/errors/skips (17m48s); backend summary 9 PASS/8 conditional SKIP and related analytics 5 PASS/5 conditional SKIP. HTTP200 success:false rejection covered. Real Kotlin/FastAPI finance bridge and new APK are pending; previous APK does not contain this change. This is not full native finance, device or provider acceptance.
<!-- native-finance-20261112:end -->

# SCS360 mobil uygulamaları

> Native registry kabulü (12 Kasım): tam Android74/74PASS,0skip13m5s; ardından yalnız test kaydırma/görsel kontrolleri güçlendirildi,10/10UI takip testi PASS. Aynı uygulama kaynağı gerçek FastAPI/geçiciSQLite ile CRUD,102öğrenci sayfalama,403/404/409/422, minimal yazma rolü, klinik not korunması ve Room sentinel aynılığı testini geçti. Üç gerçek pencere qualifierında sistem font2 ve48dp hedefler kontrol edildi; üç native PNG incelendi. Sınıf kaydetme rasteri senkron tutarsızlığından kanıta alınmadı; tam viewport geometri kontrolü geçti. Debug APK imzası doğrulandı; SHA256 79c6a7c512e7071453146e2c3087996955341eedef51c4517c745dfc8d24a8a1. Veli ilişkileri, özel rol native login/görünümü, diğer native işlemler ve gerçek cihaz/HTTPS/sağlayıcı/mağaza kabulü açık.


> Önceki native yoklama kabulü: gerçek FastAPI/geçiciSQLite/Kotlin HTTP senaryosu PASS; tam Android58/58PASS,0skip5m26s. Sonrasında yalnız test görsel/scroll çağrıları değişti; native render testi3pencere/2xfont ile ayrıca PASS ve görseller incelendi. Uygulama kaynağı değişmeden debug APK üretildi ve imza doğrulandı; SHA256 64f8121912246a1fc3ec82626cb20dfa24b4859389e506f81229f8a1367aa3ca. Aşağıdaki51test/APK önceki checkpointtir. Tüm native eşitleme, aylık yoklama, gerçek A26/HTTPS/ödeme kabulü tamamlanmadı.

- `mobile-android/`: mevcut Kotlin/Jetpack Compose Android ekranları. Bu ekranlar korunarak web API ve işlemleriyle tek tek eşitleniyor.
- `mobile/`: ayrı Expo 57 istemcisi; Android, iOS ve web/PWA kaynakları.
- Backend ve web: https://github.com/gsavasci539/SCS360

## Android geliştirme

Uygulama derlemesi JDK17 ile geçti; Android36 Robolectric testleri Java21+ gerektirir (yerelde Java23 kullanılıyor). Gradle9.3.1 ve Android SDK36.1 gerekir. Resmî Gradle9.3.1 wrapper kaynakta vardır ve dağıtım SHA256 değeri pinlidir. `mobile-android/` içinde (Windows PowerShell için `./gradlew` yerine `./gradlew.bat` kullanın):

```sh
./gradlew --max-workers 1 --no-parallel :app:testDebugUnitTest
./gradlew --max-workers 1 --no-parallel :app:assembleDebug -PAPI_BASE_URL=https://YOUR_BACKEND/api/
```

`YOUR_BACKEND` sahibi tarafından sağlanacak yer tutucudur. Varsayılan HTTP adresi yalnız yerel emülatör geliştirmesi içindir. Gerçek API adresini ve Firebase yapılandırmasını yerel olarak sağlayın. Merchant anahtarları mobil uygulamaya konulmaz.

## Expo geliştirme

`mobile/` içinde `npm ci`, `npm test`, ardından `npm start` çalıştırın. `.env.example` içindeki `EXPO_PUBLIC_API_BASE_URL` için sahibinin onayladığı API adresini girin. iOS yerel derlemesi macOS/Xcode gerektirir.

## Kabul durumu

Güncel native kaynak51/51 testten geçti; hata ve atlanan test yoktur. Bu paket responsive, API scope, Retrofit tanım ve token kasası regresyonlarını içerir. Aynı Kotlin kaynakla debug APK derlendi; imza ve paket metadata kontrolü geçti. APK yerelde tutulur; burada kaynaklar ve kabul kanıtı bulunur. Ayrıntılar `docs/VERIFICATION_20261111.md` içindedir. Bütün native işlemler web ile henüz eşit değildir.

Gerçek A26 cihazı, sağlayıcı sandbox anahtarları/HTTPS callback ve mağaza abonelik ürünleri için kabul açık. Debug APK üretim sürümü değildir ve bu kaynak deposuna eklenmez. App Store/Google Play ürünleri henüz tanımlı olmadığından native SaaS satın alımı kapalıdır. Kurumların kendi okul/kurs tahsilatları ayrı sunucu checkout hesabı kullanır.

Gizli anahtarlar, gerçek kullanıcı kayıtları, yerel veritabanları, imzalama dosyaları ve derleme önbellekleri bu kaynak aktarımına dahil edilmez.
