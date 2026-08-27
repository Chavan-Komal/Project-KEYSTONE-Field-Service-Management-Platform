import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { listWorkOrders } from '../api/workOrders';
import type { WorkOrder, WorkOrderStatus } from '../types';
import { PriorityChip } from '../components/PriorityChip';
import { SlaDot } from '../components/SlaDot';

const COLUMNS: { status: WorkOrderStatus; label: string }[] = [
  { status: 'NEW', label: 'New' },
  { status: 'ASSIGNED', label: 'Assigned' },
  { status: 'IN_PROGRESS', label: 'In Progress' },
  { status: 'ON_HOLD', label: 'On Hold' },
  { status: 'COMPLETED', label: 'Completed' },
  { status: 'CLOSED', label: 'Closed' }
];

export function DispatcherBoard() {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      // Board view pulls a large page and groups client-side by status.
      // (List/filter/search views elsewhere use server-side pagination — Section 9, F4.)
      const page = await listWorkOrders({ q: search || undefined, size: 200 });
      setOrders(page.content);
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const grouped = COLUMNS.map((col) => ({
    ...col,
    items: orders.filter((o) => o.status === col.status)
  }));

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Work Order Board</h1>
          <p>Every open job, grouped by status — the dispatcher's single view of the operation.</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/new-request')}>
          + New Work Order
        </button>
      </div>

      <div style={{ marginBottom: 16 }}>
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by code, title, or site…"
          style={{
            width: 320,
            padding: '8px 12px',
            borderRadius: 8,
            border: '1px solid var(--border)',
            fontSize: 13
          }}
        />
      </div>

      {loading ? (
        <div className="empty-state">Loading the board…</div>
      ) : (
        <div className="board">
          {grouped.map((col) => (
            <div className="board-col" key={col.status}>
              <div className="board-col-head">
                <span className="label">{col.label}</span>
                <span className="count">{col.items.length}</span>
              </div>

              {col.items.length === 0 && (
                <div style={{ fontSize: 12, color: 'var(--ink-soft)', padding: '4px 2px' }}>No jobs</div>
              )}

              {col.items.map((wo) => (
                <div className="wo-card" key={wo.id} onClick={() => navigate(`/work-orders/${wo.id}`)}>
                  <div className="code">{wo.code}</div>
                  <div className="title">{wo.title}</div>
                  <div style={{ fontSize: 11.5, color: 'var(--ink-soft)', marginBottom: 6 }}>
                    {wo.siteName} · {wo.customerName}
                  </div>
                  <div className="meta">
                    <PriorityChip priority={wo.priority} />
                    <SlaDot state={wo.slaState} />
                  </div>
                  {wo.assignedToName && (
                    <div style={{ fontSize: 11, color: 'var(--ink-soft)', marginTop: 6 }}>
                      → {wo.assignedToName}
                    </div>
                  )}
                </div>
              ))}
            </div>
          ))}
        </div>
      )}
    </>
  );
}
