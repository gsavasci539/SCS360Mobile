const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function amount(value) {
  const text = String(value).trim();
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(text) || Number(text) <= 0) throw Error('Geçersiz ödeme tutarı');
  return Number(text).toFixed(2);
}
export function hostedUrl(value) {
  const u = new URL(value);
  const h = u.hostname;
  const allowed = (h === 'sandbox-api.iyzipay.com' || h === 'api.iyzipay.com') && u.pathname.startsWith('/checkoutform/')
    || ['cpp.iyzipay.com', 'sandbox-cpp.iyzipay.com'].includes(h) && u.pathname === '/' && Boolean(u.searchParams.get('token'))
    || h === 'www.paytr.com' && u.pathname.startsWith('/odeme/guvenli/')
    || (h === 'craftgate.io' || h.endsWith('.craftgate.io'))
    || ['app.sipay.com.tr', 'provisioning.sipay.com.tr'].includes(h) && u.pathname.startsWith('/ccpayment/makepayment/');
  if (u.protocol !== 'https:' || u.username || u.password || u.port && u.port !== '443' || u.hash || !allowed) throw Error('Ödeme adresi doğrulanamadı');
  return u.href;
}
const unwrap = (response) => response.data?.data ?? response.data;

// One durable attempt per authenticated actor + selected institution + debt.
// Storage never contains payer, credentials, access tokens or hosted URLs.
export function checkout({ api, storage, newId, scope, debtId }) {
  if (!scope || !uuid.test(debtId)) throw Error('Oturum veya borç bilgisi eksik');
  const key = `scs360:school-checkout:v1:${scope}:${debtId}`;
  let pending, busy = false;
  const read = async () => {
    const raw = await storage.getItem(key);
    if (!raw) return null;
    const saved = JSON.parse(raw);
    if (saved.debt_id !== debtId || !uuid.test(saved.request_id) || amount(saved.amount) !== saved.amount) throw Error('Önceki ödeme kaydı doğrulanamadı');
    return saved;
  };
  const validate = (result, saved) => {
    if (!result || result.debt_id !== debtId || (result.id ?? result.conversation_id) !== saved.request_id || amount(result.amount) !== saved.amount) throw Error('Ödeme sonucu borç ve tutarla eşleşmiyor');
    if (!['INITIALIZING', 'PENDING', 'PAID_REVIEW', 'PAID', 'FAILED', 'EXPIRED', 'REFUNDED', 'FRAUD_REVIEW', 'PROCESSING', 'REVIEW'].includes(result.status)) throw Error('Ödeme durumu doğrulanamadı');
    if (result.status === 'PAID' && !result.payment_id) throw Error('Tahsilat kaydı doğrulanamadı');
    return result;
  };
  async function exclusive(action) {
    if (busy) throw Error('Ödeme isteği sürüyor');
    busy = true;
    try { return await action(); } finally { busy = false; }
  }
  return {
    read,
    start: (value, buyer) => exclusive(async () => {
      let saved = await read();
      const selected = amount(value);
      if (saved && saved.amount !== selected) throw Error('Önceki ödemenin sonucunu kontrol edin');
      if (!saved) {
        saved = { request_id: newId(), debt_id: debtId, amount: selected };
        if (!uuid.test(saved.request_id)) throw Error('Ödeme kimliği üretilemedi');
        await storage.setItem(key, JSON.stringify(saved));
        const check = await read();
        if (check?.request_id !== saved.request_id) throw Error('Ödeme kimliği saklanamadı');
      }
      pending ??= { ...saved, currency: 'TRY', payment_channel: 'MOBILE', buyer: { ...buyer } };
      return validate(unwrap(await api.post('/payments/online/initialize', pending)), saved);
    }),
    refresh: () => exclusive(async () => {
      const saved = await read();
      if (!saved) return null;
      return validate(unwrap(await api.post(`/payments/online/checkouts/${saved.request_id}/refresh`)), saved);
    }),
    status: () => exclusive(async () => {
      const saved = await read();
      return saved ? validate(unwrap(await api.get(`/payments/online/checkouts/${saved.request_id}`)), saved) : null;
    }),
    resume: () => exclusive(async () => {
      const saved = await read();
      if (!saved) throw Error('Önce ödeme oluşturun');
      const result = validate(unwrap(await api.post(`/payments/online/checkouts/${saved.request_id}/resume`)), saved);
      return result.can_resume ? hostedUrl(result.checkout_url ?? result.payment_page_url) : null;
    }),
    clearFailed: () => exclusive(async () => {
      const saved = await read();
      if (!saved) return;
      const result = validate(unwrap(await api.get(`/payments/online/checkouts/${saved.request_id}`)), saved);
      if (!['FAILED', 'REFUNDED', 'FRAUD_REVIEW', 'PROCESSING', 'REVIEW'].includes(result.status)) throw Error('Önceki ödeme hâlâ kontrol edilmeli');
      await storage.removeItem(key);
      pending = undefined;
    }),
  };
}
