import { api } from './client';
import type { Customer, Site, Page } from '../types';

// GET /api/customers — staff-only, backs the "which customer?" picker when
// raising a work order on someone's behalf.
export async function listCustomers(): Promise<Customer[]> {
  const { data } = await api.get<Page<Customer>>('/customers', { params: { size: 200 } });
  return data.content;
}

// GET /api/customers/{id}/sites — staff-only.
export async function listSitesForCustomer(customerId: string): Promise<Site[]> {
  const { data } = await api.get<Site[]>(`/customers/${customerId}/sites`);
  return data;
}

// GET /api/customers/me/sites — the logged-in customer's own sites, scoped
// server-side from the JWT (no ID to supply).
export async function listMySites(): Promise<Site[]> {
  const { data } = await api.get<Site[]>('/customers/me/sites');
  return data;
}
