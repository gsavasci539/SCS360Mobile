import React, { useCallback, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, spacing, borderRadius, fontSize, fontWeight, shadows } from '../config/theme';
import { moderateScale, moderateVerticalScale } from 'react-native-size-matters';
import {
  getColumnCount,
  getCardWidth,
  getBottomPadding,
  getTabBarHeight,
  getResponsiveContainerStyle,
  responsiveIconSize,
  useResponsive,
} from '../utils/responsive';

export default function MoreScreen({ navigation }) {
  const { columnCount, tabBarHeight, deviceType } = useResponsive();
  const cardWidth = getCardWidth(columnCount, spacing.lg);
  const bottomPadding = getBottomPadding(tabBarHeight);

  const menuItems = useMemo(() => [
    {
      icon: 'calendar-outline',
      title: 'Yoklama',
      subtitle: 'Sınıf yoklaması',
      bg: '#e8f5f0',
      color: colors.success,
      screen: 'Attendance',
    },
    {
      icon: 'school-outline',
      title: 'Sınıflar',
      subtitle: 'Sınıf yönetimi',
      bg: '#e8f4fd',
      color: '#2563eb',
      screen: 'Classes',
    },
    {
      icon: 'chatbubbles-outline',
      title: 'İletişim',
      subtitle: 'Duyuru ve bildirimler',
      bg: '#fef3c7',
      color: '#d97706',
      screen: 'Communications',
    },
    {
      icon: 'bar-chart-outline',
      title: 'Raporlar',
      subtitle: 'Detaylı raporlar',
      bg: '#fff0ef',
      color: colors.danger,
      screen: 'Reports',
    },
    {
      icon: 'library-outline',
      title: 'Kaynaklar',
      subtitle: 'Şube ve kullanıcılar',
      bg: '#f3e8ff',
      color: '#9333ea',
      screen: 'Resources',
    },
    {
      icon: 'shield-checkmark-outline',
      title: 'Roller',
      subtitle: 'Rol ve yetki yönetimi',
      bg: '#ecfdf5',
      color: '#059669',
      screen: 'Roles',
    },
    {
      icon: 'person-circle-outline',
      title: 'Kullanıcılar',
      subtitle: 'Kullanıcı yönetimi',
      bg: '#fff1f2',
      color: '#dc2626',
      screen: 'Users',
    },
  ], []);

  const MenuItem = useCallback(({ item }) => (
    <TouchableOpacity
      style={[styles.menuItem, { width: cardWidth }]}
      onPress={() => navigation.navigate(item.screen)}
    >
      <View style={[styles.menuIcon, { backgroundColor: item.bg }]}>
        <Ionicons name={item.icon} size={responsiveIconSize(24)} color={item.color} />
      </View>
      <Text style={styles.menuTitle}>{item.title}</Text>
      <Text style={styles.menuSubtitle}>{item.subtitle}</Text>
    </TouchableOpacity>
  ), [navigation, cardWidth]);

  return (
    <SafeAreaView style={styles.container} edges={['top', 'bottom']}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
        showsVerticalScrollIndicator={false}
      >
        <View style={[getResponsiveContainerStyle(), styles.content]}>
          <View style={styles.header}>
            <Text style={styles.headerTitle}>Diğer</Text>
            <Text style={styles.headerSubtitle}>Tüm özelliklere erişin</Text>
          </View>

          <View style={styles.menuGrid}>
            {menuItems.map((item, index) => (
              <MenuItem key={index} item={item} />
            ))}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
  },
  content: {
    paddingHorizontal: spacing.lg,
  },
  header: {
    paddingVertical: spacing.xl,
  },
  headerTitle: {
    fontSize: fontSize.giant,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
  },
  headerSubtitle: {
    fontSize: fontSize.md,
    color: colors.muted,
    marginTop: 4,
  },
  menuGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.lg,
    paddingBottom: spacing.xl,
    justifyContent: 'flex-start',
  },
  menuItem: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xl,
    padding: spacing.lg,
    alignItems: 'center',
    minHeight: moderateVerticalScale(120),
    justifyContent: 'center',
    ...shadows.small,
  },
  menuIcon: {
    width: moderateScale(44),
    height: moderateScale(44),
    borderRadius: borderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  menuTitle: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    textAlign: 'center',
    lineHeight: moderateVerticalScale(20),
  },
  menuSubtitle: {
    fontSize: fontSize.sm,
    color: colors.muted,
    textAlign: 'center',
    marginTop: spacing.xs,
    lineHeight: moderateVerticalScale(16),
  },
});
