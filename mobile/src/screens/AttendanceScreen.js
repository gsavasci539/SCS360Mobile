import React, { useEffect, useState, memo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Modal,
  ScrollView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import api from '../config/api';
import { colors, spacing, borderRadius, fontSize, fontWeight, shadows } from '../config/theme';
import { getBottomPadding, getTabBarHeight, getResponsiveContainerStyle, responsiveIconSize, useResponsive } from '../utils/responsive';

export default function AttendanceScreen({ navigation }) {
  const [classes, setClasses] = useState([]);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [students, setStudents] = useState([]);
  const [attendance, setAttendance] = useState({});
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchClasses();
  }, []);

  const fetchClasses = async () => {
    try {
      const response = await api.get('/classes');
      const data = response.data?.items || response.data || [];
      setClasses(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Classes fetch error:', error);
      setClasses([]);
    } finally {
      setLoading(false);
    }
  };

  const handleTakeAttendance = async (classItem) => {
    setSelectedClass(classItem);
    setModalVisible(true);
    try {
      const response = await api.get(`/classes/${classItem.id}/students`);
      setStudents(response.data || []);
      const initialAttendance = {};
      (response.data || []).forEach(student => {
        initialAttendance[student.id] = 'present';
      });
      setAttendance(initialAttendance);
    } catch (error) {
      console.error('Fetch students error:', error);
      setStudents([]);
    }
  };

  const saveAttendance = async () => {
    try {
      const attendanceRecords = Object.entries(attendance).map(([studentId, status]) => ({
        student_id: parseInt(studentId),
        status,
        date: selectedDate,
        class_id: selectedClass.id,
      }));
      await api.post('/attendance/bulk', { records: attendanceRecords });
      setModalVisible(false);
      Alert.alert('Başarılı', 'Yoklama kaydedildi');
    } catch (error) {
      console.error('Save attendance error:', error);
      Alert.alert('Hata', 'Yoklama kaydedilemedi');
    }
  };

  const renderClass = ({ item }) => (
    <TouchableOpacity
      style={styles.classCard}
      onPress={() => handleTakeAttendance(item)}
    >
      <View style={styles.classInfo}>
        <View style={[styles.classIcon, { backgroundColor: colors.brand }]}>
          <Ionicons name="people" size={responsiveIconSize(19)} color={colors.surface} />
        </View>
        <View style={styles.classDetails}>
          <Text style={styles.className}>{item.name || 'İsimsiz Sınıf'}</Text>
          <Text style={styles.classMeta}>
            {item.student_count || 0} öğrenci • {item.branch_name || '—'}
          </Text>
        </View>
      </View>
      <Ionicons name="chevron-forward" size={responsiveIconSize(18)} color={colors.muted} />
    </TouchableOpacity>
  );

  if (loading) {
    return (
      <SafeAreaView style={styles.loadingContainer} edges={['top', 'bottom']}>
        <ActivityIndicator size="large" color={colors.brand} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={['top', 'bottom']}>
      <View style={[getResponsiveContainerStyle(), styles.content]}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Yoklama</Text>
          <TouchableOpacity
            style={styles.calendarButton}
            onPress={() => Alert.alert('Tarih Seç', 'Tarih seçici yakında eklenecek')}
          >
            <Ionicons name="calendar" size={responsiveIconSize(17)} color={colors.brand} />
            <Text style={styles.dateText}>{selectedDate}</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.summaryContainer}>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryValue}>0</Text>
          <Text style={styles.summaryLabel}>Bugün Alınan</Text>
        </View>
        <View style={[styles.summaryCard, styles.summaryCardMiddle]}>
          <Text style={styles.summaryValue}>0</Text>
          <Text style={styles.summaryLabel}>Bekleyen</Text>
        </View>
        <View style={styles.summaryCard}>
          <Text style={styles.summaryValue}>{classes.length}</Text>
          <Text style={styles.summaryLabel}>Toplam Sınıf</Text>
        </View>
      </View>

      <FlatList
        data={classes}
        renderItem={renderClass}
        keyExtractor={(item) => item.id?.toString()}
        contentContainerStyle={[styles.list, { paddingBottom: bottomPadding }]}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Ionicons name="school-outline" size={responsiveIconSize(52)} color={colors.muted} style={styles.emptyIcon} />
            <Text style={styles.emptyText}>Sınıf bulunamadı</Text>
          </View>
        }
      />
      </View>

      {/* Attendance Taking Modal */}
      <Modal
        visible={modalVisible}
        animationType="slide"
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setModalVisible(false)}>
              <Ionicons name="close" size={24} color={colors.ink} />
            </TouchableOpacity>
            <Text style={styles.modalTitle}>Yoklama Al</Text>
            <TouchableOpacity onPress={saveAttendance}>
              <Text style={styles.modalSave}>Kaydet</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.classInfoHeader}>
              <Text style={styles.modalClassName}>{selectedClass?.name || 'Sınıf'}</Text>
              <Text style={styles.classDate}>{selectedDate}</Text>
            </View>

            <View style={styles.attendanceSummary}>
              <View style={styles.summaryItem}>
                <Text style={styles.summaryCount}>{Object.values(attendance).filter(s => s === 'present').length}</Text>
                <Text style={styles.summaryLabel}>Mevcut</Text>
              </View>
              <View style={styles.summaryItem}>
                <Text style={styles.summaryCount}>{Object.values(attendance).filter(s => s === 'absent').length}</Text>
                <Text style={styles.summaryLabel}>Yok</Text>
              </View>
              <View style={styles.summaryItem}>
                <Text style={styles.summaryCount}>{Object.values(attendance).filter(s => s === 'late').length}</Text>
                <Text style={styles.summaryLabel}>Geç</Text>
              </View>
            </View>

            {students.length === 0 ? (
              <Text style={styles.emptyText}>Bu sınıfta öğrenci yok</Text>
            ) : (
              students.map((student) => (
                <View key={student.id} style={styles.studentRow}>
                  <View style={styles.studentRowInfo}>
                    <Text style={styles.studentRowName}>{student.full_name}</Text>
                    <Text style={styles.studentRowNumber}>{student.student_number || '—'}</Text>
                  </View>
                  <View style={styles.attendanceButtons}>
                    <TouchableOpacity
                      style={[styles.attendanceButton, attendance[student.id] === 'present' && styles.attendanceButtonActive]}
                      onPress={() => setAttendance({ ...attendance, [student.id]: 'present' })}
                    >
                      <Ionicons name="checkmark" size={18} color={attendance[student.id] === 'present' ? colors.surface : colors.brand} />
                    </TouchableOpacity>
                    <TouchableOpacity
                      style={[styles.attendanceButton, attendance[student.id] === 'absent' && styles.attendanceButtonActiveAbsent]}
                      onPress={() => setAttendance({ ...attendance, [student.id]: 'absent' })}
                    >
                      <Ionicons name="close" size={18} color={attendance[student.id] === 'absent' ? colors.surface : colors.danger} />
                    </TouchableOpacity>
                    <TouchableOpacity
                      style={[styles.attendanceButton, attendance[student.id] === 'late' && styles.attendanceButtonActiveLate]}
                      onPress={() => setAttendance({ ...attendance, [student.id]: 'late' })}
                    >
                      <Ionicons name="time" size={18} color={attendance[student.id] === 'late' ? colors.surface : colors.warning} />
                    </TouchableOpacity>
                  </View>
                </View>
              ))
            )}
          </ScrollView>
        </View>
      </Modal>
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
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.xl,
  },
  headerTitle: {
    fontSize: fontSize.giant,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
  },
  calendarButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
    borderColor: colors.line,
  },
  dateText: {
    marginLeft: spacing.sm,
    fontSize: fontSize.md,
    color: colors.ink,
    fontWeight: fontWeight.medium,
  },
  summaryContainer: {
    flexDirection: 'row',
    paddingHorizontal: spacing.xxl,
    marginBottom: spacing.xxl,
    gap: spacing.md,
  },
  summaryCard: {
    flex: 1,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xxxl,
    padding: spacing.lg,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  summaryCardMiddle: {
    marginHorizontal: spacing.sm,
  },
  summaryValue: {
    fontSize: fontSize.massive,
    fontWeight: fontWeight.extrabold,
    color: colors.ink,
  },
  summaryLabel: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.muted,
    marginTop: spacing.sm,
  },
  list: {
    padding: spacing.xxl,
    gap: spacing.sm,
  },
  classCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xxxl,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  classInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  classIcon: {
    width: 38,
    height: 38,
    borderRadius: borderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.md,
  },
  classDetails: {
    flex: 1,
  },
  className: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  classMeta: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: 3,
  },
  emptyContainer: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  emptyIcon: {
    marginBottom: spacing.md,
  },
  emptyText: {
    fontSize: fontSize.md,
    color: colors.muted,
  },
  modalContainer: {
    flex: 1,
    backgroundColor: colors.canvas,
    paddingBottom: 70,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.xl,
    paddingTop: 60,
    backgroundColor: colors.surface,
    ...shadows.small,
  },
  modalTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  modalSave: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.brand,
  },
  modalContent: {
    flex: 1,
    padding: spacing.xl,
  },
  classInfoHeader: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    marginBottom: spacing.lg,
    ...shadows.small,
  },
  className: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  classDate: {
    fontSize: fontSize.md,
    color: colors.muted,
    marginTop: spacing.xs,
  },
  modalClassName: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  attendanceSummary: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    marginBottom: spacing.lg,
    ...shadows.small,
  },
  summaryItem: {
    flex: 1,
    alignItems: 'center',
  },
  summaryCount: {
    fontSize: fontSize.giant,
    fontWeight: fontWeight.extrabold,
    color: colors.brand,
  },
  summaryLabel: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: spacing.xs,
  },
  studentRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    marginBottom: spacing.sm,
    ...shadows.small,
  },
  studentRowInfo: {
    flex: 1,
  },
  studentRowName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  studentRowNumber: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: 2,
  },
  attendanceButtons: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  attendanceButton: {
    width: 40,
    height: 40,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.canvas,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.line,
  },
  attendanceButtonActive: {
    backgroundColor: colors.brand,
    borderColor: colors.brand,
  },
  attendanceButtonActiveAbsent: {
    backgroundColor: colors.danger,
    borderColor: colors.danger,
  },
  attendanceButtonActiveLate: {
    backgroundColor: colors.warning,
    borderColor: colors.warning,
  },
});
