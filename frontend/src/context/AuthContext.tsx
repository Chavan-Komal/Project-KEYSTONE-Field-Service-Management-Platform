import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import type { User } from '../types';
import { login as loginRequest, register as registerRequest, RegisterPayload } from '../api/auth';

interface AuthContextValue {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readStoredUser(): User | null {
  const raw = localStorage.getItem('keystone_user');
  if (!raw) return null;
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(readStoredUser());

  const login = useCallback(async (email: string, password: string) => {
    const { token, user: loggedInUser } = await loginRequest(email, password);
    localStorage.setItem('keystone_token', token);
    localStorage.setItem('keystone_user', JSON.stringify(loggedInUser));
    setUser(loggedInUser);
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const { token, user: newUser } = await registerRequest(payload);
    localStorage.setItem('keystone_token', token);
    localStorage.setItem('keystone_user', JSON.stringify(newUser));
    setUser(newUser);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('keystone_token');
    localStorage.removeItem('keystone_user');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
