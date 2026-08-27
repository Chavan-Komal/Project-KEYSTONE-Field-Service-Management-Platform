import { api } from './client';
import type { WorkOrder, WorkOrderStatus, Page, DashboardSummary } from '../types';

export interface WorkOrderFilters {
  status?: WorkOrderStatus;
  q?: string;
  page?: number;
  size?: number;
}

// GET /api/work-orders — role-scoped + paginated on the server.
export async function listWorkOrders(filters: WorkOrderFilters = {}): Promise<Page<WorkOrder>> {
  const { data } = await api.get<Page<WorkOrder>>('/work-orders', { params: filters });
  return data;
}

// GET /api/work-orders/{id} — includes status history, parts, time logs.
export async function getWorkOrder(id: string): Promise<WorkOrder> {
  const { data } = await api.get<WorkOrder>(`/work-orders/${id}`);
  return data;
}

export interface CreateWorkOrderPayload {
  title: string;
  description?: string;
  priority: string;
  customerId: string;
  siteId: string;
}

// POST /api/work-orders
export async function createWorkOrder(payload: CreateWorkOrderPayload): Promise<WorkOrder> {
  const { data } = await api.post<WorkOrder>('/work-orders', payload);
  return data;
}

// POST /api/work-orders/{id}/assign
export async function assignWorkOrder(id: string, technicianId: string): Promise<WorkOrder> {
  const { data } = await api.post<WorkOrder>(`/work-orders/${id}/assign`, { technicianId });
  return data;
}

// POST /api/work-orders/{id}/status — server rejects illegal transitions with 409.
export async function transitionStatus(
  id: string,
  toStatus: WorkOrderStatus,
  note?: string
): Promise<WorkOrder> {
  const { data } = await api.post<WorkOrder>(`/work-orders/${id}/status`, { toStatus, note });
  return data;
}

// POST /api/work-orders/{id}/parts — transactional stock decrement on the server.
export async function logPartUsage(id: string, partId: string, qtyUsed: number): Promise<WorkOrder> {
  const { data } = await api.post<WorkOrder>(`/work-orders/${id}/parts`, { partId, qtyUsed });
  return data;
}

// POST /api/work-orders/{id}/time
export async function logTime(id: string, minutes: number, note?: string): Promise<WorkOrder> {
  const { data } = await api.post<WorkOrder>(`/work-orders/${id}/time`, { minutes, note });
  return data;
}

// GET /api/reports/summary — dashboard metrics for managers.
export async function getDashboardSummary(): Promise<DashboardSummary> {
  const { data } = await api.get<DashboardSummary>('/reports/summary');
  return data;
}
