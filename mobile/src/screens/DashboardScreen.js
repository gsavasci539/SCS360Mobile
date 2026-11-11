import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  FlatList,
  Dimensions,
  RefreshControl,
  Image,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import api, { formatApiError } from '../config/api';
import useAuthStore from '../store/authStore';
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

export default function DashboardScreen({ navigation }) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const user = useAuthStore((state) => state.user);
  const { width, columnCount, tabBarHeight, deviceType } = useResponsive();
  const cardWidth = getCardWidth(columnCount, spacing.lg);

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  const fetchDashboardStats = async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else if (!stats) setLoading(true);
    setError(null);
    try {
      const response = await api.get('/dashboard/summary');
      const payload = response.data?.data || response.data;
      setStats(payload);
    } catch (err) {
      console.error('Dashboard stats error:', err);
      setError(formatApiError(err));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const StatCard = useCallback(({ icon, title, value, color, statBg, statInk, onPress }) => (
    <TouchableOpacity 
      style={[styles.statCard, { width: cardWidth }]} 
      onPress={onPress}
    >
      <View style={[styles.statIcon, { backgroundColor: statBg }]}>
        <Ionicons name={icon} size={responsiveIconSize(19)} color={statInk} />
      </View>
      <View style={styles.statContent}>
        <Text style={styles.statLabel}>{title}</Text>
        <Text style={styles.statValue}>{value || '—'}</Text>
      </View>
    </TouchableOpacity>
  ), []);

  if (loading) {
    return (
      <SafeAreaView style={styles.loadingContainer} edges={['top', 'bottom']}>
        <ActivityIndicator size="large" color={colors.brand} />
      </SafeAreaView>
    );
  }

  const statCards = useMemo(() => [
    {
      icon: 'people-outline',
      title: 'Toplam Öğrenci',
      value: stats?.total_students,
      statBg: '#e6f3ef',
      statInk: colors.brand,
      onPress: () => navigation.navigate('Students'),
    },
    {
      icon: 'cash-outline',
      title: 'Toplam Ödeme',
      value: stats?.total_payments,
      statBg: '#e8f5f0',
      statInk: colors.success,
      onPress: () => navigation.navigate('Finance'),
    },
    {
      icon: 'calendar-outline',
      title: 'Bugün Yoklama',
      value: stats?.today_attendance,
      statBg: '#fff3df',
      statInk: colors.warning,
      onPress: () => navigation.navigate('Attendance'),
    },
    {
      icon: 'notifications-outline',
      title: 'Bildirimler',
      value: stats?.notifications,
      statBg: '#fff0ef',
      statInk: colors.danger,
      onPress: () => navigation.navigate('Communications'),
    },
  ], [stats]);

  const quickActions = useMemo(() => [
    {
      icon: 'person-add-outline',
      title: 'Öğrenci Ekle',
      subtitle: 'Yeni öğrenci kaydı',
      bg: '#e6f3ef',
      color: colors.brand,
      onPress: () => navigation.navigate('Students'),
    },
    {
      icon: 'checkmark-circle-outline',
      title: 'Yoklama Al',
      subtitle: 'Sınıf yoklaması',
      bg: '#e8f5f0',
      color: colors.success,
      onPress: () => navigation.navigate('Attendance'),
    },
    {
      icon: 'card-outline',
      title: 'Ödeme Al',
      subtitle: 'Ödeme kaydı',
      bg: '#fff3df',
      color: colors.warning,
      onPress: () => navigation.navigate('Finance'),
    },
    {
      icon: 'school-outline',
      title: 'Sınıflar',
      subtitle: 'Sınıf yönetimi',
      bg: '#e8f4fd',
      color: '#2563eb',
      onPress: () => navigation.navigate('Classes'),
    },
    {
      icon: 'chatbubbles-outline',
      title: 'İletişim',
      subtitle: 'Duyuru ve bildirimler',
      bg: '#fef3c7',
      color: '#d97706',
      onPress: () => navigation.navigate('Communications'),
    },
    {
      icon: 'bar-chart-outline',
      title: 'Raporlar',
      subtitle: 'Detaylı raporlar',
      bg: '#fff0ef',
      color: colors.danger,
      onPress: () => navigation.navigate('Reports'),
    },
    {
      icon: 'library-outline',
      title: 'Kaynaklar',
      subtitle: 'Şube ve kullanıcılar',
      bg: '#f3e8ff',
      color: '#9333ea',
      onPress: () => navigation.navigate('Resources'),
    },
    {
      icon: 'shield-checkmark-outline',
      title: 'Roller',
      subtitle: 'Rol ve yetki yönetimi',
      bg: '#ecfdf5',
      color: '#059669',
      onPress: () => navigation.navigate('Roles'),
    },
    {
      icon: 'person-circle-outline',
      title: 'Kullanıcılar',
      subtitle: 'Kullanıcı yönetimi',
      bg: '#fff1f2',
      color: '#dc2626',
      onPress: () => navigation.navigate('Users'),
    },
  ], []);

  const bottomPadding = getBottomPadding(tabBarHeight);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView 
        style={styles.scrollView}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => fetchDashboardStats(true)}
            colors={[colors.brand]}
            tintColor={colors.brand}
          />
        }
      >
        <View style={[getResponsiveContainerStyle(), styles.content]}>
          <View style={styles.header}>
            <View style={styles.headerBrandRow}>
              <View style={styles.welcomeBlock}>
                <Text style={styles.welcomeText}>Hoş Geldiniz,</Text>
                <Text style={styles.userName}>{user?.full_name || 'Kullanıcı'}</Text>
              </View>
              <Image
                source={require('../../assets/scs360_mark.png')}
                style={styles.headerLogo}
                resizeMode="contain"
              />
            </View>
          </View>

          {error ? (
            <View style={styles.errorBanner}>
              <Ionicons name="alert-circle-outline" size={20} color={colors.danger} />
              <Text style={styles.errorText}>{error}</Text>
              <TouchableOpacity onPress={() => fetchDashboardStats(false)} style={styles.retryButton}>
                <Text style={styles.retryText}>Tekrar Dene</Text>
              </TouchableOpacity>
            </View>
          ) : null}

          <FlatList
            data={statCards}
            renderItem={({ item }) => (
              <StatCard
                icon={item.icon}
                title={item.title}
                value={item.value}
                statBg={item.statBg}
                statInk={item.statInk}
                onPress={item.onPress}
              />
            )}
            keyExtractor={(item, index) => index.toString()}
            numColumns={columnCount}
            scrollEnabled={false}
            columnWrapperStyle={styles.statsGrid}
            contentContainerStyle={styles.statsContainer}
          />

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Hızlı İşlemler</Text>
            <View style={styles.quickList}>
              {quickActions.map((action, index) => (
                <TouchableOpacity
                  key={index}
                  style={styles.quickItem}
                  onPress={action.onPress}
                >
                  <View style={[styles.quickIcon, { backgroundColor: action.bg }]}>
                    <Ionicons name={action.icon} size={responsiveIconSize(17)} color={action.color} />
                  </View>
                  <View style={styles.quickCopy}>
                    <Text style={styles.quickCopyTitle}>{action.title}</Text>
                    <Text style={styles.quickCopySubtitle}>{action.subtitle}</Text>
                  </View>
                  <Ionicons name="chevron-forward" size={responsiveIconSize(18)} color={colors.muted} />
                </TouchableOpacity>
              ))}
            </View>
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
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    paddingVertical: spacing.xl,
  },
  headerBrandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  welcomeBlock: {
    flex: 1,
  },
  headerLogo: {
    width: 44,
    height: 44,
    borderRadius: 12,
  },
  welcomeText: {
    fontSize: fontSize.lg,
    color: colors.muted,
  },
  userName: {
    fontSize: fontSize.giant,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
    marginTop: 4,
  },
  statsContainer: {
    paddingVertical: spacing.lg,
  },
  statsGrid: {
    gap: spacing.lg,
  },
  statCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xxxl,
    padding: spacing.lg,
    minHeight: moderateVerticalScale(120),
    justifyContent: 'center',
    ...shadows.small,
  },
  statIcon: {
    width: moderateScale(36),
    height: moderateScale(36),
    borderRadius: borderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  statContent: {
    flex: 1,
  },
  statLabel: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.muted,
    lineHeight: moderateVerticalScale(16),
  },
  statValue: {
    fontSize: fontSize.giant,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
    marginTop: spacing.xs,
    lineHeight: moderateVerticalScale(32),
  },
  section: {
    paddingVertical: spacing.xl,
  },
  sectionTitle: {
    fontSize: fontSize.xxxl,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    marginBottom: spacing.lg,
  },
  quickList: {
    gap: spacing.sm,
    padding: 0,
  },
  quickItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.lg,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.surface,
    minHeight: moderateVerticalScale(80),
    ...shadows.small,
  },
  quickIcon: {
    width: moderateScale(36),
    height: moderateScale(36),
    borderRadius: borderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
  },
  quickCopy: {
    flex: 1,
  },
  quickCopyTitle: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  quickCopySubtitle: {
    marginTop: 3,
    color: colors.muted,
    fontSize: fontSize.sm,
  },
  errorBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff0ef',
    borderRadius: borderRadius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
    gap: spacing.sm,
    borderWidth: 1,
    borderColor: '#fecaca',
  },
  errorText: {
    flex: 1,
    fontSize: fontSize.sm,
    color: colors.danger,
    fontWeight: fontWeight.medium,
  },
  retryButton: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    backgroundColor: colors.danger,
    borderRadius: borderRadius.sm,
  },
  retryText: {
    color: '#ffffff',
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
  },
});
