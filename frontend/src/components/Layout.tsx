import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

interface NavItem {
  to: string;
  label: string;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { to: '/board', label: 'Work Order Board', roles: ['DISPATCHER', 'MANAGER'] },
  { to: '/my-jobs', label: 'My Jobs', roles: ['TECHNICIAN'] },
  { to: '/dashboard', label: 'Dashboard', roles: ['MANAGER'] },
  { to: '/portal', label: 'My Requests', roles: ['CUSTOMER'] },
  { to: '/new-request', label: 'Raise a Request', roles: ['CUSTOMER', 'DISPATCHER', 'MANAGER'] }
];

const ROLE_LABEL: Record<Role, string> = {
  DISPATCHER: 'Dispatcher',
  TECHNICIAN: 'Technician',
  MANAGER: 'Manager',
  CUSTOMER: 'Customer'
};

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  if (!user) return <>{children}</>;

  const visibleItems = NAV_ITEMS.filter((item) => item.roles.includes(user.role));

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span className="dot" />
          <span>KEYSTONE</span>
        </div>

        <nav>
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <span className="role-pill">{ROLE_LABEL[user.role]}</span>
          <div style={{ fontSize: 13, fontWeight: 600, color: '#fff' }}>{user.name}</div>
          <div style={{ fontSize: 11.5, color: '#7d80a0' }}>{user.email}</div>
          <button className="logout-btn" onClick={logout}>
            Log out
          </button>
        </div>
      </aside>

      <main className="main">{children}</main>
    </div>
  );
}
