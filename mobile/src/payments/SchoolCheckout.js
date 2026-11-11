import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AppState, Linking, Text, TextInput, View, Button } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Crypto from 'expo-crypto';
import api, { formatApiError } from '../config/api';
import { checkout } from './checkout.mjs';

export default function SchoolCheckout({ debt, scope, onComplete }) {
  const controller = useMemo(() => checkout({ api, storage: AsyncStorage, newId: Crypto.randomUUID, scope, debtId: debt.id }), [scope, debt.id]);
  const [result, setResult] = useState(null), [error, setError] = useState(''), [busy, setBusy] = useState(false), [saved, setSaved] = useState(null);
  const [buyer, setBuyer] = useState({ identityNumber: '', gsmNumber: '', registrationAddress: '', city: '', country: '' });
  const active = useRef(true), completed = useRef(false), inFlight = useRef(false);
  async function run(action) {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true); setError('');
    try {
      const value = await action();
      if (!active.current) return;
      if (value && typeof value === 'object') setResult(value);
      setSaved(await controller.read());
      if (value?.status === 'PAID' && !completed.current) { completed.current = true; onComplete(); }
    } catch (e) { if (active.current) setError(formatApiError(e)); }
    finally { if (active.current) { setBusy(false); setSaved(await controller.read().catch(() => null)); } inFlight.current = false; }
  }
  useEffect(() => {
    active.current = true; run(() => controller.status());
    const listener = AppState.addEventListener('change', state => { if (state === 'active') run(() => controller.refresh()); });
    return () => { active.current = false; listener.remove(); };
  }, [controller]);
  const terminal = result?.status === 'PAID';
  return <View style={{ padding: 16, gap: 10 }}>
    <Text>Kuruma ödeme · {saved?.amount || debt.remaining_amount} TRY</Text>
    <Text>Kart bilgilerinizi yalnız sağlayıcının sayfasında girin. Tarayıcıdan dönüş, ödeme onayı sayılmaz.</Text>
    {error ? <Text accessibilityRole="alert">{error}</Text> : null}
    {result ? <Text accessibilityLiveRegion="polite">{result.message || result.status}</Text> : null}
    {!terminal && !result?.can_resume && <>
      {Object.entries({ identityNumber: 'Kimlik / pasaport numarası', gsmNumber: 'Telefon', registrationAddress: 'Fatura adresi', city: 'Şehir', country: 'Ülke' }).map(([key, label]) =>
        <TextInput key={key} accessibilityLabel={label} placeholder={label} value={buyer[key]} editable={!busy}
          autoComplete="off" onChangeText={value => setBuyer(previous => ({ ...previous, [key]: value }))} style={{ padding: 10, borderWidth: 1, borderColor: '#aaa' }} />)}
      <Button title={saved ? 'Aynı ödeme işlemini tekrar gönder' : 'Ödeme sayfasını hazırla'} disabled={busy || !debt.online_payment_available || Object.values(buyer).some(v => !v.trim())}
        onPress={() => run(() => controller.start(saved?.amount || debt.remaining_amount, buyer))} />
    </>}
    {result?.can_resume && <Button title="Güvenli ödeme sayfasını aç" disabled={busy} onPress={() => run(async () => { const url = await controller.resume(); if (url) await Linking.openURL(url); })} />}
    {saved && !terminal && <Button title="Sunucudan ödeme sonucunu kontrol et" disabled={busy} onPress={() => run(() => controller.refresh())} />}
    {['FAILED', 'REFUNDED'].includes(result?.status) && <Button title="Yeni işlem için önceki kaydı kapat" disabled={busy} onPress={() => run(async () => { await controller.clearFailed(); setResult(null); })} />}
  </View>;
}
