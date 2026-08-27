import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Sends the user to the landing page appropriate for their role.
export function Home() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;

  switch (user.role) {
    case 'DISPATCHER':
      return <Navigate to="/board" replace />;
    case 'TECHNICIAN':
      return <Navigate to="/my-jobs" replace />;
    case 'MANAGER':
      return <Navigate to="/dashboard" replace />;
    case 'CUSTOMER':
      return <Navigate to="/portal" replace />;
    default:
      return <Navigate to="/login" replace />;
  }
}
