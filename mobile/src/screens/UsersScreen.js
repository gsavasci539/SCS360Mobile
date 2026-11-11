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
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView } from 'react-native-safe-area-context';
import api from '../config/api';
import { colors, spacing, borderRadius, fontSize, fontWeight, shadows } from '../config/theme';
import { getBottomPadding, getTabBarHeight, getResponsiveContainerStyle, responsiveIconSize, useResponsive } from '../utils/responsive';

export default function UsersScreen({ navigation }) {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState({
    email: '',
    full_name: '',
    role_id: '',
    phone: '',
    is_active: true,
  });
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/users');
      const data = response.data?.items || response.data || [];
      setUsers(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Users error:', error);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const response = await api.get('/roles');
      const data = response.data?.items || response.data || [];
      setRoles(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Roles error:', error);
      setRoles([]);
    }
  };

  const openModal = (user = null) => {
    setEditingUser(user);
    setFormData(user || {
      email: '',
      full_name: '',
      role_id: '',
      phone: '',
      is_active: true,
    });
    setModalVisible(true);
  };

  const saveUser = async () => {
    try {
      if (editingUser) {
        await api.put(`/users/${editingUser.id}`, formData);
      } else {
        await api.post('/users', formData);
      }
      setModalVisible(false);
      fetchUsers();
    } catch (error) {
      console.error('Save error:', error);
      alert('Kaydedilemedi');
    }
  };

  const deleteUser = async (id) => {
    try {
      await api.delete(`/users/${id}`);
      fetchUsers();
    } catch (error) {
      console.error('Delete error:', error);
      alert('Silinemedi');
    }
  };

  const toggleUserStatus = async (user) => {
    try {
      await api.patch(`/users/${user.id}`, { is_active: !user.is_active });
      fetchUsers();
    } catch (error) {
      console.error('Toggle status error:', error);
    }
  };

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
        <Text style={styles.headerTitle}>Kullanıcılar</Text>
        <Text style={styles.headerSubtitle}>Sistem kullanıcıları yönetimi</Text>
      </View>

      <ScrollView 
        style={styles.scrollView}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
      >
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => openModal()}
        >
          <Ionicons name="add" size={responsiveIconSize(20)} color={colors.surface} />
          <Text style={styles.addButtonText}>Kullanıcı Ekle</Text>
        </TouchableOpacity>

        {users.length === 0 ? (
          <Text style={styles.emptyText}>Henüz kullanıcı yok</Text>
        ) : (
          users.map((user) => (
            <View key={user.id} style={styles.card}>
              <View style={styles.cardHeader}>
                <View style={styles.cardInfo}>
                  <View style={styles.userHeader}>
                    <Text style={styles.cardTitle}>{user.full_name}</Text>
                    <View style={[styles.statusBadge, { backgroundColor: user.is_active ? '#e6f3ef' : '#fff0ef' }]}>
                      <Text style={[styles.statusText, { color: user.is_active ? colors.brand : colors.danger }]}>
                        {user.is_active ? 'Aktif' : 'Pasif'}
                      </Text>
                    </View>
                  </View>
                  <Text style={styles.cardMeta}>{user.email}</Text>
                  {user.phone && <Text style={styles.cardMeta}>{user.phone}</Text>}
                  {user.role_name && (
                    <Text style={styles.cardRole}>{user.role_name}</Text>
                  )}
                </View>
                <View style={styles.cardActions}>
                  <TouchableOpacity onPress={() => toggleUserStatus(user)}>
                    <Ionicons 
                      name={user.is_active ? 'toggle-outline' : 'toggle'} 
                      size={responsiveIconSize(24)} 
                      color={user.is_active ? colors.brand : colors.muted} 
                    />
                  </TouchableOpacity>
                  <TouchableOpacity onPress={() => openModal(user)}>
                    <Ionicons name="create-outline" size={responsiveIconSize(20)} color={colors.brand} />
                  </TouchableOpacity>
                  <TouchableOpacity onPress={() => deleteUser(user.id)}>
                    <Ionicons name="trash-outline" size={responsiveIconSize(20)} color={colors.danger} />
                  </TouchableOpacity>
                </View>
              </View>
            </View>
          ))
        )}
      </ScrollView>

      <Modal
        visible={modalVisible}
        animationType="slide"
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setModalVisible(false)}>
              <Ionicons name="close" size={responsiveIconSize(24)} color={colors.ink} />
            </TouchableOpacity>
            <Text style={styles.modalTitle}>{editingUser ? 'Kullanıcı Düzenle' : 'Yeni Kullanıcı'}</Text>
            <TouchableOpacity onPress={saveUser}>
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
              <Text style={styles.formLabel}>Rol</Text>
              <View style={styles.roleSelector}>
                {roles.map((role) => (
                  <TouchableOpacity
                    key={role.id}
                    style={[styles.roleOption, formData.role_id === String(role.id) && styles.selectedRole]}
                    onPress={() => setFormData({ ...formData, role_id: String(role.id) })}
                  >
                    <Text style={[styles.roleOptionText, formData.role_id === String(role.id) && styles.selectedRoleText]}>
                      {role.name}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>

            {!editingUser && (
              <View style={styles.formGroup}>
                <Text style={styles.formLabel}>Şifre</Text>
                <TextInput
                  style={styles.formInput}
                  value={formData.password}
                  onChangeText={(text) => setFormData({ ...formData, password: text })}
                  placeholder="••••••••"
                  secureTextEntry
                />
              </View>
            )}

            <View style={styles.formGroup}>
              <TouchableOpacity
                style={[styles.checkboxRow, formData.is_active && styles.checkboxRowActive]}
                onPress={() => setFormData({ ...formData, is_active: !formData.is_active })}
              >
                <View style={[styles.checkbox, formData.is_active && styles.checkboxChecked]}>
                  {formData.is_active && <Ionicons name="checkmark" size={responsiveIconSize(16)} color={colors.surface} />}
                </View>
                <Text style={styles.checkboxLabel}>Aktif kullanıcı</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        </View>
      </Modal>
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
  addButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.brand,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  addButtonText: {
    color: colors.surface,
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    marginLeft: spacing.sm,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    marginBottom: spacing.sm,
    ...shadows.small,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  cardInfo: {
    flex: 1,
  },
  userHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  cardTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    marginRight: spacing.sm,
  },
  statusBadge: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.sm,
  },
  statusText: {
    fontSize: fontSize.xs,
    fontWeight: fontWeight.bold,
  },
  cardMeta: {
    fontSize: fontSize.md,
    color: colors.muted,
    marginTop: 2,
  },
  cardRole: {
    fontSize: fontSize.sm,
    color: colors.brand,
    marginTop: spacing.xs,
    fontWeight: fontWeight.bold,
  },
  cardActions: {
    flexDirection: 'row',
    gap: spacing.md,
    alignItems: 'center',
  },
  emptyText: {
    fontSize: fontSize.md,
    color: colors.muted,
    textAlign: 'center',
    padding: spacing.xl,
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
  roleSelector: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  roleOption: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  selectedRole: {
    backgroundColor: colors.brand,
    borderColor: colors.brand,
  },
  roleOptionText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  selectedRoleText: {
    color: colors.surface,
  },
  checkboxRow: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  checkboxRowActive: {
    borderColor: colors.brand,
    backgroundColor: '#e6f3ef',
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: borderRadius.sm,
    backgroundColor: colors.canvas,
    borderWidth: 2,
    borderColor: colors.line,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.md,
  },
  checkboxChecked: {
    backgroundColor: colors.brand,
    borderColor: colors.brand,
  },
  checkboxLabel: {
    fontSize: fontSize.md,
    color: colors.ink,
  },
});
