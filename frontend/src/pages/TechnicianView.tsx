import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { listWorkOrders } from '../api/workOrders';
import type { WorkOrder } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { PriorityChip } from '../components/PriorityChip';
import { SlaDot } from '../components/SlaDot';

// F5 — Technician field view: only their assigned jobs, responsive for a phone.
export function TechnicianView() {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      // The server scopes this to "assigned to me" based on the JWT — the
      // client does not (and should not) need to pass its own user id.
      const page = await listWorkOrders({ size: 100 });
      setOrders(page.content);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openJobs = orders.filter((o) => !['CLOSED', 'CANCELLED'].includes(o.status));

  return (
    <>
      <div className="page-header">
        <div>
          <h1>My Jobs</h1>
          <p>Everything assigned to you, ordered by SLA urgency.</p>
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Loading your jobs…</div>
      ) : openJobs.length === 0 ? (
        <div className="empty-state">No jobs assigned right now. Nice and clear.</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, maxWidth: 560 }}>
          {openJobs.map((wo) => (
            <div
              key={wo.id}
              className="card"
              style={{ padding: '14px 16px', cursor: 'pointer' }}
              onClick={() => navigate(`/work-orders/${wo.id}`)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <span className="mono" style={{ fontSize: 12, color: 'var(--ink-soft)' }}>
                  {wo.code}
                </span>
                <StatusBadge status={wo.status} />
              </div>
              <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>{wo.title}</div>
              <div style={{ fontSize: 12.5, color: 'var(--ink-soft)', marginBottom: 8 }}>
                {wo.siteName} · {wo.customerName}
              </div>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <PriorityChip priority={wo.priority} />
                <SlaDot state={wo.slaState} />
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
