# SCS360 mobil uygulamaları

- `mobile-android/`: mevcut Kotlin/Jetpack Compose Android ekranları. Bu ekranlar korunarak web API ve işlemleriyle tek tek eşitleniyor.
- `mobile/`: ayrı Expo 57 istemcisi; Android, iOS ve web/PWA kaynakları.
- Backend ve web: https://github.com/gsavasci539/SCS360

## Android geliştirme

JDK17, Gradle9.3.1 ve Android SDK36.1 gerekir. Kaynakta Gradle wrapper yoktur; belirtilen Gradle sürümünü kurun. `mobile-android/` içinde:

```sh
gradle --max-workers 1 --no-parallel :app:testDebugUnitTest
gradle --max-workers 1 --no-parallel :app:assembleDebug -PAPI_BASE_URL=https://YOUR_BACKEND/api/
```

`YOUR_BACKEND` sahibi tarafından sağlanacak yer tutucudur. Varsayılan HTTP adresi yalnız yerel emülatör geliştirmesi içindir. Gerçek API adresini ve Firebase yapılandırmasını yerel olarak sağlayın. Merchant anahtarları mobil uygulamaya konulmaz.

## Expo geliştirme

`mobile/` içinde `npm ci`, `npm test`, ardından `npm start` çalıştırın. `.env.example` içindeki `EXPO_PUBLIC_API_BASE_URL` için sahibinin onayladığı API adresini girin. iOS yerel derlemesi macOS/Xcode gerektirir.

## Kabul durumu

Son doğrulanmış native responsive checkpoint40/40 testten geçti; bu sonuç daha sonraki API/oturum değişikliklerini kapsamaz. Güncel API/oturum kaynağının derlemesi ve yeni testleri devam ediyor. Ayrıntılar `docs/VERIFICATION_20261111.md` içindedir. Bütün native işlemler web ile henüz eşit değildir.

Gerçek A26 cihazı, sağlayıcı sandbox anahtarları/HTTPS callback ve mağaza abonelik ürünleri için kabul açık. Eski APK güncel kaynağın çıktısı değildir; bu depoda yayınlanmaz. App Store/Google Play ürünleri henüz tanımlı olmadığından native SaaS satın alımı kapalıdır. Kurumların kendi okul/kurs tahsilatları ayrı sunucu checkout hesabı kullanır.

Gizli anahtarlar, gerçek kullanıcı kayıtları, yerel veritabanları, imzalama dosyaları ve derleme önbellekleri bu kaynak aktarımına dahil edilmez.
