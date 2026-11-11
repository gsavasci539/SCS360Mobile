import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import api from '../config/api';
import { colors, spacing, borderRadius, fontSize, fontWeight, shadows } from '../config/theme';
import { getBottomPadding, getTabBarHeight, getResponsiveContainerStyle, responsiveIconSize, useResponsive } from '../utils/responsive';

const reportTabs = [
  { key: 'monthly-payments', label: 'Aylık Tahsilat' },
  { key: 'debts', label: 'Borçlar' },
  { key: 'overdue-debts', label: 'Geciken Borçlar' },
  { key: 'attendance', label: 'Devamsızlık' },
  { key: 'class-occupancy', label: 'Sınıf Doluluğu' },
  { key: 'branch-performance', label: 'Şube Performansı' },
];

export default function ReportsScreen({ navigation }) {
  const [activeTab, setActiveTab] = useState(reportTabs[0].key);
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [branches, setBranches] = useState([]);
  const [selectedBranch, setSelectedBranch] = useState('');
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchReportData();
    fetchBranches();
  }, [activeTab, selectedBranch]);

  const fetchReportData = async () => {
    setLoading(true);
    try {
      const response = await api.get(`/reports/${activeTab}`, {
        params: {
          year: new Date().getFullYear(),
          month: new Date().getMonth() + 1,
          branch_id: selectedBranch || undefined,
        },
      });
      setReportData(response.data || null);
    } catch (error) {
      console.error('Report data error:', error);
      setReportData(null);
    } finally {
      setLoading(false);
    }
  };

  const fetchBranches = async () => {
    try {
      const response = await api.get('/branches');
      const data = response.data?.items || response.data || [];
      setBranches(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Branches error:', error);
      setBranches([]);
    }
  };

  const exportReport = async (format) => {
    try {
      await api.get(`/reports/export/${format}`, {
        params: {
          report_type: activeTab,
          year: new Date().getFullYear(),
          month: new Date().getMonth() + 1,
          branch_id: selectedBranch || undefined,
        },
        responseType: 'blob',
      });
      alert(`${format.toUpperCase()} raporu indirildi`);
    } catch (error) {
      console.error('Export error:', error);
      alert('İndirme başarısız');
    }
  };

  const renderReportContent = () => {
    if (loading) {
      return (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.brand} />
        </View>
      );
    }

    if (!reportData) {
      return <Text style={styles.emptyText}>Rapor verisi bulunamadı</Text>;
    }

    switch (activeTab) {
      case 'monthly-payments':
        return renderMonthlyPayments();
      case 'debts':
        return renderDebts();
      case 'overdue-debts':
        return renderOverdueDebts();
      case 'attendance':
        return renderAttendance();
      case 'class-occupancy':
        return renderClassOccupancy();
      case 'branch-performance':
        return renderBranchPerformance();
      default:
        return <Text style={styles.emptyText}>Rapor yükleniyor...</Text>;
    }
  };

  const renderMonthlyPayments = memo(() => (
    <View style={styles.reportContent}>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Toplam Tahsilat</Text>
        <Text style={styles.summaryValue}>₺{reportData.total?.toLocaleString() || '0'}</Text>
      </View>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>İşlem Sayısı</Text>
        <Text style={styles.summaryValue}>{reportData.count || 0}</Text>
      </View>
    </View>
  ));

  const renderDebts = memo(() => (
    <View style={styles.reportContent}>
      {reportData.by_status ? Object.entries(reportData.by_status).map(([status, count]) => (
        <View key={status} style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>{status}</Text>
          <Text style={styles.summaryValue}>{count}</Text>
        </View>
      )) : (
        <Text style={styles.emptyText}>Borç verisi yok</Text>
      )}
    </View>
  ));

  const renderOverdueDebts = memo(() => (
    <View style={styles.reportContent}>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Geciken Toplam</Text>
        <Text style={styles.summaryValue}>₺{reportData.total_overdue?.toLocaleString() || '0'}</Text>
      </View>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Öğrenci Sayısı</Text>
        <Text style={styles.summaryValue}>{reportData.student_count || 0}</Text>
      </View>
    </View>
  ));

  const renderAttendance = memo(() => (
    <View style={styles.reportContent}>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Katılım Oranı</Text>
        <Text style={styles.summaryValue}>{reportData.attendance_rate || 0}%</Text>
      </View>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Devamsızlık</Text>
        <Text style={styles.summaryValue}>{reportData.absent_count || 0}</Text>
      </View>
    </View>
  ));

  const renderClassOccupancy = memo(() => (
    <View style={styles.reportContent}>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Ortalama Doluluk</Text>
        <Text style={styles.summaryValue}>{reportData.avg_occupancy || 0}%</Text>
      </View>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Toplam Sınıf</Text>
        <Text style={styles.summaryValue}>{reportData.total_classes || 0}</Text>
      </View>
    </View>
  ));

  const renderBranchPerformance = memo(() => (
    <View style={styles.reportContent}>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Toplam Gelir</Text>
        <Text style={styles.summaryValue}>₺{reportData.total_revenue?.toLocaleString() || '0'}</Text>
      </View>
      <View style={styles.summaryCard}>
        <Text style={styles.summaryLabel}>Yeni Kayıt</Text>
        <Text style={styles.summaryValue}>{reportData.new_students || 0}</Text>
      </View>
    </View>
  ));

  return (
    <SafeAreaView style={styles.container} edges={['top', 'bottom']}>
      <View style={[getResponsiveContainerStyle(), styles.content]}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Raporlar</Text>
          <Text style={styles.headerSubtitle}>Kurum performans analizi</Text>
        </View>

        <View style={styles.actionBar}>
          <TouchableOpacity style={styles.actionButton} onPress={() => exportReport('csv')}>
            <Ionicons name="download-outline" size={responsiveIconSize(18)} color={colors.ink} />
            <Text style={styles.actionButtonText}>CSV</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.actionButton} onPress={() => exportReport('excel')}>
            <Ionicons name="grid-outline" size={responsiveIconSize(18)} color={colors.ink} />
            <Text style={styles.actionButtonText}>Excel</Text>
          </TouchableOpacity>
        </View>

        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabsContainer}>
          {reportTabs.map((tab) => (
            <TouchableOpacity
              key={tab.key}
              style={[styles.tab, activeTab === tab.key && styles.activeTab]}
              onPress={() => setActiveTab(tab.key)}
            >
              <Text style={[styles.tabText, activeTab === tab.key && styles.activeTabText]}>
                {tab.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <View style={styles.filterSection}>
          <Text style={styles.filterLabel}>Şube:</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
            <TouchableOpacity
              style={[styles.filterChip, selectedBranch === '' && styles.selectedFilter]}
              onPress={() => setSelectedBranch('')}
            >
              <Text style={[styles.filterChipText, selectedBranch === '' && styles.selectedFilterText]}>
                Tüm Şubeler
              </Text>
            </TouchableOpacity>
            {branches.map((branch) => (
              <TouchableOpacity
                key={branch.id}
                style={[styles.filterChip, selectedBranch === String(branch.id) && styles.selectedFilter]}
                onPress={() => setSelectedBranch(String(branch.id))}
              >
              <Text style={[styles.filterChipText, selectedBranch === String(branch.id) && styles.selectedFilterText]}>
                {branch.name}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      <ScrollView 
        style={styles.scrollView}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
      >
        {renderReportContent()}
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
    backgroundColor: colors.surface,
    ...shadows.small,
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
  actionBar: {
    flexDirection: 'row',
    gap: spacing.sm,
    padding: spacing.lg,
    backgroundColor: colors.surface,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.canvas,
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    paddingHorizontal: spacing.md,
    gap: spacing.xs,
  },
  actionButtonText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  tabsContainer: {
    backgroundColor: colors.surface,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
  },
  tab: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.lg,
    marginRight: spacing.sm,
    backgroundColor: colors.canvas,
  },
  activeTab: {
    backgroundColor: colors.brand,
  },
  tabText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.muted,
  },
  activeTabText: {
    color: colors.surface,
  },
  filterSection: {
    padding: spacing.lg,
    backgroundColor: colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
  },
  filterLabel: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.muted,
    marginBottom: spacing.sm,
  },
  filterScroll: {
    flexDirection: 'row',
  },
  filterChip: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.canvas,
    marginRight: spacing.sm,
    borderWidth: 1,
    borderColor: colors.line,
  },
  selectedFilter: {
    backgroundColor: colors.brand,
    borderColor: colors.brand,
  },
  filterChipText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  selectedFilterText: {
    color: colors.surface,
  },
  loadingContainer: {
    padding: spacing.xl,
    alignItems: 'center',
  },
  reportContent: {
    gap: spacing.md,
  },
  summaryCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.xl,
    ...shadows.small,
  },
  summaryLabel: {
    fontSize: fontSize.md,
    color: colors.muted,
    marginBottom: spacing.xs,
  },
  summaryValue: {
    fontSize: fontSize.giant,
    fontWeight: fontWeight.extrabold,
    color: colors.brand,
  },
  emptyText: {
    fontSize: fontSize.md,
    color: colors.muted,
    textAlign: 'center',
    padding: spacing.xl,
  },
});
