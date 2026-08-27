import { useEffect, useState, useCallback, FormEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getWorkOrder,
  transitionStatus,
  logTime,
  logPartUsage,
  assignWorkOrder
} from '../api/workOrders';
import type { WorkOrder, WorkOrderStatus } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { PriorityChip } from '../components/PriorityChip';
import { SlaDot } from '../components/SlaDot';
import { useAuth } from '../context/AuthContext';

// Mirrors the guarded transition diagram in Section 07 of the brief.
// The server is the real source of truth (409 on illegal jumps) — this
// just avoids offering buttons that would obviously be rejected.
const ALLOWED_NEXT: Record<WorkOrderStatus, WorkOrderStatus[]> = {
  NEW: ['ASSIGNED', 'CANCELLED'],
  ASSIGNED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['ON_HOLD', 'COMPLETED'],
  ON_HOLD: ['IN_PROGRESS'],
  COMPLETED: ['CLOSED', 'IN_PROGRESS'], // reopen
  CLOSED: [],
  CANCELLED: []
};

export function WorkOrderDetail() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [minutes, setMinutes] = useState('');
  const [note, setNote] = useState('');

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await getWorkOrder(id);
      setWo(data);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleTransition(status: WorkOrderStatus) {
    if (!wo) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await transitionStatus(wo.id, status, note || undefined);
      setWo(updated);
      setNote('');
    } catch (err: any) {
      // 409 = illegal transition rejected by the service-layer state machine.
      setError(err?.response?.data?.message ?? 'That transition was rejected by the server.');
    } finally {
      setBusy(false);
    }
  }

  async function handleLogTime(e: FormEvent) {
    e.preventDefault();
    if (!wo || !minutes) return;
    setBusy(true);
    try {
      const updated = await logTime(wo.id, Number(minutes), note || undefined);
      setWo(updated);
      setMinutes('');
      setNote('');
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <div className="empty-state">Loading work order…</div>;
  if (!wo) return <div className="empty-state">Work order not found.</div>;

  const canTransition = user?.role === 'DISPATCHER' || user?.role === 'MANAGER' || user?.role === 'TECHNICIAN';

  return (
    <>
      <div className="page-header">
        <div>
          <button className="btn btn-ghost btn-sm" onClick={() => navigate(-1)} style={{ marginBottom: 10 }}>
            ← Back
          </button>
          <h1 className="mono">{wo.code}</h1>
          <p>{wo.title}</p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <StatusBadge status={wo.status} />
          <PriorityChip priority={wo.priority} />
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 20 }}>
        <div className="card" style={{ padding: 20 }}>
          <h3 style={{ fontSize: 14, marginBottom: 12 }}>Details</h3>
          <p style={{ fontSize: 13, color: 'var(--ink-soft)', marginBottom: 16 }}>
            {wo.description || 'No description provided.'}
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, fontSize: 13 }}>
            <div>
              <div style={{ color: 'var(--ink-soft)', fontSize: 11 }}>Customer</div>
              <div>{wo.customerName}</div>
            </div>
            <div>
              <div style={{ color: 'var(--ink-soft)', fontSize: 11 }}>Site</div>
              <div>{wo.siteName}</div>
            </div>
            <div>
              <div style={{ color: 'var(--ink-soft)', fontSize: 11 }}>Assigned to</div>
              <div>{wo.assignedToName || 'Unassigned'}</div>
            </div>
            <div>
              <div style={{ color: 'var(--ink-soft)', fontSize: 11 }}>SLA due</div>
              <div>
                {new Date(wo.slaDueAt).toLocaleString()} <SlaDot state={wo.slaState} />
              </div>
            </div>
          </div>

          {canTransition && ALLOWED_NEXT[wo.status].length > 0 && (
            <div style={{ marginTop: 20, borderTop: '1px solid var(--border)', paddingTop: 16 }}>
              <h3 style={{ fontSize: 13, marginBottom: 8 }}>Move status</h3>
              <input
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Optional note for the status history…"
                style={{ width: '100%', padding: '7px 10px', border: '1px solid var(--border)', borderRadius: 7, fontSize: 12.5, marginBottom: 10 }}
              />
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {ALLOWED_NEXT[wo.status].map((next) => (
                  <button
                    key={next}
                    className="btn btn-ghost btn-sm"
                    disabled={busy}
                    onClick={() => handleTransition(next)}
                  >
                    → {next.replace('_', ' ')}
                  </button>
                ))}
              </div>
            </div>
          )}

          {user?.role === 'TECHNICIAN' && (
            <div style={{ marginTop: 20, borderTop: '1px solid var(--border)', paddingTop: 16 }}>
              <h3 style={{ fontSize: 13, marginBottom: 8 }}>Log time</h3>
              <form onSubmit={handleLogTime} style={{ display: 'flex', gap: 8 }}>
                <input
                  type="number"
                  min={1}
                  value={minutes}
                  onChange={(e) => setMinutes(e.target.value)}
                  placeholder="Minutes"
                  style={{ width: 100, padding: '7px 10px', border: '1px solid var(--border)', borderRadius: 7 }}
                />
                <button className="btn btn-primary btn-sm" type="submit" disabled={busy}>
                  Log
                </button>
              </form>
            </div>
          )}
        </div>

        <div className="card" style={{ padding: 20 }}>
          <h3 style={{ fontSize: 14, marginBottom: 12 }}>Status history</h3>
          {(wo.history ?? []).length === 0 && (
            <div style={{ fontSize: 12.5, color: 'var(--ink-soft)' }}>No history yet.</div>
          )}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {(wo.history ?? []).map((h) => (
              <div key={h.id} style={{ fontSize: 12.5, borderLeft: '2px solid var(--accent)', paddingLeft: 10 }}>
                <div>
                  <strong>{h.fromStatus ?? 'created'}</strong> → <strong>{h.toStatus}</strong>
                </div>
                <div style={{ color: 'var(--ink-soft)' }}>
                  {h.changedBy} · {new Date(h.changedAt).toLocaleString()}
                </div>
                {h.note && <div style={{ color: 'var(--ink-soft)', fontStyle: 'italic' }}>"{h.note}"</div>}
              </div>
            ))}
          </div>

          <h3 style={{ fontSize: 14, margin: '18px 0 10px' }}>Parts & time</h3>
          <div style={{ fontSize: 12.5, color: 'var(--ink-soft)' }}>
            {(wo.parts ?? []).length} part{(wo.parts ?? []).length === 1 ? '' : 's'} logged ·{' '}
            {(wo.timeLogs ?? []).reduce((sum, t) => sum + t.minutes, 0)} minutes total
          </div>
        </div>
      </div>
    </>
  );
}
