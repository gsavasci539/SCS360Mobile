import { test } from 'node:test';
import assert from 'node:assert/strict';
import { checkout, hostedUrl } from '../src/payments/checkout.mjs';
const id = '00000000-0000-4000-8000-000000000001', debtId = '00000000-0000-4000-8000-000000000002';
function fixture() {
  const values = new Map(), calls = [];
  let response = { id, debt_id: debtId, amount: '12.30', status: 'PENDING' }, fail = false;
  const api = { post: async (url, body) => { calls.push({ url, body }); if (fail) throw Error('timeout'); return { data: { data: response } }; }, get: async () => ({ data: { data: response } }) };
  const storage = { getItem: async k => values.get(k), setItem: async (k, v) => values.set(k, v), removeItem: async k => values.delete(k) };
  const args = { api, storage, newId: () => id, scope: 'actor:institution', debtId };
  return { args, values, calls, set: (v) => response = v, fail: v => fail = v, controller: checkout(args) };
}
test('timeout retry keeps identity and exact body without persisting payer', async () => {
  const f = fixture(); f.fail(true);
  await assert.rejects(f.controller.start('12.30', { identityNumber: 'private' }), /timeout/);
  f.fail(false); await f.controller.start('12.30', { identityNumber: 'edited' });
  assert.equal(f.calls[0].body, f.calls[1].body);
  assert.ok(![...f.values.values()].join().includes('private'));
  await assert.rejects(f.controller.start('12.31', {}), /Önceki/);
});
test('storage failure prevents any provider request', async () => {
  const f = fixture(); f.args.storage.setItem = async () => { throw Error('storage'); };
  await assert.rejects(checkout(f.args).start('12.30', {}), /storage/); assert.equal(f.calls.length, 0);
});
test('reload recovers same attempt; paid needs matching server payment record', async () => {
  const f = fixture(); await f.controller.start('12.30', {});
  const reopened = checkout(f.args); assert.equal((await reopened.status()).id, id);
  f.set({ id, debt_id: debtId, amount: '12.30', status: 'PAID' });
  await assert.rejects(reopened.refresh(), /Tahsilat/);
  f.set({ id, debt_id: debtId, amount: '12.30', status: 'PAID', payment_id: 'receipt' });
  assert.equal((await reopened.refresh()).status, 'PAID');
  await assert.rejects(reopened.clearFailed(), /kontrol/);
});
test('untrusted returns and mismatched responses cannot become payments', async () => {
  for (const url of ['javascript:alert(1)', 'https://www.paytr.com.evil/odeme/guvenli/a', 'https://www.paytr.com@evil/odeme/guvenli/a', 'http://app.sipay.com.tr/ccpayment/makepayment/a']) assert.throws(() => hostedUrl(url));
  assert.equal(hostedUrl('https://www.paytr.com/odeme/guvenli/a'), 'https://www.paytr.com/odeme/guvenli/a');
  assert.equal(hostedUrl('https://sandbox-cpp.iyzipay.com?token=synthetic'), 'https://sandbox-cpp.iyzipay.com/?token=synthetic');
  const f = fixture(); f.set({ id, debt_id: debtId, amount: '12.31', status: 'PAID', payment_id: 'x' });
  await assert.rejects(f.controller.start('12.30', {}), /eşleşmiyor/);
});
test('only verified failure permits another attempt', async () => {
  const f = fixture(); await f.controller.start('12.30', {});
  await assert.rejects(f.controller.clearFailed(), /kontrol/);
  f.set({ id, debt_id: debtId, amount: '12.30', status: 'FAILED' });
  await f.controller.clearFailed(); assert.equal(await f.controller.read(), null);
});
