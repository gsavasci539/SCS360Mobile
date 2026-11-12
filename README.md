Native debt create real Kotlin/FastAPI temporary DB acceptance PASS: 102 real write-authorized options, committed-ACK-loss replay creates one100.01 debt, Room ledger unchanged, no payments or notification jobs. Release preflight rejects missing API and HTTP emulator endpoint; release taskgraph validates first. Full current Android regression pending. Real A26/iOS/provider/store/release acceptance remains OPEN.

<!-- native-debt-create-20261112:start -->
WIP native canonical debt create: durable actor/workspace/branch request UUID, complete write-authorized student choices, exact whitelist payload and server ACK. Legacy create pre-HTTP Room writes and fake invoice identifiers removed; both finance entry points use the canonical form. Backend12 serial/concurrent retry and branch-option cases PASS across temporary SQLite/PostgreSQL44.67s. Initial affected native7PASS; final persistence guard test, real Kotlin/FastAPI acceptance and new APK pending.
<!-- native-debt-create-20261112:end -->

Backend debt create accepts optional request_id UUID: same actor/school/student/type/amount/due/description replays existing debt, changed/deleted/colliding request rejected409; organization lock and primary key enforce uniqueness. SQLite+PostgreSQL30 targeted regression PASS, final error-response adjustment6 PASS (overlapping tests, not36 unique). Android nonce/body integration remains OPEN. Android source unchanged; last signed debug APK600e36ac remains current.

Native invoice draft UI: six targeted UI/scope tests PASS5m41s, same-source debug APK/signature PASS2m17s. Bounded native scrollable form, blank amount/date/student defaults, 48dp actions and read-only controls; actual portal dialog closes on workspace switch. Previous AlertDialog idling scenario now passes with replacement form. Backend submission DTO, create idempotency, write-authorized student options and full feature acceptance remain OPEN.

Native finance presentation scope: 14/14 affected scope/cash/receipt tests PASS1m53s; same-source debug APK/signature PASS2m46s. Actor/organization/workspace/branch/role/permission/logout changes reset finance dialogs, while refresh preserves drafts. Receipt stale-session content hides. Prior full93Android/906backend checkpoint is historical; current full Android not rerun. Invoice AlertDialog test idling remains an explicit UI test limitation.

<!-- native-receipt-20261112:start -->
Current automated acceptance: backend906 unique PASS/2 intentionally database-specific SKIP across isolated SQLite and PostgreSQL runs; Android93/93PASS; actual Kotlin/FastAPI receipt/payment linking, lost-ACK replay and server PDF PASS. Same-source signed debug APK ready locally. Frontend unchanged: prior344PASS remains applicable. Physical A26/IME/Android print service, real provider/store acceptance and unfinished native feature parity remain open.
<!-- native-receipt-20261112:end -->

<!-- native-cash-20261112:start -->
Native cash/transfer ledger checkpoint: Android full90/90 PASS (16m49s); real Kotlin/FastAPI temporary SQLite lost-ACK replay and partial payments1 PASS/1PGdeselected (107.57s); backend integrity12 PASS/12PGdeselected. Same-source debug APK/signature PASS2m43s. Physical A26, actual provider/store acceptance, other finance rows/documents and portal-local context lifecycle remain open.
<!-- native-cash-20261112:end -->

<!-- native-finance-20261112:start -->
Native recorded finance summary accepted: full Android 82/82 PASS (17m48s), real Kotlin/FastAPI temporary SQLite acceptance 1 PASS/1 PostgreSQL deselected (115.55s), same-source debug APK/signature PASS (2m48s). Debt/payment row mutations, parent flows, physical A26 and actual provider/store acceptance remain open.
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

Önceki tahakkuk-oluşturma temeli: 100/100 native regresyon testi geçti (25 sınıf; hata/atlanan yok). Aynı kaynaktan debug APK imza ve paket doğrulaması yapıldı. Tahakkuk oluşturma gerçek API ve geçici DB ile kabul edildi; bütün native işlemler web ile henüz eşit değildir. Güncel kanıt docs/acceptance/native-debt-create-20261112.json ve docs/VERIFICATION_20261111.md içindedir.

Gerçek A26 cihazı, sağlayıcı sandbox anahtarları/HTTPS callback ve mağaza abonelik ürünleri için kabul açık. Debug APK üretim sürümü değildir ve bu kaynak deposuna eklenmez. App Store/Google Play ürünleri henüz tanımlı olmadığından native SaaS satın alımı kapalıdır. Kurumların kendi okul/kurs tahsilatları ayrı sunucu checkout hesabı kullanır.

Gizli anahtarlar, gerçek kullanıcı kayıtları, yerel veritabanları, imzalama dosyaları ve derleme önbellekleri bu kaynak aktarımına dahil edilmez.


Önceki tahakkuk ledger checkpoint: native104/104 test ve gerçek API/tempDB kabulü geçti; aynı kaynaktan debug APK doğrulandı. Backend tam SQLite/PG kabulü 922 benzersiz PASS, yalnız2 belgelenmiş DB varyantı skip. Bütün uygulama web paritesi ve satış kabulü tamamlanmadı; docs/acceptance/native-debt-ledger-20261112.json açık işleri listeler.


Edit-form kuruş/taslak/ACK ve scope düzeltmesi kabul edildi; önceki104/104 test ve o kaynağın APK doğrulandı. Satış kabulü ve diğer modüller açık; docs/acceptance/native-debt-edit-ui-20261112.json.


Gider oluşturma/listeleme/silme ve web değişikliklerinin native görünürlüğü kabul edildi: önceki109/109 test ve aynı kaynak debug APK; backend 940 benzersiz PASS/2 DB varyantı skip. Yerel gider fallback ve örnek harcamalar kaldırıldı. Native gider düzenleme UI ve diğer modüller/fiziksel sağlayıcı-mağaza kabulü açık; docs/acceptance/native-expense-20261112.json.

Güncel finans portalı gider silme checkpoint:112 benzersiz native test geçti (6hedef+106regresyon,tekrar yok). Seçili kayıt/onay/hata/kapsam/büyük yazı kabulü ve aynı kaynak debug APK doğrulandı. Native düzenleme/notlar ve diğer modüller/gerçek cihaz/sağlayıcı/mağaza/yayın kabulü açık; docs/acceptance/native-expense-delete-20261112.json.
