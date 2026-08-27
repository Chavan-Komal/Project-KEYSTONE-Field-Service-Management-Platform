import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createWorkOrder } from '../api/workOrders';
import { useAuth } from '../context/AuthContext';

export function NewWorkOrder() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [customerId, setCustomerId] = useState('');
  const [siteId, setSiteId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const isCustomer = user?.role === 'CUSTOMER';

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const wo = await createWorkOrder({ title, description, priority, customerId, siteId });
      navigate(`/work-orders/${wo.id}`);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Could not create the work order. Check the required fields.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>{isCustomer ? 'Raise a Request' : 'New Work Order'}</h1>
          <p>Every request enters the same governed lifecycle, starting at NEW.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="card" style={{ padding: 24, maxWidth: 520 }}>
        {error && <div className="error-banner">{error}</div>}

        <div className="field">
          <label htmlFor="title">Title</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required />
        </div>

        <div className="field">
          <label htmlFor="description">Description</label>
          <input
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="What needs doing, and any relevant context"
          />
        </div>

        {!isCustomer && (
          <>
            <div className="field">
              <label htmlFor="customerId">Customer ID</label>
              <input id="customerId" value={customerId} onChange={(e) => setCustomerId(e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="siteId">Site ID</label>
              <input id="siteId" value={siteId} onChange={(e) => setSiteId(e.target.value)} required />
            </div>
          </>
        )}

        {isCustomer && (
          <div className="field">
            <label htmlFor="siteId">Which site?</label>
            <input
              id="siteId"
              value={siteId}
              onChange={(e) => setSiteId(e.target.value)}
              placeholder="Select your site"
              required
            />
          </div>
        )}

        <div className="field">
          <label htmlFor="priority">Priority</label>
          <select
            id="priority"
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
            style={{ width: '100%', padding: '9px 11px', border: '1px solid var(--border)', borderRadius: 8 }}
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>

        <button className="btn btn-primary" type="submit" disabled={submitting} style={{ marginTop: 8 }}>
          {submitting ? 'Submitting…' : 'Submit'}
        </button>
      </form>
    </>
  );
}
