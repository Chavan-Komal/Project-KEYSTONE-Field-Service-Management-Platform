import { useState, FormEvent, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api/auth';

// Step 2 of the self-service reset flow — reached from the emailed link
// (/reset-password?token=…). Sets a new password, then bounces to sign in.
export function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get('token') ?? '';

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (!token) setError('This reset link is missing its token. Request a new link.');
  }, [token]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirm) {
      setError('The two passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await resetPassword(token, password);
      setDone(true);
      setTimeout(() => navigate('/login', { replace: true }), 2500);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Could not reset your password. The link may have expired.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: 3, background: 'var(--accent)' }} />
          <h1 style={{ fontSize: 18 }}>KEYSTONE</h1>
        </div>
        <p style={{ fontSize: 12.5, color: 'var(--ink-soft)', marginBottom: 20 }}>
          Choose a new password for your account.
        </p>

        {error && <div className="error-banner">{error}</div>}

        {done ? (
          <>
            <div
              style={{
                background: 'var(--green-soft)',
                color: 'var(--green)',
                fontSize: 12.5,
                padding: '10px 12px',
                borderRadius: 8,
                marginBottom: 14
              }}
            >
              Password updated. Taking you to sign in…
            </div>
            <p style={{ fontSize: 12.5, color: 'var(--ink-soft)', textAlign: 'center' }}>
              <Link to="/login">Sign in now</Link>
            </p>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="password">New password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 8 characters"
                minLength={8}
                required
                autoFocus
                disabled={!token}
              />
            </div>
            <div className="field">
              <label htmlFor="confirm">Confirm new password</label>
              <input
                id="confirm"
                type="password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                placeholder="Re-enter it"
                minLength={8}
                required
                disabled={!token}
              />
            </div>
            <button
              className="btn btn-primary"
              type="submit"
              style={{ width: '100%' }}
              disabled={loading || !token}
            >
              {loading ? 'Updating…' : 'Update password'}
            </button>

            <p style={{ fontSize: 12.5, color: 'var(--ink-soft)', marginTop: 16, textAlign: 'center' }}>
              <Link to="/login">Back to sign in</Link>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}
