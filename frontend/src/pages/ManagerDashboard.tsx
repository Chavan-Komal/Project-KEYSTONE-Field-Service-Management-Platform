import { useEffect, useState } from 'react';
import { getDashboardSummary } from '../api/workOrders';
import type { DashboardSummary, WorkOrderStatus } from '../types';

const STATUS_ORDER: WorkOrderStatus[] = [
  'NEW',
  'ASSIGNED',
  'IN_PROGRESS',
  'ON_HOLD',
  'COMPLETED',
  'CLOSED',
  'CANCELLED'
];

// F8 — Dashboard & reporting: counts by status, overdue work, SLA compliance,
// broken down by technician. Handles empty and loading states per the
// acceptance criteria.
export function ManagerDashboard() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [errored, setErrored] = useState(false);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const data = await getDashboardSummary();
        setSummary(data);
      } catch {
        setErrored(true);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <div className="empty-state">Loading dashboard…</div>;
  if (errored || !summary) return <div className="empty-state">Could not load dashboard metrics.</div>;

  const totalOpen = STATUS_ORDER.filter((s) => s !== 'CLOSED' && s !== 'CANCELLED').reduce(
    (sum, s) => sum + (summary.countsByStatus[s] ?? 0),
    0
  );

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>Live operational snapshot across every open work order.</p>
        </div>
      </div>

      <div className="stat-grid">
        <div className="card stat-card">
          <div className="value">{totalOpen}</div>
          <div className="label">Open work orders</div>
        </div>
        <div className="card stat-card">
          <div className="value" style={{ color: summary.overdueCount > 0 ? 'var(--red)' : 'var(--ink)' }}>
            {summary.overdueCount}
          </div>
          <div className="label">Overdue / breaching SLA</div>
        </div>
        <div className="card stat-card">
          <div className="value">{Math.round(summary.slaComplianceRate * 100)}%</div>
          <div className="label">SLA compliance (30 days)</div>
        </div>
        <div className="card stat-card">
          <div className="value">{summary.countsByStatus.CLOSED ?? 0}</div>
          <div className="label">Closed this period</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 20 }}>
        <div className="card" style={{ padding: 20 }}>
          <h3 style={{ fontSize: 14, marginBottom: 14 }}>Work orders by status</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {STATUS_ORDER.map((status) => {
              const count = summary.countsByStatus[status] ?? 0;
              const max = Math.max(...Object.values(summary.countsByStatus), 1);
              return (
                <div key={status} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ width: 90, fontSize: 11.5, color: 'var(--ink-soft)' }}>
                    {status.replace('_', ' ')}
                  </div>
                  <div style={{ flex: 1, background: '#eef0f6', borderRadius: 6, height: 10 }}>
                    <div
                      style={{
                        width: `${(count / max) * 100}%`,
                        background: 'var(--accent)',
                        height: '100%',
                        borderRadius: 6
                      }}
                    />
                  </div>
                  <div style={{ width: 24, textAlign: 'right', fontSize: 12, fontWeight: 600 }}>{count}</div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="card" style={{ padding: 20 }}>
          <h3 style={{ fontSize: 14, marginBottom: 14 }}>Load by technician</h3>
          {summary.byTechnician.length === 0 ? (
            <div style={{ fontSize: 12.5, color: 'var(--ink-soft)' }}>No assignments yet.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Technician</th>
                  <th>Open jobs</th>
                </tr>
              </thead>
              <tbody>
                {summary.byTechnician.map((t) => (
                  <tr key={t.technicianName}>
                    <td>{t.technicianName}</td>
                    <td>{t.openCount}</td>
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
