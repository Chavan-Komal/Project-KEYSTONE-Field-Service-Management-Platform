import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

interface Props {
  children: ReactNode;
  allow?: Role[]; // if omitted, any authenticated role can view
}

/**
 * Client-side gate only — this is a UX convenience, NOT the security boundary.
 * Every endpoint must re-check the role server-side (Section 03 of the brief).
 */
export function ProtectedRoute({ children, allow }: Props) {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (allow && !allow.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
