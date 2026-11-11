import React, { useEffect, useRef, useState } from 'react';
import { View, Text, ScrollView, Button, TextInput, Modal } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AsyncStorage from '@react-native-async-storage/async-storage';
import api, { API_BASE_URL, formatApiError } from '../config/api';
import useAuthStore from '../store/authStore';
import SchoolCheckout from '../payments/SchoolCheckout';

const data = response => response.data?.data ?? response.data;
export default function FinanceScreen() {
  const user = useAuthStore(s => s.user);
  const [rows, setRows] = useState([]), [error, setError] = useState(''), [busy, setBusy] = useState(false), [selected, setSelected] = useState(null);
  const [scope, setScope] = useState(''), [revision, setRevision] = useState(0);
  const workspace = useAuthStore(s => s.workspace);
  const permissions = user?.is_super_admin && workspace ? ["finance.write"] : user?.permissions || [];
  const canPay = permissions.includes('finance.write') || permissions.includes('parent.make_payment');
  const permissionSignature = [...permissions].sort().join(',');
  const sessionKey = `${user?.id}:${user?.organization_id}:${workspace?.id}:${permissionSignature}`;
  const currentSession = useRef(sessionKey), selectionSequence = useRef(0);
  currentSession.current = sessionKey;
  useEffect(() => {
    let active = true;
    (async () => {
      setBusy(true); setError(''); setRows([]); setSelected(null);
      try {
        const organization = await AsyncStorage.getItem('workspaceOrganizationId') || user?.organization_id || '';
        if (user?.is_super_admin && !organization) throw Error('Finans için açık bir kurum seçin.');
        if (active) setScope(`${API_BASE_URL}:${user?.id}:${organization}`);
        let items = [];
        if (permissions.includes('finance.write')) {
          let page = 1, total;
          do {
            const response = data(await api.get('/payments/debt-options', { params: { page, page_size: 100 } }));
            if (!Array.isArray(response.items)) throw Error('Borç listesi doğrulanamadı.');
            items.push(...response.items); total = response.total;
            if (response.items.length === 0 && items.length < total) throw Error('Borç listesi eksik yüklendi.');
            page++;
          } while (items.length < total);
        } else if (permissions.includes('parent.view_finances')) {
          const children = data(await api.get('/parent/my-children'));
          for (const child of children) {
            const result = data(await api.get(`/parent/child/${child.student_id}/financial-invoices`));
            items.push(...result.invoices.filter(row => Number(row.remaining_amount) > 0 && row.status !== 'CANCELLED').map(row => ({ id: row.debt_id, name: row.description || 'Borç kaydı' })));
          }
        } else throw Error('Finans görüntüleme yetkiniz bulunmuyor.');
        if (active) setRows(items);
      } catch (e) { if (active) setError(formatApiError(e)); }
      finally { if (active) setBusy(false); }
    })();
    return () => { active = false; };
  }, [user?.id, user?.organization_id, workspace?.id, permissionSignature, revision]);
  async function choose(row) {
    const expectedSession = sessionKey, sequence = ++selectionSequence.current;
    setError(''); setBusy(true);
    const current = () => currentSession.current === expectedSession && selectionSequence.current === sequence;
    try { const result = data(await api.get(`/payments/online/debts/${row.id}`)); if (current()) setSelected(result); }
    catch (e) { if (current()) setError(formatApiError(e)); }
    finally { if (current()) setBusy(false); }
  }
  return <SafeAreaView style={{ flex: 1 }}><ScrollView contentContainerStyle={{ padding: 20, gap: 14 }}>
    <Text style={{ fontSize: 26, fontWeight: 'bold' }}>Kurum tahsilatları</Text>
    <Text>Okul/kurs hizmet bedelleri kurumun kendi ödeme hesabına gider. SaaS üyeliği ayrı yönetilir.</Text>
    {error ? <Text accessibilityRole="alert">{error}</Text> : null}
    <Button title={busy ? 'Yükleniyor…' : 'Listeyi yenile'} disabled={busy} onPress={() => setRevision(v => v + 1)} />
    {!busy && !error && !rows.length ? <Text>Açık borç kaydı bulunmuyor.</Text> : null}
    {rows.map(row => <View key={row.id} style={{ padding: 12, borderWidth: 1, borderColor: '#ccc', borderRadius: 10 }}>
      <Text>{row.name}</Text>{canPay && <Button title="Ödeme bilgileri" disabled={busy} onPress={() => choose(row)} />}
    </View>)}
    <Modal visible={Boolean(selected)} onRequestClose={() => setSelected(null)} animationType="slide">
      <SafeAreaView style={{ flex: 1 }}><ScrollView>
        <Button title="Listeye dön" onPress={() => setSelected(null)} />
        {selected && <SchoolCheckout key={`${scope}:${selected.id}`} debt={selected} scope={scope} onComplete={() => setRevision(v => v + 1)} />}
      </ScrollView></SafeAreaView>
    </Modal>
  </ScrollView></SafeAreaView>;
}
