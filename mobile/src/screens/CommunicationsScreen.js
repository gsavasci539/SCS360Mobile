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

export default function CommunicationsScreen({ navigation }) {
  const [announcements, setAnnouncements] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('announcements');
  const [modalVisible, setModalVisible] = useState(false);
  const [newAnnouncement, setNewAnnouncement] = useState({
    title: '',
    content: '',
    audience: 'ALL_PARENTS',
    channels: ['EMAIL'],
  });
  const { tabBarHeight } = useResponsive();
  const bottomPadding = getBottomPadding(tabBarHeight);

  useEffect(() => {
    fetchCommunications();
  }, []);

  const fetchCommunications = async () => {
    try {
      const [annRes, notifRes, tempRes] = await Promise.all([
        api.get('/announcements'),
        api.get('/notifications'),
        api.get('/notifications/templates'),
      ]);
      setAnnouncements(Array.isArray(annRes.data?.items || annRes.data) ? (annRes.data.items || annRes.data) : []);
      setNotifications(Array.isArray(notifRes.data?.items || notifRes.data) ? (notifRes.data.items || notifRes.data) : []);
      setTemplates(Array.isArray(tempRes.data?.items || tempRes.data) ? (tempRes.data.items || tempRes.data) : []);
    } catch (error) {
      console.error('Communications error:', error);
      setAnnouncements([]);
      setNotifications([]);
      setTemplates([]);
    } finally {
      setLoading(false);
    }
  };

  const createAnnouncement = async () => {
    try {
      const response = await api.post('/announcements', newAnnouncement);
      // Send the announcement after creation
      await api.post(`/announcements/${response.data.id}/send`);
      setModalVisible(false);
      setNewAnnouncement({ title: '', content: '', audience: 'ALL_PARENTS', channels: ['EMAIL'] });
      fetchCommunications();
    } catch (error) {
      console.error('Create announcement error:', error);
    }
  };

  const sendAnnouncement = async (id) => {
    try {
      await api.post(`/announcements/${id}/send`);
      fetchCommunications();
    } catch (error) {
      console.error('Send announcement error:', error);
    }
  };

  const renderAnnouncements = () => {
    return (
      <View style={styles.tabContent}>
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => setModalVisible(true)}
        >
          <Ionicons name="add" size={responsiveIconSize(20)} color={colors.surface} />
          <Text style={styles.addButtonText}>Duyuru Oluştur</Text>
        </TouchableOpacity>
        {announcements.length === 0 ? (
          <Text style={styles.emptyText}>Henüz duyuru yok</Text>
        ) : (
          announcements.map((item) => (
            <View key={item.id} style={styles.card}>
              <View style={styles.cardHeader}>
                <Text style={styles.cardTitle}>{item.title}</Text>
                <View style={[styles.statusBadge, { backgroundColor: item.status === 'SENT' ? '#e6f3ef' : '#fff3df' }]}>
                  <Text style={[styles.statusText, { color: item.status === 'SENT' ? colors.brand : colors.warning }]}>
                    {item.status === 'SENT' ? 'Gönderildi' : 'Taslak'}
                  </Text>
                </View>
              </View>
              <Text style={styles.cardContent} numberOfLines={2}>{item.content}</Text>
              <View style={styles.cardFooter}>
                <Text style={styles.cardMeta}>{item.audience}</Text>
                {item.status === 'DRAFT' && (
                  <TouchableOpacity onPress={() => sendAnnouncement(item.id)}>
                    <Text style={styles.sendButton}>Gönder</Text>
                  </TouchableOpacity>
                )}
              </View>
            </View>
          ))
        )}
      </View>
    );
  };

  const renderNotifications = () => {
    return (
      <View style={styles.tabContent}>
        {notifications.length === 0 ? (
          <Text style={styles.emptyText}>Henüz bildirim hareketi yok</Text>
        ) : (
          notifications.map((item) => (
            <View key={item.id} style={styles.card}>
              <View style={styles.cardHeader}>
                <Text style={styles.cardTitle}>{item.subject}</Text>
                <View style={[styles.statusBadge, { backgroundColor: item.status === 'SENT' ? '#e6f3ef' : item.status === 'FAILED' ? '#fff0ef' : '#fff3df' }]}>
                  <Text style={[styles.statusText, { color: item.status === 'SENT' ? colors.brand : item.status === 'FAILED' ? colors.danger : colors.warning }]}>
                    {item.status === 'SENT' ? 'Gönderildi' : item.status === 'FAILED' ? 'Başarısız' : 'Bekliyor'}
                  </Text>
                </View>
              </View>
              <View style={styles.cardFooter}>
                <Text style={styles.cardMeta}>{item.channel}</Text>
                <Text style={styles.cardMeta}>{item.retry_count || 0} deneme</Text>
              </View>
            </View>
          ))
        )}
      </View>
    );
  };

  const renderTemplates = () => (
    <View style={styles.tabContent}>
      {templates.length === 0 ? (
        <Text style={styles.emptyText}>Henüz şablon yok</Text>
      ) : (
        templates.map((item) => (
          <View key={item.id} style={styles.card}>
            <Text style={styles.cardTitle}>{item.name}</Text>
            <Text style={styles.cardMeta}>{item.code}</Text>
            <Text style={styles.cardMeta}>{item.channel}</Text>
          </View>
        ))
      )}
    </View>
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
          <Text style={styles.headerTitle}>İletişim</Text>
          <Text style={styles.headerSubtitle}>Duyurular ve bildirimler</Text>
        </View>

        <View style={styles.tabs}>
          <TouchableOpacity
            style={[styles.tab, activeTab === 'announcements' && styles.activeTab]}
            onPress={() => setActiveTab('announcements')}
          >
            <Text style={[styles.tabText, activeTab === 'announcements' && styles.activeTabText]}>Duyurular</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tab, activeTab === 'notifications' && styles.activeTab]}
            onPress={() => setActiveTab('notifications')}
          >
            <Text style={[styles.tabText, activeTab === 'notifications' && styles.activeTabText]}>Bildirimler</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tab, activeTab === 'templates' && styles.activeTab]}
            onPress={() => setActiveTab('templates')}
          >
            <Text style={[styles.tabText, activeTab === 'templates' && styles.activeTabText]}>Şablonlar</Text>
          </TouchableOpacity>
        </View>

        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPadding }]}
        >
          {activeTab === 'announcements' && renderAnnouncements()}
          {activeTab === 'notifications' && renderNotifications()}
          {activeTab === 'templates' && renderTemplates()}
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
            <Text style={styles.modalTitle}>Yeni Duyuru</Text>
            <TouchableOpacity onPress={createAnnouncement}>
              <Text style={styles.modalSave}>Kaydet</Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Başlık</Text>
              <TextInput
                style={styles.formInput}
                value={newAnnouncement.title}
                onChangeText={(text) => setNewAnnouncement({ ...newAnnouncement, title: text })}
                placeholder="Duyuru başlığı"
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>İçerik</Text>
              <TextInput
                style={[styles.formInput, styles.formTextarea]}
                value={newAnnouncement.content}
                onChangeText={(text) => setNewAnnouncement({ ...newAnnouncement, content: text })}
                placeholder="Duyuru içeriği"
                multiline
                numberOfLines={4}
              />
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Hedef Kitle</Text>
              <View style={styles.optionButtons}>
                {['ALL_PARENTS', 'BRANCH_PARENTS', 'CLASS_PARENTS'].map((option) => (
                  <TouchableOpacity
                    key={option}
                    style={[styles.optionButton, newAnnouncement.audience === option && styles.selectedOption]}
                    onPress={() => setNewAnnouncement({ ...newAnnouncement, audience: option })}
                  >
                    <Text style={[styles.optionText, newAnnouncement.audience === option && styles.selectedOptionText]}>
                      {option === 'ALL_PARENTS' ? 'Tüm Veliler' : option === 'BRANCH_PARENTS' ? 'Şube Velileri' : 'Sınıf Velileri'}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>

            <View style={styles.formGroup}>
              <Text style={styles.formLabel}>Kanallar</Text>
              <View style={styles.channelButtons}>
                {['EMAIL', 'SMS', 'WHATSAPP'].map((channel) => (
                  <TouchableOpacity
                    key={channel}
                    style={[styles.channelButton, newAnnouncement.channels.includes(channel) && styles.selectedChannel]}
                    onPress={() => {
                      const channels = newAnnouncement.channels.includes(channel)
                        ? newAnnouncement.channels.filter(c => c !== channel)
                        : [...newAnnouncement.channels, channel];
                      setNewAnnouncement({ ...newAnnouncement, channels });
                    }}
                  >
                    <Text style={[styles.channelText, newAnnouncement.channels.includes(channel) && styles.selectedChannelText]}>
                      {channel === 'EMAIL' ? 'E-posta' : channel === 'SMS' ? 'SMS' : 'WhatsApp'}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
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
  tabContent: {
    gap: spacing.md,
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
    ...shadows.small,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  cardTitle: {
    fontSize: fontSize.lg,
    fontWeight: fontWeight.bold,
    color: colors.ink,
    flex: 1,
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
  cardContent: {
    fontSize: fontSize.md,
    color: colors.muted,
    marginBottom: spacing.sm,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  cardMeta: {
    fontSize: fontSize.sm,
    color: colors.muted,
  },
  sendButton: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.brand,
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
  optionButtons: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  optionButton: {
    flex: 1,
    padding: spacing.md,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.surface,
    alignItems: 'center',
    ...shadows.small,
  },
  selectedOption: {
    backgroundColor: colors.brand,
  },
  optionText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  selectedOptionText: {
    color: colors.surface,
  },
  channelButtons: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  channelButton: {
    flex: 1,
    padding: spacing.md,
    borderRadius: borderRadius.lg,
    backgroundColor: colors.surface,
    alignItems: 'center',
    ...shadows.small,
  },
  selectedChannel: {
    backgroundColor: colors.brand,
  },
  channelText: {
    fontSize: fontSize.sm,
    fontWeight: fontWeight.bold,
    color: colors.ink,
  },
  selectedChannelText: {
    color: colors.surface,
  },
});
