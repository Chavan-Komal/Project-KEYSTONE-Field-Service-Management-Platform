import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { requestPasswordReset } from '../api/auth';

// Step 1 of the self-service reset flow. The backend returns the same generic
// message whether or not the email exists; in dev/demo it also returns the
// reset link (keystone.auth.expose-reset-token), which we surface here.
export function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [resetUrl, setResetUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await requestPasswordReset(email);
      setMessage(res.message);
      setResetUrl(res.resetUrl);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  let resetPath: string | null = null;
  if (resetUrl) {
    try {
      const u = new URL(resetUrl);
      resetPath = u.pathname + u.search;
    } catch {
      resetPath = resetUrl.startsWith('/') ? resetUrl : null;
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
          Enter your account email and we'll send you a link to choose a new password.
        </p>

        {error && <div className="error-banner">{error}</div>}

        {message ? (
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
              {message}
            </div>

            {resetPath && (
              <p style={{ fontSize: 12.5, color: 'var(--ink-soft)', marginBottom: 14 }}>
                Demo mode — no mail server configured. Use this link directly:{' '}
                <Link to={resetPath}>reset your password</Link>
              </p>
            )}

            <p style={{ fontSize: 12.5, color: 'var(--ink-soft)', textAlign: 'center' }}>
              <Link to="/login">Back to sign in</Link>
            </p>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@meridianfm.com"
                required
                autoFocus
              />
            </div>
            <button className="btn btn-primary" type="submit" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Sending…' : 'Send reset link'}
            </button>

            <p style={{ fontSize: 12.5, color: 'var(--ink-soft)', marginTop: 16, textAlign: 'center' }}>
              Remembered it? <Link to="/login">Sign in</Link>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}
