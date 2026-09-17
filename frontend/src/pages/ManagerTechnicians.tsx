import { useEffect, useState, FormEvent } from 'react';
import { listTechnicians } from '../api/workOrders';
import { createTechnician, updateTechnicianBase } from '../api/technicians';
import type { Technician } from '../types';

// F-manager: technician accounts are manager-provisioned (Section 03), never
// self-registered. This is the roster screen — add a technician, and set or
// change each one's home-base address (used by the nearest-technician
// dispatch suggestion and the tracking map).
export function ManagerTechnicians() {
  const [technicians, setTechnicians] = useState<Technician[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [baseAddress, setBaseAddress] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editAddress, setEditAddress] = useState('');
  const [savingBase, setSavingBase] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setTechnicians(await listTechnicians());
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createTechnician({ name, email, password, baseAddress: baseAddress || undefined });
      setName('');
      setEmail('');
      setPassword('');
      setBaseAddress('');
      await load();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Could not create that technician account.');
    } finally {
      setSubmitting(false);
    }
  }

  function startEditingBase(t: Technician) {
    setEditingId(t.id);
    setEditAddress(t.baseAddress ?? '');
  }

  async function saveBase(id: string) {
    setSavingBase(true);
    try {
      await updateTechnicianBase(id, editAddress);
      setEditingId(null);
      await load();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Could not update that base address.');
    } finally {
      setSavingBase(false);
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Technicians</h1>
          <p>Manage the technician roster and each one's home-base location for dispatch and the tracking map.</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.3fr', gap: 20, alignItems: 'start' }}>
        <form onSubmit={handleCreate} className="card" style={{ padding: 20 }}>
          <h3 style={{ fontSize: 14, marginBottom: 14 }}>Add a technician</h3>
          {error && <div className="error-banner">{error}</div>}

          <div className="field">
            <label htmlFor="tech-name">Name</label>
            <input id="tech-name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="tech-email">Email</label>
            <input id="tech-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="tech-password">Temporary password</label>
            <input
              id="tech-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="tech-base">Home-base address (optional)</label>
            <input
              id="tech-base"
              value={baseAddress}
              onChange={(e) => setBaseAddress(e.target.value)}
              placeholder="e.g. Shivaji Nagar, Pune, MH"
            />
          </div>

          <button className="btn btn-primary" type="submit" disabled={submitting} style={{ width: '100%' }}>
            {submitting ? 'Adding…' : 'Add technician'}
          </button>
        </form>

        <div className="card" style={{ padding: 20 }}>
          <h3 style={{ fontSize: 14, marginBottom: 14 }}>Roster ({technicians.length})</h3>
          {loading ? (
            <div className="empty-state">Loading…</div>
          ) : technicians.length === 0 ? (
            <div className="empty-state">No technicians yet — add the first one.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Base address</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {technicians.map((t) => (
                  <tr key={t.id}>
                    <td>{t.name}</td>
                    <td>{t.email}</td>
                    <td>
                      {editingId === t.id ? (
                        <div style={{ display: 'flex', gap: 6 }}>
                          <input
                            value={editAddress}
                            onChange={(e) => setEditAddress(e.target.value)}
                            style={{ flex: 1, padding: '5px 8px', border: '1px solid var(--border)', borderRadius: 6, fontSize: 12 }}
                            autoFocus
                          />
                        </div>
                      ) : t.baseAddress ? (
                        <>
                          {t.baseAddress}
                          {t.baseLatitude == null && (
                            <span style={{ color: 'var(--ink-soft)' }}> (couldn't locate on map)</span>
                          )}
                        </>
                      ) : (
                        <span style={{ color: 'var(--ink-soft)' }}>Not set</span>
                      )}
                    </td>
                    <td>
                      {editingId === t.id ? (
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={savingBase}
                            onClick={() => saveBase(t.id)}
                          >
                            Save
                          </button>
                          <button className="btn btn-ghost btn-sm" onClick={() => setEditingId(null)} disabled={savingBase}>
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <button className="btn btn-ghost btn-sm" onClick={() => startEditingBase(t)}>
                          {t.baseAddress ? 'Edit' : 'Set base'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </>
  );
}
