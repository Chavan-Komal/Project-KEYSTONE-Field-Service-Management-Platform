import type { WorkOrderStatus } from '../types';

export function StatusBadge({ status }: { status: WorkOrderStatus }) {
  const label = status.replace('_', ' ');
  return <span className={`status-badge status-${status}`}>{label}</span>;
}
