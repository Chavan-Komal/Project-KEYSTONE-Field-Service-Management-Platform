import { useEffect, useRef, useState, FormEvent } from 'react';
import { listWorkOrders } from '../api/workOrders';
import { useAuth } from '../context/AuthContext';
import { respond, SUGGESTIONS } from '../chatbot/responder';
import type { WorkOrder } from '../types';

interface Message {
  from: 'bot' | 'me';
  text: string;
}

/** Customer-only support chatbot. Rule-based (no external API — free to run,
 * works offline) but answers using the caller's real work orders, fetched
 * through the same server-scoped endpoint "My Requests" uses, so it never
 * sees another customer's data. */
export function SupportChatbot() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [draft, setDraft] = useState('');
  const [typing, setTyping] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const firstName = user?.name?.split(' ')[0] ?? 'there';

  useEffect(() => {
    if (!open || messages.length > 0) return;
    setMessages([{ from: 'bot', text: `Hi ${firstName}! I'm the KEYSTONE support assistant. Ask me about a request, or pick something below.` }]);
    listWorkOrders({ size: 100 }).then((page) => setOrders(page.content)).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, typing]);

  function send(text: string) {
    const trimmed = text.trim();
    if (!trimmed) return;
    setMessages((m) => [...m, { from: 'me', text: trimmed }]);
    setDraft('');
    setTyping(true);
    // Small delay so the reply doesn't just pop in — it's a rule engine, not
    // a network call, so there's nothing to actually wait on.
    setTimeout(() => {
      const reply = respond(trimmed, { workOrders: orders, firstName });
      setMessages((m) => [...m, { from: 'bot', text: reply }]);
      setTyping(false);
    }, 350);
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    send(draft);
  }

  if (user?.role !== 'CUSTOMER') return null;

  return (
    <>
      {open && (
        <div
          role="dialog"
          aria-label="KEYSTONE support chat"
          style={{
            position: 'fixed',
            right: 24,
            bottom: 88,
            width: 340,
            maxWidth: 'calc(100vw - 48px)',
            height: 460,
            maxHeight: 'calc(100vh - 140px)',
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 16,
            boxShadow: '0 24px 60px -20px rgba(20,20,40,0.35)',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            zIndex: 1000
          }}
        >
          <div
            style={{
              padding: '14px 16px',
              background: 'var(--navy)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <div>
              <div style={{ fontSize: 13.5, fontWeight: 700 }}>KEYSTONE Support</div>
              <div style={{ fontSize: 11, color: '#9a9dc4' }}>Usually replies instantly</div>
            </div>
            <button
              onClick={() => setOpen(false)}
              aria-label="Close chat"
              style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer', padding: 4, fontSize: 16, lineHeight: 1 }}
            >
              ×
            </button>
          </div>

          <div ref={listRef} style={{ flex: 1, overflowY: 'auto', padding: '14px 12px', display: 'flex', flexDirection: 'column', gap: 8 }}>
            {messages.map((m, i) => (
              <div
                key={i}
                style={{
                  alignSelf: m.from === 'me' ? 'flex-end' : 'flex-start',
                  maxWidth: '85%',
                  background: m.from === 'me' ? 'var(--accent)' : 'var(--bg)',
                  color: m.from === 'me' ? '#fff' : 'var(--ink)',
                  padding: '8px 11px',
                  borderRadius: 12,
                  borderBottomRightRadius: m.from === 'me' ? 3 : 12,
                  borderBottomLeftRadius: m.from === 'bot' ? 3 : 12,
                  fontSize: 12.5,
                  lineHeight: 1.5,
                  whiteSpace: 'pre-wrap'
                }}
              >
                {m.text}
              </div>
            ))}
            {typing && (
              <div style={{ alignSelf: 'flex-start', color: 'var(--ink-soft)', fontSize: 11.5, padding: '2px 4px' }}>typing…</div>
            )}
          </div>

          {messages.length <= 1 && (
            <div style={{ padding: '0 12px 10px', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  onClick={() => send(s)}
                  style={{
                    fontSize: 11.5,
                    padding: '6px 10px',
                    borderRadius: 100,
                    border: '1px solid var(--border)',
                    background: 'var(--surface)',
                    color: 'var(--accent)',
                    cursor: 'pointer'
                  }}
                >
                  {s}
                </button>
              ))}
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 6, padding: 10, borderTop: '1px solid var(--border)' }}>
            <input
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="Ask about a request…"
              aria-label="Message"
              style={{
                flex: 1,
                padding: '8px 10px',
                border: '1px solid var(--border)',
                borderRadius: 8,
                fontSize: 12.5
              }}
            />
            <button
              type="submit"
              aria-label="Send"
              disabled={!draft.trim()}
              className="btn btn-primary btn-sm"
              style={{ padding: '0 14px' }}
            >
              Send
            </button>
          </form>
        </div>
      )}

      <button
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? 'Close support chat' : 'Open support chat'}
        style={{
          position: 'fixed',
          right: 24,
          bottom: 24,
          width: 52,
          height: 52,
          borderRadius: '50%',
          border: 'none',
          background: 'var(--accent)',
          color: '#fff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 10px 24px -6px rgba(91,79,224,0.65)',
          cursor: 'pointer',
          zIndex: 1000,
          fontSize: open ? 22 : 20,
          fontWeight: 600
        }}
      >
        {open ? '×' : '💬'}
      </button>
    </>
  );
}
