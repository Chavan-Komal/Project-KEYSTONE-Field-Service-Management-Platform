import { useEffect, useState, useRef } from 'react';
import type { Attachment } from '../types';
import { fetchAttachmentBlob, uploadAttachment } from '../api/workOrders';

const MAX_FILE_BYTES = 5 * 1024 * 1024;

interface Props {
  workOrderId: string;
  attachments: Attachment[];
  canUpload: boolean;
  onUploaded: () => void;
}

/**
 * Photos attached to a work order — typically the customer's pictures of the
 * issue. Each image is fetched as an authenticated blob (a JWT can't ride on a
 * plain <img src>) and shown as a thumbnail; clicking one opens it full-size.
 */
export function AttachmentGallery({ workOrderId, attachments, canUpload, onUploaded }: Props) {
  const [urls, setUrls] = useState<Record<string, string>>({});
  const [expanded, setExpanded] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let cancelled = false;
    const created: string[] = [];

    (async () => {
      for (const att of attachments) {
        if (urls[att.id]) continue;
        try {
          const url = await fetchAttachmentBlob(workOrderId, att.id);
          created.push(url);
          if (!cancelled) setUrls((prev) => ({ ...prev, [att.id]: url }));
        } catch {
          /* skip an image that failed to load */
        }
      }
    })();

    return () => {
      cancelled = true;
      created.forEach((u) => URL.revokeObjectURL(u));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attachments, workOrderId]);

  async function handleUpload(list: FileList | null) {
    if (!list || list.length === 0) return;
    const files = Array.from(list);
    const bad = files.find((f) => !f.type.startsWith('image/') || f.size > MAX_FILE_BYTES);
    if (bad) {
      setError(!bad.type.startsWith('image/') ? `"${bad.name}" isn't an image.` : `"${bad.name}" is over 5 MB.`);
      return;
    }
    setError(null);
    setUploading(true);
    try {
      for (const file of files) {
        await uploadAttachment(workOrderId, file);
      }
      onUploaded();
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Upload failed. Try again.');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }

  return (
    <div className="card" style={{ padding: 20, marginTop: 20 }}>
      <h3 style={{ fontSize: 14, marginBottom: 12 }}>
        Photos {attachments.length > 0 && <span style={{ color: 'var(--ink-soft)' }}>({attachments.length})</span>}
      </h3>

      {error && <div className="error-banner">{error}</div>}

      {attachments.length === 0 ? (
        <div style={{ fontSize: 12.5, color: 'var(--ink-soft)' }}>No photos attached.</div>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
          {attachments.map((att) => (
            <button
              key={att.id}
              type="button"
              onClick={() => urls[att.id] && setExpanded(urls[att.id])}
              title={att.filename}
              style={{
                width: 96,
                height: 96,
                padding: 0,
                border: '1px solid var(--border)',
                borderRadius: 8,
                overflow: 'hidden',
                background: 'var(--bg)',
                cursor: urls[att.id] ? 'zoom-in' : 'default'
              }}
            >
              {urls[att.id] ? (
                <img
                  src={urls[att.id]}
                  alt={att.filename}
                  style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                />
              ) : (
                <span style={{ fontSize: 10, color: 'var(--ink-soft)' }}>loading…</span>
              )}
            </button>
          ))}
        </div>
      )}

      {canUpload && (
        <div style={{ marginTop: 14, borderTop: '1px solid var(--border)', paddingTop: 14 }}>
          <label htmlFor="add-photo" style={{ fontSize: 12, fontWeight: 600, color: 'var(--ink-soft)' }}>
            Add a photo
          </label>
          <input
            id="add-photo"
            ref={fileInputRef}
            type="file"
            accept="image/*"
            multiple
            disabled={uploading}
            onChange={(e) => handleUpload(e.target.files)}
            style={{ display: 'block', fontSize: 12.5, marginTop: 6 }}
          />
          {uploading && <div style={{ fontSize: 12, color: 'var(--ink-soft)', marginTop: 6 }}>Uploading…</div>}
        </div>
      )}

      {expanded && (
        <div
          onClick={() => setExpanded(null)}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(5,6,20,0.8)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: 24,
            cursor: 'zoom-out'
          }}
        >
          <img
            src={expanded}
            alt="Attachment full size"
            style={{ maxWidth: '100%', maxHeight: '100%', borderRadius: 10, boxShadow: '0 20px 60px rgba(0,0,0,0.5)' }}
          />
        </div>
      )}
    </div>
  );
}
