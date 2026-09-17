import { api } from './client';
import type { MapPin } from '../types';

// GET /api/map/overview — manager-only, org-wide technician + open work order pins.
export async function getManagerMapOverview(): Promise<MapPin[]> {
  const { data } = await api.get<MapPin[]>('/map/overview');
  return data;
}

// GET /api/map/my-requests — customer-only, their own sites + assigned technicians.
export async function getCustomerMapOverview(): Promise<MapPin[]> {
  const { data } = await api.get<MapPin[]>('/map/my-requests');
  return data;
}
