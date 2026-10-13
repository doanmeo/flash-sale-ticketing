import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach Bearer Token from localStorage
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Auto Refresh or Redirect on 401
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => {
    // If backend envelope has custom business error (e.g. code != 1000)
    if (response.data && typeof response.data.code === 'number' && response.data.code !== 1000) {
      return Promise.reject(new Error(response.data.message || 'Thao tác thất bại'));
    }
    return response;
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Capture friendly error message from backend ApiResponse
    const serverMessage = error.response?.data?.message;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (originalRequest.url?.includes('/api/auth/login') || originalRequest.url?.includes('/api/auth/register')) {
        return Promise.reject(new Error(serverMessage || 'Đăng nhập không thành công'));
      }

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('auth:logout'));
        return Promise.reject(new Error(serverMessage || 'Phiên đăng nhập đã hết hạn'));
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const res = await axios.post<ApiResponse<{ accessToken: string; refreshToken?: string }>>(
          `${API_BASE_URL}/api/auth/refresh`,
          { refreshToken }
        );

        if (res.data.code === 1000 && res.data.data) {
          const newAccessToken = res.data.data.accessToken;
          localStorage.setItem('accessToken', newAccessToken);
          if (res.data.data.refreshToken) {
            localStorage.setItem('refreshToken', res.data.data.refreshToken);
          }
          apiClient.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          processQueue(null, newAccessToken);
          return apiClient(originalRequest);
        } else {
          throw new Error('Refresh token invalid');
        }
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('auth:logout'));
        return Promise.reject(new Error('Phiên làm việc hết hạn, vui lòng đăng nhập lại'));
      } finally {
        isRefreshing = false;
      }
    }

    // Extract detailed error message from Spring Boot response envelope or validation errors
    const responseData = error.response?.data as Record<string, unknown> | undefined;
    const serverMessage =
      (responseData?.message as string) ||
      (responseData?.error as string) ||
      (typeof responseData?.data === 'string' ? responseData.data : undefined);

    const message = serverMessage || error.message || 'Lỗi kết nối máy chủ';
    return Promise.reject(new Error(message));
  }
);
