import { useState, useEffect, FormEvent, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { createWorkOrder, uploadAttachment } from '../api/workOrders';
import { listCustomers, listSitesForCustomer, listMySites } from '../api/customers';
import { useAuth } from '../context/AuthContext';
import type { Customer, Site } from '../types';

const MAX_FILE_BYTES = 5 * 1024 * 1024;

export function NewWorkOrder() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [customerId, setCustomerId] = useState('');
  const [siteId, setSiteId] = useState('');
  const [photos, setPhotos] = useState<File[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [loadingSites, setLoadingSites] = useState(false);

  const isCustomer = user?.role === 'CUSTOMER';

  // Staff: load the customer list once, up front.
  useEffect(() => {
    if (isCustomer) return;
    listCustomers().then(setCustomers);
  }, [isCustomer]);

  // Staff: reload the site picker whenever the chosen customer changes.
  useEffect(() => {
    if (isCustomer || !customerId) {
      if (!isCustomer) setSites([]);
      return;
    }
    setLoadingSites(true);
    setSiteId('');
    listSitesForCustomer(customerId)
      .then(setSites)
      .finally(() => setLoadingSites(false));
  }, [isCustomer, customerId]);

  // Customer: load their own sites once — no customer picker needed.
  useEffect(() => {
    if (!isCustomer) return;
    setLoadingSites(true);
    listMySites()
      .then(setSites)
      .finally(() => setLoadingSites(false));
  }, [isCustomer]);

  function addFiles(list: FileList | null) {
    if (!list) return;
    const incoming = Array.from(list);
    const rejected = incoming.find(
      (f) => !f.type.startsWith('image/') || f.size > MAX_FILE_BYTES
    );
    if (rejected) {
      setError(
        !rejected.type.startsWith('image/')
          ? `"${rejected.name}" isn't an image.`
          : `"${rejected.name}" is over 5 MB.`
      );
      return;
    }
    setError(null);
    setPhotos((prev) => [...prev, ...incoming]);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  function removePhoto(index: number) {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const wo = await createWorkOrder({ title, description, priority, customerId, siteId });
      for (const file of photos) {
        await uploadAttachment(wo.id, file);
      }
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
              <label htmlFor="customerId">Customer</label>
              <select
                id="customerId"
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                required
                style={{ width: '100%', padding: '9px 11px', border: '1px solid var(--border)', borderRadius: 8 }}
              >
                <option value="">Select a customer…</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="siteId">Site</label>
              <select
                id="siteId"
                value={siteId}
                onChange={(e) => setSiteId(e.target.value)}
                required
                disabled={!customerId || loadingSites}
                style={{ width: '100%', padding: '9px 11px', border: '1px solid var(--border)', borderRadius: 8 }}
              >
                <option value="">
                  {!customerId ? 'Pick a customer first…' : loadingSites ? 'Loading sites…' : 'Select a site…'}
                </option>
                {sites.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} — {s.address}
                  </option>
                ))}
              </select>
            </div>
          </>
        )}

        {isCustomer && (
          <div className="field">
            <label htmlFor="siteId">Which site?</label>
            <select
              id="siteId"
              value={siteId}
              onChange={(e) => setSiteId(e.target.value)}
              required
              disabled={loadingSites}
              style={{ width: '100%', padding: '9px 11px', border: '1px solid var(--border)', borderRadius: 8 }}
            >
              <option value="">{loadingSites ? 'Loading your sites…' : 'Select your site…'}</option>
              {sites.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} — {s.address}
                </option>
              ))}
            </select>
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

        <div className="field">
          <label htmlFor="photos">Photos of the issue (optional)</label>
          <input
            id="photos"
            ref={fileInputRef}
            type="file"
            accept="image/*"
            multiple
            onChange={(e) => addFiles(e.target.files)}
            style={{ fontSize: 12.5 }}
          />
          <div style={{ fontSize: 11, color: 'var(--ink-soft)', marginTop: 4 }}>
            JPEG / PNG, up to 5 MB each.
          </div>

          {photos.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 10 }}>
              {photos.map((file, i) => (
                <div key={i} style={{ position: 'relative' }}>
                  <img
                    src={URL.createObjectURL(file)}
                    alt={file.name}
                    style={{
                      width: 72,
                      height: 72,
                      objectFit: 'cover',
                      borderRadius: 8,
                      border: '1px solid var(--border)'
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => removePhoto(i)}
                    aria-label={`Remove ${file.name}`}
                    style={{
                      position: 'absolute',
                      top: -6,
                      right: -6,
                      width: 18,
                      height: 18,
                      borderRadius: '50%',
                      border: 'none',
                      background: 'var(--red)',
                      color: '#fff',
                      fontSize: 11,
                      lineHeight: '18px',
                      cursor: 'pointer',
                      padding: 0
                    }}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <button className="btn btn-primary" type="submit" disabled={submitting} style={{ marginTop: 8 }}>
          {submitting ? 'Submitting…' : 'Submit'}
        </button>
      </form>
    </>
  );
}
