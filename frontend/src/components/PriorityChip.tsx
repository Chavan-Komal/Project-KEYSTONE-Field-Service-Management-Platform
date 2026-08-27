import type { Priority } from '../types';

export function PriorityChip({ priority }: { priority: Priority }) {
  return <span className={`chip chip-priority-${priority}`}>{priority}</span>;
}
