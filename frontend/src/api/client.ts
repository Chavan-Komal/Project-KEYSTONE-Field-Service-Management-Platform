import axios from 'axios';

/**
 * Central axios instance.
 * - Base URL points at the Spring Boot API (proxied via vite.config.ts in dev).
 * - Attaches the JWT (if present) to every request.
 * - On 401 (expired/invalid token) it clears the session and bounces to /login.
 */
export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('keystone_token');
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('keystone_token');
      localStorage.removeItem('keystone_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
