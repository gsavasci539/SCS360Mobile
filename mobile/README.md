# SCS360 Mobile

SCS360 Okul Yönetim Sistemi için mobil uygulama. Hem iOS hem de Android platformlarında çalışır.

## Teknoloji Stack

- **React Native** (Expo SDK 57)
- **React Navigation** (Bottom Tabs ve Native Stack)
- **Zustand** (State Management)
- **Axios** (API istekleri)
- **Expo Vector Icons** (İkonlar)

## Özellikler

- 🔐 Kullanıcı girişi ve kimlik doğrulama
- 📊 Dashboard ile genel istatistikler
- 👥 Öğrenci yönetimi (liste, arama, detay)
- 📅 Yoklama takibi
- 💰 Finans yönetimi (ödeme takibi)
- ⚙️ Ayarlar ve profil yönetimi

## Gereksinimler

- Node.js 18+ 
- npm veya yarn
- iOS geliştirme için macOS (Xcode)
- Android geliştirme için Android Studio

## Kurulum

```bash
cd SCS360Mobile
npm install
```

## Çalıştırma

### Web (Geliştirme)
```bash
npm start
```
Tarayıcıda açmak için `w` tuşuna basın.

### Android
```bash
npm run android
```
Android emülatörü veya bağlı cihaz gerektirir.

### iOS (Sadece macOS)
```bash
npm run ios
```
iOS Simulator'da açılır.

## Yapılandırma

API URL'sini `src/config/api.js` dosyasından değiştirebilirsiniz:

```javascript
const API_BASE_URL = 'https://scs360.com.tr/api';
```

## Proje Yapısı

```
SCS360Mobile/
├── src/
│   ├── config/
│   │   └── api.js           # API yapılandırması
│   ├── navigation/
│   │   └── AppNavigator.js  # Navigasyon yapısı
│   ├── screens/
│   │   ├── LoginScreen.js
│   │   ├── DashboardScreen.js
│   │   ├── StudentsScreen.js
│   │   ├── AttendanceScreen.js
│   │   ├── FinanceScreen.js
│   │   └── SettingsScreen.js
│   └── store/
│       └── authStore.js     # Auth state management
├── App.js
└── package.json
```

## Demo Giriş

Backend'de demo verisi varsa:
- E-posta: admin@demo.com
- Şifre: Admin123*

## Build

### Production Build (Android)
```bash
eas build --platform android
```

### Production Build (iOS)
```bash
eas build --platform ios
```

## Notlar

- Uygulama backend API'sine bağlanır, backend'in çalıştığından emin olun
- iOS build için Apple Developer Account gerekebilir
- Android build için signing key yapılandırması gerekebilir
