let queue = Promise.resolve();
export function sessionUpdate(action) {
  const next = queue.then(action, action);
  queue = next.catch(() => {});
  return next;
}
export const readSession = storage => sessionUpdate(async () => ({
  token: await storage.getItem('token'),
  refreshToken: await storage.getItem('refreshToken'),
  workspace: await storage.getItem('workspaceOrganizationId'),
}));
export async function applyHeaders(config, storage) {
  const current = await readSession(storage);
  if (current.token) config.headers.Authorization = `Bearer ${current.token}`;
  else delete config.headers.Authorization;
  if (current.workspace) config.headers['X-SCS360-Organization-Id'] = current.workspace;
  else delete config.headers['X-SCS360-Organization-Id'];
  return config;
}
export const commitRefresh = (storage, expected, access, refresh) => sessionUpdate(async () => {
  if (!expected || await storage.getItem('refreshToken') !== expected) return false;
  await storage.setItem('token', access);
  if (refresh) await storage.setItem('refreshToken', refresh);
  return true;
});
export const clearRefresh = (storage, expected) => sessionUpdate(async () => {
  if (expected && await storage.getItem('refreshToken') === expected) await storage.multiRemove(['token', 'refreshToken', 'user', 'workspaceOrganizationId']);
});
