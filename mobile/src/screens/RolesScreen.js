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

export default function RolesScreen({ navigation }) {
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [selectedRole, setSelectedRole] = useState(null);
  const [assignedPermissions, setAssignedPermissions] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [newRole, setNewRole] = useState({ name: '', slug: '', description: '' });
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchRoles();
    fetchPermissions();
  }, []);

  useEffect(() => {
    if (selectedRole) {
      fetchRolePermissions();
    }
  }, [selectedRole]);

  const fetchRoles = async () => {
    try {
      const response = await api.get('/roles');
      const data = response.data?.items || response.data || [];
      setRoles(Array.isArray(data) ? data : []);
      if (!selectedRole && Array.isArray(data) && data[0]) {
        setSelectedRole(data[0]);
      }
    } catch (error) {
      console.error('Roles error:', error);
      setRoles([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchPermissions = async () => {
    try {
      const response = await api.get('/permissions');
      const data = response.data?.items || response.data || [];
      setPermissions(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Permissions error:', error);
      setPermissions([]);
    }
  };

  const fetchRolePermissions = async () => {
    try {
      const response = await api.get(`/roles/${selectedRole.id}/permissions`);
      const data = response.data || [];
      const ids = Array.isArray(data) 
        ? data.map(p => typeof p === 'string' ? p : p.id)
        : [];
      setAssignedPermissions(new Set(ids));
    } catch (error) {
      console.error('Role permissions error:', error);
      setAssignedPermissions(new Set());
    }
  };

  const togglePermission = (permissionId) => {
    const newAssigned = new Set(assignedPermissions);
    if (newAssigned.has(permissionId)) {
      newAssigned.delete(permissionId);
    } else {
      newAssigned.add(permissionId);
    }
    setAssignedPermissions(newAssigned);
  };

  const savePermissions = async () => {
    try {
      await api.put(`/roles/${selectedRole.id}/permissions`, {
        permission_ids: [...assignedPermissions],
      });
      alert('Yetkiler güncellendi');
    } catch (error) {
      console.error('Save permissions error:', error);
      alert('Kaydedilemedi');
    }
  };

  const createRole = async () => {
    try {
      await api.post('/roles', newRole);
      setModalVisible(false);
      setNewRole({ name: '', slug: '', description: '' });
      fetchRoles();
    } catch (error) {
      console.error('Create role error:', error);
      alert('Rol oluşturulamadı');
    }
  };

  const groupedPermissions = permissions.reduce((acc, permission) => {
    const module = permission.module || 'Diğer';
    if (!acc[module]) acc[module] = [];
    acc[module].push(permission);
    return acc;
  }, {});

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
          <Text style={styles.headerTitle}>Roller ve Yetkiler</Text>
          <Text style={styles.headerSubtitle}>Erişim kontrolü yönetimi</Text>
        </View>

        <View style={styles.actionBar}>
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => setModalVisible(true)}
          >
            <Ionicons name="add" size={responsiveIconSize(18)} color={colors.surface} />
            <Text style={styles.actionButtonText}>Rol Ekle</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.actionButton, styles.saveButton]}
            onPress={savePermissions}
            disabled={!selectedRole}
          >
            <Ionicons name="save-outline" size={responsiveIconSize(18)} color={colors.surface} />
            <Text style={styles.actionButtonText}>Kaydet</Text>
          </TouchableOpacity>
        </View>

        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
        >
          <View style={styles.rolesList}>
            <Text style={styles.sectionTitle}>Roller</Text>
            {roles.map((role) => (
              <TouchableOpacity
                key={role.id}
                style={[styles.roleItem, selectedRole?.id === role.id && styles.selectedRole]}
                onPress={() => setSelectedRole(role)}
              >
                <View style={styles.roleIcon}>
                  <Ionicons name="shield-checkmark-outline" size={responsiveIconSize(20)} color={colors.brand} />
                </View>
                <View style={styles.roleInfo}>
                  <Text style={styles.roleName}>{role.name}</Text>
                  <Text style={styles.roleSlug}>{role.slug}</Text>
              </View>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.permissionsSection}>
          <Text style={styles.sectionTitle}>Yetki Matrisi</Text>
          {selectedRole ? (
            <ScrollView style={styles.permissionsList}>
              {Object.entries(groupedPermissions).map(([module, perms]) => (
                <View key={module} style={styles.permissionGroup}>
                  <Text style={styles.moduleTitle}>{module}</Text>
                  {perms.map((permission) => (
                    <TouchableOpacity
                      key={permission.id}
                      style={[styles.permissionItem, assignedPermissions.has(permission.id) && styles.selectedPermission]}
                      onPress={() => togglePermission(permission.id)}
                    >
                      <Text style={styles.permissionName}>{permission.name}</Text>
                      {assignedPermissions.has(permission.id) && (
                        <Ionicons name="checkmark" size={18} color={colors.brand} />
                      )}
                    </TouchableOpacity>
                  ))}
                </View>
              ))}
            </ScrollView>
          ) : (
            <Text style={styles.emptyText}>Rol seçin</Text>
          )}
        </View>
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
              <Ionicons name="close" size={responsiveIconSize(24)} color={colors.ink} />
            </TouchableOpacity>
            <Text style={styles.modalTitle}>Yeni Rol</Text>
            <TouchableOpacity onPress={createRole}>
              <Text style={styles.modalSave}>Kaydet</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Rol Adı</Text>
              <TextInput
                style={styles.formInput}
                value={newRole.name}
                onChangeText={(text) => setNewRole({ ...newRole, name: text })}
                placeholder="Rol adı"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Slug</Text>
              <TextInput
                style={styles.formInput}
                value={newRole.slug}
                onChangeText={(text) => setNewRole({ ...newRole, slug: text })}
                placeholder="rol-slug"
                autoCapitalize="none"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Açıklama</Text>
              <TextInput
                style={[styles.formInput, styles.formTextarea]}
                value={newRole.description}
                onChangeText={(text) => setNewRole({ ...newRole, description: text })}
                placeholder="Rol açıklaması"
                multiline
                numberOfLines={4}
              />
            </View>
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
  actionBar: {
    flexDirection: 'row',
    gap: spacing.sm,
    padding: spacing.lg,
    backgroundColor: colors.surface,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.brand,
    borderRadius: borderRadius.lg,
    padding: spacing.sm,
    paddingHorizontal: spacing.md,
    gap: spacing.xs,
  },
  saveButton: {
    backgroundColor: colors.success,
  },
  actionButtonText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.surface,
  },
  rolesList: {
    flex: 1,
    maxWidth: 200,
  },
  sectionTitle: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    marginBottom: spacing.md,
  },
  roleItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.sm,
    borderWidth: 1,
    borderColor: 'transparent',
    ...shadows.small,
  },
  selectedRole: {
    borderColor: colors.brand,
    backgroundColor: '#e6f3ef',
  },
  roleIcon: {
    width: 36,
    height: 36,
    borderRadius: borderRadius.lg,
    backgroundColor: '#e6f3ef',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.sm,
  },
  roleInfo: {
    flex: 1,
  },
  roleName: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  roleSlug: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: 2,
  },
  permissionsSection: {
    flex: 2,
  },
  permissionsList: {
    flex: 1,
  },
  permissionGroup: {
    marginBottom: spacing.lg,
  },
  moduleTitle: {
    fontSize: fontSize.md,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    marginBottom: spacing.sm,
    textTransform: 'capitalize',
  },
  permissionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.xs,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  selectedPermission: {
    borderColor: colors.brand,
    backgroundColor: '#e6f3ef',
  },
  permissionName: {
    fontSize: fontSize.md,
    color: colors.ink,
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
  formTextarea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
});
