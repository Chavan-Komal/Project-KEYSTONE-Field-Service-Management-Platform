import { api } from './client';
import type { User } from '../types';

export interface LoginResponse {
  token: string;
  user: User;
}

// POST /api/auth/login  — Section 10 of the brief.
export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/auth/login', { email, password });
  return data;
}

export interface RegisterPayload {
  companyName: string;
  name: string;
  email: string;
  password: string;
}

// POST /api/auth/register — self-service sign-up, always lands in the CUSTOMER role.
export async function register(payload: RegisterPayload): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/auth/register', payload);
  return data;
}

export interface ForgotPasswordResponse {
  message: string;
  // Present only in dev/demo (backend keystone.auth.expose-reset-token=true) —
  // in production the link travels by email instead.
  resetUrl: string | null;
}

// POST /api/auth/forgot-password — always 200 with the same message,
// whether or not the email matched an account.
export async function requestPasswordReset(email: string): Promise<ForgotPasswordResponse> {
  const { data } = await api.post<ForgotPasswordResponse>('/auth/forgot-password', { email });
  return data;
}

// POST /api/auth/reset-password — consumes the token from the emailed link.
export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await api.post('/auth/reset-password', { token, newPassword });
}
