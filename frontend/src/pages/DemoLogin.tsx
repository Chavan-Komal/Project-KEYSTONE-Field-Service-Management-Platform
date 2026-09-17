import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const DEMO_ACCOUNTS: { role: string; name: string; email: string; blurb: string }[] = [
  { role: 'Dispatcher', name: 'Divya Kulkarni', email: 'dispatcher@keystone.dev', blurb: 'Assigns and tracks every open job on the Work Order Board.' },
  { role: 'Technician', name: 'Rahul Shinde', email: 'technician@keystone.dev', blurb: 'Sees only the jobs assigned to them, sorted by SLA urgency.' },
  { role: 'Manager', name: 'Priya Deshpande', email: 'manager@keystone.dev', blurb: 'Org-wide dashboard: SLA compliance, load by technician, throughput.' },
  { role: 'Customer', name: 'Meridian Front Desk', email: 'customer@keystone.dev', blurb: 'Raises and tracks their own requests, with a support chatbot.' }
];

export function DemoLogin() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loadingEmail, setLoadingEmail] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loginAs(email: string) {
    setError(null);
    setLoadingEmail(email);
    try {
      await login(email, 'Password123!');
      const redirectTo = (location.state as { from?: string })?.from ?? '/';
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError('Could not sign in to that demo account.');
    } finally {
      setLoadingEmail(null);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-card" style={{ maxWidth: 460 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 3, background: 'var(--accent)' }} />
          <h1 style={{ fontSize: 18 }}>KEYSTONE</h1>
        </div>
        <p style={{ fontSize: 12.5, color: 'var(--ink-soft)', marginBottom: 4 }}>
          Pick a role to explore — each one lands on its own dashboard.
        </p>
        <p style={{ fontSize: 12.5, marginBottom: 20 }}>
          <Link to="/login">← Back to sign in</Link>
        </p>

        {error && <div className="error-banner">{error}</div>}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {DEMO_ACCOUNTS.map((acc) => (
            <button
              key={acc.email}
              type="button"
              disabled={loadingEmail !== null}
              onClick={() => loginAs(acc.email)}
              style={{
                textAlign: 'left',
                padding: '14px 16px',
                borderRadius: 10,
                border: '1px solid var(--border)',
                background: 'var(--bg)',
                cursor: loadingEmail ? 'default' : 'pointer'
              }}
            >
              <div style={{ fontSize: 14, fontWeight: 700, color: 'var(--accent)' }}>
                {loadingEmail === acc.email ? 'Signing in…' : acc.role}
              </div>
              <div style={{ fontSize: 12, fontWeight: 600, marginTop: 2 }}>{acc.name}</div>
              <div style={{ fontSize: 11.5, color: 'var(--ink-soft)', marginTop: 4 }}>{acc.blurb}</div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
