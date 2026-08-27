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
