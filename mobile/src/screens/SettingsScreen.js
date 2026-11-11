import React, { useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Image,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import api from '../config/api';
import useAuthStore from '../store/authStore';
import { colors, spacing, borderRadius, fontSize, fontWeight, shadows } from '../config/theme';
import { getBottomPadding, getTabBarHeight, getResponsiveContainerStyle, responsiveIconSize, useResponsive } from '../utils/responsive';

export default function SettingsScreen({ navigation }) {
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const logout = useAuthStore((state) => state.logout);
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchUserProfile();
  }, []);

  const fetchUserProfile = async () => {
    try {
      const response = await api.get('/auth/me');
      setUser(response.data);
    } catch (error) {
      console.error('Fetch profile error:', error);
    }
  };

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
      logout();
      navigation.getParent()?.replace('Login');
    } catch (error) {
      console.error('Logout error:', error);
      logout();
      navigation.getParent()?.replace('Login');
    }
  };

  const SettingItem = useCallback(({ icon, title, subtitle, onPress, color = colors.brand }) => (
    <TouchableOpacity style={styles.settingItem} onPress={onPress}>
      <View style={[styles.iconContainer, { backgroundColor: color + '20' }]}>
        <Ionicons name={icon} size={responsiveIconSize(19)} color={color} />
      </View>
      <View style={styles.settingInfo}>
        <Text style={styles.settingTitle}>{title}</Text>
        {subtitle && <Text style={styles.settingSubtitle}>{subtitle}</Text>}
      </View>
      <Ionicons name="chevron-forward" size={responsiveIconSize(18)} color={colors.muted} />
    </TouchableOpacity>
  ), []);

  return (
    <SafeAreaView style={styles.container} edges={['top', 'bottom']}>
      <View style={[getResponsiveContainerStyle(), styles.content]}>
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
        >
          <View style={styles.header}>
            <Text style={styles.headerTitle}>Ayarlar</Text>
          </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Hesap</Text>
        <View style={styles.profileCard}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {user?.full_name?.charAt(0)?.toUpperCase() || '?'}
            </Text>
          </View>
          <View style={styles.profileInfo}>
            <Text style={styles.profileName}>{user?.full_name || 'Kullanıcı'}</Text>
            <Text style={styles.profileEmail}>{user?.email || '—'}</Text>
            <Text style={styles.profileRole}>{user?.role || '—'}</Text>
          </View>
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Genel</Text>
        <SettingItem
          icon="person-outline"
          title="Profil Düzenle"
          onPress={() => Alert.alert('Bilgi', 'Profil düzenleme yakında eklenecek')}
        />
        <SettingItem
          icon="notifications-outline"
          title="Bildirimler"
          subtitle="Bildirim tercihlerini yönet"
          onPress={() => Alert.alert('Bilgi', 'Bildirim ayarları yakında eklenecek')}
        />
        <SettingItem
          icon="language-outline"
          title="Dil"
          subtitle="Türkçe"
          onPress={() => Alert.alert('Bilgi', 'Dil seçimi yakında eklenecek')}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Uygulama</Text>
        <SettingItem
          icon="information-circle-outline"
          title="Hakkında"
          subtitle="SCS360 v1.0.0"
          onPress={() => Alert.alert('SCS360', 'Okul Yönetim Sistemi Mobil Uygulaması\nVersiyon 1.0.0')}
        />
        <SettingItem
          icon="document-text-outline"
          title="Gizlilik Politikası"
          onPress={() => Alert.alert('Bilgi', 'Gizlilik politikası yakında eklenecek')}
        />
        <SettingItem
          icon="help-circle-outline"
          title="Yardım ve Destek"
          onPress={() => Alert.alert('Bilgi', 'Yardım ve destek yakında eklenecek')}
        />
      </View>

      <View style={styles.section}>
        <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
          <Ionicons name="log-out-outline" size={responsiveIconSize(19)} color={colors.danger} />
          <Text style={styles.logoutText}>Çıkış Yap</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.appFooter}>
        <Image
          source={require('../../assets/scs360_mark.png')}
          style={styles.footerLogo}
          resizeMode="contain"
        />
        <Text style={styles.footerBrand}>SCS360</Text>
        <Text style={styles.footerVersion}>Sürüm 1.0.0 (Build 1)</Text>
        <Text style={styles.footerCopyright}>© 2026 SCS360 Kurumsal Okul Yönetim Platformu</Text>
      </View>
    </ScrollView>
    </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  content: {
    paddingHorizontal: spacing.lg,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
  },
  header: {
    paddingVertical: spacing.xl,
  },
  headerTitle: {
    fontSize: fontSize.giant,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
  },
  section: {
    marginBottom: spacing.xxl,
  },
  sectionTitle: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.muted,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    paddingHorizontal: spacing.xxl,
    marginBottom: spacing.md,
  },
  profileCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    marginHorizontal: spacing.xxl,
    borderRadius: borderRadius.xxxl,
    padding: spacing.xl,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  avatar: {
    width: 38,
    height: 38,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.brand,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.md,
  },
  avatarText: {
    color: colors.surface,
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
  },
  profileInfo: {
    flex: 1,
  },
  profileName: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  profileEmail: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: spacing.sm,
  },
  profileRole: {
    fontSize: fontSize.xs,
    color: colors.text.light,
    marginTop: spacing.sm,
  },
  settingItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    marginHorizontal: spacing.xxl,
    marginBottom: spacing.md,
    borderRadius: borderRadius.xxxl,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  iconContainer: {
    width: 34,
    height: 34,
    borderRadius: borderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.md,
  },
  settingInfo: {
    flex: 1,
  },
  settingTitle: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  settingSubtitle: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: spacing.sm,
  },
  logoutButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface,
    marginHorizontal: spacing.xxl,
    borderRadius: borderRadius.xxxl,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  logoutText: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.danger,
    marginLeft: spacing.sm,
  },
  appFooter: {
    alignItems: 'center',
    paddingVertical: spacing.xl,
    gap: 4,
  },
  footerLogo: {
    width: 36,
    height: 36,
    borderRadius: 10,
    marginBottom: 4,
  },
  footerBrand: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  footerVersion: {
    fontSize: fontSize.xs,
    color: colors.muted,
  },
  footerCopyright: {
    fontSize: 10,
    color: colors.muted,
    marginTop: 2,
  },
});
