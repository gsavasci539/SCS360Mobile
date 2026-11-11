import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Modal,
  TextInput,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import api from '../config/api';
import { colors, spacing, borderRadius, fontSize, fontWeight, shadows } from '../config/theme';
import { getBottomPadding, getTabBarHeight, getResponsiveContainerStyle, responsiveIconSize, useResponsive } from '../utils/responsive';

export default function ClassesScreen({ navigation }) {
  const [classes, setClasses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [students, setStudents] = useState([]);
  const [availableStudents, setAvailableStudents] = useState([]);

  // Ensure students is always an array
  const safeStudents = Array.isArray(students) ? students : [];
  const safeAvailableStudents = Array.isArray(availableStudents) ? availableStudents : [];
  const [searchText, setSearchText] = useState('');
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
      console.error('Classes error:', error);
      setClasses([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchClassStudents = async (classItem) => {
    try {
      const response = await api.get(`/classes/${classItem.id}/students`);
      const data = response.data?.items || response.data || [];
      setStudents(Array.isArray(data) ? data : []);
      setAvailableStudents([]);
    } catch (error) {
      console.error('Class students error:', error);
      setStudents([]);
      setAvailableStudents([]);
    }
  };

  const openClassDetail = (classItem) => {
    setSelectedClass(classItem);
    fetchClassStudents(classItem);
    setModalVisible(true);
  };

  const assignStudent = async (student) => {
    try {
      await api.post(`/classes/${selectedClass.id}/students`, { student_id: student.id });
      setStudents([...safeStudents, student]);
      setAvailableStudents(safeAvailableStudents.filter(s => s.id !== student.id));
    } catch (error) {
      console.error('Assign student error:', error);
      Alert.alert('Hata', 'Öğrenci atanamadı');
    }
  };

  const removeStudent = async (student) => {
    try {
      await api.delete(`/classes/${selectedClass.id}/students/${student.id}`);
      setStudents(safeStudents.filter(s => s.id !== student.id));
      setAvailableStudents([...safeAvailableStudents, student]);
    } catch (error) {
      console.error('Remove student error:', error);
      Alert.alert('Hata', 'Öğrenci çıkarılamadı');
    }
  };

  const filteredAvailable = safeAvailableStudents.filter(s =>
    s.full_name.toLowerCase().includes(searchText.toLowerCase())
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
          <Text style={styles.headerTitle}>Sınıflar</Text>
          <Text style={styles.headerSubtitle}>{classes.length} sınıf</Text>
        </View>

        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
        >
          {classes.map((classItem) => (
            <TouchableOpacity
              key={classItem.id}
              style={styles.classCard}
              onPress={() => openClassDetail(classItem)}
            >
              <View style={styles.classHeader}>
                <View style={[styles.classIcon, { backgroundColor: '#e6f3ef' }]}>
                  <Ionicons name="people-outline" size={responsiveIconSize(24)} color={colors.brand} />
                </View>
                <View style={styles.classInfo}>
                  <Text style={styles.className}>{classItem.name}</Text>
                  <Text style={styles.classBranch}>{classItem.branch_name}</Text>
                </View>
                <Ionicons name="chevron-forward" size={responsiveIconSize(20)} color={colors.muted} />
              </View>
              <View style={styles.classStats}>
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>{classItem.student_count}</Text>
                  <Text style={styles.statLabel}>Öğrenci</Text>
                </View>
                <View style={styles.statItem}>
                  <Text style={styles.statValue}>{classItem.capacity || 30}</Text>
                  <Text style={styles.statLabel}>Kapasite</Text>
                </View>
              </View>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

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
            <Text style={styles.modalTitle}>{selectedClass?.name}</Text>
            <View style={{ width: 24 }} />
          </View>

          <View style={styles.modalContent}>
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Atanmış Öğrenciler ({safeStudents.length})</Text>
              {safeStudents.length === 0 ? (
                <Text style={styles.emptyText}>Henüz öğrenci atanmamış</Text>
              ) : (
                safeStudents.map((student) => (
                  <TouchableOpacity
                    key={student.id}
                    style={styles.studentItem}
                    onPress={() => removeStudent(student)}
                  >
                    <View style={styles.studentInfo}>
                      <Text style={styles.studentName}>{student.full_name}</Text>
                      <Text style={styles.studentEmail}>{student.email}</Text>
                    </View>
                    <Ionicons name="remove-circle-outline" size={24} color={colors.danger} />
                  </TouchableOpacity>
                ))
              )}
            </View>

            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Atanabilir Öğrenciler ({filteredAvailable.length})</Text>
              <TextInput
                style={styles.searchInput}
                placeholder="Öğrenci ara..."
                value={searchText}
                onChangeText={setSearchText}
              />
              {filteredAvailable.length === 0 ? (
                <Text style={styles.emptyText}>Atanabilir öğrenci yok</Text>
              ) : (
                filteredAvailable.map((student) => (
                  <TouchableOpacity
                    key={student.id}
                    style={styles.studentItem}
                    onPress={() => assignStudent(student)}
                  >
                    <View style={styles.studentInfo}>
                      <Text style={styles.studentName}>{student.full_name}</Text>
                      <Text style={styles.studentEmail}>{student.email}</Text>
                    </View>
                    <Ionicons name="add-circle-outline" size={24} color={colors.brand} />
                  </TouchableOpacity>
                ))
              )}
            </View>
          </View>
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
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
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
  content: {
    flex: 1,
    padding: spacing.lg,
  },
  classCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xxxl,
    padding: spacing.xl,
    marginBottom: spacing.md,
    ...shadows.small,
  },
  classHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  classIcon: {
    width: 48,
    height: 48,
    borderRadius: borderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.md,
  },
  classInfo: {
    flex: 1,
  },
  className: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  classBranch: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: 2,
  },
  classStats: {
    flexDirection: 'row',
    gap: spacing.xl,
  },
  statItem: {
    flex: 1,
  },
  statValue: {
    fontSize: fontSize.xl,
    fontWeight: fontWeight.extrabold,
    color: colors.brand,
  },
  statLabel: {
    fontSize: fontSize.sm,
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
  modalContent: {
    flex: 1,
    padding: spacing.xl,
  },
  section: {
    marginBottom: spacing.xl,
  },
  sectionTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    marginBottom: spacing.md,
  },
  searchInput: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.md,
    fontSize: fontSize.md,
    ...shadows.small,
  },
  studentItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.sm,
    ...shadows.small,
  },
  studentInfo: {
    flex: 1,
  },
  studentName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  studentEmail: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: 2,
  },
  emptyText: {
    fontSize: fontSize.md,
    color: colors.muted,
    textAlign: 'center',
    padding: spacing.xl,
  },
});
