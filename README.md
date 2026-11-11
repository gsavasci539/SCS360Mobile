# SCS360 mobil uygulamaları

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

> Yeni native yoklama checkpoint: gerçek FastAPI + geçici SQLite + Kotlin HTTP istemcisi kabulü geçti. 102 öğrenci/101 mevcut satır sayfalı yüklendi; mevcut not/HALF_DAY/sürüm korundu; kayıp yanıta aynı PUT gövdesi tek audit ile tekrarlandı; eski sürüm409, salt-okuma403, kurum dışı404, bildirim işi0. Tam yeni Android58 test paketi şu anda çalışıyor; aşağıdaki51PASS ve APK önceki kaynağa aittir. Güncel APK henüz üretilmedi. Aylık/derse ait native yoklama API bağlantısı açık; sahte takvim gösterilmez.
