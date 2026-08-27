import type { SlaState } from '../types';

const CLASS_MAP: Record<SlaState, string> = {
  OK: 'sla-ok',
  AT_RISK: 'sla-risk',
  BREACHED: 'sla-breach'
};

const LABEL_MAP: Record<SlaState, string> = {
  OK: 'On track',
  AT_RISK: 'At risk',
  BREACHED: 'Breached'
};

export function SlaDot({ state }: { state: SlaState }) {
  return (
    <span title={LABEL_MAP[state]} style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
      <span className={`sla-dot ${CLASS_MAP[state]}`} />
      <span style={{ fontSize: 11, color: 'var(--ink-soft)' }}>{LABEL_MAP[state]}</span>
    </span>
  );
}
