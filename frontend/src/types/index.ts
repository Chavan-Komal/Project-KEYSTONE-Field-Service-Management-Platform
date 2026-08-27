export type Role = 'DISPATCHER' | 'TECHNICIAN' | 'MANAGER' | 'CUSTOMER';

export type WorkOrderStatus =
  | 'NEW'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CLOSED'
  | 'CANCELLED';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type SlaState = 'OK' | 'AT_RISK' | 'BREACHED';

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export interface Customer {
  id: string;
  name: string;
  contactEmail: string;
}

export interface Site {
  id: string;
  customerId: string;
  name: string;
  address: string;
}

export interface WorkOrderStatusHistoryEntry {
  id: string;
  fromStatus: WorkOrderStatus | null;
  toStatus: WorkOrderStatus;
  changedBy: string;
  changedAt: string;
  note?: string;
}

export interface PartUsage {
  id: string;
  partId: string;
  partName: string;
  qtyUsed: number;
  unitCost: number;
}

export interface TimeLogEntry {
  id: string;
  technicianId: string;
  technicianName: string;
  minutes: number;
  note?: string;
}

export interface WorkOrder {
  id: string;
  code: string;
  title: string;
  description?: string;
  priority: Priority;
  status: WorkOrderStatus;
  customerId: string;
  customerName: string;
  siteId: string;
  siteName: string;
  assignedTo?: string;
  assignedToName?: string;
  slaDueAt: string;
  slaState: SlaState;
  createdAt: string;
  history?: WorkOrderStatusHistoryEntry[];
  parts?: PartUsage[];
  timeLogs?: TimeLogEntry[];
}

export interface DashboardSummary {
  countsByStatus: Record<WorkOrderStatus, number>;
  overdueCount: number;
  slaComplianceRate: number;
  byTechnician: { technicianName: string; openCount: number }[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}
