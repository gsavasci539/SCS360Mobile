import { test } from 'node:test';
import assert from 'node:assert/strict';
import { applyHeaders, commitRefresh, clearRefresh, sessionUpdate } from '../src/config/authStorage.mjs';
function storage() {
  const values = new Map([['token', 'old-access'], ['refreshToken', 'old-refresh'], ['workspaceOrganizationId', 'selected-school']]);
  return { values, getItem: async key => values.get(key), setItem: async (key, value) => values.set(key, value), multiRemove: async keys => keys.forEach(key => values.delete(key)) };
}
test('request headers always use the current account and remove stale workspace', async () => {
  const s = storage(), config = { headers: { Authorization: 'Bearer previous-default' } };
  await applyHeaders(config, s); assert.equal(config.headers.Authorization, 'Bearer old-access');
  s.values.clear(); await applyHeaders(config, s);
  assert.equal(config.headers.Authorization, undefined); assert.equal(config.headers['X-SCS360-Organization-Id'], undefined);
});
test('a late refresh cannot replace or clear a newly signed-in account', async () => {
  const s = storage();
  await sessionUpdate(async () => { await s.setItem('token', 'new-access'); await s.setItem('refreshToken', 'new-refresh'); });
  assert.equal(await commitRefresh(s, 'old-refresh', 'late-old-access', 'late-old-refresh'), false);
  await clearRefresh(s, 'old-refresh');
  assert.equal(s.values.get('token'), 'new-access');
});
test('refresh rotation and logout serialize without resurrecting a session', async () => {
  const s = storage();
  await Promise.all([commitRefresh(s, 'old-refresh', 'rotated-access', 'rotated-refresh'), sessionUpdate(() => s.multiRemove(['token', 'refreshToken']))]);
  assert.equal(await commitRefresh(s, 'rotated-refresh', 'late-access', 'late-refresh'), false);
  assert.equal(s.values.get('token'), undefined);
});
