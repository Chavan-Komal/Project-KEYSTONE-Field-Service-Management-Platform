import { api } from './client';
import type { WorkOrder, WorkOrderStatus, Page, DashboardSummary, Technician } from '../types';

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

// GET /api/users/technicians — the full technician roster (dispatcher/manager only).
export async function listTechnicians(): Promise<Technician[]> {
  const { data } = await api.get<Technician[]>('/users/technicians');
  return data;
}

// GET /api/work-orders/{id}/nearest-technicians — technicians sorted by
// distance from their base to this work order's site (dispatcher/manager only).
export async function nearestTechnicians(workOrderId: string): Promise<Technician[]> {
  const { data } = await api.get<Technician[]>(`/work-orders/${workOrderId}/nearest-technicians`);
  return data;
}

// POST /api/work-orders/{id}/attachments — multipart image upload (≤5MB, images only).
export async function uploadAttachment(workOrderId: string, file: File): Promise<WorkOrder> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<WorkOrder>(`/work-orders/${workOrderId}/attachments`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return data;
}

// GET /api/work-orders/{id}/attachments/{attachmentId} — streams the image bytes.
// Fetched as a blob (the JWT can't ride on a plain <img src>), caller wraps it
// in URL.createObjectURL for display.
export async function fetchAttachmentBlob(workOrderId: string, attachmentId: string): Promise<string> {
  const { data } = await api.get(`/work-orders/${workOrderId}/attachments/${attachmentId}`, {
    responseType: 'blob'
  });
  return URL.createObjectURL(data as Blob);
}
