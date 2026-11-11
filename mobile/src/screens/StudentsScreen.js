import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  TextInput,
  ActivityIndicator,
  Modal,
  ScrollView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import api from '../config/api';
import { colors, spacing, borderRadius, fontSize, fontWeight, shadows } from '../config/theme';
import { getBottomPadding, getTabBarHeight, getResponsiveContainerStyle, responsiveIconSize, useResponsive } from '../utils/responsive';

export default function StudentsScreen({ navigation }) {
  const [students, setStudents] = useState([]);
  const [filteredStudents, setFilteredStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchText, setSearchText] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [editingStudent, setEditingStudent] = useState(null);
  const [formData, setFormData] = useState({
    full_name: '',
    email: '',
    phone: '',
    class_name: '',
    student_number: '',
  });
  const [classes, setClasses] = useState([]);
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchStudents();
    fetchClasses();
  }, []);

  useEffect(() => {
    if (searchText) {
      const filtered = students.filter(
        (student) =>
          student.full_name?.toLowerCase().includes(searchText.toLowerCase()) ||
          student.email?.toLowerCase().includes(searchText.toLowerCase())
      );
      setFilteredStudents(filtered);
    } else {
      setFilteredStudents(students);
    }
  }, [searchText, students]);

  const fetchStudents = async () => {
    try {
      const response = await api.get('/students');
      console.log('Students response:', response.data);
      const data = response.data?.items || response.data || [];
      setStudents(Array.isArray(data) ? data : []);
      setFilteredStudents(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Students fetch error:', error.response?.data || error.message);
      console.error('Full error:', error);
      setStudents([]);
      setFilteredStudents([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchClasses = async () => {
    try {
      const response = await api.get('/classes');
      console.log('Classes response:', response.data);
      const data = response.data?.items || response.data || [];
      setClasses(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Classes fetch error:', error.response?.data || error.message);
      setClasses([]);
    }
  };

  const saveStudent = async () => {
    try {
      if (editingStudent) {
        await api.put(`/students/${editingStudent.id}`, formData);
      } else {
        await api.post('/students', formData);
      }
      setModalVisible(false);
      fetchStudents();
    } catch (error) {
      console.error('Save student error:', error);
      alert('Kaydedilemedi');
    }
  };

  const deleteStudent = async () => {
    try {
      await api.delete(`/students/${editingStudent.id}`);
      setModalVisible(false);
      fetchStudents();
    } catch (error) {
      console.error('Delete student error:', error);
      alert('Silinemedi');
    }
  };

  const renderStudent = ({ item }) => (
    <TouchableOpacity
      style={styles.studentCard}
      onPress={() => {
        setEditingStudent(item);
        setFormData({
          full_name: item.full_name || '',
          email: item.email || '',
          phone: item.phone || '',
          class_name: item.class_name || '',
          student_number: item.student_number || '',
        });
        setModalVisible(true);
      }}
    >
      <View style={styles.studentInfo}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {item.full_name?.charAt(0)?.toUpperCase() || '?'}
          </Text>
        </View>
        <View style={styles.studentDetails}>
          <Text style={styles.studentName}>{item.full_name || 'İsimsiz'}</Text>
          <Text style={styles.studentEmail}>{item.email || '—'}</Text>
          <Text style={styles.studentClass}>
            {item.class_name ? `Sınıf: ${item.class_name}` : 'Sınıf atanmamış'}
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
          <Text style={styles.headerTitle}>Öğrenciler</Text>
          <TouchableOpacity
            style={styles.addButton}
            onPress={() => {
              setEditingStudent(null);
              setFormData({ full_name: '', email: '', phone: '', class_name: '', student_number: '' });
              setModalVisible(true);
            }}
          >
            <Ionicons name="add" size={responsiveIconSize(24)} color={colors.surface} />
          </TouchableOpacity>
        </View>

        <View style={styles.searchContainer}>
          <Ionicons name="search" size={responsiveIconSize(18)} color={colors.muted} style={styles.searchIcon} />
          <TextInput
            style={styles.searchInput}
            placeholder="Öğrenci ara..."
            placeholderTextColor={colors.muted}
            value={searchText}
            onChangeText={setSearchText}
          />
        </View>

        <FlatList
          data={filteredStudents}
          renderItem={renderStudent}
          keyExtractor={(item) => item.id?.toString()}
          contentContainerStyle={[styles.list, { paddingBottom: bottomPadding }]}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Ionicons name="people-outline" size={responsiveIconSize(52)} color={colors.muted} style={styles.emptyIcon} />
              <Text style={styles.emptyText}>Öğrenci bulunamadı</Text>
            </View>
          }
        />
      </View>

      {/* Student Form Modal */}
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
            <Text style={styles.modalTitle}>{editingStudent ? 'Öğrenci Düzenle' : 'Yeni Öğrenci'}</Text>
            <TouchableOpacity onPress={saveStudent}>
              <Text style={styles.modalSave}>Kaydet</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Ad Soyad</Text>
              <TextInput
                style={styles.formInput}
                value={formData.full_name}
                onChangeText={(text) => setFormData({ ...formData, full_name: text })}
                placeholder="Ad Soyad"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>E-posta</Text>
              <TextInput
                style={styles.formInput}
                value={formData.email}
                onChangeText={(text) => setFormData({ ...formData, email: text })}
                placeholder="email@example.com"
                autoCapitalize="none"
                keyboardType="email-address"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Telefon</Text>
              <TextInput
                style={styles.formInput}
                value={formData.phone}
                onChangeText={(text) => setFormData({ ...formData, phone: text })}
                placeholder="+90 555 123 4567"
                keyboardType="phone-pad"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Öğrenci No</Text>
              <TextInput
                style={styles.formInput}
                value={formData.student_number}
                onChangeText={(text) => setFormData({ ...formData, student_number: text })}
                placeholder="12345"
                keyboardType="number-pad"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Sınıf</Text>
              <View style={styles.classSelector}>
                {classes.map((cls) => (
                  <TouchableOpacity
                    key={cls.id}
                    style={[styles.classOption, formData.class_name === cls.name && styles.selectedClass]}
                    onPress={() => setFormData({ ...formData, class_name: cls.name })}
                  >
                    <Text style={[styles.classOptionText, formData.class_name === cls.name && styles.selectedClassText]}>
                      {cls.name}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>

            {editingStudent && (
              <TouchableOpacity style={styles.deleteButton} onPress={deleteStudent}>
                <Ionicons name="trash-outline" size={20} color={colors.danger} />
                <Text style={styles.deleteButtonText}>Öğrenciyi Sil</Text>
              </TouchableOpacity>
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
  addButton: {
    width: 38,
    height: 38,
    backgroundColor: colors.brand,
    borderRadius: borderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
    ...shadows.small,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    marginBottom: spacing.lg,
    paddingHorizontal: spacing.lg,
    borderRadius: borderRadius.lg,
    height: 40,
    borderWidth: 1,
    borderColor: colors.line,
  },
  searchIcon: {
    marginRight: spacing.md,
  },
  searchInput: {
    flex: 1,
    fontSize: fontSize.md,
    color: colors.ink,
  },
  list: {
    padding: spacing.xxl,
    gap: spacing.sm,
  },
  studentCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xxxl,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  studentInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
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
  studentDetails: {
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
    marginTop: 3,
  },
  studentClass: {
    fontSize: fontSize.xs,
    color: colors.text.light,
    marginTop: 2,
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
  formGroup: {
    marginBottom: spacing.xl,
  },
  formLabel: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    marginBottom: spacing.sm,
  },
  formInput: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    fontSize: fontSize.md,
    ...shadows.small,
  },
  classSelector: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  classOption: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  selectedClass: {
    backgroundColor: colors.brand,
    borderColor: colors.brand,
  },
  classOptionText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  selectedClassText: {
    color: colors.surface,
  },
  deleteButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#fff0ef',
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginTop: spacing.xl,
    borderWidth: 1,
    borderColor: '#f5c6c6',
  },
  deleteButtonText: {
    color: colors.danger,
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    marginLeft: spacing.sm,
  },
});
