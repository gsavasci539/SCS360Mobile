import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { applyHeaders, readSession, commitRefresh, clearRefresh } from './authStorage.mjs';

// Supplied by the deployment owner; no embedded production host or merchant keys.
export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_BASE_URL || '').trim().replace(/\/$/, '');

export let lastApiLog = {
  timestamp: new Date().toISOString(),
  url: null,
  method: null,
  status: null,
  data: null,
  error: null,
};

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
  },
});

// Single-flight refresh token queue to deduplicate concurrent refresh calls
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Centralized Request Interceptor
api.interceptors.request.use(
  async (config) => {
    try {
      if (!API_BASE_URL) throw new Error('API adresi yapılandırılmamış (EXPO_PUBLIC_API_BASE_URL).');
      const endpoint = new URL(API_BASE_URL);
      const dev = typeof __DEV__ !== 'undefined' && __DEV__;
      if (endpoint.username || endpoint.password || endpoint.search || endpoint.hash || endpoint.protocol !== 'https:' && !(dev && endpoint.protocol === 'http:')) throw new Error('Geçerli HTTPS API adresi gerekli.');
      await applyHeaders(config, AsyncStorage);
    } catch (error) {
      return Promise.reject(error);
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Centralized Response Interceptor for data unwrapping, 401 token refresh and error handling
api.interceptors.response.use(
  (response) => {
    lastApiLog = {
      timestamp: new Date().toLocaleTimeString('tr-TR'),
      url: response.config?.url || '',
      method: (response.config?.method || 'GET').toUpperCase(),
      status: response.status,
      data: null,
      error: null,
    };

    // Transparently unpack FastAPI success_response({ success: true, data: ... })
    if (
      response.data &&
      typeof response.data === 'object' &&
      response.data.success === true &&
      response.data.data !== undefined
    ) {
      const inner = response.data.data;
      if (inner && typeof inner === 'object') {
        if (!Array.isArray(inner)) {
          Object.assign(response.data, inner);
        } else {
          response.data.items = inner;
        }
      }
    }
    return response;
  },
  async (error) => {
    lastApiLog = {
      timestamp: new Date().toLocaleTimeString('tr-TR'),
      url: error.config?.url || '',
      method: (error.config?.method || 'POST').toUpperCase(),
      status: error.response?.status || 'AĞ HATASI',
      data: null,
      error: 'İstek başarısız',
    };

    const originalRequest = error.config;

    // Handle 401 Unauthorized with token refresh mutex
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      let refreshToken;
      try {
        refreshToken = (await readSession(AsyncStorage)).refreshToken;
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        const cleanRefreshUrl = `${API_BASE_URL}/auth/refresh`;
        const response = await axios.post(
          cleanRefreshUrl,
          { refresh_token: refreshToken },
          { timeout: 15000 }
        );

        const payload = response.data?.data || response.data;
        const newAccessToken = payload?.access_token || payload?.token;
        const newRefreshToken = payload?.refresh_token;

        if (newAccessToken) {
          if (!await commitRefresh(AsyncStorage, refreshToken, newAccessToken, newRefreshToken)) throw new Error('Oturum değişti; eski yenileme yanıtı kullanılmadı.');
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          processQueue(null, newAccessToken);
          return api(originalRequest);
        } else {
          throw new Error('Invalid refresh response payload');
        }
      } catch (refreshError) {
        processQueue(refreshError, null);
        delete api.defaults.headers.common.Authorization;
        await clearRefresh(AsyncStorage, refreshToken);
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

/**
 * Health check helper querying public endpoint /ready
 */
export async function checkBackend() {
  const response = await api.get('/ready');
  return response.data;
}

/**
 * User-friendly Turkish error message formatter for network and API responses
 */
export function formatApiError(error) {
  if (!error) return 'Bilinmeyen bir hata oluştu.';
  if (error.response) {
    const status = error.response.status;
    const data = error.response.data;
    
    // FastAPI custom error response: { success: false, message: "...", errors: [...] }
    const message = data?.message || data?.detail;
    const errorsList = data?.errors;
    let extraDetail = '';
    if (Array.isArray(errorsList) && errorsList.length > 0) {
      extraDetail = errorsList.map((e) => e.msg || e.message || JSON.stringify(e)).join(', ');
    } else if (Array.isArray(message)) {
      extraDetail = message.map((d) => d.msg || JSON.stringify(d)).join(', ');
    }

    if (message && typeof message === 'string') {
      return extraDetail ? `${message} (${extraDetail})` : message;
    }
    if (extraDetail) return extraDetail;

    switch (status) {
      case 400: return 'Gönderilen istek formatı geçersiz (HTTP 400).';
      case 401: return 'E-posta veya şifre hatalı (HTTP 401).';
      case 403: return 'Bu işlem için yetkiniz bulunmamaktadır (HTTP 403).';
      case 404: return 'İstenen API servisi bulunamadı (HTTP 404).';
      case 422: return 'Lütfen geçerli bir e-posta adresi ve şifre giriniz (HTTP 422).';
      case 429: return 'Çok fazla giriş denemesi yapıldı. Güvenlik nedeniyle lütfen 1 dakika bekleyin (HTTP 429).';
      case 500:
      case 502:
      case 503: return 'Sunucu geçici olarak yanıt vermiyor (HTTP ' + status + '). Lütfen daha sonra tekrar deneyin.';
      default: return `Sunucu hatası (${status}).`;
    }
  } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
    return 'Bağlantı zaman aşımına uğradı (Timeout 30s). Sunucuya ulaşılamıyor.';
  } else if (error.request) {
    return 'Sunucuya bağlanılamadı. Lütfen telefonunuzun internet bağlantısını kontrol edin.';
  }
  return error.message || 'İşlem gerçekleştirilemedi.';
}

export default api;
