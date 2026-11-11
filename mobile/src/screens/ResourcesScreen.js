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

const resourceTypes = [
  { key: 'branches', label: 'Şubeler', endpoint: '/branches' },
  { key: 'users', label: 'Kullanıcılar', endpoint: '/users' },
];

export default function ResourcesScreen({ navigation }) {
  const [activeType, setActiveType] = useState(resourceTypes[0].key);
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [formData, setFormData] = useState({ name: '', code: '', description: '' });
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchResources();
  }, [activeType]);

  const fetchResources = async () => {
    setLoading(true);
    const resourceType = resourceTypes.find(r => r.key === activeType);
    try {
      const response = await api.get(resourceType.endpoint);
      const data = response.data?.items || response.data || [];
      setResources(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Resources error:', error);
      setResources([]);
    } finally {
      setLoading(false);
    }
  };

  const openModal = (item = null) => {
    setEditingItem(item);
    setFormData(item || { name: '', code: '', description: '' });
    setModalVisible(true);
  };

  const saveResource = async () => {
    const resourceType = resourceTypes.find(r => r.key === activeType);
    try {
      if (editingItem) {
        await api.put(`${resourceType.endpoint}/${editingItem.id}`, formData);
      } else {
        await api.post(resourceType.endpoint, formData);
      }
      setModalVisible(false);
      fetchResources();
    } catch (error) {
      console.error('Save error:', error);
    }
  };

  const deleteResource = async (id) => {
    const resourceType = resourceTypes.find(r => r.key === activeType);
    try {
      await api.delete(`${resourceType.endpoint}/${id}`);
      fetchResources();
    } catch (error) {
      console.error('Delete error:', error);
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
          <Text style={styles.headerTitle}>Kaynaklar</Text>
          <Text style={styles.headerSubtitle}>Şubeler, öğretmenler ve dersler</Text>
        </View>

        <View style={styles.tabs}>
          {resourceTypes.map((type) => (
            <TouchableOpacity
              key={type.key}
              style={[styles.tab, activeType === type.key && styles.activeTab]}
              onPress={() => setActiveType(type.key)}
            >
              <Text style={[styles.tabText, activeType === type.key && styles.activeTabText]}>
                {type.label}
              </Text>
            </TouchableOpacity>
          ))}
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
          <Text style={styles.addButtonText}>Yeni Ekle</Text>
        </TouchableOpacity>

        {resources.length === 0 ? (
          <Text style={styles.emptyText}>Henüz kayıt yok</Text>
        ) : (
          resources.map((item) => (
            <View key={item.id} style={styles.card}>
              <View style={styles.cardHeader}>
                <View style={styles.cardInfo}>
                  <Text style={styles.cardTitle}>{item.name}</Text>
                  {item.code && <Text style={styles.cardMeta}>{item.code}</Text>}
                  {item.description && <Text style={styles.cardDescription}>{item.description}</Text>}
                </View>
                <View style={styles.cardActions}>
                  <TouchableOpacity onPress={() => openModal(item)}>
                    <Ionicons name="create-outline" size={responsiveIconSize(20)} color={colors.brand} />
                  </TouchableOpacity>
                  <TouchableOpacity onPress={() => deleteResource(item.id)}>
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
            <Text style={styles.modalTitle}>{editingItem ? 'Düzenle' : 'Yeni Kayıt'}</Text>
            <TouchableOpacity onPress={saveResource}>
              <Text style={styles.modalSave}>Kaydet</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Ad</Text>
              <TextInput
                style={styles.formInput}
                value={formData.name}
                onChangeText={(text) => setFormData({ ...formData, name: text })}
                placeholder="Kayıt adı"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Kod</Text>
              <TextInput
                style={styles.formInput}
                value={formData.code}
                onChangeText={(text) => setFormData({ ...formData, code: text })}
                placeholder="Kod (opsiyonel)"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Açıklama</Text>
              <TextInput
                style={[styles.formInput, styles.formTextarea]}
                value={formData.description}
                onChangeText={(text) => setFormData({ ...formData, description: text })}
                placeholder="Açıklama (opsiyonel)"
                multiline
                numberOfLines={4}
              />
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
  tabs: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    gap: spacing.sm,
  },
  tab: {
    flex: 1,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: borderRadius.lg,
    alignItems: 'center',
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
  cardTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  cardMeta: {
    fontSize: fontSize.sm,
    color: colors.muted,
    marginTop: 2,
  },
  cardDescription: {
    fontSize: fontSize.md,
    color: colors.muted,
    marginTop: spacing.xs,
  },
  cardActions: {
    flexDirection: 'row',
    gap: spacing.md,
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
