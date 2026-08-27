import { useEffect, useState, useCallback } from 'react';
import { listWorkOrders } from '../api/workOrders';
import type { WorkOrder } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { useNavigate } from 'react-router-dom';

// F9 — Customer portal: the server scopes results to this customer's own
// organisation only. The UI never needs (or is given) a way to see anyone
// else's work orders — that boundary is enforced in the repository layer.
export function CustomerPortal() {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const page = await listWorkOrders({ size: 100 });
      setOrders(page.content);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <>
      <div className="page-header">
        <div>
          <h1>My Requests</h1>
          <p>Track every request you've raised, from submission to close-out.</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/new-request')}>
          + Raise a request
        </button>
      </div>

      {loading ? (
        <div className="empty-state">Loading your requests…</div>
      ) : orders.length === 0 ? (
        <div className="empty-state">You haven't raised any requests yet.</div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Code</th>
                <th>Title</th>
                <th>Site</th>
                <th>Status</th>
                <th>Raised</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((wo) => (
                <tr key={wo.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/work-orders/${wo.id}`)}>
                  <td className="mono">{wo.code}</td>
                  <td>{wo.title}</td>
                  <td>{wo.siteName}</td>
                  <td>
                    <StatusBadge status={wo.status} />
                  </td>
                  <td>{new Date(wo.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
