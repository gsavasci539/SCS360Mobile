import api from '../config/api';
import { sessionUpdate } from '../config/authStorage.mjs';
import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';

const useAuthStore = create((set, get) => ({
  user: null,
  token: null,
  refreshToken: null,
  isAuthenticated: false,
  workspace: null,
  setWorkspace: async (workspace) => {
    if (workspace) await AsyncStorage.setItem('workspaceOrganizationId', workspace.id);
    else await AsyncStorage.removeItem('workspaceOrganizationId');
    set({ workspace });
  },

  login: async (userData, token, refreshToken = null) => sessionUpdate(async () => {
    try {
      await AsyncStorage.removeItem('workspaceOrganizationId');
      await AsyncStorage.setItem('token', token);
      await AsyncStorage.setItem('user', JSON.stringify(userData));
      if (refreshToken) {
        await AsyncStorage.setItem('refreshToken', refreshToken);
      } else await AsyncStorage.removeItem('refreshToken');
      set({ user: userData, token, refreshToken, isAuthenticated: true, workspace: null });
    } catch (error) {
      console.error('Login storage error:', error);
    }
  }),

  logout: async () => sessionUpdate(async () => {
    try {
      delete api.defaults.headers.common.Authorization;
      await AsyncStorage.removeItem('workspaceOrganizationId');
      await AsyncStorage.removeItem('token');
      await AsyncStorage.removeItem('user');
      await AsyncStorage.removeItem('refreshToken');
      set({ user: null, token: null, refreshToken: null, isAuthenticated: false, workspace: null });
    } catch (error) {
      console.error('Logout storage error:', error);
    }
  }),

  setUser: async (userData) => {
    set({ user: userData });
    await AsyncStorage.setItem('user', JSON.stringify(userData));
  },

  loadUser: async () => {
    try {
      const token = await AsyncStorage.getItem('token');
      const refreshToken = await AsyncStorage.getItem('refreshToken');
      const userStr = await AsyncStorage.getItem('user');
      const user = userStr && userStr !== 'undefined' ? JSON.parse(userStr) : null;
      if (token && user) {
        await AsyncStorage.removeItem('workspaceOrganizationId');
        set({ user, token, refreshToken, isAuthenticated: true });
        const response = await api.get('/auth/me');
        if (get().isAuthenticated && get().token === token) await get().setUser(response.data?.data || response.data);
      }
    } catch (error) {
      console.error('Load user storage error:', error);
    }
  },
}));

export default useAuthStore;
