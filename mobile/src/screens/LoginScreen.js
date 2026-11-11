import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Image,
  Modal,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import api, { formatApiError, checkBackend, lastApiLog, API_BASE_URL } from '../config/api';
import useAuthStore from '../store/authStore';
import { colors, spacing, borderRadius, fontSize, fontWeight, commonStyles } from '../config/theme';

export default function LoginScreen({ navigation }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showLogModal, setShowLogModal] = useState(false);
  const [healthStatus, setHealthStatus] = useState(null);
  const [checkingHealth, setCheckingHealth] = useState(false);

  const testServerHealth = async () => {
    setCheckingHealth(true);
    try {
      const data = await checkBackend();
      setHealthStatus({ ok: true, data });
    } catch (err) {
      setHealthStatus({ ok: false, error: formatApiError(err) });
    } finally {
      setCheckingHealth(false);
    }
  };

  const login = useAuthStore((state) => state.login);

  const handleLogin = async () => {
    if (!email || !password) {
      Alert.alert('Hata', 'E-posta ve şifre alanları zorunludur.');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post('/auth/login', { email, password });
      
      // 1. ADIM: Sunucudan gelen ham veriyi kontrol edelim
      if (!response.data) {
        Alert.alert('Giriş Hatası', 'Sunucudan boş veri döndü.');
        setLoading(false);
        return;
      }

      const payload = response.data?.data || response.data || {};
      const access_token = payload.access_token || payload.token || response.data?.access_token;
      const refresh_token = payload.refresh_token || response.data?.refresh_token;
      const user = payload.user || response.data?.user || { email, full_name: email.split('@')[0] };

      if (!access_token) {
        Alert.alert(
          'Giriş Başarısız',
          'Sunucudan geçerli oturum bilgisi alınamadı. Lütfen e-posta ve şifrenizi kontrol edin.'
        );
        setLoading(false);
        return;
      }

      // Her şey doğruysa giriş yap ve yönlendir
      await login(user, access_token, refresh_token);
      const me = await api.get('/auth/me');
      await useAuthStore.getState().setUser(me.data?.data || me.data);
    } catch (error) {
      const errorMsg = formatApiError(error);
      Alert.alert('Giriş Hatası', errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    Alert.prompt(
      'Şifremi Unuttum',
      'E-posta adresinizi girin',
      [
        { text: 'İptal', style: 'cancel' },
        {
          text: 'Gönder',
          onPress: async (email) => {
            if (!email) {
              Alert.alert('Hata', 'E-posta adresi zorunludur');
              return;
            }
            try {
              await api.post('/auth/forgot-password', { email });
              Alert.alert('Başarılı', 'Şifre sıfırlama linki e-posta adresinize gönderildi');
            } catch (error) {
              console.error('Forgot password error:', error);
              Alert.alert('Hata', 'İşlem başarısız');
            }
          },
        },
      ],
      'plain-text',
      ''
    );
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.loginPanel}>
          <View style={styles.loginBrand}>
            <Image
              source={require('../../assets/scs360_mark.png')}
              style={styles.brandMarkImage}
              resizeMode="contain"
            />
            <View>
              <Text style={styles.brandTitle}>SCS360</Text>
              <Text style={styles.brandSubtitle}>Yönetim Merkezi</Text>
            </View>
          </View>

          <Text style={styles.loginTitle}>Giriş Yap</Text>
          <Text style={styles.loginLead}>
            Okul yönetim sisteminize erişmek için kimlik bilgilerinizi girin.
          </Text>

          <View style={styles.loginForm}>
            <View style={styles.field}>
              <Text style={styles.fieldLabel}>E-posta</Text>
              <View style={styles.inputContainer}>
                <Ionicons name="mail-outline" size={18} color={colors.muted} style={styles.inputIcon} />
                <TextInput
                  style={styles.input}
                  placeholder="E-posta adresiniz"
                  placeholderTextColor={colors.muted}
                  value={email}
                  onChangeText={setEmail}
                  autoCapitalize="none"
                  keyboardType="email-address"
                />
              </View>
            </View>

            <View style={styles.field}>
              <Text style={styles.fieldLabel}>Şifre</Text>
              <View style={styles.inputContainer}>
                <Ionicons name="lock-closed-outline" size={18} color={colors.muted} style={styles.inputIcon} />
                <TextInput
                  style={styles.input}
                  placeholder="Şifreniz"
                  placeholderTextColor={colors.muted}
                  value={password}
                  onChangeText={setPassword}
                  secureTextEntry={!showPassword}
                />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.passwordToggle}>
                  <Ionicons
                    name={showPassword ? 'eye-outline' : 'eye-off-outline'}
                    size={18}
                    color={colors.muted}
                  />
                </TouchableOpacity>
              </View>
            </View>

            <TouchableOpacity
              style={[styles.loginButton, loading && styles.loginButtonDisabled]}
              onPress={handleLogin}
              disabled={loading}
            >
              <Text style={styles.loginButtonText}>
                {loading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.forgotButton} onPress={handleForgotPassword}>
              <Text style={styles.forgotText}>Şifremi Unuttum</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.logButton}
              onPress={() => {
                testServerHealth();
                setShowLogModal(true);
              }}
            >
              <Ionicons name="pulse-outline" size={16} color={colors.brand} />
              <Text style={styles.logButtonText}>Sunucu Bağlantı Durumu & Log</Text>
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>

      {/* Sunucu Durumu & Log Modalı */}
      <Modal visible={showLogModal} animationType="slide" transparent>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <View style={styles.modalHeaderTitleRow}>
                <Ionicons name="server" size={20} color={colors.navy} />
                <Text style={styles.modalTitle}>Sunucu & Bağlantı Logu</Text>
              </View>
              <TouchableOpacity onPress={() => setShowLogModal(false)}>
                <Ionicons name="close" size={22} color={colors.muted} />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.modalBody}>
              <View style={styles.logSection}>
                <Text style={styles.logSectionLabel}>Hedef Sunucu (Base URL):</Text>
                <Text style={styles.logCodeText}>{API_BASE_URL}</Text>
              </View>

              <View style={styles.logSection}>
                <Text style={styles.logSectionLabel}>Sağlık Durumu (GET /ready):</Text>
                {checkingHealth ? (
                  <Text style={styles.logSubText}>Kontrol ediliyor...</Text>
                ) : healthStatus?.ok ? (
                  <Text style={styles.logSuccessText}>
                    ✅ Sunucu Aktif ({JSON.stringify(healthStatus.data)})
                  </Text>
                ) : (
                  <Text style={styles.logErrorText}>
                    ❌ {healthStatus?.error || 'Henüz test edilmedi'}
                  </Text>
                )}
              </View>

              <View style={styles.logSection}>
                <Text style={styles.logSectionLabel}>Son Yapılan API İsteği:</Text>
                <Text style={styles.logSubText}>
                  {lastApiLog.method ? `${lastApiLog.method} ${lastApiLog.url}` : 'Henüz istek yapılmadı'}
                </Text>
                <Text style={styles.logSubText}>
                  Durum: {lastApiLog.status || '—'} | Saat: {lastApiLog.timestamp}
                </Text>
              </View>

              <View style={styles.logSection}>
                <Text style={styles.logSectionLabel}>Sunucudan Dönen Son Yanıt / Hata:</Text>
                <View style={styles.logBox}>
                  <Text style={styles.logJsonText}>
                    {lastApiLog.data
                      ? JSON.stringify(lastApiLog.data, null, 2)
                      : lastApiLog.error || 'Kayıt bulunamadı'}
                  </Text>
                </View>
              </View>

              <View style={styles.logTipSection}>
                <Text style={styles.logTipTitle}>💡 Giriş Yapamıyorsanız:</Text>
                <Text style={styles.logTipText}>
                  1. E-posta adresinizi eksiksiz ve küçük harfle giriniz (örn: admin@scs360.com.tr).
                </Text>
                <Text style={styles.logTipText}>
                  2. Şifrenizi dikkatlice yazınız (büyük/küçük harf duyarlıdır).
                </Text>
                <Text style={styles.logTipText}>
                  3. Backend, dakikada en fazla 5 denemeye izin verir. Hata aldıysanız 1 dakika bekleyiniz.
                </Text>
              </View>
            </ScrollView>

            <TouchableOpacity style={styles.modalCloseButton} onPress={() => setShowLogModal(false)}>
              <Text style={styles.modalCloseButtonText}>Kapat</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  loginPanel: {
    width: '100%',
    paddingHorizontal: spacing.xxl,
    paddingVertical: spacing.xxxl,
  },
  loginBrand: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    marginBottom: spacing.xxxl,
  },
  brandMark: {
    width: 42,
    height: 42,
    borderRadius: borderRadius.xl,
    backgroundColor: colors.accent,
    justifyContent: 'center',
    alignItems: 'center',
  },
  brandMarkImage: {
    width: 46,
    height: 46,
    borderRadius: 14,
  },
  brandTitle: {
    fontSize: fontSize.huge,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
    letterSpacing: -0.5,
  },
  brandSubtitle: {
    marginTop: 5,
    color: colors.text.light,
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  loginTitle: {
    fontSize: fontSize.colossal,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
    letterSpacing: -1.2,
    marginBottom: spacing.md,
  },
  loginLead: {
    color: colors.muted,
    fontSize: fontSize.lg,
    lineHeight: 22,
    marginBottom: spacing.xxl,
  },
  loginForm: {
    gap: spacing.lg,
  },
  field: {
    gap: spacing.sm,
  },
  fieldLabel: {
    color: colors.text.primary,
    fontSize: fontSize.base,
    fontWeight: fontWeight.bold,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.lg,
    height: 48,
  },
  inputIcon: {
    marginRight: spacing.md,
  },
  input: {
    flex: 1,
    fontSize: fontSize.md,
    color: colors.ink,
  },
  passwordToggle: {
    padding: spacing.sm,
  },
  loginButton: {
    backgroundColor: colors.brand,
    borderRadius: borderRadius.lg,
    height: 48,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: spacing.sm,
  },
  loginButtonDisabled: {
    backgroundColor: colors.line,
  },
  loginButtonText: {
    color: colors.surface,
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
  },
  forgotButton: {
    alignItems: 'center',
    marginTop: spacing.md,
  },
  forgotText: {
    color: colors.brand,
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
  },
  logButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    backgroundColor: '#f8fafc',
    marginTop: spacing.md,
  },
  logButtonText: {
    color: colors.brand,
    fontSize: fontSize.sm,
    fontWeight: fontWeight.semibold,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing.xl,
  },
  modalContent: {
    width: '100%',
    maxHeight: '85%',
    backgroundColor: '#ffffff',
    borderRadius: borderRadius.xxl,
    padding: spacing.xl,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 8,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  modalHeaderTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  modalTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
  },
  modalBody: {
    marginVertical: spacing.md,
  },
  logSection: {
    marginBottom: spacing.md,
  },
  logSectionLabel: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
    color: colors.muted,
    textTransform: 'uppercase',
    marginBottom: 4,
  },
  logCodeText: {
    fontSize: fontSize.sm,
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    color: colors.brand,
    fontWeight: fontWeight.bold,
    backgroundColor: '#f1f5f9',
    padding: 8,
    borderRadius: borderRadius.sm,
  },
  logSubText: {
    fontSize: fontSize.sm,
    color: colors.ink,
    marginTop: 2,
  },
  logSuccessText: {
    fontSize: fontSize.sm,
    color: '#059669',
    fontWeight: fontWeight.semibold,
  },
  logErrorText: {
    fontSize: fontSize.sm,
    color: colors.danger,
    fontWeight: fontWeight.semibold,
  },
  logBox: {
    backgroundColor: '#0f172a',
    borderRadius: borderRadius.md,
    padding: 10,
    marginTop: 4,
    maxHeight: 180,
  },
  logJsonText: {
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    fontSize: 11,
    color: '#38bdf8',
  },
  logTipSection: {
    backgroundColor: '#fef3c7',
    borderRadius: borderRadius.md,
    padding: 12,
    marginTop: 8,
    marginBottom: spacing.md,
    borderLeftWidth: 4,
    borderLeftColor: '#f59e0b',
  },
  logTipTitle: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: '#92400e',
    marginBottom: 4,
  },
  logTipText: {
    fontSize: fontSize.xs,
    color: '#78350f',
    lineHeight: 18,
    marginTop: 2,
  },
  modalCloseButton: {
    backgroundColor: colors.navy,
    borderRadius: borderRadius.lg,
    paddingVertical: 12,
    alignItems: 'center',
    marginTop: spacing.sm,
  },
  modalCloseButtonText: {
    color: '#ffffff',
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
  },
});