import React, { useEffect, useState } from 'react';
import { Text, View, Button, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import api, { formatApiError } from '../config/api';
import useAuthStore from '../store/authStore';

export default function PlatformWorkspaceScreen() {
  const [rows, setRows] = useState([]), [error, setError] = useState(''), [busy, setBusy] = useState(false), [more, setMore] = useState(true);
  const select = useAuthStore(s => s.setWorkspace), logout = useAuthStore(s => s.logout);
  async function load(offset = 0) {
    if (busy) return;
    setBusy(true); setError('');
    try {
      const response = await api.get('/platform/tenants', { params: { offset, limit: 20 } });
      const list = response.data?.data ?? response.data;
      if (!Array.isArray(list)) throw Error('Kurum listesi doğrulanamadı');
      setRows(previous => offset ? [...previous, ...list] : list); setMore(list.length === 20);
    } catch (e) { setError(formatApiError(e)); }
    finally { setBusy(false); }
  }
  useEffect(() => { load(); }, []);
  return <SafeAreaView style={{ flex: 1 }}><ScrollView contentContainerStyle={{ padding: 20, gap: 14 }}>
    <Text style={{ fontSize: 26, fontWeight: 'bold' }}>SaaS yönetimi</Text>
    <Text>Kurum sayfalarına geçmek için açıkça bir kurum seçin.</Text>
    {error ? <Text accessibilityRole="alert">{error}</Text> : null}
    <Button title="Listeyi yenile" disabled={busy} onPress={() => load()} />
    {rows.map(row => <View key={row.id} style={{ padding: 12, borderWidth: 1, borderColor: '#ccc' }}>
      <Text>{row.name}</Text><Text>{row.access?.status || 'Üyelik bilgisi yok'}</Text>
      <Button title="Kurum çalışma alanına geç" disabled={busy} onPress={() => select({ id: row.id, name: row.name })} />
    </View>)}
    {more && <Button title="Diğer kurumları yükle" disabled={busy} onPress={() => load(rows.length)} />}
    <Button title="Çıkış yap" onPress={logout} />
  </ScrollView></SafeAreaView>;
}
